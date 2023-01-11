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

import models.userAnswersToEtmp.Individual
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{Ignore, OptionValues}
import play.api.libs.json.{JsValue, Json}
import utils.{PensionSchemeGenerators, SchemaValidatorForTests}


@Ignore
class IndividualWritesSpec extends AnyWordSpec with Matchers with OptionValues with PensionSchemeGenerators with SchemaValidatorForTests {

  "An Individual object" should {

    "map correctly to an update payload for API 1468" when {

      "validate individualDetails for establisherDetails write with schema" in {
        forAll(individualGen) {
          individual => {

            val mappedIndividual: JsValue = Json.toJson(individual)(Individual.individualUpdateWrites)

            val valid = Json.obj("individualDetails" -> Json.arr(mappedIndividual))

            validateJson(elementToValidate = valid,
              schemaFileName = "api1468_schema.json",
              schemaNodePath = "#/properties/establisherAndTrustDetailsType/establisherDetails/individualDetails").isSuccess mustBe true
          }
        }
      }

      "invalidate individualDetails for establisherDetails write with schema when json is invalid" in {

        forAll(individualGen) {
          individual => {

            val invalidCompany = individual.copy(utr = Some("adsasdasd"))

            val mappedIndividual: JsValue = Json.toJson(invalidCompany)(Individual.individualUpdateWrites)

            val inValid = Json.obj("individualDetails" -> Json.arr(mappedIndividual))

            validateJson(elementToValidate = inValid,
              schemaFileName = "api1468_schema.json",
              schemaNodePath = "#/properties/establisherAndTrustDetailsType/establisherDetails/individualDetails").isError mustBe true
          }
        }
      }


      "validate individualDetails for trusteeDetailsType write with schema" in {
        forAll(individualGen) {
          individual => {

            val mappedIndividual: JsValue = Json.toJson(individual)(Individual.individualUpdateWrites)

            val valid = Json.obj("individualDetails" -> Json.arr(mappedIndividual))

            validateJson(elementToValidate = valid,
              schemaFileName = "api1468_schema.json",
              schemaNodePath = "#/properties/establisherAndTrustDetailsType/trusteeDetailsType/individualDetails").isSuccess mustBe true
          }
        }
      }

      "invalidate individualDetails for trusteeDetailsType write with schema when json is invalid" in {

        forAll(individualGen) {
          individual => {

            val invalidCompany = individual.copy(utr = Some("adsasdasd"))

            val mappedIndividual: JsValue = Json.toJson(invalidCompany)(Individual.individualUpdateWrites)

            val inValid = Json.obj("individualDetails" -> Json.arr(mappedIndividual))

            validateJson(elementToValidate = inValid,
              schemaFileName = "api1468_schema.json",
              schemaNodePath = "#/properties/establisherAndTrustDetailsType/trusteeDetailsType/individualDetails").isError mustBe true
          }
        }
      }
    }
  }
}
