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

   "map correctly to an update payload for company TrusteeDetails API 1468" when {

      "validate companyTrusteeDetails write with schema" in {
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

            val valid = Json.obj("companyTrusteeDetailsType" -> Json.arr(mappedCompany))

            validator.validate(schema, valid).isSuccess mustBe true
          }
        }
      }

      "invalidate companyTrusteeDetails write with schema when json is invalid" in {

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

            val inValid = Json.obj("companyTrusteeDetailsType" -> Json.arr(mappedCompany))

            validator.validate(schema, inValid).isError mustBe true
          }
        }
      }
    }
  }
}
