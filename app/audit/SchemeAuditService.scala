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

package audit

import audit.{SchemeType => AuditSchemeType}
import models.enumeration.{SchemeType => EnumSchemeType}
import models.userAnswersToEtmp.PensionsScheme
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HttpException, UpstreamErrorResponse}

import scala.util.{Failure, Success, Try}

class SchemeAuditService {

  def sendSchemeDetailsEvent(userIdNumber: String)
                            (sendEvent: SchemeDetailsAuditEvent => Unit): PartialFunction[Try[Either[HttpException, JsValue]], Unit] = {


    case Success(Right(schemeDetails)) =>
      sendEvent(
        SchemeDetailsAuditEvent(
          userIdNumber = userIdNumber,
          status = Status.OK,
          payload = Some(schemeDetails)
        )
      )
    case Success(Left(e)) =>
      sendEvent(
        SchemeDetailsAuditEvent(
          userIdNumber = userIdNumber,
          status = e.responseCode,
          payload = None
        )
      )
    case Failure(e: UpstreamErrorResponse) =>
      sendEvent(
        SchemeDetailsAuditEvent(
          userIdNumber = userIdNumber,
          status = e.statusCode,
          payload = None
        )
      )
    case Failure(e: HttpException) =>
      sendEvent(
        SchemeDetailsAuditEvent(
          userIdNumber = userIdNumber,
          status = e.responseCode,
          payload = None
        )
      )

  }

  def sendPspSchemeDetailsEvent(pspId: String)
                               (sendEvent: PspSchemeDetailsAuditEvent => Unit): PartialFunction[Try[Either[HttpException, JsValue]], Unit] = {
    case Success(Right(pspSchemeSubscription)) =>
      sendEvent(
        PspSchemeDetailsAuditEvent(
          pspId = pspId,
          status = Status.OK,
          payload = Some(pspSchemeSubscription)
        )
      )
    case Success(Left(e)) =>
      sendEvent(
        PspSchemeDetailsAuditEvent(
          pspId = pspId,
          status = e.responseCode,
          payload = None
        )
      )
    case Failure(e: UpstreamErrorResponse) =>
      sendEvent(
        PspSchemeDetailsAuditEvent(
          pspId = pspId,
          status = e.statusCode,
          payload = None
        )
      )
    case Failure(e: HttpException) =>
      sendEvent(
        PspSchemeDetailsAuditEvent(
          pspId = pspId,
          status = e.responseCode,
          payload = None
        )
      )
  }

  def sendListOfSchemesEvent(
                              idType: String,
                              idValue: String
                            )(
                              sendEvent: ListOfSchemesAudit => Unit
                            ): PartialFunction[Try[Either[HttpException, JsValue]], Unit] = {

    case Success(Right(listOfSchemes)) =>
      sendEvent(ListOfSchemesAudit(idType, idValue, Status.OK, Some(listOfSchemes)))

    case Success(Left(e)) =>
      sendEvent(ListOfSchemesAudit(idType, idValue, e.responseCode, None))

    case Failure(e: UpstreamErrorResponse) =>
      sendEvent(ListOfSchemesAudit(idType, idValue, e.statusCode, None))

    case Failure(e: HttpException) =>
      sendEvent(ListOfSchemesAudit(idType, idValue, e.responseCode, None))
  }

  private def translateSchemeType(pensionsScheme: PensionsScheme) = {
    if (pensionsScheme.customerAndSchemeDetails.isSchemeMasterTrust) {
      Some(AuditSchemeType.masterTrust)
    }
    else {
      pensionsScheme.customerAndSchemeDetails.schemeStructure.map {
        case EnumSchemeType.single.value => AuditSchemeType.singleTrust
        case EnumSchemeType.group.value => AuditSchemeType.groupLifeDeath
        case EnumSchemeType.corp.value => AuditSchemeType.bodyCorporate
        case _ => AuditSchemeType.other
      }
    }
  }

