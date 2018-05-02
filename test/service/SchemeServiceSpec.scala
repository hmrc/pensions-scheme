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

import base.SpecBase
import connector.BarsConnector
import models.{BankAccount, PensionsScheme}
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.mockito.Matchers.{eq => eqTo}
import org.scalatest.concurrent.ScalaFutures._
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SchemeServiceSpec extends SpecBase with MockitoSugar {
  import SchemeServiceSpec._

  "retrievePensionScheme" must {
    "return the pension scheme with invalid Bank flag as true if bank account is entered and is an invalid account" in {
      when(mockBarsConnector.invalidBankAccount(eqTo(BankAccount("001100", "111")))(any(), any())).thenReturn(
        Future.successful(true))

      val result = schemeService.retrievePensionScheme(validData)

      whenReady(result) { res =>
        val resData = validData.as[PensionsScheme]
        res mustBe resData.copy(customerAndSchemeDetails = resData.customerAndSchemeDetails.copy(haveInvalidBank = invalidAccount))
        res.customerAndSchemeDetails.haveInvalidBank mustEqual invalidAccount
      }
    }

    "return the pension scheme with invalid Bank flag as false" when {
      "bank account is entered and is not an invalid account" in {
        when(mockBarsConnector.invalidBankAccount(eqTo(BankAccount("001100", "111")))(any(), any())).thenReturn(
          Future.successful(false))

        val result = schemeService.retrievePensionScheme(validData)

        whenReady(result) { res =>
          res mustBe validData.as[PensionsScheme]
          res.customerAndSchemeDetails.haveInvalidBank mustEqual notInvalidAccount
        }
      }

      "there is no bank account entered" in {
        val validData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")

        val result = schemeService.retrievePensionScheme(validData)

        whenReady(result) { res =>
          res mustBe validData.as[PensionsScheme]
          res.customerAndSchemeDetails.haveInvalidBank mustEqual notInvalidAccount
        }
      }
    }
  }
}

object SchemeServiceSpec extends SpecBase with MockitoSugar {
  val mockBarsConnector = mock[BarsConnector]
  implicit val hc = HeaderCarrier()
  val schemeService = new SchemeService(mockBarsConnector)
  val invalidAccount = true
  val notInvalidAccount = false

  val validData = readJsonFromFile("/data/validSchemeRegistrationRequest.json").as[JsObject] + (
    "uKBankDetails" -> Json.obj(
      "bankName" -> "my bank name",
      "accountName" -> "my account name",
      "sortCode" -> Json.obj(
        "first" -> "00",
        "second" -> "11",
        "third" -> "00"
      ),
      "accountNumber" -> "111",
      "date" -> "2010-02-02"
    ))
}
