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

package models.Reads.schemes

import models.CorrespondenceAddress
import models.schemes.PreviousAddressInfo
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}

class PreviousAddressInfoReadsSpec extends WordSpec with MustMatchers with OptionValues with PSASchemeDetailsGenerator {

  "A Json payload containing previous address details" should {

    "read into a valid previous address details object" when {

      "we have a isPreviousAddressLast12Month" in {
        forAll(previousAddressGenerator) { previousAddressDetails =>
          previousAddressDetails.as(PreviousAddressInfo.apiReads).isPreviousAddressLast12Month mustBe (
            previousAddressDetails \ "isPreviousAddressLast12Month").as[Boolean]
        }
      }

      "we have a previousAddress" in {
        forAll(previousAddressGenerator) { previousAddressDetails =>
          previousAddressDetails.as(PreviousAddressInfo.apiReads).previousAddress mustBe (
            previousAddressDetails \ "previousAddress").asOpt(CorrespondenceAddress.reads)
        }
      }

      "we don't have a previousAddress" in {
        forAll(previousAddressGenerator) { previousAddressDetails =>
          val inputWithoutPreviousAddress = previousAddressDetails - "previousAddress"

          inputWithoutPreviousAddress.as(PreviousAddressInfo.apiReads).previousAddress mustBe None
        }

      }
    }
  }
}
