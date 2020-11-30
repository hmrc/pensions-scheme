/*
 * Copyright 2020 HM Revenue & Customs
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

import java.util.UUID.randomUUID

import audit._
import com.google.inject.{ImplementedBy, Inject}
import config.AppConfig
import models.FeatureToggle.Enabled
import models.FeatureToggleName.IntegrationFrameworkGetSchemeDetails
import models.etmpToUserAnswers.SchemeSubscriptionDetailsTransformer
import play.Logger
import play.api.http.Status._
import play.api.libs.json.{JsError, JsObject, JsResultException, JsSuccess, JsValue, Writes}
import play.api.mvc.RequestHeader
import service.FeatureToggleService
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.InvalidPayloadHandler

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[SchemeConnectorImpl])
trait SchemeConnector {
  def registerScheme(psaId: String, registerData: JsValue)(implicit
                                                           headerCarrier: HeaderCarrier,
                                                           ec: ExecutionContext,
                                                           request: RequestHeader): Future[HttpResponse]

  def listOfSchemes(psaId: String)(implicit
                                   headerCarrier: HeaderCarrier,
                                   ec: ExecutionContext,
                                   request: RequestHeader): Future[HttpResponse]
  
  def listOfSchemes(idType: String, idValue: String)(implicit
                                                     headerCarrier: HeaderCarrier,
                                                     ec: ExecutionContext,
                                                     request: RequestHeader): Future[HttpResponse]

  def getCorrelationId(requestId: Option[String]): String

  def getSchemeDetails(psaId: String, schemeIdType: String, idNumber: String)(implicit headerCarrier: HeaderCarrier,
                                                                              ec: ExecutionContext,
                                                                              request: RequestHeader): Future[Either[HttpResponse, JsValue]]

  def updateSchemeDetails(pstr: String, data: JsValue)(implicit
                                                       headerCarrier: HeaderCarrier,
                                                       ec: ExecutionContext,
                                                       request: RequestHeader): Future[HttpResponse]

}

class SchemeConnectorImpl @Inject()(
                                     http: HttpClient,
                                     config: AppConfig,
                                     auditService: AuditService,
                                     invalidPayloadHandler: InvalidPayloadHandler,
                                     schemeSubscriptionDetailsTransformer: SchemeSubscriptionDetailsTransformer,
                                     schemeSubscriptionDetailsTransformerDES: models.etmpToUserAnswers.DES.SchemeSubscriptionDetailsTransformer,
                                     schemeAuditService: SchemeAuditService,
                                     headerUtils: HeaderUtils,
                                     featureToggleService: FeatureToggleService
                                   ) extends SchemeConnector with HttpErrorFunctions {

  case class SchemeFailedMapToUserAnswersException() extends Exception

  override def getCorrelationId(requestId: Option[String]): String = {
    requestId.getOrElse {
      Logger.error("No Request Id found while calling register with Id")
      randomUUID.toString
    }.replaceAll("(govuk-tax-|-)", "").slice(0, 32)
  }

  override def registerScheme(psaId: String, registerData: JsValue)(implicit
                                                                    headerCarrier: HeaderCarrier,
                                                                    ec: ExecutionContext,
                                                                    request: RequestHeader): Future[HttpResponse] = {

    val url = config.schemeRegistrationUrl.format(psaId)
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeader(implicitly[HeaderCarrier](headerCarrier)))

    Logger.debug(s"[PSA-Scheme-Outgoing-Payload] - ${registerData.toString()}")

    http.POST[JsValue, HttpResponse](url, registerData)(
      implicitly[Writes[JsValue]],
      implicitly[HttpReads[HttpResponse]],
      implicitly[HeaderCarrier](hc),
      implicitly[ExecutionContext]
    ) map { response =>
        response.status match {
          case BAD_REQUEST if response.body.contains("INVALID_PAYLOAD") =>
            invalidPayloadHandler.logFailures(
              "/resources/schemas/schemeSubscription.json", registerData, url
            )
          case _ => Unit
        }
        response
      }
  }

  override def getSchemeDetails(psaId: String, schemeIdType: String,
    idNumber: String)(implicit
    headerCarrier: HeaderCarrier,
    ec: ExecutionContext,
    request: RequestHeader): Future[Either[HttpResponse, JsValue]] = {
    featureToggleService.get(IntegrationFrameworkGetSchemeDetails).flatMap {
      case Enabled(IntegrationFrameworkGetSchemeDetails) =>
        val (url, hc) = (config.schemeDetailsIFUrl.format(schemeIdType, idNumber),
          HeaderCarrier(extraHeaders = headerUtils.integrationFrameworkHeader(implicitly[HeaderCarrier](headerCarrier))))
        Logger.debug(s"Calling get scheme details API on IF with url $url and hc $hc")
        http.GET[HttpResponse](url)(implicitly, hc, implicitly).map(response =>
          handleSchemeDetailsResponse(response)) andThen
          schemeAuditService.sendSchemeDetailsEvent(psaId)(auditService.sendEvent)
      case _ =>
        val (url, hc) = (config.schemeDetailsUrl.format(schemeIdType, idNumber),
          HeaderCarrier(extraHeaders = desHeader(implicitly[HeaderCarrier](headerCarrier))))
        Logger.debug(s"Calling get scheme details API on DES with url $url and hc $hc")
        http.GET[HttpResponse](url)(implicitly, hc, implicitly).map(response =>
          handleSchemeDetailsResponseDES(response)) andThen
          schemeAuditService.sendSchemeDetailsEvent(psaId)(auditService.sendEvent)
    }
  }

  override def listOfSchemes(psaId: String)(implicit
                                            headerCarrier: HeaderCarrier,
                                            ec: ExecutionContext,
                                            request: RequestHeader): Future[HttpResponse] = {
    val listOfSchemesUrl = config.listOfSchemesUrl.format(psaId)
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeader(implicitly[HeaderCarrier](headerCarrier)))

    http.GET[HttpResponse](listOfSchemesUrl)(implicitly[HttpReads[HttpResponse]], implicitly[HeaderCarrier](hc),
      implicitly[ExecutionContext])
  }

  override def listOfSchemes(idType: String, idValue: String)(implicit
                                                              headerCarrier: HeaderCarrier,
                                                              ec: ExecutionContext,
                                                              request: RequestHeader): Future[HttpResponse] = {
    val listOfSchemesUrl = config.listOfSchemesIFUrl.format(idType, idValue)

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders =
      headerUtils.integrationFrameworkHeader(implicitly[HeaderCarrier](headerCarrier)))
    Logger.debug(s"Calling list of schemes API on IF with url $listOfSchemesUrl")

    http.GET[HttpResponse](listOfSchemesUrl)(implicitly[HttpReads[HttpResponse]], implicitly[HeaderCarrier](hc),
      implicitly[ExecutionContext])
  }

  override def updateSchemeDetails(pstr: String, data: JsValue)(implicit
                                                                headerCarrier: HeaderCarrier,
                                                                ec: ExecutionContext,
                                                                request: RequestHeader): Future[HttpResponse] = {

    val url = config.updateSchemeUrl.format(pstr)
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeader(implicitly[HeaderCarrier](headerCarrier)))

    Logger.debug(s"[Update-Scheme-Outgoing-Payload] - ${data.toString()}")

    http.POST[JsValue, HttpResponse](url, data)(
      implicitly[Writes[JsValue]],
      implicitly[HttpReads[HttpResponse]],
      implicitly[HeaderCarrier](hc),
      implicitly[ExecutionContext]
    ) map { response =>
        response.status match {
          case BAD_REQUEST if response.body.contains("INVALID_PAYLOAD") =>
            invalidPayloadHandler.logFailures(
              "/resources/schemas/schemeVariationSchema.json", data, url
            )
          case _ => Unit
        }
        response
      }
  }

  private def desHeader(implicit hc: HeaderCarrier): Seq[(String, String)] = {
    val requestId = getCorrelationId(hc.requestId.map(_.value))

    Seq("Environment" -> config.desEnvironment, "Authorization" -> config.authorization,
      "Content-Type" -> "application/json", "CorrelationId" -> requestId)
  }

  private def handleSchemeDetailsResponse(response: HttpResponse)(
    implicit requestHeader: RequestHeader, executionContext: ExecutionContext): Either[HttpResponse, JsObject] = {
    Logger.debug(s"Get-Scheme-details-response from IF API - ${response.json}")
    response.status match {
      case OK =>
          response.json.transform(schemeSubscriptionDetailsTransformer.transformToUserAnswers) match {
            case JsSuccess(value, _) =>
              Logger.debug(s"Get-Scheme-details-UserAnswersJson - $value")
              Right(value)
            case JsError(e) => throw JsResultException(e)
          }
      case _ =>
        Left(response)
    }
  }

  private def handleSchemeDetailsResponseDES(response: HttpResponse)(
    implicit requestHeader: RequestHeader, executionContext: ExecutionContext): Either[HttpResponse, JsObject] = {
    Logger.debug(s"Get-Scheme-details-response from DES API - $response")
    response.status match {
      case OK =>
        val userAnswersJson =
          response.json.transform(
            schemeSubscriptionDetailsTransformerDES.transformToUserAnswers
          ).getOrElse(throw new SchemeFailedMapToUserAnswersException)
        Logger.debug(s"Get-Scheme-details-UserAnswersJson - $userAnswersJson")
        Right(userAnswersJson)
      case _ =>
        Left(response)
    }
  }
}
