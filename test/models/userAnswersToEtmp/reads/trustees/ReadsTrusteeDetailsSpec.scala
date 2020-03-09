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

package models.userAnswersToEtmp.reads.trustees

import models.userAnswersToEtmp.reads.CommonGenerator.trusteesGen
import models.userAnswersToEtmp.trustee.TrusteeDetails
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}

class ReadsTrusteeDetailsSpec extends WordSpec with MustMatchers with OptionValues {

  "ReadsTrusteeDetails" must {

    "read multiple trustees with filtering out the deleted ones" in {
      forAll(trusteesGen) { json =>
        val trusteeDetails = json.as[TrusteeDetails](TrusteeDetails.readsTrusteeDetails)

        trusteeDetails.companyTrusteeDetail.head.organizationName mustBe
          (json \ "trustees" \ 0 \ "companyDetails" \ "companyName").as[String]

        trusteeDetails.individualTrusteeDetail.head.personalDetails.firstName mustBe
          (json \ "trustees" \ 3 \ "trusteeDetails" \ "firstName").as[String]

        trusteeDetails.partnershipTrusteeDetail.head.organizationName mustBe
          (json \ "trustees" \ 4 \ "partnershipDetails" \ "name").as[String]
      }
    }
  }
}


