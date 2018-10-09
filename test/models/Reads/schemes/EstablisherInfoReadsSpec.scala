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
import models.schemes.{CompanyDetails, EstablisherInfo, IndividualInfo, PartnershipDetails}
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.Reads

class EstablisherInfoReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples with PSASchemeDetailsGenerator {

  "A JSON payload containing establisher details" should {

    "read into a valid establisher details object" when {

      "we have a individualDetails" in {
        forAll(establisherDetailsGenerator) { establisherDetails =>
          establisherDetails.as(EstablisherInfo.apiReads).individual mustBe (establisherDetails \ "individualDetails").as(
            Reads.seq(IndividualInfo.apiReads))
          establisherDetails.as(EstablisherInfo.apiReads).individual.length mustBe 1
        }
      }

      "we have multiple individualDetails" in {
        forAll(establisherDetailsGenerator(2)) { establisherDetails =>
          establisherDetails.as(EstablisherInfo.apiReads).individual mustBe (establisherDetails \ "individualDetails").as(
            Reads.seq(IndividualInfo.apiReads))
          establisherDetails.as(EstablisherInfo.apiReads).individual.length mustBe 2
        }
      }

      "we don't have a individualDetails" in {
        forAll(establisherDetailsGenerator) { establisherDetails =>
          val inputWithoutIndividualDetails = establisherDetails - "individualDetails"

          inputWithoutIndividualDetails.as(EstablisherInfo.apiReads).individual mustBe Nil
        }
      }

      "we have a companyOrOrganisationDetails" in {
        forAll(establisherDetailsGenerator) { establisherDetails =>
          establisherDetails.as(EstablisherInfo.apiReads).company mustBe (establisherDetails \ "companyOrOrganisationDetails").as(
            Reads.seq(CompanyDetails.apiReads))
          establisherDetails.as(EstablisherInfo.apiReads).company.length mustBe 1
        }
      }

      "we have multiple companyOrOrganisationDetails" in {
        forAll(establisherDetailsGenerator(2)) { establisherDetails =>
          val actulaMultipleCompanyOrOrganisationDetails = establisherDetails.as(EstablisherInfo.apiReads)

          actulaMultipleCompanyOrOrganisationDetails.company mustBe (establisherDetails \ "companyOrOrganisationDetails").as(
            Reads.seq(CompanyDetails.apiReads))
          actulaMultipleCompanyOrOrganisationDetails.company.length mustBe 2
        }
      }

      "we don't have a companyOrOrganisationDetails" in {
        forAll(establisherDetailsGenerator) { establisherDetails =>
          val inputWithoutIndividualDetails = establisherDetails - "companyOrOrganisationDetails"

          inputWithoutIndividualDetails.as(EstablisherInfo.apiReads).company mustBe Nil
        }
      }

      "we have a partnershipTrusteeDetail" in {
        forAll(establisherDetailsGenerator) { establisherDetails =>
          establisherDetails.as(EstablisherInfo.apiReads).partnership mustBe (establisherDetails \ "partnershipTrusteeDetail").as(
            Reads.seq(PartnershipDetails.apiReads))
          establisherDetails.as(EstablisherInfo.apiReads).partnership.length mustBe 1
        }
      }

      "we have multiple partnershipTrusteeDetail" in {
        forAll(establisherDetailsGenerator(2)) { establisherDetails =>
          val actualMultiplePrtnershipTrusteeDetail = establisherDetails.as(EstablisherInfo.apiReads)

          actualMultiplePrtnershipTrusteeDetail.partnership mustBe (establisherDetails \ "partnershipTrusteeDetail").as(
            Reads.seq(PartnershipDetails.apiReads))
          actualMultiplePrtnershipTrusteeDetail.partnership.length mustBe 2
        }
      }

      "we don't have a partnershipTrusteeDetail" in {
        forAll(establisherDetailsGenerator) { establisherDetails =>
          val inputWithoutIndividualDetails = establisherDetails - "partnershipTrusteeDetail"

          inputWithoutIndividualDetails.as(EstablisherInfo.apiReads).partnership mustBe Nil
        }
      }
    }
  }
}
