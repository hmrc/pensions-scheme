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

package models

import org.scalatest.{MustMatchers, OptionValues, WordSpecLike}
import play.api.libs.json.{JsSuccess, Json}

class BankDetailsSpec extends MustMatchers with WordSpecLike with OptionValues {

  "BankDetails reads" should {

    "return true when sortCodeIsPresentOnEISCD returns 'yes'" in {
      val json = Json.obj("accountNumberWithSortCodeIsValid" -> true, "sortCodeIsPresentOnEISCD" -> "yes")

      Json.fromJson[ValidateBankDetailsResponse](json) mustEqual JsSuccess(ValidateBankDetailsResponse(true, true))
    }

    "return false when sortCodeIsPresentOnEISCD returns 'no'" in {
      val json = Json.obj("accountNumberWithSortCodeIsValid" -> true, "sortCodeIsPresentOnEISCD" -> "no")

      Json.fromJson[ValidateBankDetailsResponse](json) mustEqual JsSuccess(ValidateBankDetailsResponse(true, false))
    }

    "return true when sortCodeIsPresentOnEISCD returns 'error'" in {
      val json = Json.obj("accountNumberWithSortCodeIsValid" -> true, "sortCodeIsPresentOnEISCD" -> "error")

      Json.fromJson[ValidateBankDetailsResponse](json) mustEqual JsSuccess(ValidateBankDetailsResponse(true, false))
    }
  }
}
