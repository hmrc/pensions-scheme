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

package models.Reads.schemes

import models.CorrespondenceAddress
import models.schemes.{AddressDetails, PreviousAddressDetails}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsBoolean, JsString, Json}

class AddressDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues {

  val addressDetails = Json.obj("nonUKAddress" -> JsBoolean(false), "line1" -> JsString("line1"), "line2" -> JsString("line2"), "line3" -> JsString("line3"), "line4" -> JsString("line4"),
    "postalCode" -> JsString("NE1"), "countryCode" -> JsString("GB"))

  val previousAddressDetails = Json.obj("isPreviousAddressLast12Month" -> JsBoolean(true), "previousAddress" -> addressDetails)

  "A JSON payload containing address details" when {

    val result = addressDetails.as(AddressDetails.apiReads)

    "with non uk flag 1" in {
      result.isNonUK mustBe (addressDetails \ "nonUKAddress").as[Boolean]
    }

    "with addressLine 1" in {
      result.addressLine1 mustBe (addressDetails \ "line1").as[String]
    }

    "with addressLine 2" in {
      result.addressLine2 mustBe (addressDetails \ "line2").as[String]
    }

    "with addressLine 3" in {
      result.addressLine3.value mustBe (addressDetails \ "line3").as[String]
    }

    "with no address line 3" in {
      val result = (addressDetails - "line3").as(AddressDetails.apiReads)

      result.addressLine3 mustBe None
    }

    "with addressLine 4" in {
      result.addressLine4.value mustBe (addressDetails \ "line4").as[String]
    }

    "with no address line 4" in {
      val result = (addressDetails - "line4").as(AddressDetails.apiReads)

      result.addressLine4 mustBe None
    }

    "with postalCode" in {
      result.postalCode.value mustBe (addressDetails \ "postalCode").as[String]
    }

    "with no postalCode" in {
      val result = (addressDetails - "postalCode").as(AddressDetails.apiReads)

      result.postalCode mustBe None
    }

    "with countryCode" in {
      result.countryCode mustBe (addressDetails \ "countryCode").as[String]
    }
  }

  "A Json payload containing previous address details" should {

    "read into a valid previous address details object" when {

      "we have a isPreviousAddressLast12Month" in {
        previousAddressDetails.as(PreviousAddressDetails.apiReads).isPreviousAddressLast12Month mustBe (previousAddressDetails \ "isPreviousAddressLast12Month").as[Boolean]
      }

      "we have a previousAddress" in {
        previousAddressDetails.as(PreviousAddressDetails.apiReads).previousAddress.value mustBe (previousAddressDetails \ "previousAddress").as(AddressDetails.apiReads)
      }

      "we don't have a previousAddress" in {
        val inputWithoutPreviousAddress = previousAddressDetails - "previousAddress"

        inputWithoutPreviousAddress.as(PreviousAddressDetails.apiReads).previousAddress mustBe None
      }
    }
  }
}
