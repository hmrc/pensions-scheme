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
import models.schemes.PreviousAddressDetails
import org.scalatest.{MustMatchers, OptionValues, WordSpec}

class PreviousAddressDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues with MockSchemeData {

   "A Json payload containing previous address details" should {

    "read into a valid previous address details object" when {

      "we have a isPreviousAddressLast12Month" in {
        previousAddressDetails.as(PreviousAddressDetails.apiReads).isPreviousAddressLast12Month mustBe (previousAddressDetails \ "isPreviousAddressLast12Month").as[Boolean]
      }

      "we have a previousAddress" in {
        previousAddressDetails.as(PreviousAddressDetails.apiReads).previousAddress.value mustBe (previousAddressDetails \ "previousAddress").as(CorrespondenceAddress.reads)
      }

      "we don't have a previousAddress" in {
        val inputWithoutPreviousAddress = previousAddressDetails - "previousAddress"

        inputWithoutPreviousAddress.as(PreviousAddressDetails.apiReads).previousAddress mustBe None
      }
    }
  }
}
