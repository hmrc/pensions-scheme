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

package models.userAnswersToEtmp.writes

import models.userAnswersToEtmp.establisher.CompanyEstablisher
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.libs.json.{JsValue, Json}
import utils.{PensionSchemeGenerators, SchemaValidatorForTests}

class CompanyEstablisherWritesSpec extends AnyWordSpec with Matchers with OptionValues with PensionSchemeGenerators with SchemaValidatorForTests {

  private def jsonValidator(value: JsValue) = validateJson(elementToValidate =
    Json.obj("establisherAndTrustDetailsType" ->
      Json.obj("establisherDetails" ->
        value
      )
    ),
    schemaFileName = "api1468_schema.json",
    relevantProperties = Array("establisherAndTrustDetailsType"),
    relevantDefinitions = Some(Array(
      Array("establisherAndTrustDetailsType", "establisherDetails"),
      Array("establisherDetailsType", "companyOrOrganisationDetails"),
      Array("establisherCompanyOrOrgDetailsType"),
      Array("specialCharStringType"),
      Array("addressType"),
      Array("addressType"),
      Array("addressLineType"),
      Array("countryCodes"),
      Array("contactDetailsType")
    )))

  "A company object" should {

    "map correctly to an update payload for company establisherDetails API 1468" when {

      "validate establisherDetails write with schema" in {
        forAll(companyEstablisherGen) {
          company => {

            val mappedCompany: JsValue = Json.toJson(company)(CompanyEstablisher.updateWrites)

            val valid = Json.obj("companyOrOrganisationDetails" -> Json.arr(mappedCompany))

            jsonValidator(valid) mustBe Set()
          }
        }
      }

      "invalidate companyTrusteeDetails write with schema when" in {

        forAll(companyEstablisherGen) {
          company => {

            val invalidCompany = company.copy(utr = Some("adsasdasd"))

            val mappedCompany: JsValue = Json.toJson(invalidCompany)(CompanyEstablisher.updateWrites)

            val inValid = Json.obj("companyOrOrganisationDetails" -> Json.arr(mappedCompany))

            jsonValidator(inValid).isEmpty mustBe false

          }
        }
      }
    }
  }
}
