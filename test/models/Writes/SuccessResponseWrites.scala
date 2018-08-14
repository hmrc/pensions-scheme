/*
 * Copyright 2018 HM Revenue & Customs
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

import models._
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.Json

class SuccessResponseWrites extends WordSpec with MustMatchers with OptionValues with Samples {
  "A success response object" should {
    "Map an address as default type" when {
      val successRespone = SuccessResponse("test", "test", true, Some(IndividualType("Test", None, "Test")), Some(OrganisationType("Test")), ukAddressSample, ContactCommDetailsType())
      val response = Json.toJson(successRespone)

      "We have an address" in {
        response.toString() must include("addressLine1")
      }

      "We have a safeId" in {
        response.toString() must include("safeId")
      }

      "We have a sapNumber" in {
        response.toString() must include("sapNumber")
      }

      "We have a isAnIndividual" in {
        response.toString() must include("isAnIndividual")
      }

      "We have a individual" in {
        response.toString() must include("individual")
      }

      "We have a organisation" in {
        response.toString() must include("organisation")
      }

      "We have contactDetails" in {
        response.toString() must include("contactDetails")
      }
    }
  }
}
