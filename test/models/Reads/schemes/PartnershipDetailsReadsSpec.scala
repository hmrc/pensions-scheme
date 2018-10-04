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

import models.schemes.{IndividualDetails, PartnershipDetails, PreviousAddressDetails}
import models.{ContactDetails, CorrespondenceAddress, Samples}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.Json

class PartnershipDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples with MockSchemeData {


  "A JSON payload containing establisher partnership details" should {

    "read into a valid company or organisationDetails object" when {

      val result = establisherPartnershipDetails.as(PartnershipDetails.apiReads)

      "we have a partnershipName" in {
        result.partnershipName mustBe (establisherPartnershipDetails \ "partnershipName").as[String]
      }

      "we have a utr" in {
        result.utr.value mustBe (establisherPartnershipDetails \ "utr").as[String]
      }

      "we don't have a utr" in {
        val inputWithoutUTR= establisherPartnershipDetails - "utr"

        inputWithoutUTR.as[PartnershipDetails](PartnershipDetails.apiReads).utr mustBe None
      }

      "we have a vatRegistrationNumber" in {
        result.vatRegistration.value mustBe (establisherPartnershipDetails \ "vatRegistrationNumber").as[String]
      }

      "we don't have a vatRegistrationNumber" in {
        val inputWithoutVatRegistrationNumber = establisherPartnershipDetails - "vatRegistrationNumber"

        inputWithoutVatRegistrationNumber.as[PartnershipDetails](PartnershipDetails.apiReads).vatRegistration mustBe None
      }

      "we don't have a payeReference" in {
        val inputWithoutPayeReference = establisherPartnershipDetails - "payeReference"

        inputWithoutPayeReference.as[PartnershipDetails](PartnershipDetails.apiReads).payeRef mustBe None
      }

      "we have a correspondenceAddressDetails" in {
        result.address mustBe (establisherPartnershipDetails \ "correspondenceAddressDetails").as[CorrespondenceAddress]
      }

      "we have a correspondenceContactDetails" in {
        result.contact mustBe (establisherPartnershipDetails \ "correspondenceContactDetails").as(ContactDetails.apiReads)
      }

      "we have a previousAddressDetails" in {
        result.previousAddress mustBe (establisherPartnershipDetails \ "previousAddressDetails").as(PreviousAddressDetails.apiReads)
      }

      "we have a partnerDetails" in {
        result.partnerDetails.value mustBe (establisherPartnershipDetails \ "partnerDetails").as(PartnershipDetails.seq(IndividualDetails.apiReads))
        result.partnerDetails.value.length mustBe 1
      }

      "we have multiple partnerDetails" in {
        val establisherPartnershipDetails = Json.obj("partnershipName" -> "abc organisation", "utr"-> "7897700000",
          "vatRegistrationNumber"-> "789770000", "payeReference" -> "9999", "correspondenceAddressDetails"-> addressDetails,
          "correspondenceContactDetails" -> fullContactDetails, "previousAddressDetails" -> previousAddressDetails,
          "partnerDetails" -> Json.arr(individualDetails,individualDetails))
        val result = establisherPartnershipDetails.as(PartnershipDetails.apiReads)

        result.partnerDetails.value mustBe (establisherPartnershipDetails \ "partnerDetails").as(PartnershipDetails.seq(IndividualDetails.apiReads))
        result.partnerDetails.value.length mustBe 2
      }

      "we don't have a partnerDetails" in {
        val inputWithoutPartnerDetails = establisherPartnershipDetails - "partnerDetails"

        inputWithoutPartnerDetails.as[PartnershipDetails](PartnershipDetails.apiReads).partnerDetails mustBe None
      }
    }
  }
}
