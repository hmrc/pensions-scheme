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

import models.{Address, ForeignAddress, UkAddress}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._

class AddressReadsSpec extends WordSpec with MustMatchers with OptionValues {
  "A JSON Payload with an address" should {
    "Map correctly to an Address type" when {
      "We have common elements of an address for a UK and NON UK address" when {
        val input = Json.obj("lines" -> JsArray(Seq(JsString("line1"),JsString("line2"))),
          "country" -> JsObject(Map("name" -> JsString("GB"))),
          "postcode" -> JsString("Test"))

        "We have line 1 in the address" in {
          val result = input.as[(String,Option[String],Option[String],Option[String],String)](Address.commonAddressElementsReads)

          result._1 mustBe "line1"
        }

        "We have line 2 in the address" in {
          val result = input.as[(String,Option[String],Option[String],Option[String],String)](Address.commonAddressElementsReads)

          result._2.value mustBe "line2"
        }

        "We have line 3 in the address" in {
          val input = Json.obj("lines" -> JsArray(Seq(JsString("line1"),JsString("line2"),JsString("line3"))),
            "country" -> JsObject(Map("name" -> JsString("GB"))),
            "postcode" -> JsString("Test"))
          val result = input.as[(String,Option[String],Option[String],Option[String],String)](Address.commonAddressElementsReads)

          result._3.value mustBe "line3"
        }

        "We have line 4 in the address" in {
          val input = Json.obj("lines" -> JsArray(Seq(JsString("line1"),JsString("line2"),JsString("line3"),JsString("line4"))),
            "country" -> JsObject(Map("name" -> JsString("GB"))),
            "postcode" -> JsString("Test"))
          val result = input.as[(String,Option[String],Option[String],Option[String],String)](Address.commonAddressElementsReads)

          result._4.value mustBe "line4"
        }

        "We have a countryCode" in {
          val result = input.as[(String,Option[String],Option[String],Option[String],String)](Address.commonAddressElementsReads)

          result._5 mustBe "GB"
        }
      }


      "We have a UK address" when {
        "We have a postCode" in {
          val input = Json.obj("lines" -> JsArray(Seq(JsString("line1"),JsString("line2"))),
            "country" -> JsObject(Map("name" -> JsString("GB"))),
            "postcode" -> JsString("Test"))

          val result = input.as[UkAddress](UkAddress.apiReads)

          result.postalCode mustBe "Test"
        }
      }

      "We have a non UK address" when {
        val address = Json.obj("lines" -> JsArray(Seq(JsString("line1"),JsString("line2"))),
          "country" -> JsObject(Map("name" -> JsString("IT"))))

        "We have a postCode" in {
          val result = (address + ("postcode" -> JsString("Test"))).as[ForeignAddress](ForeignAddress.apiReads)

          result.postalCode.value mustBe "Test"
        }


        "We don't have a postCode" in {
          val result = address.as[ForeignAddress](ForeignAddress.apiReads)

          result.postalCode mustBe None
        }
      }
    }
  }
}
