/*
 * Copyright 2020 HM Revenue & Customs
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

package models.etmpToUseranswers.DES

import models.etmpToUserAnswers.DES.AddressTransformer
import models.etmpToUserAnswers.DES.SchemeDetailsTransformer
import org.scalatest.prop.PropertyChecks.forAll

class SchemeDetailsTransformationSpec extends TransformationSpec {

  val addressTransformer = new AddressTransformer
  val schemeDetailsTransformer = new SchemeDetailsTransformer(addressTransformer)

  "A DES payload with Scheme details" must {
    "have the scheme details transformed correctly to valid user answers format" in {

      forAll(schemeDetailsGen) {
        schemeDetails =>
          val (desSchemeDetails, userAnswersSchemeDetails) = schemeDetails
          val result = desSchemeDetails.transform(schemeDetailsTransformer.userAnswersSchemeDetailsReads).get
          result mustBe userAnswersSchemeDetails
      }
    }
  }

}
