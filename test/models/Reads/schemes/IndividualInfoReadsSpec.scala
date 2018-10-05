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

import models.schemes._
import models.{ContactDetails, CorrespondenceAddress, Samples}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}

class IndividualInfoReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples with SchemeDetailsStubJsonData {

  "A JSON payload containing personal details" should {

    "read into a valid personal details object" when {

      val actualResult = personalDetails.as(PersonalInfo.apiReads)

      "we have a firstName" in {
        actualResult.name.firstName mustBe (personalDetails \ "firstName").as[String]
      }

      "we have a middleName" in {
        actualResult.name.middleName.value mustBe (personalDetails \ "middleName").as[String]
      }

      "we don't have a middleName" in {
        val inputWithoutMiddleName = personalDetails - "middleName"

        inputWithoutMiddleName.as(PersonalInfo.apiReads).name.middleName mustBe None
      }

      "we have a lastName" in {
        actualResult.name.lastName mustBe (personalDetails \ "lastName").as[String]
      }

      "we have a dateOfBirth" in {
        actualResult.dateOfBirth mustBe (personalDetails \ "dateOfBirth").as[String]
      }

    }
  }

  "A JSON payload containing individuals details" should {

    "read into a valid individuals details object" when {

      val actualResult = individualDetails.as(IndividualInfo.apiReads)

      "we have a personalDetails" in {
        actualResult.personalDetails mustBe (individualDetails \ "personDetails").as(PersonalInfo.apiReads)
      }

      "we have a nino" in {
        actualResult.nino.value mustBe (individualDetails \ "nino").as[String]
      }

      "we don't have a nino" in {
        val inputWithoutNino = individualDetails - "nino"

        inputWithoutNino.as(IndividualInfo.apiReads).nino mustBe None
      }

      "we have a utr" in {
        actualResult.utr.value mustBe (individualDetails \ "utr").as[String]
      }

      "we don't have a utr" in {
        val inputWithoutUTR = individualDetails - "utr"

        inputWithoutUTR.as(IndividualInfo.apiReads).utr mustBe None
      }

      "we have a correspondenceAddressDetails" in {
        actualResult.address mustBe (individualDetails \ "correspondenceAddressDetails").as(CorrespondenceAddress.reads)
      }

      "we have a correspondenceContactDetails" in {
        actualResult.contact mustBe (individualDetails \ "correspondenceContactDetails").as(ContactDetails.apiReads)
      }

      "we have a previousAddressDetails" in {
        actualResult.previousAddress mustBe (individualDetails \ "previousAddressDetails").as(PreviousAddressInfo.apiReads)
      }

    }
  }
}
