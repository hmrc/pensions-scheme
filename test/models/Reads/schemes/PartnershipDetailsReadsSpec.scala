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

import models.schemes.{IndividualInfo, PartnershipDetails, PreviousAddressInfo}
import models.{ContactDetails, CorrespondenceAddress, Samples}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{Json, Reads}

class PartnershipDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples with SchemeDetailsStubJsonData {


  "A JSON payload containing establisher partnership details" should {

    "read into a valid company or organisationDetails object" when {

      val actualResult = establisherPartnershipDetails.as(PartnershipDetails.apiReads)

      "we have a partnershipName" in {
        actualResult.partnershipName mustBe (establisherPartnershipDetails \ "partnershipName").as[String]
      }

      "we have a utr" in {
        actualResult.utr.value mustBe (establisherPartnershipDetails \ "utr").as[String]
      }

      "we don't have a utr" in {
        val inputWithoutUTR= establisherPartnershipDetails - "utr"

        inputWithoutUTR.as(PartnershipDetails.apiReads).utr mustBe None
      }

      "we have a vatRegistrationNumber" in {
        actualResult.vatRegistration.value mustBe (establisherPartnershipDetails \ "vatRegistrationNumber").as[String]
      }

      "we don't have a vatRegistrationNumber" in {
        val inputWithoutVatRegistrationNumber = establisherPartnershipDetails - "vatRegistrationNumber"

        inputWithoutVatRegistrationNumber.as(PartnershipDetails.apiReads).vatRegistration mustBe None
      }

      "we don't have a payeReference" in {
        val inputWithoutPayeReference = establisherPartnershipDetails - "payeReference"

        inputWithoutPayeReference.as(PartnershipDetails.apiReads).payeRef mustBe None
      }

      "we have a correspondenceAddressDetails" in {
        actualResult.address mustBe (establisherPartnershipDetails \ "correspondenceAddressDetails").as[CorrespondenceAddress]
      }

      "we have a correspondenceContactDetails" in {
        actualResult.contact mustBe (establisherPartnershipDetails \ "correspondenceContactDetails").as(ContactDetails.apiReads)
      }

      "we have a previousAddressDetails" in {
        actualResult.previousAddress mustBe (establisherPartnershipDetails \ "previousAddressDetails").as(PreviousAddressInfo.apiReads)
      }

      "we have a partnerDetails" in {
        actualResult.partnerDetails mustBe (establisherPartnershipDetails \ "partnerDetails").as(Reads.seq(IndividualInfo.apiReads))
        actualResult.partnerDetails.length mustBe 1
      }

      "we have multiple partnerDetails" in {
        val establisherPartnershipDetails = Json.obj("partnershipName" -> "abc organisation", "utr"-> "7897700000",
          "vatRegistrationNumber"-> "789770000", "payeReference" -> "9999", "correspondenceAddressDetails"-> addressDetails,
          "correspondenceContactDetails" -> fullContactDetails, "previousAddressDetails" -> previousAddressDetails,
          "partnerDetails" -> Json.arr(individualDetails,individualDetails))
        val actualMultiplePartnerDetails = establisherPartnershipDetails.as(PartnershipDetails.apiReads)

        actualMultiplePartnerDetails.partnerDetails mustBe (establisherPartnershipDetails \ "partnerDetails").as(
          Reads.seq(IndividualInfo.apiReads))
        actualMultiplePartnerDetails.partnerDetails.length mustBe 2
      }

      "we don't have a partnerDetails" in {
        val inputWithoutPartnerDetails = establisherPartnershipDetails - "partnerDetails"

        inputWithoutPartnerDetails.as(PartnershipDetails.apiReads).partnerDetails mustBe Nil
      }
    }
  }
}
