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
import models.Individual
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsValue, Json}
import utils.{PensionSchemeGenerators, SchemaValidatorForTests}



class IndividualWritesSpec extends WordSpec with MustMatchers with OptionValues with PensionSchemeGenerators {

  val schemaValidator = SchemaValidatorForTests()

  "An Individual object" should {

    "map correctly to an update payload for API 1468" when {

      "validate individualDetails for establisherDetails write with schema" in {
        forAll(individualGen) {
          individual => {

            val rootSchema = JsonSource.schemaFromUrl(getClass.getResource("/schemas/api1468_schema.json")).get

            val validator = SchemaValidator().addSchema("/schemas/api1468_schema.json", rootSchema)

            val schema = JsonSource.schemaFromString(
              """{
                |  "additionalProperties": {
                |  "$ref": "/schemas/api1468_schema.json#/properties/establisherAndTrustDetailsType/establisherDetails/individualDetails" }
                |}""".stripMargin).get

            val mappedIndividual: JsValue = Json.toJson(individual)(Individual.establisherIndividualDetailsUpdateWrites)

            val valid = Json.obj("individualDetails" -> Json.arr(mappedIndividual))

            validator.validate(schema, valid).isSuccess mustBe true
          }
        }
      }

      "invalidate individualDetails for establisherDetails write with schema when json is invalid" in {

        forAll(individualGen) {
          individual => {

            val invalidCompany = individual.copy(utr = Some("adsasdasd"))

            val mappedIndividual: JsValue = Json.toJson(invalidCompany)(Individual.individualTrusteeDetailsUpdateWrites)

            val rootSchema = JsonSource.schemaFromUrl(getClass.getResource("/schemas/api1468_schema.json")).get

            val validator = SchemaValidator().addSchema("/schemas/api1468_schema.json", rootSchema)

            val schema = JsonSource.schemaFromString(
              """{
                |  "additionalProperties": {
                |  "$ref": "/schemas/api1468_schema.json#/properties/establisherAndTrustDetailsType/establisherDetails/individualDetails" }
                |}""".stripMargin).get

            val inValid = Json.obj("individualDetails" -> Json.arr(mappedIndividual))

            validator.validate(schema, inValid).isError mustBe true
          }
        }
      }


      "validate individualDetails for trusteeDetailsType write with schema" in {
        forAll(individualGen) {
          individual => {

            val rootSchema = JsonSource.schemaFromUrl(getClass.getResource("/schemas/api1468_schema.json")).get

            val validator = SchemaValidator().addSchema("/schemas/api1468_schema.json", rootSchema)

            val schema = JsonSource.schemaFromString(
              """{
                |  "additionalProperties": {
                |  "$ref": "/schemas/api1468_schema.json#/properties/establisherAndTrustDetailsType/trusteeDetailsType/individualDetails" }
                |}""".stripMargin).get

            val mappedIndividual: JsValue = Json.toJson(individual)(Individual.individualTrusteeDetailsUpdateWrites)

            val valid = Json.obj("individualDetails" -> Json.arr(mappedIndividual))

            validator.validate(schema, valid).isSuccess mustBe true
          }
        }
      }

      "invalidate individualDetails for trusteeDetailsType write with schema when json is invalid" in {

        forAll(individualGen) {
          individual => {

            val invalidCompany = individual.copy(utr = Some("adsasdasd"))

            val mappedIndividual: JsValue = Json.toJson(invalidCompany)(Individual.individualTrusteeDetailsUpdateWrites)

            val rootSchema = JsonSource.schemaFromUrl(getClass.getResource("/schemas/api1468_schema.json")).get

            val validator = SchemaValidator().addSchema("/schemas/api1468_schema.json", rootSchema)

            val schema = JsonSource.schemaFromString(
              """{
                |  "additionalProperties": {
                |  "$ref": "/schemas/api1468_schema.json#/properties/establisherAndTrustDetailsType/trusteeDetailsType/individualDetails" }
                |}""".stripMargin).get

            val inValid = Json.obj("individualDetails" -> Json.arr(mappedIndividual))

            validator.validate(schema, inValid).isError mustBe true
          }
        }
      }
    }
  }
}
