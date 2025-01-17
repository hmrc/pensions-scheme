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

import models.userAnswersToEtmp.Individual
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.libs.json.{JsValue, Json}
import utils.{PensionSchemeGenerators, SchemaValidatorForTests}



class IndividualWritesSpec extends AnyWordSpec with Matchers with OptionValues with PensionSchemeGenerators with SchemaValidatorForTests {


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
      Array("establisherDetailsType", "individualDetails"),
      Array("establisherIndividualDetailsType"),
      Array("specialCharStringType"),
      Array("addressType"),
      Array("addressType"),
      Array("addressLineType"),
      Array("countryCodes"),
      Array("contactDetailsType")
    )))

  private def trusteeJsonValidator(value: JsValue) = validateJson(elementToValidate =
    Json.obj("establisherAndTrustDetailsType" ->
      Json.obj("trusteeDetailsType" ->
        value
      )
    ),
    schemaFileName = "api1468_schema.json",
    relevantProperties = Array("establisherAndTrustDetailsType"),
    relevantDefinitions = Some(Array(
      Array("establisherAndTrustDetailsType", "trusteeDetailsType"),
      Array("trusteeDetailsType", "individualDetails"),
      Array("individualTrusteeDetailsType"),
      Array("specialCharStringType"),
      Array("addressType"),
      Array("addressType"),
      Array("addressLineType"),
      Array("countryCodes"),
      Array("contactDetailsType")
    )))

  "An Individual object" should {

    "map correctly to an update payload for API 1468" when {

      "validate individualDetails for establisherDetails write with schema" in {
        forAll(individualGen) {
          individual => {

            val mappedIndividual: JsValue = Json.toJson(individual)(Individual.individualUpdateWrites)

            val valid = Json.obj("individualDetails" -> Json.arr(mappedIndividual))

            jsonValidator(valid) mustBe Set()

//            validateJson(elementToValidate = valid,
//              schemaFileName = "api1468_schema.json",
//              schemaNodePath = "#/properties/establisherAndTrustDetailsType/establisherDetails/individualDetails").isSuccess mustBe true
          }
        }
      }

      "invalidate individualDetails for establisherDetails write with schema when json is invalid" in {

        forAll(individualGen) {
          individual => {

            val invalidCompany = individual.copy(utr = Some("adsasdasd"))

            val mappedIndividual: JsValue = Json.toJson(invalidCompany)(Individual.individualUpdateWrites)

            val inValid = Json.obj("individualDetails" -> Json.arr(mappedIndividual))

            jsonValidator(inValid).isEmpty mustBe false
          }
        }
      }


      "validate individualDetails for trusteeDetailsType write with schema" in {
        forAll(individualGen) {
          individual => {

            val mappedIndividual: JsValue = Json.toJson(individual)(Individual.individualUpdateWrites)

            val valid = Json.obj("individualDetails" -> Json.arr(mappedIndividual))
            trusteeJsonValidator(valid) mustBe Set()
          }
        }
      }

      "invalidate individualDetails for trusteeDetailsType write with schema when json is invalid" in {

        forAll(individualGen) {
          individual => {

            val invalidCompany = individual.copy(utr = Some("adsasdasd"))

            val mappedIndividual: JsValue = Json.toJson(invalidCompany)(Individual.individualUpdateWrites)

            val inValid = Json.obj("individualDetails" -> Json.arr(mappedIndividual))

            trusteeJsonValidator(inValid).isEmpty mustBe false
          }
        }
      }
    }
  }
}
