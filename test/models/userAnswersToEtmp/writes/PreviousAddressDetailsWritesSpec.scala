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

import models.Samples
import models.userAnswersToEtmp.PreviousAddressDetails
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{Ignore, OptionValues}
import play.api.libs.json.{JsValue, Json}
import utils.SchemaValidatorForTests

import scala.util.Random

@Ignore
class PreviousAddressDetailsWritesSpec extends AnyWordSpec with Matchers with OptionValues with Samples with SchemaValidatorForTests {

  "A previous address details object" should {
    "Map previous address details inner object as `previousaddresdetail`" when {
      "required" in {
        val previousAddress = PreviousAddressDetails(true, Some(ukAddressSample))
        val result = Json.toJson(previousAddress)(PreviousAddressDetails.psaSubmissionWrites)

        result.toString() must include("\"previousAddressDetail\":")
      }
    }
  }

  "A partnership object" should {
    "parse correctly to a valid If format for variations api - API 1468" when {
      "we have a valid partnership" in {
        val previousAddress = PreviousAddressDetails(true, Some(ukAddressSample))

        val mappedPreviousAddress: JsValue = Json.toJson(previousAddress)(PreviousAddressDetails.psaUpdateWrites)
        val testJsValue = Json.obj("previousAddressDetails" -> mappedPreviousAddress)

        validateJson(elementToValidate = testJsValue,
          schemaFileName = "api1468_schema.json",
          schemaNodePath = "#/properties/establisherAndTrustDetailsType/trusteeDetailsType/individualDetails/items/previousAddressDetails").isSuccess mustBe true
      }
    }

    "return errors when incoming data cannot be parsed to a valid If format for variations api - API 1468" when {
      "we have an invalid partnership" in {
        val invalidPreviousAddress = PreviousAddressDetails(true, Some(ukAddressSample.copy(addressLine1 = Random.alphanumeric.take(40).mkString)))

        val mappedPreviousAddress: JsValue = Json.toJson(invalidPreviousAddress)(PreviousAddressDetails.psaUpdateWrites)
        val testJsValue = Json.obj("previousAddressDetails" -> mappedPreviousAddress)

        validateJson(elementToValidate = testJsValue,
          schemaFileName = "api1468_schema.json",
          schemaNodePath = "#/properties/establisherAndTrustDetailsType/trusteeDetailsType/individualDetails/items/previousAddressDetails").isError mustBe true
      }
    }
  }
}
