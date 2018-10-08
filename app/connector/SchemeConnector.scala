/*
 * Copyright 2018 HM Revenue & Customs
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
import models.schemes.PsaSchemeDetails
import play.Logger
import play.api.http.Status._
import play.api.libs.json.{JsValue, Writes, Json}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.InvalidPayloadHandler

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

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

  def getSchemeDetails(schemeIdType: String, idNumber: String)(implicit headerCarrier: HeaderCarrier,
                                                               ec: ExecutionContext,
                                                               request: RequestHeader): Future[Either[HttpException, PsaSchemeDetails]]

}

class SchemeConnectorImpl @Inject()(
                                     http: HttpClient,
                                     config: AppConfig,
                                     auditService: AuditService,
                                     invalidPayloadHandler: InvalidPayloadHandler
                                   ) extends SchemeConnector with HttpErrorFunctions {

  def desHeader(implicit hc: HeaderCarrier): Seq[(String, String)] = {
    val requestId = getCorrelationId(hc.requestId.map(_.value))

    Seq("Environment" -> config.desEnvironment, "Authorization" -> config.authorization,
      "Content-Type" -> "application/json", "CorrelationId" -> requestId)
  }

  def getCorrelationId(requestId: Option[String]): String = {
    requestId.getOrElse {
      Logger.error("No Request Id found while calling register with Id")
      randomUUID.toString
    }.replaceAll("(govuk-tax-|-)", "").slice(0, 32)
  }

  //scalastyle:off cyclomatic.complexity
  private def handleResponse(response: HttpResponse): Either[HttpException, PsaSchemeDetails] = {
    val badResponseSeq = Seq("INVALID_CORRELATION_ID", "INVALID_PAYLOAD", "INVALID_IDTYPE", "INVALID_SRN", "INVALID_PSTR", "INVALID_CORRELATIONID")
    response.status match {
      case OK => response.json.validate[PsaSchemeDetails](PsaSchemeDetails.apiReads).fold(
        error => {

          println("############")

          println(Json.prettyPrint(response.json))

          println("############")

          invalidPayloadHandler.logFailures("/resources/schemas/schemeDetailsReponse.json", response.json)
          Left(new BadRequestException("INVALID PAYLOAD"))
        },
        value => Right(value))
      case BAD_REQUEST if badResponseSeq.exists(response.body.contains(_)) => Left(new BadRequestException(response.body))
      case CONFLICT if response.body.contains("DUPLICATE_SUBMISSION") => Left(new ConflictException(response.body))
      case NOT_FOUND => Left(new NotFoundException(response.body))
      case FORBIDDEN if response.body.contains("INVALID_BUSINESS_PARTNER") => Left(new ForbiddenException(response.body))
      case status if is4xx(status) => throw Upstream4xxResponse(response.body, status, BAD_REQUEST, response.allHeaders)
      case status if is5xx(status) => throw Upstream5xxResponse(response.body, status, BAD_GATEWAY)
      case status => throw new Exception(s"Subscription failed with status $status. Response body: '${response.body}'")
    }
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

  override def getSchemeDetails(schemeIdType: String, idNumber: String)(implicit
                                                                        headerCarrier: HeaderCarrier,
                                                                        ec: ExecutionContext,
                                                                        request: RequestHeader): Future[Either[HttpException, PsaSchemeDetails]] = {

    implicit val rds: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
      override def read(method: String, url: String, response: HttpResponse): HttpResponse = response
    }

    val schemeDetailsUrl = config.schemeDetailsUrl.format(schemeIdType, idNumber)
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeader(implicitly[HeaderCarrier](headerCarrier)))

    http.GET[HttpResponse](schemeDetailsUrl)(implicitly, hc, implicitly) map handleResponse
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

}
