/*
 * Copyright 2022 HM Revenue & Customs
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

import audit.{AuditService, SchemeAuditService}
import com.google.inject.{ImplementedBy, Inject}
import config.AppConfig
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{BadRequestException, HttpClient, _}
import utils.{HttpResponseHelper, InvalidPayloadHandler}

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
                    ): Future[Either[HttpException, JsValue]]

  def listOfSchemes(
                     idType: String,
                     idValue: String
                   )(
                     implicit
                     headerCarrier: HeaderCarrier,
                     ec: ExecutionContext,
                     request: RequestHeader
                   ): Future[Either[HttpException, JsValue]]

  def updateSchemeDetails(pstr: String, data: JsValue)
                         (implicit headerCarrier: HeaderCarrier,
                          ec: ExecutionContext,
                          request: RequestHeader): Future[Either[HttpException, JsValue]]

}

class SchemeConnectorImpl @Inject()(
                                     http: HttpClient,
                                     config: AppConfig,
                                     invalidPayloadHandler: InvalidPayloadHandler,
                                     headerUtils: HeaderUtils,
                                     schemeAuditService: SchemeAuditService,
                                     auditService: AuditService
                                   )
  extends SchemeConnector
    with HttpResponseHelper {

  private val logger = Logger(classOf[SchemeConnectorImpl])

  case class SchemeFailedMapToUserAnswersException() extends Exception

  override def registerScheme(
                               psaId: String,
                               registerData: JsValue
                             )(
                               implicit
                               headerCarrier: HeaderCarrier,
                               ec: ExecutionContext,
                               request: RequestHeader
                             ): Future[Either[HttpException, JsValue]] = {

    val (url, hc, schemaPath) =
      (config.schemeRegistrationIFUrl.format(psaId),
        HeaderCarrier(extraHeaders = headerUtils.integrationFrameworkHeader(implicitly[HeaderCarrier](headerCarrier))),
        "/resources/schemas/schemeSubscriptionIF.json")

    logger.debug(s"[Register-Scheme-Outgoing-Payload] - ${registerData.toString()}")

    http.POST[JsValue, HttpResponse](url, registerData)(
      implicitly[Writes[JsValue]],
      implicitly[HttpReads[HttpResponse]],
      implicitly[HeaderCarrier](hc),
      implicitly[ExecutionContext]
    ) map { response =>
      response.status match {
        case OK =>
          Right(response.json)
        case BAD_REQUEST if response.body.contains("INVALID_PAYLOAD") =>
          invalidPayloadHandler.logFailures(schemaPath, registerData, url)
          throw new BadRequestException(
            badRequestMessage("Register scheme", url, response.body)
          )
        case _ => Left(handleErrorResponse("Register scheme", url, response))
      }
    }
  }

  override def listOfSchemes(
                              idType: String,
                              idValue: String
                            )(
                              implicit
                              headerCarrier: HeaderCarrier,
                              ec: ExecutionContext,
                              request: RequestHeader
                            ): Future[Either[HttpException, JsValue]] = {
    val listOfSchemesUrl = config.listOfSchemesUrl.format(idType, idValue)

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders =
      headerUtils.integrationFrameworkHeader(implicitly[HeaderCarrier](headerCarrier)))
    logger.debug(s"Calling list of schemes API on IF with url $listOfSchemesUrl")

    http.GET[HttpResponse](listOfSchemesUrl)(
      implicitly[HttpReads[HttpResponse]],
      implicitly[HeaderCarrier](hc),
      implicitly[ExecutionContext]
    ).map { response =>
      response.status match {
        case OK =>
          logger.debug(s"Call to list of schemes API on IF was successful with response ${response.json}")
          Right(response.json)
        case _ => Left(handleErrorResponse("Scheme details", listOfSchemesUrl, response))
      }
    } andThen schemeAuditService.sendListOfSchemesEvent(idType, idValue)(auditService.sendEvent)
  }

  override def updateSchemeDetails(pstr: String, data: JsValue)(implicit
                                                                headerCarrier: HeaderCarrier,
                                                                ec: ExecutionContext,
                                                                request: RequestHeader): Future[Either[HttpException, JsValue]] = {

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
        case OK => Right(response.json)
        case BAD_REQUEST if response.body.contains("INVALID_PAYLOAD") =>
          invalidPayloadHandler.logFailures(schemaPath, data, url)
          throw new BadRequestException(
            badRequestMessage("Register scheme", url, response.body)
          )
        case _ => Left(handleErrorResponse("POST", url, response))
      }
    }
  }
}
