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
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http._
import play.api.http.Status._
import uk.gov.hmrc.play.bootstrap.http.HttpClient

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
                                       (implicit hc: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, JsValue]] = {

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeader)

    val registerWithIdUrl = config.registerWithIdIndividualUrl.format(nino)

    Logger.debug(s"[Pensions-Scheme-Header-Carrier]-${desHeader.toString()}")

    http.POST(registerWithIdUrl, registerData,desHeader)(implicitly,implicitly[HttpReads[HttpResponse]],HeaderCarrier(),implicitly) map {
      response =>
        require(response.status == 200)
        Right(response.json)
    } recoverWith {
      case e: BadRequestException if e.message.contains("INVALID_NINO") => Future.successful(Left(e))
      case e: BadRequestException if e.message.contains("INVALID_PAYLOAD") => Future.successful(Left(e))
      case e: NotFoundException => Future.successful(Left(e))
      case e: Upstream4xxResponse if e.upstreamResponseCode == CONFLICT => Future.successful(Left(new ConflictException(e.message)))
    } andThen sendPSARegistrationEvent(true, user, "Individual", registerData, withIdIsUk) andThen logWarning("registerWithIdIndividual")

  }

  override def registerWithIdOrganisation(utr: String, user: User, registerData: JsValue)
                                         (implicit hc: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, JsValue]] = {
    val registerWithIdUrl = config.registerWithIdOrganisationUrl.format(utr)
    val psaType: String = organisationPsaType(registerData)

    http.POST(registerWithIdUrl, registerData, desHeader)(implicitly,implicitly[HttpReads[HttpResponse]],HeaderCarrier(),implicitly) map {
      response =>
        require(response.status == 200)
        Right(response.json)
    } recoverWith {
      case e: BadRequestException if e.message.contains("INVALID_PAYLOAD") => Future.successful(Left(e))
      case e: NotFoundException => Future.successful(Left(e))
      case e: Upstream4xxResponse if e.upstreamResponseCode == CONFLICT => Future.successful(Left(new ConflictException(e.message)))
    } andThen sendPSARegistrationEvent(true, user, psaType, registerData, withIdIsUk) andThen logWarning("registerWithIdOrganisation")

  }

  override def registrationNoIdOrganisation(user: User, registerData: OrganisationRegistrant)
                                           (implicit hc: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[Either[HttpException, JsValue]] = {

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeader)
    val schemeAdminRegisterUrl = config.registerWithoutIdOrganisationUrl

    http.POST(schemeAdminRegisterUrl, registerData)(OrganisationRegistrant.apiWrites, implicitly[HttpReads[HttpResponse]], implicitly, implicitly) map {
      response =>
        require(response.status == 200)
        Right(response.json)
    } recoverWith {
      case e: BadRequestException if e.message.contains("INVALID_PAYLOAD") => Future.successful(Left(e))
      case e: NotFoundException => Future.successful(Left(e))
      case e: Upstream4xxResponse if e.upstreamResponseCode == CONFLICT => Future.successful(Left(new ConflictException(e.message)))
    } andThen sendPSARegistrationEvent(false, user, "Organisation", Json.toJson(registerData), noIdIsUk(registerData)) andThen logWarning("registrationNoIdOrganisation")

  }

  private def organisationPsaType(registerData: JsValue): String = {
    (registerData \ "organisation" \ "organisationType").validate[String].fold(
      _ => "Unknown",
      organisationType => organisationType
    )
  }

  private def noIdIsUk(organisation: OrganisationRegistrant)(response: JsValue): Option[Boolean] = {
    organisation.address match {
      case _: UkAddress => Some(true)
      case _ => Some(false)
    }
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

  private def sendPSARegistrationEvent(withId: Boolean, user: User, psaType: String, registerData: JsValue, isUk: JsValue => Option[Boolean])
                                      (implicit request: RequestHeader, ec: ExecutionContext): PartialFunction[Try[Either[HttpException, JsValue]], Unit] = {

    case Success(Right(json)) =>
      auditService.sendEvent(
        PSARegistration(
          withId = withId,
          externalId = user.externalId,
          psaType = psaType,
          found = true,
          isUk = isUk(json),
          status = Status.OK,
          request = registerData,
          response = Some(json)
        )
      )
    case Success(Left(e)) =>
      auditService.sendEvent(
        PSARegistration(
          withId = withId,
          externalId = user.externalId,
          psaType = psaType,
          found = false,
          isUk = None,
          status = e.responseCode,
          request = registerData,
          response = None
        )
      )
    case Failure(t) =>
      Logger.error("Error in registration connector", t)

  }

  private def logWarning(endpoint: String): PartialFunction[Try[Either[HttpException, JsValue]], Unit] = {
    case Success(Left(e: HttpException)) => Logger.warn(s"RegistrationConnector.$endpoint received error response from DES", e)
  }

}

@ImplementedBy(classOf[RegistrationConnectorImpl])
trait RegistrationConnector {
  def registerWithIdIndividual(nino: String, user: User, registerData: JsValue)(implicit
                                                                                hc: HeaderCarrier,
                                                                                ec: ExecutionContext,
                                                                                request: RequestHeader): Future[Either[HttpException, JsValue]]

  def registerWithIdOrganisation(utr: String, user: User, registerData: JsValue)(implicit
                                                                                 hc: HeaderCarrier,
                                                                                 ec: ExecutionContext,
                                                                                 request: RequestHeader): Future[Either[HttpException, JsValue]]

  def registrationNoIdOrganisation(user: User, registerData: OrganisationRegistrant)(implicit
                                                                         hc: HeaderCarrier,
                                                                         ec: ExecutionContext,
                                                                         request: RequestHeader): Future[Either[HttpException, JsValue]]
}
