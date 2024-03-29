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

package models.etmpToUserAnswers

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.libs.json._

class AddressTransformationSpec extends TransformationSpec {

  import AddressTransformationSpec._

  val addressTransformer = new AddressTransformer

  "if payload containing an address" when {
    "transformed using getAddress" must {
      "map correctly to the user answers address" in {
        forAll(addressJsValueGen()) {
          address => {
            val (ifAddress, userAnswersExpectedAddress) = address
            lazy val transformedJson = ifAddress.transform(addressTransformer.getAddress(__ \ Symbol("userAnswersAddress"), __ \ Symbol("ifAddress"))).asOpt.value

            transformedJson mustBe userAnswersExpectedAddress
          }
        }
      }
    }

    "transformed using getDifferent Address" must {
      "map correctly to the user answers address" in {
        forAll(addressJsValueGen(isDifferent = true)) {
          address => {
            val (ifAddress, userAnswersExpectedAddress) = address
            lazy val transformedJson = ifAddress.transform(addressTransformer.getDifferentAddress(__ \ Symbol("userAnswersAddress"), __ \ Symbol("ifAddress"))).asOpt.value

            transformedJson mustBe userAnswersExpectedAddress
          }
        }
      }
    }
  }

  "if payload containing previous address" when {
    "transformed using getAddressYears" must {
      "map correctly to user answers address years for under a year" in {
        lazy val transformedJson = ifPayload().transform(addressTransformer.
          getAddressYears(__ \ Symbol("addressYears"))).asOpt.value

        (transformedJson \ "addressYears").as[String] mustBe "under_a_year"
      }

      "map correctly to user answers address years for over a year" in {
        lazy val transformedJson = ifPayload(false).transform(addressTransformer.
          getAddressYears(__ \ Symbol("addressYears"))).asOpt.value

        (transformedJson \ "addressYears").as[String] mustBe "over_a_year"
      }
    }

    "transformed using getPreviousAddress" must {
      "map correctly to user answers previous address" in {
        lazy val transformedJson = ifPayload().transform(addressTransformer.
          getPreviousAddress(__ \ Symbol("previousAddress"))).asOpt.value

        (transformedJson \ "previousAddress" \ "addressLine1").as[String] mustBe "a1"
        (transformedJson \ "previousAddress" \ "addressLine2").as[String] mustBe "a2"
        (transformedJson \ "previousAddress" \ "addressLine3").as[String] mustBe "a3"
        (transformedJson \ "previousAddress" \ "addressLine4").as[String] mustBe "a4"
        (transformedJson \ "previousAddress" \ "postcode").as[String] mustBe "1234"
        (transformedJson \ "previousAddress" \ "country").as[String] mustBe "GB"
      }
    }
  }
}

object AddressTransformationSpec {

  def ifPayload(isPrevious: Boolean = true): JsObject = Json.obj(
    "previousAddressDetails" -> Json.obj(
      "isPreviousAddressLast12Month" -> isPrevious,
      "previousAddress" -> Json.obj(
        "line1" -> "a1",
        "line2" -> "a2",
        "line3" -> "a3",
        "line4" -> "a4",
        "postalCode" -> "1234",
        "countryCode" -> "GB"
      )
    )
  )
}
