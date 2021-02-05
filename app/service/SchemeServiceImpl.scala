/*
 * Copyright 2021 HM Revenue & Customs
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

import audit.{AuditService, ListOfSchemesAudit, SchemeList, SchemeSubscription, SchemeUpdate, SchemeType => AuditSchemeType}
import com.google.inject.Inject
import connector.{BarsConnector, SchemeConnector}
import models.FeatureToggleName.TCMP
import models.ListOfSchemes
import models.enumeration.SchemeType
import models.userAnswersToEtmp.PensionsScheme.pensionSchemeHaveInvalidBank
import models.userAnswersToEtmp.{BankAccount, PensionsScheme}
import play.api.Logger
import play.api.http.Status
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpException, HttpResponse}
import utils.ValidationUtils.genResponse

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class SchemeServiceImpl @Inject()(
                                   schemeConnector: SchemeConnector,
                                   barsConnector: BarsConnector,
                                   auditService: AuditService,
                                   featureToggleService: FeatureToggleService
                                 ) extends SchemeService {

  private val logger = Logger(classOf[SchemeServiceImpl])

  override def listOfSchemes(psaId: String)
                            (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[HttpResponse] = {

    schemeConnector.listOfSchemes(psaId) andThen {
      case Success(httpResponse) =>
        sendSchemeListEvent(psaId, httpResponse.status, Some(httpResponse.json))
      case Failure(error: HttpException) =>
        sendSchemeListEvent(psaId, error.responseCode, None)
    }

  }

  override def listOfSchemes(idType: String, idValue: String)
                            (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[HttpResponse] = {

    schemeConnector.listOfSchemes(idType, idValue)(headerCarrier, implicitly, implicitly) andThen {
      case Success(httpResponse) =>
        auditService.sendEvent(ListOfSchemesAudit(idType, idValue, httpResponse.status, Some(httpResponse.json)))
      case Failure(error: HttpException) =>
        auditService.sendEvent(ListOfSchemesAudit(idType, idValue, error.responseCode, None))
    }
  }

  private def sendSchemeListEvent(psaId: String, status: Int, response: Option[JsValue])(implicit request: RequestHeader, ec: ExecutionContext): Unit = {

    auditService.sendEvent(SchemeList(psaId, status, response))

  }

  private val tcmpToggleOffTranformer: JsValue => JsObject = json => json.as[JsObject].transform(
    __.json.update(
      (__ \ "customerAndSchemeDetails" \ "isReguledSchemeInvestment").json.copyFrom(
        (__ \ "customerAndSchemeDetails" \ "isRegulatedSchemeInvestment").json.pick
      )
    ) andThen
      (__ \ "customerAndSchemeDetails" \ "isRegulatedSchemeInvestment").json.prune
  ).getOrElse(throw RegisterSchemeToggleOffTransformFailed)

  case object RegisterSchemeToggleOffTransformFailed extends Exception

  override def registerScheme(psaId: String, json: JsValue)
                             (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[HttpResponse] = {
    featureToggleService.get(TCMP).flatMap { tcmpToggle =>
      json.validate[PensionsScheme](PensionsScheme.registerApiReads(tcmpToggle.isEnabled)).fold(
        invalid = {
          errors =>
            val ex = JsResultException(errors)
            logger.warn("Invalid pension scheme", ex)
            Future.failed(new BadRequestException("Invalid pension scheme"))
        },
        valid = { validPensionsScheme =>
          readBankAccount(json).fold(
            error => Future.failed(error),
            bankAccount => haveInvalidBank(bankAccount, validPensionsScheme, psaId).flatMap {
              pensionsScheme =>
                val registerData = if(tcmpToggle.isEnabled) Json.toJson(pensionsScheme) else tcmpToggleOffTranformer(Json.toJson(pensionsScheme))
                schemeConnector.registerScheme(psaId, registerData, tcmpToggle.isEnabled) andThen {
                  case Success(httpResponse) =>
                    sendSchemeSubscriptionEvent(psaId, pensionsScheme, bankAccount.isDefined, Status.OK, Some(httpResponse.json))
                  case Failure(error: HttpException) =>
                    sendSchemeSubscriptionEvent(psaId, pensionsScheme, bankAccount.isDefined, error.responseCode, None)
                }
            }
          )
        }
      )
    }
  }

  override def updateScheme(pstr: String, psaId: String, json: JsValue)(implicit headerCarrier: HeaderCarrier,
                                                                        ec: ExecutionContext, request: RequestHeader): Future[HttpResponse] = {
    featureToggleService.get(TCMP).flatMap { tcmpToggle =>
      json.validate[PensionsScheme](PensionsScheme.updateApiReads(tcmpToggle.isEnabled)).fold(
        invalid = {
          errors =>
            val ex = JsResultException(errors)
            logger.warn("Invalid pension scheme", ex)
            Future.failed(new BadRequestException("Invalid pension scheme"))
        },
        valid = { validPensionsScheme =>
          val updatedScheme = Json.toJson(validPensionsScheme)(PensionsScheme.updateWrite(psaId, tcmpToggle.isEnabled))
          logger.debug(s"[Update-Scheme-Outgoing-Payload]$updatedScheme")
          schemeConnector.updateSchemeDetails(pstr, updatedScheme, tcmpToggle.isEnabled) andThen {
            case Success(httpResponse) =>
              sendSchemeUpdateEvent(psaId, validPensionsScheme, httpResponse.status, Some(httpResponse.json))
            case Failure(error: HttpException) =>
              sendSchemeUpdateEvent(psaId, validPensionsScheme, error.responseCode, None)
          }
        })
    }
  }

  private[service] def readBankAccount(json: JsValue): Either[BadRequestException, Option[BankAccount]] = {

    (json \ "uKBankDetails").validateOpt[BankAccount](BankAccount.apiReads).fold(
      errors => {
        val ex = JsResultException(errors)
        logger.warn("Invalid bank account details", ex)
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
            logger.debug("[Invalid-Bank-Account]")
            pensionSchemeHaveInvalidBank.set(pensionsScheme, true)
          case false =>
            logger.debug("[Valid-Bank-Account]")
            pensionSchemeHaveInvalidBank.set(pensionsScheme, false)
        }
      case None =>
        Future.successful(pensionSchemeHaveInvalidBank.set(pensionsScheme, false))
    }

  }

  private def translateSchemeType(pensionsScheme: PensionsScheme) = {
    if (pensionsScheme.customerAndSchemeDetails.isSchemeMasterTrust) {
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
  }

  private def sendSchemeUpdateEvent(psaId: String, pensionsScheme: PensionsScheme, status: Int, response: Option[JsValue])
                                   (implicit request: RequestHeader, ec: ExecutionContext): Unit = {
    auditService.sendEvent(translateSchemeUpdateEvent(psaId, pensionsScheme, status, response))
  }

  private[service] def translateSchemeUpdateEvent
  (psaId: String, pensionsScheme: PensionsScheme, status: Int, response: Option[JsValue]): SchemeUpdate = {
    SchemeUpdate(
      psaIdentifier = psaId,
      schemeType = translateSchemeType(pensionsScheme),
      status = status,
      request = Json.toJson(pensionsScheme),
      response = response
    )
  }

  private def sendSchemeSubscriptionEvent(psaId: String, pensionsScheme: PensionsScheme, hasBankDetails: Boolean, status: Int, response: Option[JsValue])
                                         (implicit request: RequestHeader, ec: ExecutionContext): Unit = {
    auditService.sendEvent(translateSchemeSubscriptionEvent(psaId, pensionsScheme, hasBankDetails, status, response))
  }

  private[service] def translateSchemeSubscriptionEvent
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

  override def getPstrFromSrn(srn: String, idType: String, idValue: String)
                             (implicit headerCarrier: HeaderCarrier,
                              ec: ExecutionContext, request: RequestHeader): Future[String] =
    listOfSchemes(idType, idValue).map { response =>
      response.status match {
        case OK =>
          response.json.convertTo[ListOfSchemes].schemeDetails.flatMap { listOfSchemes =>
            listOfSchemes.find(_.referenceNumber == srn).flatMap(_.pstr)
          }.getOrElse(throw pstrException)

        case _ => throw pstrException
      }
    }

  val pstrException: BadRequestException = new BadRequestException("PSTR could not be retrieved from SRN to call the get psp scheme details API")

}
