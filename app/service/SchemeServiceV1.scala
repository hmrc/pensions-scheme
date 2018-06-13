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

import audit.{AuditService, SchemeSubscription, SchemeType => AuditSchemeType}
import com.google.inject.Inject
import config.AppConfig
import connector.{BarsConnector, SchemeConnector}
import models.PensionsScheme._
import models.ReadsEstablisherDetails.readsEstablisherDetails
import models._
import models.enumeration.SchemeType
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.{JsResultException, JsValue, Json}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpException, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class SchemeServiceV1 @Inject()(schemeConnector: SchemeConnector, barsConnector: BarsConnector,
                                auditService: AuditService, appConfig: AppConfig)
  extends SchemeServiceImpl(schemeConnector, auditService, appConfig) {

  override def registerScheme(psaId: String, json: JsValue)
      (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[HttpResponse] = {

    jsonToPensionsSchemeModel(json).fold(
      badRequestException => Future.failed(badRequestException),
      validPensionsScheme => {
        haveInvalidBank(json, validPensionsScheme, psaId).flatMap {
          case (pensionsScheme, hasBankDetails) => schemeConnector.registerScheme(psaId, Json.toJson(pensionsScheme)) andThen {
            case Success(httpResponse) =>
              sendSchemeSubscriptionEvent(psaId, pensionsScheme, hasBankDetails, Status.OK, Some(httpResponse.json))
            case Failure(error: HttpException) =>
              sendSchemeSubscriptionEvent(psaId, pensionsScheme, hasBankDetails, error.responseCode, None)
          }
        }
      }
    )

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

  def haveInvalidBank(json: JsValue, pensionsScheme: PensionsScheme, psaId: String)
                     (implicit ec: ExecutionContext, hc: HeaderCarrier, rh: RequestHeader): Future[(PensionsScheme, Boolean)] = {

    readBankAccount(json).fold(
      jsResultException => Future.failed(jsResultException),
      {
        case Some(bankAccount) => isBankAccountInvalid(bankAccount, psaId).map {
          case true =>
            Logger.debug("[Invalid-Bank-Account]")
            (pensionSchemeHaveInvalidBank.set(pensionsScheme, true), true)
          case false =>
            Logger.debug("[Valid-Bank-Account]")
            (pensionSchemeHaveInvalidBank.set(pensionsScheme, false), true)
        }
        case None =>
          Logger.debug("[Valid-Bank-Account]")
          Future.successful((pensionsScheme, false))
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

  private def isBankAccountInvalid(bankAccount: BankAccount, psaId: String)
      (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, rh: RequestHeader): Future[Boolean] = {

    barsConnector.invalidBankAccount(bankAccount, psaId)

  }

  protected def sendSchemeSubscriptionEvent(psaId: String, pensionsScheme: PensionsScheme, hasBankDetails: Boolean, status: Int, response: Option[JsValue])
                                           (implicit request: RequestHeader, ec: ExecutionContext): Unit = {

    auditService.sendEvent(translateSchemeSubscriptionEvent(psaId, pensionsScheme, hasBankDetails, status, response))

  }

  private[service] def translateSchemeSubscriptionEvent
  (psaId: String, pensionsScheme: PensionsScheme, hasBankDetails: Boolean, status: Int, response: Option[JsValue]): SchemeSubscription = {

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
      hasValidBankDetails = hasBankDetails && !pensionsScheme.customerAndSchemeDetails.haveInvalidBank,
      status = status,
      request = Json.toJson(pensionsScheme),
      response = response
    )

  }

}
