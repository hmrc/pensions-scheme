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
import play.Logger
import play.api.libs.json.{JsValue, Writes}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[SchemeConnectorImpl])
trait SchemeConnector {
  def registerScheme(psaId: String, registerData: JsValue)(implicit
                                                           headerCarrier: HeaderCarrier,
                                                           ec: ExecutionContext,
                                                           request: RequestHeader): Future[HttpResponse]

  def registerPSA(registerData: JsValue)(implicit
                                         headerCarrier: HeaderCarrier,
                                         ec: ExecutionContext,
                                         request: RequestHeader): Future[HttpResponse]

  def listOfSchemes(psaId: String)(implicit
                                   headerCarrier: HeaderCarrier,
                                   ec: ExecutionContext,
                                   request: RequestHeader): Future[HttpResponse]
}

class SchemeConnectorImpl @Inject()(
                                     http: HttpClient,
                                     config: AppConfig,
                                     auditService: AuditService
                                   ) extends SchemeConnector {

  def desHeader(implicit hc: HeaderCarrier): Seq[(String, String)] = {
    val requestId = hc.requestId.map(_.value).getOrElse {
      Logger.error("No Request Id found while calling register with Id")
      randomUUID.toString
    }.replaceAll("(govuk-tax-|-)", "")

    Seq("Environment" -> config.desEnvironment, "Authorization" -> config.authorization,
      "Content-Type" -> "application/json", "CorrelationId" -> requestId)
  }

  override def registerScheme(psaId: String, registerData: JsValue)(implicit
                                                                    headerCarrier: HeaderCarrier,
                                                                    ec: ExecutionContext,
                                                                    request: RequestHeader): Future[HttpResponse] = {

    val schemeRegisterUrl = config.schemeRegistrationUrl.format(psaId)
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeader(implicitly[HeaderCarrier](headerCarrier)))

    http.POST(schemeRegisterUrl, registerData)(implicitly[Writes[JsValue]],
      implicitly[HttpReads[HttpResponse]], implicitly[HeaderCarrier](hc), implicitly[ExecutionContext])

  }

  override def registerPSA(registerData: JsValue)(implicit
                                                  headerCarrier: HeaderCarrier,
                                                  ec: ExecutionContext,
                                                  request: RequestHeader): Future[HttpResponse] = {

    val schemeAdminRegisterUrl = config.schemeAdminRegistrationUrl
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeader(implicitly[HeaderCarrier](headerCarrier)))

    http.POST[JsValue, HttpResponse](schemeAdminRegisterUrl, registerData)(implicitly[Writes[JsValue]],
      implicitly[HttpReads[HttpResponse]], implicitly[HeaderCarrier](hc), implicitly[ExecutionContext])

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
