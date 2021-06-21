/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connector

import audit._
import com.google.inject.{Inject, ImplementedBy}
import config.AppConfig
import models.etmpToUserAnswers.psaSchemeDetails.PsaSchemeDetailsTransformer
import models.etmpToUserAnswers.pspSchemeDetails.PspSchemeDetailsTransformer
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HttpClient, _}
import utils.InvalidPayloadHandler

import java.util.UUID.randomUUID
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[SchemeConnectorImpl])
trait SchemeConnector {
  def registerScheme(
                      psaId: String,
                      registerData: JsValue
                    )(
                      implicit
                      headerCarrier: HeaderCarrier,
                      ec: ExecutionContext,
                      request: RequestHeader
                    ): Future[HttpResponse]

  def listOfSchemes(
                     idType: String,
                     idValue: String
                   )(
                     implicit
                     headerCarrier: HeaderCarrier,
                     ec: ExecutionContext,
                     request: RequestHeader
                   ): Future[HttpResponse]

  def getCorrelationId(requestId: Option[String]): String

  def getSchemeDetails(
                        userIdNumber: String,
                        schemeIdNumber: String,
                        schemeIdType: String
                      )(
                        implicit
                        headerCarrier: HeaderCarrier,
                        ec: ExecutionContext,
                        request: RequestHeader
                      ): Future[Either[HttpResponse, JsValue]]

  def getPspSchemeDetails(
                           pspId: String,
                           pstr: String
                         )(
                           implicit
                           headerCarrier: HeaderCarrier,
                           ec: ExecutionContext,
                           request: RequestHeader
                         ): Future[Either[HttpResponse, JsValue]]

  def updateSchemeDetails(pstr: String, data: JsValue)
                         (implicit headerCarrier: HeaderCarrier,
                          ec: ExecutionContext,
                          request: RequestHeader): Future[HttpResponse]

}

