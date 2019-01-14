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

package models.Reads.schemes

import models.schemes._
import models.{CorrespondenceAddress, Samples}
import org.scalatest.prop.PropertyChecks._
import org.scalatest.{MustMatchers, OptionValues, WordSpec}

class IndividualInfoReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples with PSASchemeDetailsGenerator {


  "A JSON payload containing personal details" should {

    "read into a valid personal details object" when {

      "we have a firstName" in {
        forAll(personalDetailsGenerator) { personalDetails =>
          personalDetails.as(PersonalInfo.apiReads).name.firstName mustBe (personalDetails \ "firstName").as[String]
        }
      }

      "we have a middleName" in {
        forAll(personalDetailsGenerator) { personalDetails =>
          personalDetails.as(PersonalInfo.apiReads).name.middleName mustBe (personalDetails \ "middleName").asOpt[String]
        }
      }

      "we don't have a middleName" in {
        forAll(personalDetailsGenerator) { personalDetails =>
          val inputWithoutMiddleName = personalDetails - "middleName"
          inputWithoutMiddleName.as(PersonalInfo.apiReads).name.middleName mustBe None
        }

      }

      "we have a lastName" in {
        forAll(personalDetailsGenerator) { personalDetails =>
          personalDetails.as(PersonalInfo.apiReads).name.lastName mustBe (personalDetails \ "lastName").as[String]
        }
      }

      "we have a dateOfBirth" in {
        forAll(personalDetailsGenerator) { personalDetails =>
          personalDetails.as(PersonalInfo.apiReads).dateOfBirth mustBe (personalDetails \ "dateOfBirth").as[String]
        }
      }

    }
  }

  "A JSON payload containing individuals details" should {

    "read into a valid individuals details object" when {

      "we have a personalDetails" in {
        forAll(individualDetailsGenerator) { individualDetails =>
          individualDetails.as(IndividualInfo.apiReads).personalDetails mustBe (individualDetails \ "personDetails").as(
            PersonalInfo.apiReads)
        }
      }

      "we have a nino" in {
        forAll(individualDetailsGenerator) { individualDetails =>
          individualDetails.as(IndividualInfo.apiReads).nino mustBe (individualDetails \ "nino").asOpt[String]
        }
      }

      "we don't have a nino" in {
        forAll(individualDetailsGenerator) { individualDetails =>
          val inputWithoutNino = individualDetails - "nino"

          inputWithoutNino.as(IndividualInfo.apiReads).nino mustBe None
        }

      }

      "we have a utr" in {
        forAll(individualDetailsGenerator) { individualDetails =>
          individualDetails.as(IndividualInfo.apiReads).utr mustBe (individualDetails \ "utr").asOpt[String]
        }
      }

      "we don't have a utr" in {
        forAll(individualDetailsGenerator) { individualDetails =>
          val inputWithoutUTR = individualDetails - "utr"

          inputWithoutUTR.as(IndividualInfo.apiReads).utr mustBe None
        }
      }

      "we have a correspondenceAddressDetails" in {
        forAll(individualDetailsGenerator) { individualDetails =>
          individualDetails.as(IndividualInfo.apiReads).address mustBe (individualDetails \ "correspondenceAddressDetails").as(
            CorrespondenceAddress.reads)
        }
      }

      "we have a correspondenceContactDetails" in {
        forAll(individualDetailsGenerator) { individualDetails =>
          individualDetails.as(IndividualInfo.apiReads).contact mustBe (individualDetails \ "correspondenceContactDetails").as(
            IndividualContactDetails.apiReads)
        }
      }

      "we have a previousAddressDetails" in {
        forAll(individualDetailsGenerator) { individualDetails =>
          individualDetails.as(IndividualInfo.apiReads).previousAddress mustBe (individualDetails \ "previousAddressDetails").as(
            PreviousAddressInfo.apiReads)
        }
      }
    }
  }
}
