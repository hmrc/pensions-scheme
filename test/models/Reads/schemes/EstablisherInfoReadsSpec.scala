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

class EstablisherInfoReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples with SchemeDetailsStubJsonData {

  "A JSON payload containing establisher details" should {

    "read into a valid establisher details object" when {

      val actualResult = establisherDetails.as(EstablisherInfo.apiReads)

      "we have a individualDetails" in {
        actualResult.individual mustBe (establisherDetails \ "individualDetails").as(
          EstablisherInfo.seq(IndividualDetails.apiReads))
        actualResult.individual.length mustBe 1
      }

      "we have multiple individualDetails" in {
        val actualMultipleIndividualDetails = establisherDetailsWithMultipleData.as(EstablisherInfo.apiReads)

        actualMultipleIndividualDetails.individual mustBe (establisherDetailsWithMultipleData \ "individualDetails").as(
          EstablisherInfo.seq(IndividualDetails.apiReads))
        actualMultipleIndividualDetails.individual.length mustBe 2
      }

      "we don't have a individualDetails" in {
        val inputWithoutIndividualDetails = establisherDetails - "individualDetails"

        inputWithoutIndividualDetails.as(EstablisherInfo.apiReads).individual mustBe Nil
      }

      "we have a companyOrOrganisationDetails" in {
        actualResult.company mustBe (establisherDetails \ "companyOrOrganisationDetails").as(
          EstablisherInfo.seq(CompanyDetails.apiReads))
        actualResult.company.length mustBe 1
      }

      "we have multiple companyOrOrganisationDetails" in {
        val actulaMultipleCompanyOrOrganisationDetails = establisherDetailsWithMultipleData.as(EstablisherInfo.apiReads)

        actulaMultipleCompanyOrOrganisationDetails.company mustBe (establisherDetailsWithMultipleData \ "companyOrOrganisationDetails").as(
          EstablisherInfo.seq(CompanyDetails.apiReads))
        actulaMultipleCompanyOrOrganisationDetails.company.length mustBe 2
      }

      "we don't have a companyOrOrganisationDetails" in {
        val inputWithoutIndividualDetails = establisherDetails - "companyOrOrganisationDetails"

        inputWithoutIndividualDetails.as(EstablisherInfo.apiReads).company mustBe Nil
      }

      "we have a partnershipTrusteeDetail" in {
        actualResult.partnership mustBe (establisherDetails \ "partnershipTrusteeDetail").as(
          EstablisherInfo.seq(PartnershipDetails.apiReads))
        actualResult.partnership.length mustBe 1
      }

      "we have multiple partnershipTrusteeDetail" in {
        val actualMultiplePrtnershipTrusteeDetail = establisherDetailsWithMultipleData.as(EstablisherInfo.apiReads)

        actualMultiplePrtnershipTrusteeDetail.partnership mustBe (establisherDetailsWithMultipleData \ "partnershipTrusteeDetail").as(
          EstablisherInfo.seq(PartnershipDetails.apiReads))
        actualMultiplePrtnershipTrusteeDetail.partnership.length mustBe 2
      }

      "we don't have a partnershipTrusteeDetail" in {
        val inputWithoutIndividualDetails = establisherDetails - "partnershipTrusteeDetail"

        inputWithoutIndividualDetails.as(EstablisherInfo.apiReads).partnership mustBe Nil
      }
    }
  }

}
