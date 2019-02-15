/*
 * Copyright 2019 HM Revenue & Customs
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

package models.Writes

import com.eclipsesource.schema.{JsonSource, SchemaValidator}
import models.Address
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsValue, Json}
import utils.PensionSchemeGenerators

import scala.util.Random

class AddressWritesSpec extends WordSpec with MustMatchers with OptionValues with PensionSchemeGenerators {

  val rootSchema = JsonSource.schemaFromUrl(getClass.getResource("/schemas/api1468_schema.json")).get

  val validator = SchemaValidator().addSchema("/schemas/api1468_schema.json", rootSchema)

  "An updated address" should {
    "parse correctly to a valid DES format for variations api - API 1468" when {
      "we have a valid UK address" in {
        forAll(ukAddressGen) {
          address => {
            val schema = JsonSource.schemaFromString(
              """{
                |  "additionalProperties": { "$ref": "/schemas/api1468_schema.json#/properties/schemeDetails/insuranceCompanyDetails/insuranceCompanyAddressDetails" }
                |}""".stripMargin).get


            val mappedAddress: JsValue = Json.toJson(address)(Address.updateWrites)
            val testJsValue = Json.obj("insuranceCompanyAddressDetails" -> mappedAddress)

            validator.validate(schema, testJsValue).isSuccess mustBe true
          }
        }
      }

      "we have a valid international address" in {
        forAll(internationalAddressGen) {
          address => {
            val schema = JsonSource.schemaFromString(
              """{
                |  "additionalProperties": { "$ref": "/schemas/api1468_schema.json#/properties/schemeDetails/insuranceCompanyDetails/insuranceCompanyAddressDetails" }
                |}""".stripMargin).get


            val mappedAddress: JsValue = Json.toJson(address)(Address.updateWrites)
            val testJsValue = Json.obj("insuranceCompanyAddressDetails" -> mappedAddress)

            validator.validate(schema, testJsValue).isSuccess mustBe true
          }
        }
      }
    }

    "return errors when incoming data cannot be parsed to a valid DES format for variations api - API 1468" when {
      "we have an invalid UK address" in {
        forAll(ukAddressGen) {
          address => {
            val invalidAddress = address.copy(addressLine1 = Random.alphanumeric.take(40).mkString)
            val schema = JsonSource.schemaFromString(
              """{
                |  "additionalProperties": { "$ref": "/schemas/api1468_schema.json#/properties/schemeDetails/insuranceCompanyDetails/insuranceCompanyAddressDetails" }
                |}""".stripMargin).get


            val mappedAddress: JsValue = Json.toJson(invalidAddress)(Address.updateWrites)
            val testJsValue = Json.obj("insuranceCompanyAddressDetails" -> mappedAddress)

            validator.validate(schema, testJsValue).isError mustBe true
          }
        }
      }

      "we have an invalid international address" in {
        forAll(internationalAddressGen) {
          address => {
            val invalidAddress = address.copy(addressLine1 = Random.alphanumeric.take(40).mkString)
            val schema = JsonSource.schemaFromString(
              """{
                |  "additionalProperties": { "$ref": "/schemas/api1468_schema.json#/properties/schemeDetails/insuranceCompanyDetails/insuranceCompanyAddressDetails" }
                |}""".stripMargin).get


            val mappedAddress: JsValue = Json.toJson(invalidAddress)(Address.updateWrites)
            val testJsValue = Json.obj("insuranceCompanyAddressDetails" -> mappedAddress)

            validator.validate(schema, testJsValue).isError mustBe true
          }
        }
      }
    }
  }

}
