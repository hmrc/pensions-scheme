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

import audit.{SchemeUpdate, RACDACDeclarationAuditEvent, AuditService, SchemeSubscription, ListOfSchemesAudit, SchemeType => AuditSchemeType}
import com.google.inject.Inject
import connector.{BarsConnector, SchemeConnector}
import models.FeatureToggleName.RACDAC
import models.ListOfSchemes
import models.enumeration.SchemeType
import models.userAnswersToEtmp.PensionsScheme.pensionSchemeHaveInvalidBank
import models.userAnswersToEtmp.{RACDACPensionsScheme, BankAccount, PensionsScheme}
import play.api.Logger
import play.api.http.Status
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{BadRequestException, HttpResponse, HttpException, HeaderCarrier}
import utils.ValidationUtils.genResponse

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}

class SchemeServiceImpl @Inject()(
                                   schemeConnector: SchemeConnector,
                                   barsConnector: BarsConnector,
                                   auditService: AuditService,
                                   featureToggleService: FeatureToggleService
                                 ) extends SchemeService {

  private val logger = Logger(classOf[SchemeServiceImpl])

  override def listOfSchemes(idType: String, idValue: String)
                            (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[HttpResponse] = {

    schemeConnector.listOfSchemes(idType, idValue)(headerCarrier, implicitly, implicitly) andThen {
      case Success(httpResponse) =>
        logger.debug(s"\n\n\nList of schemes response: ${Json.prettyPrint(httpResponse.json)}\n\n\n")
        auditService.sendEvent(ListOfSchemesAudit(idType, idValue, httpResponse.status, Some(httpResponse.json)))
      case Failure(error: HttpException) =>
        auditService.sendEvent(ListOfSchemesAudit(idType, idValue, error.responseCode, None))
    }
  }

  case object RegisterSchemeToggleOffTransformFailed extends Exception

  private def registerNonRACDACScheme(json:JsValue, psaId: String, isRACDACEnabled: Boolean)(implicit
    headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader):Future[HttpResponse] = {
    json.validate[PensionsScheme](PensionsScheme.registerApiReads).fold(
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
              val registerData = {
                Json.toJson(pensionsScheme).as[JsObject] ++
                  /*
                    Once the rac/dac toggle is removed then the next line can be removed (as this node is
                    optional for non-RAC/DAC schemes). For now it is required as the stubs looks for this
                    node to determine whether to validate against the new RAC/DAC schema or the old one.
                  */
                  (if (isRACDACEnabled) Json.obj("racdacScheme" -> false) else Json.obj())
              }

              schemeConnector.registerScheme(psaId, registerData) andThen {
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

  private def registerRACDACScheme(json:JsValue, psaId: String)(implicit
    headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader):Future[HttpResponse] = {
    json.validate[RACDACPensionsScheme](RACDACPensionsScheme.reads).fold(
      invalid = {
        errors =>
          val ex = JsResultException(errors)
          logger.warn("Invalid RAC/DAC pension scheme", ex)
          Future.failed(new BadRequestException("Invalid RAC/DAC pension scheme"))
      },
      valid = { validRACDACPensionsScheme =>
        val registerData = Json.toJson(validRACDACPensionsScheme)
        schemeConnector.registerScheme(psaId, registerData).map { response =>
          if (response.status == OK) {
            auditService.sendExtendedEvent(RACDACDeclarationAuditEvent(registerData.as[JsObject]))
          }
          response
        }
      }
    )
  }

  private def register(json:JsValue, psaId: String, isRACDACEnabled:Boolean)(implicit
    headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader):Future[HttpResponse] = {

    def isRACDACSchemeDeclaration = (json \ "racdac" \ "declaration").toOption.exists(_.as[Boolean])

    if (isRACDACEnabled && isRACDACSchemeDeclaration) {
      registerRACDACScheme(json, psaId)
    } else {
      registerNonRACDACScheme(json, psaId, isRACDACEnabled)
    }
  }

  override def registerScheme(psaId: String, json: JsValue)
                             (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[HttpResponse] = {

    for {
      racDacToggle <- featureToggleService.get(RACDAC)
      response <- register(json, psaId, racDacToggle.isEnabled)
    } yield response
  }

  override def updateScheme(pstr: String, psaId: String, json: JsValue)(implicit headerCarrier: HeaderCarrier,
                                                                        ec: ExecutionContext, request: RequestHeader): Future[HttpResponse] = {
      json.validate[PensionsScheme](PensionsScheme.updateApiReads).fold(
        invalid = {
          errors =>
            val ex = JsResultException(errors)
            logger.warn("Invalid pension scheme", ex)
            Future.failed(new BadRequestException("Invalid pension scheme"))
        },
        valid = { validPensionsScheme =>
          val updatedScheme = Json.toJson(validPensionsScheme)(PensionsScheme.updateWrite(psaId))
          logger.debug(s"[Update-Scheme-Outgoing-Payload]$updatedScheme")
          schemeConnector.updateSchemeDetails(pstr, updatedScheme) andThen {
            case Success(httpResponse) =>
              sendSchemeUpdateEvent(psaId, validPensionsScheme, httpResponse.status, Some(httpResponse.json))
            case Failure(error: HttpException) =>
              sendSchemeUpdateEvent(psaId, validPensionsScheme, error.responseCode, None)
          }
        })
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
