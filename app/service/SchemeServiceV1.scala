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

import com.google.inject.Inject
import connector.{BarsConnector, SchemeConnector}
import models._
import models.PensionsScheme._
import models.ReadsEstablisherDetails.readsEstablisherDetails
import play.api.Logger
import play.api.libs.json.{JsResultException, JsValue, Json}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class SchemeServiceV1 @Inject()(schemeConnector: SchemeConnector, barsConnector: BarsConnector) extends SchemeService {

  def registerScheme(psaId: String, json: JsValue)(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {

    jsonToPensionsSchemeModel(json).fold(
      badRequestException => Future.failed(badRequestException),
      validPensionsScheme => {
        haveInvalidBank(json, validPensionsScheme).flatMap {
          pensionsScheme => schemeConnector.registerScheme(psaId, Json.toJson(pensionsScheme))
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

  def haveInvalidBank(json: JsValue, pensionsScheme: PensionsScheme)
                     (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[PensionsScheme] = {

    readBankAccount(json).fold(
      jsResultException => Future.failed(jsResultException),
      {
        case Some(bankAccount) => isBankAccountInvalid(bankAccount).map {
          case true => pensionSchemeHaveInvalidBank.set(pensionsScheme, true)
          case false => pensionsScheme
        }
        case None => Future.successful(pensionsScheme)
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

}
