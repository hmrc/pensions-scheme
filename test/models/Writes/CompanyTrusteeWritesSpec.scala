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

import models.{CompanyEstablisher, CompanyTrustee}
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsValue, Json}
import utils.{PensionSchemeGenerators, SchemaValidatorForTests}

class CompanyTrusteeWritesSpec extends WordSpec with MustMatchers with OptionValues with PensionSchemeGenerators {
  val schemaValidator = SchemaValidatorForTests()

  "A company object" should {
    "map correctly to an update payload for API 1468" when {
      "we have an company" in {
        forAll(companyTrusteeGen) {
          company => {
            val mappedCompany: JsValue = Json.toJson(company)(CompanyTrustee.updateWrites)

            val validationErrors = schemaValidator.validateJson(mappedCompany,"api1468_schema.json", "#/definitions/companyTrusteeDetailsType")

            validationErrors mustBe None
          }
        }
      }
    }
  }
}
