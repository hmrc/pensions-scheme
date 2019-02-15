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
import models.Partnership
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsValue, Json}
import utils.PensionSchemeGenerators

class PartnershipWritesSpec extends WordSpec with MustMatchers with OptionValues with PensionSchemeGenerators {

  val rootSchema = JsonSource.schemaFromUrl(getClass.getResource("/schemas/api1468_schema.json")).get

  val validator = SchemaValidator().addSchema("/schemas/api1468_schema.json", rootSchema)

  "A partnership object" should {
    "parse correctly to a valid DES format for variations api - API 1468" when {
      "we have a valid partnership" in {
        forAll(partnershipGen) {
          partnership => {
            val schema = JsonSource.schemaFromString(
              """{
                |  "additionalProperties": { "$ref": "/schemas/api1468_schema.json#/properties/establisherAndTrustDetailsType/establisherDetails/partnershipDetails" }
                |}""".stripMargin).get


            val mappedPartner: JsValue = Json.toJson(partnership)(Partnership.updateWrites)
            val testJsValue = Json.obj("partnershipDetails" -> Json.arr(mappedPartner))

            validator.validate(schema, testJsValue).isSuccess mustBe true
          }
        }
      }
    }

    "return errors when incoming data cannot be parsed to a valid DES format for variations api - API 1468" when {
      "we have an invalid partnership" in {
        forAll(partnershipGen) {
          partnership => {
            val invalidPartnership = partnership.copy(partnerDetails = Nil)

            val schema = JsonSource.schemaFromString(
              """{
                |  "additionalProperties": { "$ref": "/schemas/api1468_schema.json#/properties/establisherAndTrustDetailsType/establisherDetails/partnershipDetails" }
                |}""".stripMargin).get


            val mappedPartner: JsValue = Json.toJson(invalidPartnership)(Partnership.updateWrites)
            val testJsValue = Json.obj("partnershipDetails" -> Json.arr(mappedPartner))

            validator.validate(schema, testJsValue).isError mustBe true
          }
        }
      }
    }
  }
}
