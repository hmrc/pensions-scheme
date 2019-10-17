/*
 * Copyright 2019 HM Revenue & Customs
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
import models.jsonTransformations.SchemeSubscriptionDetailsTransformer
import play.Logger
import play.api.http.Status._
import play.api.libs.json.{JsValue, Writes}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.InvalidPayloadHandler

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

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

  def getCorrelationId(requestId: Option[String]): String

  def getSchemeDetails(psaId: String, schemeIdType: String, idNumber: String)(implicit headerCarrier: HeaderCarrier,
                                                                              ec: ExecutionContext,
                                                                              request: RequestHeader): Future[Either[HttpException, JsValue]]

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
                                     schemeAuditService: SchemeAuditService
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

    val schemeRegisterUrl = config.schemeRegistrationUrl.format(psaId)
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeader(implicitly[HeaderCarrier](headerCarrier)))

    Logger.debug(s"[PSA-Scheme-Outgoing-Payload] - ${registerData.toString()}")

    http.POST(schemeRegisterUrl, registerData)(implicitly[Writes[JsValue]],
      implicitly[HttpReads[HttpResponse]], implicitly[HeaderCarrier](hc), implicitly[ExecutionContext])
      .andThen {
        case Failure(x: BadRequestException) if x.message.contains("INVALID_PAYLOAD") =>
          invalidPayloadHandler.logFailures("/resources/schemas/schemeSubscription.json", registerData)
      }
  }

  override def getSchemeDetails(psaId: String, schemeIdType: String,
                                idNumber: String)(implicit
                                                  headerCarrier: HeaderCarrier,
                                                  ec: ExecutionContext,
                                                  request: RequestHeader): Future[Either[HttpException, JsValue]] = {

    implicit val rds: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
      override def read(method: String, url: String, response: HttpResponse): HttpResponse = response
    }

    val schemeDetailsUrl = config.schemeDetailsUrl.format(schemeIdType, idNumber)
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeader(implicitly[HeaderCarrier](headerCarrier)))

    http.GET[HttpResponse](schemeDetailsUrl)(implicitly, hc, implicitly)
      .map(response => handleSchemeDetailsResponse(psaId, response, schemeDetailsUrl)) andThen
      schemeAuditService.sendSchemeDetailsEvent(psaId)(auditService.sendEvent)
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

  override def updateSchemeDetails(pstr: String, data: JsValue)(implicit
                                                                headerCarrier: HeaderCarrier,
                                                                ec: ExecutionContext,
                                                                request: RequestHeader): Future[HttpResponse] = {

    val updateSchemeUrl = config.updateSchemeUrl.format(pstr)
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeader(implicitly[HeaderCarrier](headerCarrier)))

    Logger.debug(s"[Update-Scheme-Outgoing-Payload] - ${data.toString()}")

    http.POST(updateSchemeUrl, data)(implicitly[Writes[JsValue]],
      implicitly[HttpReads[HttpResponse]], implicitly[HeaderCarrier](hc), implicitly[ExecutionContext])
      .andThen {
        case Failure(x: BadRequestException) if x.message.contains("INVALID_PAYLOAD") =>
          invalidPayloadHandler.logFailures("/resources/schemas/schemeVariationSchema.json", data)
      }
  }

  private def desHeader(implicit hc: HeaderCarrier): Seq[(String, String)] = {
    val requestId = getCorrelationId(hc.requestId.map(_.value))

    Seq("Environment" -> config.desEnvironment, "Authorization" -> config.authorization,
      "Content-Type" -> "application/json", "CorrelationId" -> requestId)
  }

  private def handleSchemeDetailsResponse(psaId: String, response: HttpResponse, url: String)(
    implicit requestHeader: RequestHeader, executionContext: ExecutionContext) = {

    val badResponseSeq = Seq("INVALID_CORRELATION_ID", "INVALID_PAYLOAD", "INVALID_IDTYPE", "INVALID_SRN", "INVALID_PSTR", "INVALID_CORRELATIONID")

    response.status match {
      case OK =>
         val userAnswersJson = response.json.transform(
         schemeSubscriptionDetailsTransformer.transformToUserAnswers).getOrElse(throw new SchemeFailedMapToUserAnswersException)
         Logger.debug(s"Get-Scheme-details-UserAnswersJson - $userAnswersJson")
         Right(userAnswersJson)
      case FORBIDDEN if response.body.contains("INVALID_BUSINESS_PARTNER") => Left(new ForbiddenException(response.body))
      case _ => Left(handleErrorResponse("getSchemeDetails", url, response, badResponseSeq))
    }
  }

  private def handleErrorResponse(methodContext: String, url: String, response: HttpResponse, badResponseSeq: Seq[String]): HttpException =
    response.status match {
      case BAD_REQUEST if badResponseSeq.exists(response.body.contains(_)) => new BadRequestException(response.body)
      case NOT_FOUND => new NotFoundException(response.body)
      case status if is4xx(status) =>
        throw Upstream4xxResponse(upstreamResponseMessage(methodContext, url, status, response.body), status, status, response.allHeaders)
      case status if is5xx(status) =>
        throw Upstream5xxResponse(upstreamResponseMessage(methodContext, url, status, response.body), status, BAD_GATEWAY)
      case status =>
        throw new Exception(s"Subscription failed with status $status. Response body: '${response.body}'")

    }
}
