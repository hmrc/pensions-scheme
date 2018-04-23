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

package models.Reads

import models.{Address, ForeignAddress, Samples, UkAddress}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._

class AddressReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples {
  "A JSON Payload with an address" should {
    "Map correctly to an Address type" when {
      "We have common elements of an address for a UK and NON UK address" when {
        val address = Json.obj("lines" -> JsArray(Seq(JsString("line1"),JsString("line2"))),
          "country" -> JsObject(Map("name" -> JsString("GB"))),
          "postcode" -> JsString("NE1"))

        "We have line 1 in the address" in {
          val result = address.as[(String,Option[String],Option[String],Option[String],String)](Address.commonAddressElementsReads)

          result._1 mustBe ukAddressSample.addressLine1
        }

        "We have line 2 in the address" in {
          val result = address.as[(String,Option[String],Option[String],Option[String],String)](Address.commonAddressElementsReads)

          result._2 mustBe ukAddressSample.addressLine2
        }

        "We have line 3 in the address" in {
          val input = Json.obj("lines" -> JsArray(Seq(JsString("line1"),JsString("line2"),JsString("line3"))),
            "country" -> JsObject(Map("name" -> JsString("GB"))),
            "postcode" -> JsString("Test"))
          val result = input.as[(String,Option[String],Option[String],Option[String],String)](Address.commonAddressElementsReads)

          result._3 mustBe ukAddressSample.addressLine3
        }

        "We have line 4 in the address" in {
          val input = Json.obj("lines" -> JsArray(Seq(JsString("line1"),JsString("line2"),JsString("line3"),JsString("line4"))),
            "country" -> JsObject(Map("name" -> JsString("GB"))),
            "postcode" -> JsString("Test"))
          val result = input.as[(String,Option[String],Option[String],Option[String],String)](Address.commonAddressElementsReads)

          result._4 mustBe ukAddressSample.addressLine4
        }

        "We have a countryCode" in {
          val result = address.as[(String,Option[String],Option[String],Option[String],String)](Address.commonAddressElementsReads)

          result._5 mustBe ukAddressSample.countryCode
        }
      }


      "We have a UK address" when {
        "We have a postCode" in {
          val input = Json.obj("lines" -> JsArray(Seq(JsString("line1"),JsString("line2"))),
            "country" -> JsObject(Map("name" -> JsString("GB"))),
            "postcode" -> JsString("NE1"))

          val result = input.as[UkAddress](UkAddress.apiReads)

          result.postalCode mustBe ukAddressSample.postalCode
        }
      }

      "We have a non UK address" when {
        val address = Json.obj("lines" -> JsArray(Seq(JsString("line1"),JsString("line2"))),
          "country" -> JsObject(Map("name" -> JsString("IT"))))

        "We have a postCode" in {
          val result = (address + ("postcode" -> JsString("NE1"))).as[ForeignAddress](ForeignAddress.apiReads)

          result.postalCode mustBe nonUkAddressSample.postalCode
        }


        "We don't have a postCode" in {
          val result = address.as[ForeignAddress](ForeignAddress.apiReads)

          result.postalCode mustBe None
        }
      }

      "We have a different address format" when {
        "we have a UK address" when {
          "with addressLine 1" in {
            val address = Json.obj("addressLine1" -> JsString("line1"), "addressLine2" -> JsString("line2"), "addressLine3" -> JsString("line3"), "addressLine4" -> JsString("line4"),
              "postalCode" -> JsString("NE1"), "countryCode" -> JsString("GB"))

            val result = address.as[UkAddress](UkAddress.apiReads)

            result.addressLine1 mustBe ukAddressSample.addressLine1
          }
        }
      }
    }
  }
}
