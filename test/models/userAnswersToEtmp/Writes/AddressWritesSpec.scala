/*
 * Copyright 2020 HM Revenue & Customs
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

package models.userAnswersToEtmp.Writes

import models.userAnswersToEtmp.Address
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsValue, Json}
import utils.{PensionSchemeGenerators, SchemaValidatorForTests}

import scala.util.Random

class AddressWritesSpec extends WordSpec with MustMatchers with OptionValues with PensionSchemeGenerators with SchemaValidatorForTests {

  "An updated address" should {
    "parse correctly to a valid DES format for variations api - API 1468" when {
      "we have a valid UK address" in {
        forAll(ukAddressGen) {
          address => {
            val mappedAddress: JsValue = Json.toJson(address)(Address.updateWrites)
            val testJsValue = Json.obj("insuranceCompanyAddressDetails" -> mappedAddress)

            validateJson(elementToValidate = testJsValue,
              schemaFileName = "api1468_schema.json",
              schemaNodePath = "#/properties/schemeDetails/insuranceCompanyDetails/insuranceCompanyAddressDetails").isSuccess mustBe true
          }
        }
      }

      "we have a valid international address" in {
        forAll(internationalAddressGen) {
          address => {

            val mappedAddress: JsValue = Json.toJson(address)(Address.updateWrites)
            val testJsValue = Json.obj("insuranceCompanyAddressDetails" -> mappedAddress)

            validateJson(elementToValidate = testJsValue,
              schemaFileName = "api1468_schema.json",
              schemaNodePath = "#/properties/schemeDetails/insuranceCompanyDetails/insuranceCompanyAddressDetails").isSuccess mustBe true
          }
        }
      }
    }

    "return errors when incoming data cannot be parsed to a valid DES format for variations api - API 1468" when {
      "we have an invalid UK address" in {
        forAll(ukAddressGen) {
          address => {
            val invalidAddress = address.copy(addressLine1 = Random.alphanumeric.take(40).mkString)

            val mappedAddress: JsValue = Json.toJson(invalidAddress)(Address.updateWrites)
            val testJsValue = Json.obj("insuranceCompanyAddressDetails" -> mappedAddress)

            validateJson(elementToValidate = testJsValue,
              schemaFileName = "api1468_schema.json",
              schemaNodePath = "#/properties/schemeDetails/insuranceCompanyDetails/insuranceCompanyAddressDetails").isError mustBe true
          }
        }
      }

      "we have an invalid international address" in {
        forAll(internationalAddressGen) {
          address => {
            val invalidAddress = address.copy(addressLine1 = Random.alphanumeric.take(40).mkString)

            val mappedAddress: JsValue = Json.toJson(invalidAddress)(Address.updateWrites)
            val testJsValue = Json.obj("insuranceCompanyAddressDetails" -> mappedAddress)

            validateJson(elementToValidate = testJsValue,
              schemaFileName = "api1468_schema.json",
              schemaNodePath = "#/properties/schemeDetails/insuranceCompanyDetails/insuranceCompanyAddressDetails").isError mustBe true
          }
        }
      }
    }
  }

}
