/*
 * Copyright 2024 HM Revenue & Customs
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

import audit.{SchemeAuditService, AuditService}
import com.google.inject.Inject
import connector.{BarsConnector, SchemeConnector}
import models.ListOfSchemes
import models.userAnswersToEtmp.PensionsScheme.pensionSchemeHaveInvalidBank
import models.userAnswersToEtmp.{RACDACPensionsScheme, BankAccount, PensionsScheme}
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{BadRequestException, HttpException, HeaderCarrier}
import utils.HttpResponseHelper
import utils.ValidationUtils.genResponse

import scala.concurrent.{ExecutionContext, Future}


class SchemeServiceImpl @Inject()(
                                   schemeConnector: SchemeConnector,
                                   barsConnector: BarsConnector,
                                   auditService: AuditService,
                                   schemeAuditService: SchemeAuditService
                                 ) extends SchemeService with HttpResponseHelper {

  private val logger = Logger(classOf[SchemeServiceImpl])

  override def listOfSchemes(idType: String, idValue: String)
                            (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader):
  Future[Either[HttpException, JsValue]]= {
    schemeConnector.listOfSchemes(idType, idValue)(headerCarrier, implicitly, implicitly)
  }


  case object RegisterSchemeToggleOffTransformFailed extends Exception

  private def registerNonRACDACScheme(json: JsValue, psaId: String)
                                     (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader):
  Future[Either[HttpException, JsValue]] = {
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
              val registerData = Json.toJson(pensionsScheme).as[JsObject]
              schemeConnector.registerScheme(psaId, registerData) andThen {
                schemeAuditService.sendSchemeSubscriptionEvent(psaId, pensionsScheme, bankAccount.isDefined)(auditService.sendEvent)
              }
          }
        )
      }
    )
  }

  private def registerRACDACScheme(json: JsValue, psaId: String)(implicit
                                                                 headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader):
  Future[Either[HttpException, JsValue]] = {
    json.validate[RACDACPensionsScheme](RACDACPensionsScheme.reads).fold(
      invalid = {
        errors =>
          val ex = JsResultException(errors)
          logger.warn("Invalid RAC/DAC pension scheme", ex)
          Future.failed(new BadRequestException("Invalid RAC/DAC pension scheme"))
      },
      valid = { validRACDACPensionsScheme =>
        val registerData = Json.toJson(validRACDACPensionsScheme)
        schemeConnector.registerScheme(psaId, registerData) andThen {
          schemeAuditService.sendRACDACSchemeSubscriptionEvent(psaId, registerData)(auditService.sendExtendedEvent)
        }
      }
    )
  }

  override def registerScheme(psaId: String, json: JsValue)
                             (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader):
  Future[Either[HttpException, JsValue]] = {
    def isRACDACSchemeDeclaration = (json \ "racdac" \ "declaration").toOption.exists(_.as[Boolean])
    if (isRACDACSchemeDeclaration) {
      registerRACDACScheme(json, psaId)
    } else {
      registerNonRACDACScheme(json, psaId)
    }
  }

  override def updateScheme(pstr: String, psaId: String, json: JsValue)(implicit headerCarrier: HeaderCarrier,
                                                                        ec: ExecutionContext, request: RequestHeader):
  Future[Either[HttpException, JsValue]] = {
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
        schemeConnector.updateSchemeDetails(pstr, updatedScheme) andThen
          schemeAuditService.sendSchemeUpdateEvent(psaId, validPensionsScheme)(auditService.sendEvent)
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

  override def getPstrFromSrn(srn: String, idType: String, idValue: String)
                             (implicit headerCarrier: HeaderCarrier,
                              ec: ExecutionContext, request: RequestHeader): Future[String] =
    listOfSchemes(idType, idValue).map {
      case Right(listOfSchemesJsValue) => listOfSchemesJsValue.convertTo[ListOfSchemes].schemeDetails.flatMap { listOfSchemes =>
        listOfSchemes.find(_.referenceNumber == srn).flatMap(_.pstr)
      }.getOrElse(throw notFoundPstrException(idValue, srn))
      case Left(e: Exception) => throw pstrException(e.getMessage, idValue)
    }

  def pstrException(msg: String, idValue: String): BadRequestException = new BadRequestException(
    s"Could not retrieved schemes for PSTR:  $idValue, error message: $msg");
  def notFoundPstrException(idValue: String, srn: String): BadRequestException = new BadRequestException(s"Schemes not found for PSTR: $idValue and srn: $srn");
}
