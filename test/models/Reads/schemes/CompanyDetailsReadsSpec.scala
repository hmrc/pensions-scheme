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

import models.schemes.{CompanyDetails, IndividualDetails, PreviousAddressInfo}
import models.{ContactDetails, CorrespondenceAddress, Samples}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.Json

class CompanyDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples with SchemeDetailsStubJsonData {

  "A JSON payload containing company or organisationDetails details" should {

    "read into a valid company or organisationDetails object" when {

      val result = companyOrOrganisationDetails.as(CompanyDetails.apiReads)

      "we have a organisationName" in {
        result.organizationName mustBe (companyOrOrganisationDetails \ "organisationName").as[String]
      }

      "we have a utr" in {
        result.utr.value mustBe (companyOrOrganisationDetails \ "utr").as[String]
      }

      "we don't have a utr" in {
        val inputWithoutUTR= companyOrOrganisationDetails - "utr"

        inputWithoutUTR.as(CompanyDetails.apiReads).utr mustBe None
      }

      "we have a crnNumber" in {
        result.crn.value mustBe (companyOrOrganisationDetails \ "crnNumber").as[String]
      }

      "we don't have a crnNumber" in {
        val inputWithoutCrnNumber = companyOrOrganisationDetails - "crnNumber"

        inputWithoutCrnNumber.as(CompanyDetails.apiReads).crn mustBe None
      }

      "we have a vatRegistrationNumber" in {
        result.vatRegistration.value mustBe (companyOrOrganisationDetails \ "vatRegistrationNumber").as[String]
      }

      "we don't have a vatRegistrationNumber" in {
        val inputWithoutVatRegistrationNumber = companyOrOrganisationDetails - "vatRegistrationNumber"

        inputWithoutVatRegistrationNumber.as(CompanyDetails.apiReads).vatRegistration mustBe None
      }

      "we don't have a payeReference" in {
        val inputWithoutPayeReference = companyOrOrganisationDetails - "payeReference"

        inputWithoutPayeReference.as(CompanyDetails.apiReads).payeRef mustBe None
      }

      "we have a correspondenceAddressDetails" in {
        result.address mustBe (companyOrOrganisationDetails \ "correspondenceAddressDetails").as[CorrespondenceAddress]
      }

      "we have a correspondenceContactDetails" in {
        result.contact mustBe (companyOrOrganisationDetails \ "correspondenceContactDetails").as(ContactDetails.apiReads)
      }

      "we have a previousAddressDetails" in {
        result.previousAddress.value mustBe (companyOrOrganisationDetails \ "previousAddressDetails").as(PreviousAddressInfo.apiReads)
      }

      "we don't have a previousAddressDetails" in {
        val inputWithoutPreviousAddressDetails = companyOrOrganisationDetails - "previousAddressDetails"

        inputWithoutPreviousAddressDetails.as(CompanyDetails.apiReads).previousAddress mustBe None
      }

      "we have a directorsDetails" in {
        result.directorsDetails.value mustBe (companyOrOrganisationDetails \ "directorsDetails").as(CompanyDetails.seq(IndividualDetails.apiReads))
        result.directorsDetails.value.length mustBe 1
      }

      "we have a multiple directorsDetails" in {

        val companyOrOrganisationDetails = Json.obj("organisationName" -> "abc organisation", "utr"-> "7897700000",
          "crnNumber"-> "AA999999A", "vatRegistrationNumber"-> "789770000", "payeReference" -> "9999",
          "correspondenceAddressDetails"-> addressDetails, "correspondenceContactDetails" -> fullContactDetails,
          "previousAddressDetails" -> previousAddressDetails, "directorsDetails" -> Json.arr(individualDetails, individualDetails))
        val result = companyOrOrganisationDetails.as(CompanyDetails.apiReads)

        result.directorsDetails.value mustBe (companyOrOrganisationDetails \ "directorsDetails").as(CompanyDetails.seq(IndividualDetails.apiReads))
        result.directorsDetails.value.length mustBe 2
      }

      "we don't have a directorsDetails" in {
        val inputWithoutDirectorsDetails = companyOrOrganisationDetails - "directorsDetails"

        inputWithoutDirectorsDetails.as(CompanyDetails.apiReads).directorsDetails mustBe None
      }
    }
  }


}