/*
 * Copyright 2021 HM Revenue & Customs
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
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsValue, Json}
import utils.{PensionSchemeGenerators, SchemaValidatorForTests}

class SchemeVariationDetailsWritesSpec extends WordSpec with MustMatchers with OptionValues with PensionSchemeGenerators with SchemaValidatorForTests {

  "An PensionsScheme object" should {

    "map correctly to an update payload for scheme variation details API 1468" when {

      "validate scheme variation details write with schema" in {

        forAll(schemeDetailsVariationGen) { pensionsScheme =>

          val mappedSchemeDetails: JsValue = Json.toJson(pensionsScheme)(PensionsScheme.updateWrite("A0123456"))

          val result = validateJson(elementToValidate = mappedSchemeDetails, schemaFileName = "api1468_schema.json")

          result.isSuccess mustBe true
        }
      }

        "invalidate establisherAndTrustDetailsType write with schema for incorrect json" in {

            val pensionsScheme= schemeDetailsVariationGen.retryUntil(_.customerAndSchemeDetails.schemeName.nonEmpty).sample.get
            val invalidPensionsScheme= pensionsScheme.copy(
              customerAndSchemeDetails = pensionsScheme.customerAndSchemeDetails.copy(schemeStructure = Some("INVALID")) )

            val mappedEstablisherAndTrustDetails: JsValue = Json.toJson(invalidPensionsScheme)(PensionsScheme.updateWrite("A0123456"))

            val valid = Json.obj("establisherAndTrustDetailsType" -> mappedEstablisherAndTrustDetails)

            validateJson(elementToValidate = valid,
              schemaFileName = "api1468_schema.json",
              schemaNodePath = "#/properties/establisherAndTrustDetailsType").isError mustBe true
        }
      }
    }
  }
