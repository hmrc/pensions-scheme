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

import java.util.UUID

import com.google.inject.{ImplementedBy, Inject}
import config.AppConfig
import models.OrganisationRegistrant
import play.api.libs.json.JsValue
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class RegistrationConnectorImpl @Inject()(http: HttpClient, config: AppConfig) extends RegistrationConnector {

  val desHeader = Seq("Environment" -> config.desEnvironment, "Authorization" -> config.authorization,
    "Content-Type" -> "application/json", "CorrelationId" -> UUID.randomUUID().toString)

  override def registerWithIdIndividual(nino: String, registerData: JsValue)(implicit hc: HeaderCarrier,
                                                                             ec: ExecutionContext): Future[HttpResponse] = {
    val registerWithIdUrl = config.registerWithIdIndividualUrl.format(nino)

    http.POST(registerWithIdUrl, registerData,desHeader)(implicitly,implicitly[HttpReads[HttpResponse]],HeaderCarrier(),implicitly)
  }

  override def registerWithIdOrganisation(utr: String, registerData: JsValue)(implicit hc: HeaderCarrier,
                                                                              ec: ExecutionContext): Future[HttpResponse] = {
    val registerWithIdUrl = config.registerWithIdOrganisationUrl.format(utr)

    http.POST(registerWithIdUrl, registerData, desHeader)(implicitly,implicitly[HttpReads[HttpResponse]],HeaderCarrier(),implicitly)
  }

  override def registrationNoIdOrganisation(registerData: OrganisationRegistrant)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val schemeAdminRegisterUrl = config.registerWithoutIdOrganisationUrl

    http.POST(schemeAdminRegisterUrl, registerData, desHeader)(OrganisationRegistrant.apiWrites, implicitly[HttpReads[HttpResponse]], HeaderCarrier(), implicitly)
  }
}

@ImplementedBy(classOf[RegistrationConnectorImpl])
trait RegistrationConnector {
  def registerWithIdIndividual(nino: String, registerData: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse]

  def registerWithIdOrganisation(utr: String, registerData: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse]

  def registrationNoIdOrganisation(registerData: OrganisationRegistrant)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse]
}

