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

package models.Transformers

import models.jsonTransformations.{AddressTransformer, DirectorsOrPartnersTransformer}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import utils.PensionSchemeJsValueGenerators
import org.scalatest.prop.PropertyChecks.forAll

class DirectorsOrPartnersTransformationSpec extends WordSpec with MustMatchers with OptionValues with PensionSchemeJsValueGenerators {

  val addressTransformer = new AddressTransformer
  val directorOrPartnerTransformer = new DirectorsOrPartnersTransformer(addressTransformer)

  "A DES payload with Partner or director" must {
    "have the partner details transformed correctly to valid user answers format" that {

      "has person details in partners array" in {
        forAll(directorOrPartnerJsValueGen("partner")) {
          partnerDetails =>
            val (desPartnerDetails, userAnswersPartnerDetails) = partnerDetails
            val result = desPartnerDetails.transform(directorOrPartnerTransformer.userAnswersPartnerReads).get

            result mustBe userAnswersPartnerDetails
        }
      }
    }

    "have the director details transformed correctly to valid user answers format" that {

      "has person details in partners array" in {
        forAll(directorOrPartnerJsValueGen("director")) {
          directorDetails =>
            val (desDirectorDetails, userAnswersDirectorDetails) = directorDetails
            val result = desDirectorDetails.transform(directorOrPartnerTransformer.userAnswersDirectorReads).get

            result mustBe userAnswersDirectorDetails
        }
      }
    }
  }
}
