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

import models.userAnswersToEtmp.trustee.TrusteeDetails
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.libs.json.{JsValue, Json}
import utils.{PensionSchemeGenerators, SchemaValidatorForTests}


class TrusteeDetailsWritesSpec extends AnyWordSpec with Matchers with OptionValues with PensionSchemeGenerators with SchemaValidatorForTests {

  private def jsonValidator(value: JsValue) = validateJson(elementToValidate =
    Json.obj("establisherAndTrustDetailsType" -> value),
    schemaFileName = "api1468_schema.json",
    relevantProperties = Array("establisherAndTrustDetailsType"),
    relevantDefinitions = Some(Array(
      Array("establisherAndTrustDetailsType", "trusteeDetailsType"),
      Array("trusteeDetailsType"),
      Array("individualTrusteeDetailsType"),
      Array("companyTrusteeDetailsType"),
      Array("partnershipTrusteeDetailsType"),
      Array("specialCharStringType"),
      Array("addressType"),
      Array("addressType"),
      Array("addressLineType"),
      Array("countryCodes"),
      Array("contactDetailsType")
    )))

  "An trustee details object" should {

    "map correctly to an update payload for trusteeDetailsType API 1468" when {

      "validate trusteeDetails write with schema" in {
        forAll(trusteeDetailsGen) {
          trustee => {

            val mappedTrustee: JsValue = Json.toJson(trustee)(TrusteeDetails.updateWrites)

            val valid = Json.obj("trusteeDetailsType" -> mappedTrustee)

            jsonValidator(valid) mustBe Set()
          }
        }
      }

      "invalidate companyTrusteeDetails write with schema when" in {

        forAll(trusteeDetailsGen) {
          trustee => {

            val localTrustee = if (trustee.individualTrusteeDetail.nonEmpty) {
              trustee.copy(individualTrusteeDetail = Seq(trustee.individualTrusteeDetail(0).copy(utr = Some("12313123213123123123"))))
            } else if (trustee.companyTrusteeDetail.nonEmpty) {
              trustee.copy(companyTrusteeDetail = Seq(trustee.companyTrusteeDetail(0).copy(utr = Some("12313123213123123123"))))
            } else if (trustee.partnershipTrusteeDetail.nonEmpty) {
              trustee.copy(partnershipTrusteeDetail = Seq(trustee.partnershipTrusteeDetail(0).copy(utr = Some("12313123213123123123"))))
            } else trustee.copy(individualTrusteeDetail = Seq(individualGen.sample.get.copy(utr = Some("12313123213123123123"))))

            val mappedTrustee: JsValue = Json.toJson(localTrustee)(TrusteeDetails.updateWrites)

            val inValid = Json.obj("trusteeDetailsType" -> mappedTrustee)

            jsonValidator(inValid).isEmpty mustBe false
          }
        }
      }
    }
  }
}
