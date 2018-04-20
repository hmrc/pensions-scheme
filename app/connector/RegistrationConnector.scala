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

import audit._
import com.google.inject.{ImplementedBy, Inject}
import config.AppConfig
import models.{OrganisationRegistrant, SuccessResponse, UkAddress, User}
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import play.api.Logger


import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class RegistrationConnectorImpl @Inject()(
                                           http: HttpClient,
                                           config: AppConfig,
                                           auditService: AuditService
                                         ) extends RegistrationConnector with RawReads {

  private val desHeader = Seq(
    "Environment" -> config.desEnvironment,
    "Authorization" -> config.authorization,
    "Content-Type" -> "application/json"
  )

  override def registerWithIdIndividual(nino: String, user: User, registerData: JsValue)
                                       (implicit
                                        hc: HeaderCarrier,
                                        ec: ExecutionContext,
                                        request: RequestHeader
                                       ): Future[HttpResponse] = {

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeader)

    val registerWithIdUrl = config.registerWithIdIndividualUrl.format(nino)

    Logger.debug(s"[Pensions-Scheme-Header-Carrier]-${desHeader.toString()}")

    val result = http.POST(registerWithIdUrl, registerData,desHeader)(implicitly,implicitly[HttpReads[HttpResponse]],HeaderCarrier(),implicitly)

    result andThen {
      sendPSARegistrationEvent(true, user, "Individual", withIdIsUk)
    }

  }

  private def sendPSARegistrationEvent(withId: Boolean, user: User, psaType: String, isUk: JsValue => Option[Boolean])
      (implicit request: RequestHeader, ec: ExecutionContext): PartialFunction[Try[HttpResponse], Unit] = {

    case Success(httpResponse) =>
      auditService.sendEvent(
        PSARegistration(
          withId = withId,
          externalId = user.externalId,
          psaType = psaType,
          found = true,
          isUk = isUk(httpResponse.json)
        )
      )
    case Failure(_: NotFoundException) =>
      auditService.sendEvent(
        PSARegistration(
          withId = withId,
          externalId = user.externalId,
          psaType = psaType,
          found = false,
          isUk = None
        )
      )
    case Failure(t) =>
      Logger.error("Error in registration connector", t)

  }

  private def withIdIsUk(response: JsValue): Option[Boolean] = {

    response.validate[SuccessResponse].fold(
      _ => None,
      success => success.address match {
        case _: UkAddress => Some(true)
        case _ => Some(false)
      }
    )

  }

  override def registerWithIdOrganisation(utr: String, user: User, registerData: JsValue)(implicit
                                                                                          hc: HeaderCarrier,
                                                                                          ec: ExecutionContext,
                                                                                          request: RequestHeader): Future[HttpResponse] = {
    val registerWithIdUrl = config.registerWithIdOrganisationUrl.format(utr)
    val psaType: String = organisationPsaType(registerData)

    val result = http.POST(registerWithIdUrl, registerData, desHeader)(implicitly,implicitly[HttpReads[HttpResponse]],HeaderCarrier(),implicitly)

    result andThen {
      sendPSARegistrationEvent(true, user, psaType, withIdIsUk)
    }

  }

  private def organisationPsaType(registerData: JsValue): String = {
    (registerData \ "organisation" \ "organisationType").validate[String].fold(
      _ => "Unknown",
      organisationType => organisationType
    )
  }

  override def registrationNoIdOrganisation(user: User, registerData: OrganisationRegistrant)(implicit
                                                                                  hc: HeaderCarrier,
                                                                                  ec: ExecutionContext,
                                                                                  request: RequestHeader): Future[HttpResponse] = {
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeader)
    val schemeAdminRegisterUrl = config.registerWithoutIdOrganisationUrl

    val result = http.POST(schemeAdminRegisterUrl, registerData)(OrganisationRegistrant.apiWrites, implicitly[HttpReads[HttpResponse]], implicitly, implicitly)

    result andThen {
      sendPSARegistrationEvent(false, user, "Organisation", noIdIsUk(registerData))
    }

  }

  private def noIdIsUk(organisation: OrganisationRegistrant)(response: JsValue): Option[Boolean] = {
    organisation.address match {
      case _: UkAddress => Some(true)
      case _ => Some(false)
    }
  }

}

@ImplementedBy(classOf[RegistrationConnectorImpl])
trait RegistrationConnector {
  def registerWithIdIndividual(nino: String, user: User, registerData: JsValue)(implicit
                                                                                hc: HeaderCarrier,
                                                                                ec: ExecutionContext,
                                                                                request: RequestHeader): Future[HttpResponse]

  def registerWithIdOrganisation(utr: String, user: User, registerData: JsValue)(implicit
                                                                                 hc: HeaderCarrier,
                                                                                 ec: ExecutionContext,
                                                                                 request: RequestHeader): Future[HttpResponse]

  def registrationNoIdOrganisation(user: User, registerData: OrganisationRegistrant)(implicit
                                                                         hc: HeaderCarrier,
                                                                         ec: ExecutionContext,
                                                                         request: RequestHeader): Future[HttpResponse]
}
