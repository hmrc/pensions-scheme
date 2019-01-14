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

import models.schemes.{IndividualContactDetails, IndividualInfo, PartnershipDetails, PreviousAddressInfo}
import models.{CorrespondenceAddress, Samples}
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.Reads

class PartnershipDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples with PSASchemeDetailsGenerator {


  "A JSON payload containing establisher partnership details" should {

    "read into a valid company or organisationDetails object" when {

      "we have a partnershipName" in {
        forAll(establisherPartnershipDetailsDetailsGenerator()) { establisherPartnershipDetails =>
          establisherPartnershipDetails.as(PartnershipDetails.apiReads).partnershipName mustBe (
            establisherPartnershipDetails \ "partnershipName").as[String]
        }
      }

      "we have a utr" in {
        forAll(establisherPartnershipDetailsDetailsGenerator()) { establisherPartnershipDetails =>
          establisherPartnershipDetails.as(PartnershipDetails.apiReads).utr mustBe (establisherPartnershipDetails \ "utr").asOpt[String]
        }
      }

      "we don't have a utr" in {
        forAll(establisherPartnershipDetailsDetailsGenerator()) { establisherPartnershipDetails =>
          val inputWithoutUTR = establisherPartnershipDetails - "utr"

          inputWithoutUTR.as(PartnershipDetails.apiReads).utr mustBe None
        }
      }

      "we have a vatRegistrationNumber" in {
        forAll(establisherPartnershipDetailsDetailsGenerator()) { establisherPartnershipDetails =>
          establisherPartnershipDetails.as(PartnershipDetails.apiReads).vatRegistration mustBe (
            establisherPartnershipDetails \ "vatRegistrationNumber").asOpt[String]
        }
      }

      "we don't have a vatRegistrationNumber" in {
        forAll(establisherPartnershipDetailsDetailsGenerator()) { establisherPartnershipDetails =>
          val inputWithoutVatRegistrationNumber = establisherPartnershipDetails - "vatRegistrationNumber"

          inputWithoutVatRegistrationNumber.as(PartnershipDetails.apiReads).vatRegistration mustBe None
        }
      }

      "we don't have a payeReference" in {
        forAll(establisherPartnershipDetailsDetailsGenerator()) { establisherPartnershipDetails =>
          val inputWithoutPayeReference = establisherPartnershipDetails - "payeReference"

          inputWithoutPayeReference.as(PartnershipDetails.apiReads).payeRef mustBe None
        }
      }

      "we have a correspondenceAddressDetails" in {
        forAll(establisherPartnershipDetailsDetailsGenerator()) { establisherPartnershipDetails =>
          establisherPartnershipDetails.as(PartnershipDetails.apiReads).address mustBe (
            establisherPartnershipDetails \ "correspondenceAddressDetails").as[CorrespondenceAddress]
        }
      }

      "we have a correspondenceContactDetails" in {
        forAll(establisherPartnershipDetailsDetailsGenerator()) { establisherPartnershipDetails =>
          establisherPartnershipDetails.as(PartnershipDetails.apiReads).contact mustBe (
            establisherPartnershipDetails \ "correspondenceContactDetails").as(IndividualContactDetails.apiReads)
        }
      }

      "we have a previousAddressDetails" in {
        forAll(establisherPartnershipDetailsDetailsGenerator()) { establisherPartnershipDetails =>
          establisherPartnershipDetails.as(PartnershipDetails.apiReads).previousAddress mustBe (
            establisherPartnershipDetails \ "previousAddressDetails").as(PreviousAddressInfo.apiReads)
        }
      }

      "we have a partnerDetails" in {
        forAll(establisherPartnershipDetailsDetailsGenerator()) { establisherPartnershipDetails =>
          establisherPartnershipDetails.as(PartnershipDetails.apiReads).partnerDetails mustBe (
            establisherPartnershipDetails \ "partnerDetails").as(Reads.seq(IndividualInfo.apiReads))
          establisherPartnershipDetails.as(PartnershipDetails.apiReads).partnerDetails.length mustBe 1
        }
      }

      "we have multiple partnerDetails" in {
        forAll(establisherPartnershipDetailsDetailsGenerator(noOfElements = 2)) { establisherPartnershipDetails =>
          val actualMultiplePartnerDetails = establisherPartnershipDetails.as(PartnershipDetails.apiReads)

          actualMultiplePartnerDetails.partnerDetails mustBe (establisherPartnershipDetails \ "partnerDetails").as(
            Reads.seq(IndividualInfo.apiReads))
          actualMultiplePartnerDetails.partnerDetails.length mustBe 2
        }
      }
    }
  }
}
