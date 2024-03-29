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

package models.etmpToUserAnswers

import models.etmpToUserAnswers.psaSchemeDetails.DirectorsOrPartnersTransformer
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll

class DirectorsOrPartnersTransformationSpec extends TransformationSpec {

  val addressTransformer = new AddressTransformer
  val directorOrPartnerTransformer = new DirectorsOrPartnersTransformer(addressTransformer)

  "An IF payload with Partner or director" must {
    "have the partner details transformed correctly to valid user answers format" that {

      "has person details in partners array" in {
        forAll(directorOrPartnerJsValueGen("partner")) {
          partnerDetails =>
            val (ifPartnerDetails, userAnswersPartnerDetails) = partnerDetails
            val result = ifPartnerDetails.transform(directorOrPartnerTransformer.userAnswersPartnerReads).get

            result mustBe userAnswersPartnerDetails
        }
      }
    }

    "have the director details transformed correctly to valid user answers format" that {

      "has person details in partners array" in {
        forAll(directorOrPartnerJsValueGen("director")) {
          directorDetails =>
            val (ifDirectorDetails, userAnswersDirectorDetails) = directorDetails
            val result = ifDirectorDetails.transform(directorOrPartnerTransformer.userAnswersDirectorReads).get

            result mustBe userAnswersDirectorDetails
        }
      }
    }
  }
}
