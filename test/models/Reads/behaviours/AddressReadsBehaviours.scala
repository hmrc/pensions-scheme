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

package models.Reads.behaviours

import models.{CorrespondenceAddress, Samples}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.JsObject

trait AddressReadsBehaviours extends WordSpec with MustMatchers with OptionValues with Samples {

  def correspondenceAddressDetails(address: JsObject): Unit = {

    "A JSON payload containing address details" when {

      val result = address.as[CorrespondenceAddress]

      "with addressLine 1" in {
        result.addressLine1 mustBe (address \ "line1").as[String]
      }

      "with addressLine 2" in {
        result.addressLine2 mustBe (address \ "line2").as[String]
      }

      "with addressLine 3" in {
        result.addressLine3.value mustBe (address \ "line3").as[String]
      }

      "with no address line 3" in {
        val result = (address - "line3").as[CorrespondenceAddress]

        result.addressLine3 mustBe None
      }

      "with addressLine 4" in {
        result.addressLine4.value mustBe (address \ "line4").as[String]
      }

      "with no address line 4" in {
        val result = (address - "line4").as[CorrespondenceAddress]

        result.addressLine4 mustBe None
      }

      "with postalCode" in {
        result.postalCode.value mustBe (address \ "postalCode").as[String]
      }

    }
  }

}