  def sendSchemeSubscriptionEvent(psaId: String, pensionsScheme: PensionsScheme, hasBankDetails: Boolean)
                                 (
                                   sendEvent: SchemeSubscription => Unit
                                 ):
  PartialFunction[Try[Either[HttpException, JsValue]], Unit] = {
    case Success(Right(outputResponse)) =>
      sendEvent(translateSchemeSubscriptionEvent(psaId, pensionsScheme, hasBankDetails, Status.OK, Some(outputResponse)))

    case Success(Left(e)) =>
      sendEvent(translateSchemeSubscriptionEvent(psaId, pensionsScheme, hasBankDetails, e.responseCode, None))

    case Failure(e: UpstreamErrorResponse) =>
      sendEvent(translateSchemeSubscriptionEvent(psaId, pensionsScheme, hasBankDetails, e.statusCode, None))

    case Failure(e: HttpException) =>
      sendEvent(translateSchemeSubscriptionEvent(psaId, pensionsScheme, hasBankDetails, e.responseCode, None))
  }

  def sendRACDACSchemeSubscriptionEvent(psaId: String, registerData: JsValue)
                                       (
                                         sendEvent: RACDACDeclarationAuditEvent => Unit
                                       ):
  PartialFunction[Try[Either[HttpException, JsValue]], Unit] = {
    case Success(Right(outputResponse)) =>
      sendEvent(RACDACDeclarationAuditEvent(psaId, Status.OK, registerData, Some(outputResponse)))

    case Success(Left(e)) =>
      sendEvent(RACDACDeclarationAuditEvent(psaId, e.responseCode, registerData, None))

    case Failure(e: UpstreamErrorResponse) =>
      sendEvent(RACDACDeclarationAuditEvent(psaId, e.statusCode, registerData, None))

    case Failure(e: HttpException) =>
      sendEvent(RACDACDeclarationAuditEvent(psaId, e.responseCode, registerData, None))
  }

  def translateSchemeSubscriptionEvent
  (psaId: String, pensionsScheme: PensionsScheme, hasBankDetails: Boolean, status: Int, response: Option[JsValue]): SchemeSubscription = {
    SchemeSubscription(
      psaIdentifier = psaId,
      schemeType = translateSchemeType(pensionsScheme),
      hasIndividualEstablisher = pensionsScheme.establisherDetails.individual.nonEmpty,
      hasCompanyEstablisher = pensionsScheme.establisherDetails.companyOrOrganization.nonEmpty,
      hasPartnershipEstablisher = pensionsScheme.establisherDetails.partnership.nonEmpty,
      hasDormantCompany = pensionsScheme.pensionSchemeDeclaration.box5.getOrElse(false),
      hasBankDetails = hasBankDetails,
      hasValidBankDetails = hasBankDetails && !pensionsScheme.customerAndSchemeDetails.haveInvalidBank,
      status = status,
      request = Json.toJson(pensionsScheme),
      response = response
    )
  }

  def sendSchemeUpdateEvent(psaId: String, pensionsScheme: PensionsScheme)
                           (
                             sendEvent: SchemeUpdate => Unit
                           ):
  PartialFunction[Try[Either[HttpException, JsValue]], Unit] = {
    case Success(Right(outputResponse)) =>
      sendEvent(translateSchemeUpdateEvent(psaId, pensionsScheme, Status.OK, Some(outputResponse)))

    case Success(Left(e)) =>
      sendEvent(translateSchemeUpdateEvent(psaId, pensionsScheme, e.responseCode, None))

    case Failure(e: UpstreamErrorResponse) =>
      sendEvent(translateSchemeUpdateEvent(psaId, pensionsScheme, e.statusCode, None))

    case Failure(e: HttpException) =>
      sendEvent(translateSchemeUpdateEvent(psaId, pensionsScheme, e.responseCode, None))
  }

  private def translateSchemeUpdateEvent
  (psaId: String, pensionsScheme: PensionsScheme, status: Int, response: Option[JsValue]): SchemeUpdate = {
    SchemeUpdate(
      psaIdentifier = psaId,
      schemeType = translateSchemeType(pensionsScheme),
      status = status,
      request = Json.toJson(pensionsScheme),
      response = response
    )
  }
}
