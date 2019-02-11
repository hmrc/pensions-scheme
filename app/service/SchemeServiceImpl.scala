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

package service

import audit.{AuditService, SchemeList, SchemeSubscription, SchemeType => AuditSchemeType}
import com.google.inject.Inject
import config.AppConfig
import connector.{BarsConnector, SchemeConnector}
import models.PensionsScheme.pensionSchemeHaveInvalidBank
import models.ReadsEstablisherDetails._
import models._
import models.enumeration.SchemeType
import play.api.Logger
import play.api.http.Status
import play.api.libs.json._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpException, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class SchemeServiceImpl @Inject()(schemeConnector: SchemeConnector, barsConnector: BarsConnector,
                                  auditService: AuditService, appConfig: AppConfig) extends SchemeService {

  override def listOfSchemes(psaId: String)
                            (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[HttpResponse] = {

    schemeConnector.listOfSchemes(psaId) andThen {
      case Success(httpResponse) =>
        sendSchemeListEvent(psaId, Status.OK, Some(httpResponse.json))
      case Failure(error: HttpException) =>
        sendSchemeListEvent(psaId, error.responseCode, None)
    }

  }

  private def sendSchemeListEvent(psaId: String, status: Int, response: Option[JsValue])(implicit request: RequestHeader, ec: ExecutionContext): Unit = {

    auditService.sendEvent(SchemeList(psaId, status, response))

  }

  override def registerScheme(psaId: String, json: JsValue)
                             (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[HttpResponse] = {

    transformJsonToModel(json).fold(
      error => Future.failed(error),
      validPensionsScheme =>
        readBankAccount(json).fold(
          error => Future.failed(error),
          bankAccount => haveInvalidBank(bankAccount, validPensionsScheme, psaId).flatMap {
            pensionsScheme =>
              schemeConnector.registerScheme(psaId, Json.toJson(pensionsScheme)) andThen {
                case Success(httpResponse) =>
                  sendSchemeSubscriptionEvent(psaId, pensionsScheme, bankAccount.isDefined, Status.OK, Some(httpResponse.json))
                case Failure(error: HttpException) =>
                  sendSchemeSubscriptionEvent(psaId, pensionsScheme, bankAccount.isDefined, error.responseCode, None)
              }
          }
        )
    )

  }

  private[service] def transformJsonToModel(json: JsValue): Either[BadRequestException, PensionsScheme] = {

    val readsCustomerAndSchemeDetails: Reads[CustomerAndSchemeDetails] = CustomerAndSchemeDetails.apiReads

    val readsDeclaration = PensionSchemeDeclaration.apiReads

    val result = for {
      customerAndScheme <- json.validate[CustomerAndSchemeDetails](readsCustomerAndSchemeDetails)
      declaration <- json.validate[PensionSchemeDeclaration](readsDeclaration)
      establishers <- json.validate[EstablisherDetails](readsEstablisherDetails)
      trustees <- json.validate[TrusteeDetails](readsTrusteeDetails)
      isEstablisherOrTrusteeDetailsChanged <- json.validate[Option[Boolean]]((JsPath \ "isEstablisherOrTrusteeDetailsChanged").readNullable[Boolean])
    } yield {
      PensionsScheme(customerAndScheme, declaration, establishers, trustees, isEstablisherOrTrusteeDetailsChanged)
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

  private[service] def readBankAccount(json: JsValue): Either[BadRequestException, Option[BankAccount]] = {

    (json \ "uKBankDetails").validateOpt[BankAccount](BankAccount.apiReads).fold(
      errors => {
        val ex = JsResultException(errors)
        Logger.warn("Invalid bank account details", ex)
        Left(new BadRequestException("Invalid bank account details"))
      },
      maybeBankAccount => Right(maybeBankAccount)
    )

  }

  private[service] def haveInvalidBank(bankAccount: Option[BankAccount], pensionsScheme: PensionsScheme, psaId: String)
                                      (implicit ec: ExecutionContext, hc: HeaderCarrier, rh: RequestHeader): Future[PensionsScheme] = {

    bankAccount match {
      case Some(details) =>
        barsConnector.invalidBankAccount(details, psaId).map {
          case true =>
            Logger.debug("[Invalid-Bank-Account]")
            pensionSchemeHaveInvalidBank.set(pensionsScheme, true)
          case false =>
            Logger.debug("[Valid-Bank-Account]")
            pensionSchemeHaveInvalidBank.set(pensionsScheme, false)
        }
      case None =>
        Future.successful(pensionSchemeHaveInvalidBank.set(pensionsScheme, false))
    }

  }

  private def sendSchemeSubscriptionEvent(psaId: String, pensionsScheme: PensionsScheme, hasBankDetails: Boolean, status: Int, response: Option[JsValue])
                                         (implicit request: RequestHeader, ec: ExecutionContext): Unit = {
    auditService.sendEvent(translateSchemeSubscriptionEvent(psaId, pensionsScheme, hasBankDetails, status, response))
  }

  private[service] def translateSchemeSubscriptionEvent
  (psaId: String, pensionsScheme: PensionsScheme, hasBankDetails: Boolean, status: Int, response: Option[JsValue]): SchemeSubscription = {

    val schemeType = if (pensionsScheme.customerAndSchemeDetails.isSchemeMasterTrust) {
      Some(AuditSchemeType.masterTrust)
    }
    else {
      pensionsScheme.customerAndSchemeDetails.schemeStructure.map {
        case SchemeType.single.value => AuditSchemeType.singleTrust
        case SchemeType.group.value => AuditSchemeType.groupLifeDeath
        case SchemeType.corp.value => AuditSchemeType.bodyCorporate
        case _ => AuditSchemeType.other
      }
    }

    SchemeSubscription(
      psaIdentifier = psaId,
      schemeType = schemeType,
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


}
