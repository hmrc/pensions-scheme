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

import models.userAnswersToEtmp.establisher.Partnership
import models.userAnswersToEtmp.trustee.PartnershipTrustee
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{Ignore, OptionValues}
import play.api.libs.json.{JsValue, Json}
import utils.{PensionSchemeGenerators, SchemaValidatorForTests}
@Ignore
class PartnershipWritesSpec extends AnyWordSpec with Matchers with OptionValues with PensionSchemeGenerators with SchemaValidatorForTests {

  "An establisher partnership object" should {

    "parse correctly to a valid if format for variations api - API 1468" when {
      "we have a valid partnership" in {
        forAll(partnershipGen) {
          partnership => {

            val mappedPartner: JsValue = Json.toJson(partnership)(Partnership.updateWrites)
            val testJsValue = Json.obj("partnershipDetails" -> Json.arr(mappedPartner))

            validateJson(elementToValidate = testJsValue,
              schemaFileName = "api1468_schema.json",
              schemaNodePath = "#/properties/establisherAndTrustDetailsType/establisherDetails/partnershipDetails").isSuccess mustBe true
          }
        }
      }
    }

    "return errors when incoming data cannot be parsed to a valid if format for variations api - API 1468" when {
      "we have an invalid partnership" in {
        forAll(partnershipGen) {
          partnership => {
            val invalidPartnership = partnership.copy(partnerDetails = Nil)

            val mappedPartner: JsValue = Json.toJson(invalidPartnership)(Partnership.updateWrites)
            val testJsValue = Json.obj("partnershipDetails" -> Json.arr(mappedPartner))

            validateJson(elementToValidate = testJsValue,
              schemaFileName = "api1468_schema.json",
              schemaNodePath = "#/properties/establisherAndTrustDetailsType/establisherDetails/partnershipDetails").isError mustBe true
          }
        }
      }
    }
  }

  "A trustee partnership object" should {
    "parse correctly to a valid If format for variations api - API 1468" when {
      "we have a valid trustee partnership" in {
        forAll(partnershipTrusteeGen) {
          partnership => {

            val mappedPartner: JsValue = Json.toJson(partnership)(PartnershipTrustee.updateWrites)
            val testJsValue = Json.obj("partnershipTrusteeDetails" -> Json.arr(mappedPartner))

            validateJson(elementToValidate = testJsValue,
              schemaFileName = "api1468_schema.json",
              schemaNodePath = "#/properties/establisherAndTrustDetailsType/trusteeDetailsType/partnershipTrusteeDetails").isSuccess mustBe true
          }
        }
      }
    }

    "return errors when incoming data cannot be parsed to a valid If format for variations api - API 1468" when {
      "we have an invalid trustee partnership" in {
        forAll(partnershipTrusteeGen) {
          partnership => {
            val invalidPartnership = partnership.copy(utr = Some("invalid utr"))

            val mappedPartner: JsValue = Json.toJson(invalidPartnership)(PartnershipTrustee.updateWrites)
            val testJsValue = Json.obj("partnershipTrusteeDetails" -> Json.arr(mappedPartner))

            validateJson(elementToValidate = testJsValue,
              schemaFileName = "api1468_schema.json",
              schemaNodePath = "#/properties/establisherAndTrustDetailsType/trusteeDetailsType/partnershipTrusteeDetails").isError mustBe true
          }
        }
      }
    }
  }
}
