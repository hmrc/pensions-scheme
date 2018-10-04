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

import models.Samples
import models.schemes.{CompanyDetails, EstablisherInfo, IndividualDetails, PartnershipDetails}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}

class EstablisherInfoReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples with MockSchemeData {

  "A JSON payload containing establisher details" should {

    "read into a valid establisher details object" when {

      val result = establisherDetails.as(EstablisherInfo.apiReads)

      "we have a individualDetails" in {
        result.individual.value mustBe (establisherDetails \ "individualDetails").as(
          EstablisherInfo.seq(IndividualDetails.apiReads))
        result.individual.value.length mustBe 1
      }

      "we have multiple individualDetails" in {
        val result = establisherDetailsWithMultipleData.as(EstablisherInfo.apiReads)

        result.individual.value mustBe (establisherDetailsWithMultipleData \ "individualDetails").as(
          EstablisherInfo.seq(IndividualDetails.apiReads))
        result.individual.value.length mustBe 2
      }

      "we don't have a individualDetails" in {
        val inputWithoutIndividualDetails = establisherDetails - "individualDetails"

        inputWithoutIndividualDetails.as(EstablisherInfo.apiReads).individual mustBe None
      }

      "we have a companyOrOrganisationDetails" in {
        result.company.value mustBe (establisherDetails \ "companyOrOrganisationDetails").as(
          EstablisherInfo.seq(CompanyDetails.apiReads))
        result.company.value.length mustBe 1
      }

      "we have multiple companyOrOrganisationDetails" in {
        val result = establisherDetailsWithMultipleData.as(EstablisherInfo.apiReads)

        result.company.value mustBe (establisherDetailsWithMultipleData \ "companyOrOrganisationDetails").as(
          EstablisherInfo.seq(CompanyDetails.apiReads))
        result.company.value.length mustBe 2
      }

      "we don't have a companyOrOrganisationDetails" in {
        val inputWithoutIndividualDetails = establisherDetails - "companyOrOrganisationDetails"

        inputWithoutIndividualDetails.as(EstablisherInfo.apiReads).company mustBe None
      }

      "we have a partnershipTrusteeDetail" in {
        result.partnership.value mustBe (establisherDetails \ "partnershipTrusteeDetail").as(
          EstablisherInfo.seq(PartnershipDetails.apiReads))
        result.partnership.value.length mustBe 1
      }

      "we have multiple partnershipTrusteeDetail" in {
        val result = establisherDetailsWithMultipleData.as(EstablisherInfo.apiReads)

        result.partnership.value mustBe (establisherDetailsWithMultipleData \ "partnershipTrusteeDetail").as(
          EstablisherInfo.seq(PartnershipDetails.apiReads))
        result.partnership.value.length mustBe 2
      }

      "we don't have a partnershipTrusteeDetail" in {
        val inputWithoutIndividualDetails = establisherDetails - "partnershipTrusteeDetail"

        inputWithoutIndividualDetails.as(EstablisherInfo.apiReads).partnership mustBe None
      }
    }
  }

}
