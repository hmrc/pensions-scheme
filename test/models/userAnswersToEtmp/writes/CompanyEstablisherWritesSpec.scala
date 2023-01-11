/*
 * Copyright 2023 HM Revenue & Customs
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
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{Ignore, OptionValues}
import play.api.libs.json.{JsValue, Json}
import utils.{PensionSchemeGenerators, SchemaValidatorForTests}
@Ignore
class CompanyEstablisherWritesSpec extends AnyWordSpec with Matchers with OptionValues with PensionSchemeGenerators with SchemaValidatorForTests {

  "A company object" should {

    "map correctly to an update payload for company establisherDetails API 1468" when {

      "validate establisherDetails write with schema" in {
        forAll(companyEstablisherGen) {
          company => {

            val mappedCompany: JsValue = Json.toJson(company)(CompanyEstablisher.updateWrites)

            val valid = Json.obj("companyOrOrganisationDetails" -> Json.arr(mappedCompany))

            validateJson(elementToValidate = valid,
              schemaFileName = "api1468_schema.json",
              schemaNodePath = "#/properties/establisherAndTrustDetailsType/establisherDetails/companyOrOrganisationDetails").isSuccess mustBe true
          }
        }
      }

      "invalidate companyTrusteeDetails write with schema when" in {

        forAll(companyEstablisherGen) {
          company => {

            val invalidCompany = company.copy(utr = Some("adsasdasd"))

            val mappedCompany: JsValue = Json.toJson(invalidCompany)(CompanyEstablisher.updateWrites)

            val inValid = Json.obj("companyOrOrganisationDetails" -> Json.arr(mappedCompany))

            validateJson(elementToValidate = inValid,
              schemaFileName = "api1468_schema.json",
              schemaNodePath = "#/properties/establisherAndTrustDetailsType/establisherDetails/companyOrOrganisationDetails").isError mustBe true
          }
        }
      }
    }
  }
}
