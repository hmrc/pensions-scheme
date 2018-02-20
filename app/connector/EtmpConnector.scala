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

import com.google.inject.{ImplementedBy, Inject}
import config.AppConfig
import play.api.libs.json.JsValue
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class EtmpConnectorImpl @Inject()(http: HttpClient, config: AppConfig) extends EtmpConnector {

  val desHeader = Seq("Environment" -> config.desEnvironment, "Authorization" -> config.authorization,
    "Content-Type" -> "application/json")
  implicit val hc = HeaderCarrier(extraHeaders = desHeader)

  override def registerScheme(psaId: String, registerData: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val schemeRegisterUrl = config.schemeRegistrationUrl.format(psaId)

    http.POST(schemeRegisterUrl, registerData)
  }

  override def registerPSA(registerData: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val schemeAdminRegisterUrl = config.schemeAdminRegistrationUrl

    http.POST(schemeAdminRegisterUrl, registerData)
  }

  override def registrationNoIdIndividual(registerData: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val schemeAdminRegisterUrl = config.registrationNoIdIndividual

    http.POST(schemeAdminRegisterUrl, registerData)
  }

  override def registrationNoIdOrganisation(registerData: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val schemeAdminRegisterUrl = config.registrationNoIdOrganisation

    http.POST(schemeAdminRegisterUrl, registerData)
  }
}
@ImplementedBy(classOf[EtmpConnectorImpl])
trait EtmpConnector {
  def registerScheme(psaId: String, registerData: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse]

  def registerPSA(registerData: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse]

  def registrationNoIdIndividual(registerData: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse]

  def registrationNoIdOrganisation(registerData: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse]
}

