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

import models.userAnswersToEtmp.establisher.EstablisherDetails
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.libs.json.{JsValue, Json}
import utils.{PensionSchemeGenerators, SchemaValidatorForTests}

class EstablisherDetailsWritesSpec extends AnyWordSpec with Matchers with OptionValues with PensionSchemeGenerators with SchemaValidatorForTests{

  private def jsonValidator(value: JsValue) = validateJson(elementToValidate =
    Json.obj("establisherAndTrustDetailsType" -> value),
    schemaFileName = "api1468_schema.json",
    relevantProperties = Array("establisherAndTrustDetailsType"),
    relevantDefinitions = Some(Array(
      Array("establisherAndTrustDetailsType", "establisherDetails"),
      Array("establisherPartnershipDetailsType"),
      Array("establisherIndividualDetailsType"),
      Array("establisherDetailsType"),
      Array("establisherCompanyOrOrgDetailsType"),
      Array("individualTrusteeDetailsType"),
      Array("companyTrusteeDetailsType"),
      Array("trusteeDetailsType"),
      Array("partnershipTrusteeDetailsType"),
      Array("specialCharStringType"),
      Array("addressType"),
      Array("addressType"),
      Array("addressLineType"),
      Array("countryCodes"),
      Array("contactDetailsType")
    )))

  "An establisher details object" should {

    "map correctly to an update payload for establisherDetails API 1468" when {

      "validate establisherDetails write with schema" in {
        forAll(establisherDetailsGen) {
          establisher => {

            val mappedEstablisher: JsValue = Json.toJson(establisher)(EstablisherDetails.updateWrites)

            val valid = Json.obj("establisherDetails" -> mappedEstablisher)
            jsonValidator(valid) mustBe Set()
          }
        }
      }

      "invalidate companyTrusteeDetails write with schema when" in {

        forAll(establisherDetailsGen) {
          establisher => {

            val localEstablisher = if(establisher.individual.nonEmpty) {
              establisher.copy(individual = Seq(establisher.individual(0).copy(utr = Some("12313123213123123123"))))
            } else if (establisher.companyOrOrganization.nonEmpty) {
              establisher.copy(companyOrOrganization = Seq(establisher.companyOrOrganization(0).copy(utr = Some("12313123213123123123"))))
            } else if (establisher.partnership.nonEmpty) {
              establisher.copy(partnership = Seq(establisher.partnership(0).copy(utr = Some("12313123213123123123"))))
            } else establisher.copy(individual = Seq(individualGen.sample.get.copy(utr = Some("12313123213123123123"))))

            val mappedEstablisher: JsValue = Json.toJson(localEstablisher)(EstablisherDetails.updateWrites)

            val inValid = Json.obj("establisherDetails" -> mappedEstablisher)
            jsonValidator(inValid).isEmpty mustBe false
          }
        }
      }
    }
  }
}
