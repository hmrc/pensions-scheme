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

import models.userAnswersToEtmp.PensionsScheme
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.libs.json.{JsValue, Json}
import utils.{PensionSchemeGenerators, SchemaValidatorForTests}


class SchemeVariationDetailsWritesSpec extends AnyWordSpec with Matchers with OptionValues with PensionSchemeGenerators with SchemaValidatorForTests {

  private def jsonValidator(value: JsValue) = validateJson(elementToValidate = value,
    schemaFileName = "api1468_schema.json",
    relevantProperties = Array("establisherAndTrustDetailsType"),
    relevantDefinitions = Some(Array(
      Array("establisherAndTrustDetailsType"),
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

  "An PensionsScheme object" should {

    "map correctly to an update payload for scheme variation details API 1468" when {

      "validate scheme variation details write" in {

        forAll(schemeDetailsVariationGen) { pensionsScheme =>

          val mappedSchemeDetails: JsValue = Json.toJson(pensionsScheme)(PensionsScheme.updateWrite("A0123456"))

          val result = validateJson(elementToValidate = mappedSchemeDetails, schemaFileName = "api1468_schemaIF.json")

          result mustBe Set()
        }
      }

        "invalidate establisherAndTrustDetailsType write with schema for incorrect json" in {

            val pensionsScheme= schemeDetailsVariationGen.retryUntil(_.customerAndSchemeDetails.schemeName.nonEmpty).sample.get
            val invalidPensionsScheme= pensionsScheme.copy(
              customerAndSchemeDetails = pensionsScheme.customerAndSchemeDetails.copy(schemeStructure = Some("INVALID")) )

            val mappedEstablisherAndTrustDetails: JsValue = Json.toJson(invalidPensionsScheme)(PensionsScheme.updateWrite("A0123456"))

            val valid = Json.obj("establisherAndTrustDetailsType" -> mappedEstablisherAndTrustDetails)

            jsonValidator(valid).isEmpty mustBe false
        }
      }
    }
  }
