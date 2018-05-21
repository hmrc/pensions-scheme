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

package service

import audit.{AuditService, PSASubscription, SchemeList, SchemeSubscription, SchemeType => AuditSchemeType}
import com.google.inject.Inject
import connector.{BarsConnector, SchemeConnector}
import models.PensionsScheme._
import models.ReadsEstablisherDetails.readsEstablisherDetails
import models._
import models.enumeration.SchemeType
import play.api.Logger
import play.api.libs.json.{JsResultException, JsValue, Json}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse}
import utils.validationUtils._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SchemeServiceV1 @Inject()(schemeConnector: SchemeConnector, barsConnector: BarsConnector, auditService: AuditService) extends SchemeService {

  def registerScheme(psaId: String, json: JsValue)
      (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[HttpResponse] = {

    jsonToPensionsSchemeModel(json).fold(
      badRequestException => Future.failed(badRequestException),
      validPensionsScheme => {
        haveInvalidBank(json, validPensionsScheme).flatMap {
          case (pensionsScheme, hasBankDetails) => schemeConnector.registerScheme(psaId, Json.toJson(pensionsScheme)) andThen {
            case Success(_) =>
              sendSchemeSubscriptionEvent(psaId, pensionsScheme, hasBankDetails)
          }
        }
      }
    )

  }

  def listOfSchemes(psaId: String)
      (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[HttpResponse] = {

    schemeConnector.listOfSchemes(psaId) andThen {
      case Success(_) =>
        sendSchemeListEvent(psaId)
    }

  }

  def jsonToPensionsSchemeModel(json: JsValue): Either[BadRequestException, PensionsScheme] = {

    val result = for {
      customerAndScheme <- json.validate[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)
      declaration <- json.validate[PensionSchemeDeclaration](PensionSchemeDeclaration.apiReads)
      establishers <- json.validate[Seq[EstablisherDetails]](readsEstablisherDetails)
    } yield {
      PensionsScheme(customerAndScheme, declaration, List(establishers: _*))
    }

    result.fold(
      errors => {
        val ex = JsResultException(errors)
        Logger.warn("Invalid pension scheme", ex)
        Left(new BadRequestException("Invalid pension scheme"))
      },
      scheme => Right(scheme)
    )

  }

  def haveInvalidBank(json: JsValue, pensionsScheme: PensionsScheme)
                     (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[(PensionsScheme, Boolean)] = {

    readBankAccount(json).fold(
      jsResultException => Future.failed(jsResultException),
      {
        case Some(bankAccount) => isBankAccountInvalid(bankAccount).map {
          case true => {
            Logger.debug("[Invalid-Bank-Account]")
            (pensionSchemeHaveInvalidBank.set(pensionsScheme, true), true)
          }
          case false => {
            Logger.debug("[Valid-Bank-Account]")
            (pensionSchemeHaveInvalidBank.set(pensionsScheme, false), true)
          }
        }
        case None => {
          Logger.debug("[Valid-Bank-Account]")
          Future.successful((pensionsScheme, false))
        }
      }
    )

  }

  private def readBankAccount(json: JsValue): Either[BadRequestException, Option[BankAccount]] = {

    (json \ "uKBankDetails").validateOpt[BankAccount](BankAccount.apiReads).fold(
      errors => {
        val ex = JsResultException(errors)
        Logger.warn("Invalid bank account details", ex)
        Left(new BadRequestException("Invalid bank account details"))
      },
      maybeBankAccount => Right(maybeBankAccount)
    )

  }

  private def isBankAccountInvalid(bankAccount: BankAccount)
      (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {

    barsConnector.invalidBankAccount(bankAccount)

  }

  private def sendSchemeSubscriptionEvent(psaId: String, pensionsScheme: PensionsScheme, hasBankDetails: Boolean)
      (implicit request: RequestHeader, ec: ExecutionContext): Unit = {

    auditService.sendEvent(translateSchemeSubscriptionEvent(psaId, pensionsScheme, hasBankDetails))

  }

  def translateSchemeSubscriptionEvent(psaId: String, pensionsScheme: PensionsScheme, hasBankDetails: Boolean): SchemeSubscription = {

    val schemeType = if (pensionsScheme.customerAndSchemeDetails.isSchemeMasterTrust) {
      AuditSchemeType.masterTrust
    }
    else {
      pensionsScheme.customerAndSchemeDetails.schemeStructure match {
        case SchemeType.single.value => AuditSchemeType.singleTrust
        case SchemeType.group.value => AuditSchemeType.groupLifeDeath
        case SchemeType.corp.value => AuditSchemeType.bodyCorporate
        case _ => AuditSchemeType.other
      }
    }

    SchemeSubscription(
      psaIdentifier = psaId,
      schemeType = schemeType,
      hasIndividualEstablisher = pensionsScheme.establisherDetails.exists(establisher => establisher.`type` == "Individual"),
      hasCompanyEstablisher = pensionsScheme.establisherDetails.exists(establisher => establisher.`type` == "Company/Org"),
      hasPartnershipEstablisher = pensionsScheme.establisherDetails.exists(establisher => establisher.`type` == "Partnership"),
      hasDormantCompany = pensionsScheme.pensionSchemeDeclaration.box5.getOrElse(false),
      hasBankDetails = hasBankDetails,
      hasValidBankDetails = hasBankDetails && !pensionsScheme.customerAndSchemeDetails.haveInvalidBank
    )

  }

  private def sendSchemeListEvent(psaId: String)(implicit request: RequestHeader, ec: ExecutionContext): Unit = {

    auditService.sendEvent(SchemeList(psaId))

  }

  override def registerPSA(json: JsValue)
                          (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[HttpResponse] = {

    Try(json.convertTo[PensionSchemeAdministrator](PensionSchemeAdministrator.apiReads)) match {
      case Success(pensionSchemeAdministrator) =>
        val psaJsValue = Json.toJson(pensionSchemeAdministrator)(PensionSchemeAdministrator.psaSubmissionWrites)
        Logger.debug(s"[PSA-Registration-Outgoing-Payload]$psaJsValue")

        schemeConnector.registerPSA(psaJsValue) andThen {
          case Success(_) =>
            sendPSASubscriptionEvent(pensionSchemeAdministrator, true)
          case _ =>
            sendPSASubscriptionEvent(pensionSchemeAdministrator, false)
        }

      case Failure(e) =>
        Logger.warn(s"Bad Request returned from frontend for PSA $e")
        Future.failed(new BadRequestException(s"Bad Request returned from frontend for PSA $e"))
    }

  }

  private def sendPSASubscriptionEvent(psa: PensionSchemeAdministrator, success: Boolean)(implicit request: RequestHeader, ec: ExecutionContext): Unit = {

    auditService.sendEvent(
      PSASubscription(
        existingUser = psa.pensionSchemeAdministratoridentifierStatus.isExistingPensionSchemaAdministrator,
        success = success,
        legalStatus = psa.legalStatus
      )
    )

  }

}
