/*
 * Copyright 2023 HM Revenue & Customs
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

package models

import models.userAnswersToEtmp.{BankAccount, ValidateBankDetailsResponse}
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsSuccess, Json}

class BankDetailsSpec extends Matchers with AnyWordSpecLike with OptionValues {

  "ValidateBankDetailsResponse reads" must {

    "return true for both sortCodeIsPresentOnEISCD and accountNumberWithSortCodeIsValid when both returns 'yes'" in {
      val json = Json.obj("accountNumberWithSortCodeIsValid" -> "yes", "sortCodeIsPresentOnEISCD" -> "yes")

      Json.fromJson[ValidateBankDetailsResponse](json) mustEqual JsSuccess(ValidateBankDetailsResponse(true, true))
    }

    "return false for sortCodeIsPresentOnEISCD and true for accountNumberWithSortCodeIsValid when" +
      "sortCodeIsPresentOnEISCD returns 'no' and accountNumberWithSortCodeIsValid  'yes'" in {
      val json = Json.obj("accountNumberWithSortCodeIsValid" -> "yes", "sortCodeIsPresentOnEISCD" -> "no")

      Json.fromJson[ValidateBankDetailsResponse](json) mustEqual JsSuccess(ValidateBankDetailsResponse(true, false))
    }

    "return true for accountNumberWithSortCodeIsValid and false for sortCodeIsPresentOnEISCD when " +
      " returns 'error' and accountNumberWithSortCodeIsValid return 'yes'" in {
      val json = Json.obj("accountNumberWithSortCodeIsValid" -> "yes", "sortCodeIsPresentOnEISCD" -> "error")

      Json.fromJson[ValidateBankDetailsResponse](json) mustEqual JsSuccess(ValidateBankDetailsResponse(true, false))
    }

    "return false for both sortCodeIsPresentOnEISCD and accountNumberWithSortCodeIsValid when both returns 'no'" in {
      val json = Json.obj("accountNumberWithSortCodeIsValid" -> "no", "sortCodeIsPresentOnEISCD" -> "no")

      Json.fromJson[ValidateBankDetailsResponse](json) mustEqual JsSuccess(ValidateBankDetailsResponse(false, false))
    }

    "return false for accountNumberWithSortCodeIsValid and true for sortCodeIsPresentOnEISCD when" +
      "accountNumberWithSortCodeIsValid returns 'no' and sortCodeIsPresentOnEISCD  'yes'" in {
      val json = Json.obj("accountNumberWithSortCodeIsValid" -> "no", "sortCodeIsPresentOnEISCD" -> "yes")

      Json.fromJson[ValidateBankDetailsResponse](json) mustEqual JsSuccess(ValidateBankDetailsResponse(false, true))
    }

    "return false for accountNumberWithSortCodeIsValid and false for sortCodeIsPresentOnEISCD when " +
      " sortCodeIsPresentOnEISCD returns 'error' and accountNumberWithSortCodeIsValid return 'indeterminate'" in {
      val json = Json.obj("accountNumberWithSortCodeIsValid" -> "indeterminate", "sortCodeIsPresentOnEISCD" -> "error")

      Json.fromJson[ValidateBankDetailsResponse](json) mustEqual JsSuccess(ValidateBankDetailsResponse(false, false))
    }
  }

  "Bank Account reads" must {
    val json = Json.obj(
      "sortCode" -> Json.obj(
        "first" -> "00",
        "second" -> "11",
        "third" -> "00"
      ),
      "accountNumber" -> "111"
    )
    "read the sort code successfully" in {
      json.as[BankAccount](BankAccount.apiReads).sortCode mustEqual "001100"
    }

    "read the account number successfully" in {
      json.as[BankAccount](BankAccount.apiReads).accountNumber mustEqual "111"
    }
  }
}
