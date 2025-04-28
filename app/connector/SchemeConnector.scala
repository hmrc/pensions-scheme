/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.http.Status.*
import play.api.libs.json.*
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{BadRequestException, *}
import utils.{HttpResponseHelper, InvalidPayloadHandler}

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[SchemeConnectorImpl])
trait SchemeConnector {
  def registerScheme(
                      psaId: String,
                      registerData: JsValue
                    )(
                      implicit headerCarrier: HeaderCarrier,
                      ec: ExecutionContext,
                      request: RequestHeader
                    ): Future[Either[HttpException, JsValue]]

  def listOfSchemes(
                     idType: String,
                     idValue: String
                   )(
                     implicit headerCarrier: HeaderCarrier,
                     ec: ExecutionContext,
                     request: RequestHeader
                   ): Future[Either[HttpException, JsValue]]

  def updateSchemeDetails(pstr: String, data: JsValue)
                         (implicit headerCarrier: HeaderCarrier,
                          ec: ExecutionContext,
                          request: RequestHeader): Future[Either[HttpException, JsValue]]

}

class SchemeConnectorImpl @Inject()(
                                     httpClientV2: HttpClientV2,
                                     config: AppConfig,
                                     invalidPayloadHandler: InvalidPayloadHandler,
                                     headerUtils: HeaderUtils,
                                     schemeAuditService: SchemeAuditService,
                                     auditService: AuditService
                                   ) extends SchemeConnector with HttpResponseHelper {

  private val logger = Logger(classOf[SchemeConnectorImpl])

  override def registerScheme(
                               psaId: String,
                               registerData: JsValue
                             )(
                               implicit headerCarrier: HeaderCarrier,
                               ec: ExecutionContext,
                               request: RequestHeader
                             ): Future[Either[HttpException, JsValue]] = {

    val url = url"${config.schemeRegistrationIFUrl.format(psaId)}"
    val schemaPath = "/resources/schemas/schemeSubscriptionIF.json"

    logger.debug(s"[Register-Scheme-Outgoing-Payload] - ${registerData.toString()}")

    httpClientV2.post(url)
      .withBody(registerData)
      .setHeader(headerUtils.integrationFrameworkHeader*)
      .execute[HttpResponse].map { response =>
        response.status match {
          case OK =>
            Right(response.json)
          case BAD_REQUEST if response.body.contains("INVALID_PAYLOAD") =>
            invalidPayloadHandler.logFailures(schemaPath, registerData, url.toString)
            throw new BadRequestException(
              badRequestMessage("Register scheme", url.toString, response.body)
            )
          case _ => Left(handleErrorResponse("Register scheme", url.toString, response))
        }
      }
  }

  override def listOfSchemes(
                              idType: String,
                              idValue: String
                            )(
                              implicit headerCarrier: HeaderCarrier,
                              ec: ExecutionContext,
                              request: RequestHeader
                            ): Future[Either[HttpException, JsValue]] = {
    val listOfSchemesUrl = url"${config.listOfSchemesUrl.format(idType, idValue)}"

    logger.debug(s"Calling list of schemes API on IF with url $listOfSchemesUrl")

    httpClientV2.get(listOfSchemesUrl)
      .setHeader(headerUtils.integrationFrameworkHeader*)
      .execute[HttpResponse].map { response =>
        response.status match {
          case OK =>
            logger.debug(s"Call to list of schemes API on IF was successful with response ${response.json}")
            Right(response.json)
          case _ =>
            logger.warn(s"Response ${response.status} to list of schemes API for idType $idType and idValue $idValue")
            Left(handleErrorResponse("Scheme details", listOfSchemesUrl.toString, response))
        }
      } andThen schemeAuditService.sendListOfSchemesEvent(idType, idValue)(auditService.sendEvent)

  }

  override def updateSchemeDetails(
                                    pstr: String,
                                    data: JsValue
                                  )(
                                    implicit headerCarrier: HeaderCarrier,
                                    ec: ExecutionContext,
                                    request: RequestHeader
                                  ): Future[Either[HttpException, JsValue]] = {

    val url = url"${config.updateSchemeUrl.format(pstr)}"
    val schemaPath = "/resources/schemas/schemeVariationIFSchema.json"

    logger.debug(s"[Update-Scheme-Outgoing-Payload] - ${data.toString()}")

    httpClientV2.post(url)
      .withBody(data)
      .setHeader(headerUtils.integrationFrameworkHeader*)
      .execute[HttpResponse].map { response =>
        response.status match {
          case OK =>
            Right(response.json)
          case BAD_REQUEST if response.body.contains("INVALID_PAYLOAD") =>
            invalidPayloadHandler.logFailures(schemaPath, data, url.toString)
            throw new BadRequestException(
              badRequestMessage("Register scheme", url.toString, response.body)
            )
          case _ => Left(handleErrorResponse("POST", url.toString, response))
        }
      }
  }
}
