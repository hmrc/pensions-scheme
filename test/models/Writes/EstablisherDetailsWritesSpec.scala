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

import models.EstablisherDetails
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsValue, Json}
import utils.{PensionSchemeGenerators, SchemaValidatorForTests}

class EstablisherDetailsWritesSpec extends WordSpec with MustMatchers with OptionValues with PensionSchemeGenerators {
  val schemaValidator = SchemaValidatorForTests()

  "An establisher details object" should {
    "map correctly to an update payload for API 1468" when {
      "we have directors, companies and partnerhips" in {
        forAll(establisherDetailsGen) {
          establishers => {
            val mappedEstablishers: JsValue = Json.toJson(establishers)(EstablisherDetails.updateWrites)

            val validationErrors = schemaValidator.validateJson(mappedEstablishers,"establisherDetailsUpdate.json")

            validationErrors mustBe None
          }
        }
      }
    }
  }
}
