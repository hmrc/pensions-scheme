/*
 * Copyright 2024 HM Revenue & Customs
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
import com.google.inject.{ImplementedBy, Inject}
import config.AppConfig
import models.etmpToUserAnswers.psaSchemeDetails.PsaSchemeDetailsTransformer
import models.etmpToUserAnswers.pspSchemeDetails.PspSchemeDetailsTransformer
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http._
import utils.HttpResponseHelper

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[SchemeDetailsConnectorImpl])
trait SchemeDetailsConnector {

  def getSchemeDetails(
                        userIdNumber: String,
                        schemeIdType: String,
                        schemeIdNumber: String
                      )(
                        implicit headerCarrier: HeaderCarrier,
                        ec: ExecutionContext,
                        request: RequestHeader
                      ): Future[Either[HttpException, JsObject]]

  def getPspSchemeDetails(
                           pspId: String,
                           pstr: String
                         )(
                           implicit headerCarrier: HeaderCarrier,
                           ec: ExecutionContext,
                           request: RequestHeader
                         ): Future[Either[HttpException, JsObject]]
}

class SchemeDetailsConnectorImpl @Inject()(
                                            httpClientV2: HttpClientV2,
                                            config: AppConfig,
                                            auditService: AuditService,
                                            schemeSubscriptionDetailsTransformer: PsaSchemeDetailsTransformer,
                                            pspSchemeDetailsTransformer: PspSchemeDetailsTransformer,
                                            schemeAuditService: SchemeAuditService,
                                            headerUtils: HeaderUtils
                                          )
  extends SchemeDetailsConnector with HttpResponseHelper {

  private val logger = Logger(classOf[SchemeConnectorImpl])

  override def getSchemeDetails(
                                 psaId: String,
                                 schemeIdType: String,
                                 idNumber: String
                               )(
                                 implicit headerCarrier: HeaderCarrier,
                                 ec: ExecutionContext,
                                 request: RequestHeader
                               ): Future[Either[HttpException, JsObject]] = {

    val url = url"${config.schemeDetailsUrl.format(schemeIdType, idNumber)}"

    logger.debug(s"Calling get scheme details API on IF with url $url and hc $headerCarrier")

    httpClientV2.get(url)
      .setHeader(headerUtils.integrationFrameworkHeader: _*)
      .execute[HttpResponse].map { response =>
        handleSchemeDetailsResponse(response, url.toString)
      } andThen
      schemeAuditService.sendSchemeDetailsEvent(psaId)(auditService.sendEvent)

  }

  override def getPspSchemeDetails(
                                    pspId: String,
                                    pstr: String
                                  )(
                                    implicit headerCarrier: HeaderCarrier,
                                    ec: ExecutionContext,
                                    request: RequestHeader
                                  ): Future[Either[HttpException, JsObject]] = {

    val url = url"${config.pspSchemeDetailsUrl.format(pspId, pstr)}"
    logger.debug(s"Calling psp get scheme details API with url $url and hc $headerCarrier")

    httpClientV2.get(url)
      .setHeader(headerUtils.integrationFrameworkHeader: _*)
      .execute[HttpResponse].map { response =>
      handlePspSchemeDetailsResponse(response, url.toString)
    } andThen
      schemeAuditService.sendPspSchemeDetailsEvent(pspId)(auditService.sendExtendedEvent)
  }

  private def handleSchemeDetailsResponse(response: HttpResponse, url: String): Either[HttpException, JsObject] = {
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
        Left(handleErrorResponse("GET", url, response))
    }
  }

  private def handlePspSchemeDetailsResponse(response: HttpResponse, url: String): Either[HttpException, JsObject] = {
    logger.debug(s"Get-Psp-Scheme-details-response - ${response.json}")
    response.status match {
      case OK =>
        response.json.transform(pspSchemeDetailsTransformer.transformToUserAnswers) match {
          case JsSuccess(value, _) =>
            logger.debug(s"Get-Psp-Scheme-details-UserAnswersJson - $value")
            Right(value)
          case JsError(e) => throw JsResultException(e)
        }
      case NOT_FOUND if response.body.contains("PSP_RELATIONSHIP_NOT_FOUND") => Left(new HttpException( response.body, NOT_FOUND))
      case _ =>
        Left(handleErrorResponse("GET", url, response))
    }
  }

}
