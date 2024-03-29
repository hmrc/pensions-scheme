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

package models.userAnswersToEtmp.reads

import models._
import models.userAnswersToEtmp.{Address, CorrespondenceAddress, InternationalAddress, UkAddress}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json._

class AddressReadsSpec extends AnyWordSpec with Matchers with OptionValues with Samples {
  "A JSON Payload with an address" should {
    "Map correctly to an Address type" when {

      "we have an incoming address coming from if" when {
        val address = Json.obj("line1" -> JsString("line1"), "line2" -> JsString("line2"), "line3" -> JsString("line3"), "line4" -> JsString("line4"),
          "postalCode" -> JsString("NE1"), "countryCode" -> JsString("GB"))
        val result = address.as[CorrespondenceAddress]

        "with addressLine 1" in {
          result.addressLine1 mustBe (address \ "line1").as[String]
        }

        "with addressLine 2" in {
          result.addressLine2 mustBe (address \ "line2").as[String]
        }

        "with addressLine 3" in {
          result.addressLine3.value mustBe (address \ "line3").as[String]
        }

        "with no address line 3" in {
          val result = (address - "line3").as[CorrespondenceAddress]

          result.addressLine3 mustBe None
        }

        "with addressLine 4" in {
          result.addressLine4.value mustBe (address \ "line4").as[String]
        }

        "with no address line 4" in {
          val result = (address - "line4").as[CorrespondenceAddress]

          result.addressLine4 mustBe None
        }

        "with postalCode" in {
          result.postalCode.value mustBe (address \ "postalCode").as[String]
        }

        "with no postalCode" in {
          val result = (address - "postalCode").as[CorrespondenceAddress]

          result.postalCode mustBe None
        }

        "with countryCode" in {
          result.countryCode mustBe (address \ "countryCode").as[String]
        }
      }

      val address = Json.obj("addressLine1" -> JsString("line1"), "addressLine2" -> JsString("line2"), "addressLine3" -> JsString("line3"), "addressLine4" -> JsString("line4"),
        "postalCode" -> JsString("NE1"), "countryCode" -> JsString("GB"))

      "We have common address elements" when {
        "with addressLine 1" in {
          val result = address.as[(String, Option[String], Option[String], Option[String], String)](Address.commonAddressElementsReads)

          result._1 mustBe ukAddressSample.addressLine1
        }

        "with addressLine 2" in {
          val result = address.as[(String, Option[String], Option[String], Option[String], String)](Address.commonAddressElementsReads)

          result._2 mustBe ukAddressSample.addressLine2
        }

        "with addressLine 3" in {
          val result = address.as[(String, Option[String], Option[String], Option[String], String)](Address.commonAddressElementsReads)

          result._3 mustBe ukAddressSample.addressLine3
        }

        "with addressLine 4" in {
          val result = address.as[(String, Option[String], Option[String], Option[String], String)](Address.commonAddressElementsReads)

          result._4 mustBe ukAddressSample.addressLine4
        }

        "with countryCode" in {
          val result = address.as[(String, Option[String], Option[String], Option[String], String)](Address.commonAddressElementsReads)

          result._5 mustBe ukAddressSample.countryCode
        }

        "with a countryCode defined as `country`" in {
          val result = (address - "countryCode" + ("country" -> JsString("GB"))).as[(String, Option[String], Option[String], Option[String], String)](Address.commonAddressElementsReads)

          result._5 mustBe ukAddressSample.countryCode
        }
      }

      "we have a UK address" when {
        "with postal code" in {
          val result = address.as[Address]

          result.asInstanceOf[UkAddress].postalCode mustBe ukAddressSample.postalCode
        }

        "with postal code defined as `postcode`" in {
          val result = (address - "postalCode" + ("postcode" -> JsString("NE1"))).as[Address]

          result.asInstanceOf[UkAddress].postalCode mustBe ukAddressSample.postalCode
        }
      }

      "we have a non UK address" when {
        val address = Json.obj("addressLine1" -> JsString("line1"), "addressLine2" -> JsString("line2"), "addressLine3" -> JsString("line3"), "addressLine4" -> JsString("line4"), "countryCode" -> JsString("IT"))

        "with no postal code" in {
          val result = address.as[Address]

          result.asInstanceOf[InternationalAddress].postalCode mustBe None
        }

        "with postal code" in {
          val input = address + ("postalCode" -> JsString("NE1"))

          val result = input.as[Address]

          result.asInstanceOf[InternationalAddress].postalCode mustBe nonUkAddressSample.postalCode
        }

        "with postal code defined as `postcode`" in {
          val input = address + ("postcode" -> JsString("NE1"))

          val result = input.as[Address]

          result.asInstanceOf[InternationalAddress].postalCode mustBe nonUkAddressSample.postalCode
        }

        "with territory defined as country code" in {
          val input = address + ("countryCode" -> JsString("territory:IT"))

          val result = input.as[Address]

          result.asInstanceOf[InternationalAddress].countryCode mustBe nonUkAddressSample.countryCode
        }

        "with territory defined as country code with leading space" in {
          val input = address + ("countryCode" -> JsString("territory: IT"))

          val result = input.as[Address]

          result.asInstanceOf[InternationalAddress].countryCode mustBe nonUkAddressSample.countryCode
        }
      }
    }
  }
}