class SchemeConnectorImpl @Inject()(
                                     http: HttpClient,
                                     config: AppConfig,
                                     auditService: AuditService,
                                     invalidPayloadHandler: InvalidPayloadHandler,
                                     schemeSubscriptionDetailsTransformer: PsaSchemeDetailsTransformer,
                                     pspSchemeDetailsTransformer: PspSchemeDetailsTransformer,
                                     schemeAuditService: SchemeAuditService,
                                     headerUtils: HeaderUtils
                                   )
  extends SchemeConnector
    with HttpErrorFunctions {

  private val logger = Logger(classOf[SchemeConnectorImpl])

  case class SchemeFailedMapToUserAnswersException() extends Exception

  override def getCorrelationId(requestId: Option[String]): String = {
    requestId.getOrElse {
      logger.error("No Request Id found while calling register with Id")
      randomUUID.toString
    }.replaceAll("(govuk-tax-|-)", "").slice(0, 32)
  }

  override def registerScheme(
                               psaId: String,
                               registerData: JsValue
                             )(
                               implicit
                               headerCarrier: HeaderCarrier,
                               ec: ExecutionContext,
                               request: RequestHeader
                             ): Future[HttpResponse] = {

    val (url, hc, schemaPath) =
      (config.schemeRegistrationIFUrl.format(psaId),
        HeaderCarrier(extraHeaders = headerUtils.integrationFrameworkHeader(implicitly[HeaderCarrier](headerCarrier))),
        "/resources/schemas/schemeSubscriptionIF.json")

    logger.debug(s"[PSA-Scheme-Outgoing-Payload] - ${registerData.toString()}")

    http.POST[JsValue, HttpResponse](url, registerData)(
      implicitly[Writes[JsValue]],
      implicitly[HttpReads[HttpResponse]],
      implicitly[HeaderCarrier](hc),
      implicitly[ExecutionContext]
    ) map { response =>
      response.status match {
        case BAD_REQUEST if response.body.contains("INVALID_PAYLOAD") =>
          invalidPayloadHandler.logFailures(schemaPath, registerData, url)
        case _ => Unit
      }
      response
    }
  }

  override def getSchemeDetails(
                                 psaId: String,
                                 schemeIdType: String,
                                 idNumber: String
                               )(
                                 implicit
                                 headerCarrier: HeaderCarrier,
                                 ec: ExecutionContext,
                                 request: RequestHeader
                               ): Future[Either[HttpResponse, JsValue]] = {
      val (url, hc) = (
        config.schemeDetailsUrl.format(schemeIdType, idNumber),
        HeaderCarrier(extraHeaders = headerUtils.integrationFrameworkHeader(implicitly[HeaderCarrier](headerCarrier)))
      )

      logger.debug(s"Calling get scheme details API on IF with url $url and hc $hc")

      http.GET[HttpResponse](url)(implicitly, hc, implicitly).map(response =>
        handleSchemeDetailsResponse(response)
      ) andThen
        schemeAuditService.sendSchemeDetailsEvent(psaId)(auditService.sendEvent)
  }

  override def getPspSchemeDetails(
                                    pspId: String,
                                    pstr: String
                                  )(
                                    implicit
                                    headerCarrier: HeaderCarrier,
                                    ec: ExecutionContext,
                                    request: RequestHeader
                                  ): Future[Either[HttpResponse, JsValue]] = {

    val url = config.pspSchemeDetailsUrl.format(pspId, pstr)
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headerUtils.integrationFrameworkHeader(implicitly[HeaderCarrier](headerCarrier)))
    logger.debug(s"Calling psp get scheme details API with url $url and hc $hc")
    http.GET[HttpResponse](url)(implicitly, hc, implicitly).map(response =>
      handlePspSchemeDetailsResponse(response)) andThen
      schemeAuditService.sendPspSchemeDetailsEvent(pspId)(auditService.sendExtendedEvent)

  }

  override def listOfSchemes(
                              idType: String,
                              idValue: String
                            )(
                              implicit
                              headerCarrier: HeaderCarrier,
                              ec: ExecutionContext,
                              request: RequestHeader
                            ): Future[HttpResponse] = {
    val listOfSchemesUrl = config.listOfSchemesUrl.format(idType, idValue)

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders =
      headerUtils.integrationFrameworkHeader(implicitly[HeaderCarrier](headerCarrier)))
    logger.debug(s"Calling list of schemes API on IF with url $listOfSchemesUrl")

    http.GET[HttpResponse](listOfSchemesUrl)(
      implicitly[HttpReads[HttpResponse]],
      implicitly[HeaderCarrier](hc),
      implicitly[ExecutionContext]
    )
  }

  override def updateSchemeDetails(pstr: String, data: JsValue)(implicit
                                                                headerCarrier: HeaderCarrier,
                                                                ec: ExecutionContext,
                                                                request: RequestHeader): Future[HttpResponse] = {

    val (url, hc, schemaPath) =
      (config.updateSchemeUrl.format(pstr),
        HeaderCarrier(extraHeaders = headerUtils.integrationFrameworkHeader(implicitly[HeaderCarrier](headerCarrier))),
        "/resources/schemas/schemeVariationIFSchema.json")

    logger.debug(s"[Update-Scheme-Outgoing-Payload] - ${data.toString()}")

    http.POST[JsValue, HttpResponse](url, data)(
      implicitly[Writes[JsValue]],
      implicitly[HttpReads[HttpResponse]],
      implicitly[HeaderCarrier](hc),
      implicitly[ExecutionContext]
    ) map { response =>
      response.status match {
        case BAD_REQUEST if response.body.contains("INVALID_PAYLOAD") =>
          invalidPayloadHandler.logFailures(schemaPath, data, url)
        case _ => Unit
      }
      response
    }
  }

  private def handleSchemeDetailsResponse(response: HttpResponse): Either[HttpResponse, JsObject] = {
    logger.debug(s"Get-Scheme-details-response from IF API - ${response.json}")
    response.status match {
      case OK =>
        response.json.transform(schemeSubscriptionDetailsTransformer.transformToUserAnswers) match {
          case JsSuccess(value, _) =>
            logger.debug(s"Get-Scheme-details-UserAnswersJson - $value")
            Right(value)
          case JsError(e) => throw JsResultException(e)
        }
      case _ =>
        Left(response)
    }
  }

  private def handlePspSchemeDetailsResponse(response: HttpResponse): Either[HttpResponse, JsObject] = {
    logger.debug(s"Get-Psp-Scheme-details-response - ${response.json}")
    response.status match {
      case OK =>
        response.json.transform(pspSchemeDetailsTransformer.transformToUserAnswers) match {
          case JsSuccess(value, _) =>
            logger.debug(s"Get-Psp-Scheme-details-UserAnswersJson - $value")
            Right(value)
          case JsError(e) => throw JsResultException(e)
        }
      case _ =>
        Left(response)
    }
  }
}
