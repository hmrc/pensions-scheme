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
import models.schemes.{CompanyDetails, IndividualDetails, PartnershipDetails, TrusteeInfo}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.Reads

class TrusteeInfoReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples with SchemeDetailsStubJsonData {

  "A JSON payload containing trustee details" should {

    "read into a valid trustee details object" when {

      val actualResult = trusteeDetails.as(TrusteeInfo.apiReads)

      "we have a individualTrusteeDetails" in {
        actualResult.individual mustBe (trusteeDetails \ "individualTrusteeDetails").as(
          Reads.seq(IndividualDetails.apiReads))
        actualResult.individual.length mustBe 1
      }

      "we have multiple individualTrusteeDetails" in {
        val actualMultipleIndividualDetails = trusteeDetailsWithMultipleData.as(TrusteeInfo.apiReads)

        actualMultipleIndividualDetails.individual mustBe (trusteeDetailsWithMultipleData \ "individualTrusteeDetails").as(
          Reads.seq(IndividualDetails.apiReads))
        actualMultipleIndividualDetails.individual.length mustBe 2
      }

      "we don't have a individualTrusteeDetails" in {
        val inputWithoutIndividualDetails = trusteeDetails - "individualTrusteeDetails"

        inputWithoutIndividualDetails.as(TrusteeInfo.apiReads).individual mustBe Nil
      }

      "we have a companyTrusteeDetails" in {
        actualResult.company mustBe (trusteeDetails \ "companyTrusteeDetails").as(
          Reads.seq(CompanyDetails.apiReads))
        actualResult.company.length mustBe 1
      }

      "we have multiple companyTrusteeDetails" in {
        val actulaMultipleCompanyOrOrganisationDetails = trusteeDetailsWithMultipleData.as(TrusteeInfo.apiReads)

        actulaMultipleCompanyOrOrganisationDetails.company mustBe (trusteeDetailsWithMultipleData \ "companyTrusteeDetails").as(
          Reads.seq(CompanyDetails.apiReads))
        actulaMultipleCompanyOrOrganisationDetails.company.length mustBe 2
      }

      "we don't have a companyTrusteeDetails" in {
        val inputWithoutIndividualDetails = trusteeDetails - "companyTrusteeDetails"

        inputWithoutIndividualDetails.as(TrusteeInfo.apiReads).company mustBe Nil
      }

      "we have a partnershipTrusteeDetail" in {
        actualResult.partnership mustBe (trusteeDetails \ "partnershipTrusteeDetails").as(
          Reads.seq(PartnershipDetails.apiReads))
        actualResult.partnership.length mustBe 1
      }

      "we have multiple partnershipTrusteeDetail" in {
        val actualMultiplePrtnershipTrusteeDetail = trusteeDetailsWithMultipleData.as(TrusteeInfo.apiReads)

        actualMultiplePrtnershipTrusteeDetail.partnership mustBe (trusteeDetailsWithMultipleData \ "partnershipTrusteeDetails").as(
          Reads.seq(PartnershipDetails.apiReads))
        actualMultiplePrtnershipTrusteeDetail.partnership.length mustBe 2
      }

      "we don't have a partnershipTrusteeDetail" in {
        val inputWithoutIndividualDetails = trusteeDetails - "partnershipTrusteeDetails"

        inputWithoutIndividualDetails.as(TrusteeInfo.apiReads).partnership mustBe Nil
      }
    }
  }

}
