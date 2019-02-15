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
import models.CompanyTrustee
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsValue, Json}
import utils.PensionSchemeGenerators

class CompanyTrusteeWritesSpec extends WordSpec with MustMatchers with OptionValues with PensionSchemeGenerators {

  "A company object" should {

    "validate additionalProperties schema constraint via" in {

      val talkSchema = JsonSource.schemaFromUrl(getClass.getResource("/talk.json")).get

      val validator = SchemaValidator().addSchema("/talk.json", talkSchema)

      val schema = JsonSource.schemaFromString(
        """{
          |  "additionalProperties": { "$ref": "/talk.json#/properties/date/month" }
          |}""".stripMargin).get

      // min length 10, max length 20
//      val valid = Json.obj("title" -> "This is valid")
//      val invalid = Json.obj("title" -> "Too short")
//      val valid = Json.obj("date" -> Json.obj("year" -> "1111", "month" -> 11, "day" -> 2))
//      val invalid = Json.obj("date" -> Json.obj("year" -> 999, "month" -> 13, "day" -> 2))
      val valid = Json.obj("month" -> 11)
      val invalid = Json.obj("month" -> "13")

      println("############# v.validate(schema, valid) : " + validator.validate(schema, valid).asEither)
      println("############# v.validate(schema, invalid) : " + validator.validate(schema, invalid).asEither)

      validator.validate(schema, valid).isSuccess mustBe true
      validator.validate(schema, invalid).isError mustBe true
    }


   "map correctly to an update payload for API 1468" when {

      "validate company write converted json with schema" in {
        forAll(companyTrusteeGen) {
          company => {

            val rootSchema = JsonSource.schemaFromUrl(getClass.getResource("/schemas/api1468_schema.json")).get

            val validator = SchemaValidator().addSchema("/schemas/api1468_schema.json", rootSchema)

            val schema = JsonSource.schemaFromString(
              """{
                |  "additionalProperties": {
                |  "$ref": "/schemas/api1468_schema.json#/properties/establisherAndTrustDetailsType/trusteeDetailsType/companyTrusteeDetailsType" }
                |}""".stripMargin).get

            val mappedCompany: JsValue = Json.toJson(company)(CompanyTrustee.updateWrites)

            validator.validate(schema, mappedCompany).isSuccess mustBe true
          }
        }
      }

      "invalidate company write converted json with schema" in {

        forAll(companyTrusteeGen) {
          company => {

            val invalidCompany = company.copy(utr = Some("adsasdasd"))

            val mappedCompany: JsValue = Json.toJson(invalidCompany)(CompanyTrustee.updateWrites)

            val rootSchema = JsonSource.schemaFromUrl(getClass.getResource("/schemas/api1468_schema.json")).get

            val validator = SchemaValidator().addSchema("/schemas/api1468_schema.json", rootSchema)

            val schema = JsonSource.schemaFromString(
              """{
                |  "additionalProperties": {
                |  "$ref": "/schemas/api1468_schema.json#/properties/establisherAndTrustDetailsType/trusteeDetailsType/companyTrusteeDetailsType" }
                |}""".stripMargin).get

            validator.validate(schema, mappedCompany).isError mustBe true
          }
        }
      }

      "validate company write with schema" in {
        forAll(companyTrusteeGen) {
          company => {

            val rootSchema = JsonSource.schemaFromUrl(getClass.getResource("/schemas/api1468_schema.json")).get

            val validator = SchemaValidator().addSchema("/schemas/api1468_schema.json", rootSchema)

            val schema = JsonSource.schemaFromString(
              """{
                |  "additionalProperties": {
                |  "$ref": "/schemas/api1468_schema.json#/properties/establisherAndTrustDetailsType/trusteeDetailsType/companyTrusteeDetailsType" }
                |}""".stripMargin).get

            val result = validator.validate(schema, company, CompanyTrustee.updateWrites)

            result.isSuccess mustBe true
          }
        }
      }
    }
  }
}
