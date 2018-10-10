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

import models.schemes.{CompanyDetails, IndividualContactDetails, IndividualInfo, PreviousAddressInfo}
import models.{CorrespondenceAddress, Samples}
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.Reads

class CompanyDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples with PSASchemeDetailsGenerator {

  "A JSON payload containing company or organisationDetails details" should {

    "read into a valid company or organisationDetails object" when {

      "we have a organisationName" in {
        forAll(companyOrOrganisationDetailsGenerator()) { companyDetails =>
          companyDetails.as(CompanyDetails.apiReads).organizationName mustBe (companyDetails \ "organisationName").as[String]
        }
      }

      "we have a utr" in {
        forAll(companyOrOrganisationDetailsGenerator()) { companyDetails =>
          companyDetails.as(CompanyDetails.apiReads).utr mustBe (companyDetails \ "utr").asOpt[String]
        }
      }

      "we don't have a utr" in {
        forAll(companyOrOrganisationDetailsGenerator()) { companyDetails =>
          val inputWithoutUTR = companyDetails - "utr"

          inputWithoutUTR.as(CompanyDetails.apiReads).utr mustBe None
        }
      }

      "we have a crnNumber" in {
        forAll(companyOrOrganisationDetailsGenerator()) { companyDetails =>
          companyDetails.as(CompanyDetails.apiReads).crn mustBe (companyDetails \ "crnNumber").asOpt[String]
        }
      }

      "we don't have a crnNumber" in {
        forAll(companyOrOrganisationDetailsGenerator()) { companyDetails =>
          val inputWithoutCrnNumber = companyDetails - "crnNumber"

          inputWithoutCrnNumber.as(CompanyDetails.apiReads).crn mustBe None
        }
      }

      "we have a vatRegistrationNumber" in {
        forAll(companyOrOrganisationDetailsGenerator()) { companyDetails =>
          companyDetails.as(CompanyDetails.apiReads).vatRegistration mustBe (companyDetails \ "vatRegistrationNumber").asOpt[String]
        }
      }

      "we don't have a vatRegistrationNumber" in {
        forAll(companyOrOrganisationDetailsGenerator()) { companyDetails =>
          val inputWithoutVatRegistrationNumber = companyDetails - "vatRegistrationNumber"

          inputWithoutVatRegistrationNumber.as(CompanyDetails.apiReads).vatRegistration mustBe None
        }
      }

      "we have a payeReference" in {
        forAll(companyOrOrganisationDetailsGenerator()) { companyDetails =>
          companyDetails.as(CompanyDetails.apiReads).payeRef mustBe (companyDetails \ "payeReference").asOpt[String]
        }
      }

      "we don't have a payeReference" in {
        forAll(companyOrOrganisationDetailsGenerator()) { companyDetails =>
          val inputWithoutPayeReference = companyDetails - "payeReference"

          inputWithoutPayeReference.as(CompanyDetails.apiReads).payeRef mustBe None
        }
      }

      "we have a correspondenceAddressDetails" in {
        forAll(companyOrOrganisationDetailsGenerator()) { companyDetails =>
          companyDetails.as(CompanyDetails.apiReads).address mustBe (companyDetails \ "correspondenceAddressDetails").as[CorrespondenceAddress]
        }
      }

      "we have a correspondenceContactDetails" in {
        forAll(companyOrOrganisationDetailsGenerator()) { companyDetails =>
          companyDetails.as(CompanyDetails.apiReads).contact mustBe (companyDetails \ "correspondenceContactDetails").as(IndividualContactDetails.apiReads)
        }
      }

      "we have a previousAddressDetails" in {
        forAll(companyOrOrganisationDetailsGenerator()) { companyDetails =>
          companyDetails.as(CompanyDetails.apiReads).previousAddress mustBe (companyDetails \ "previousAddressDetails").asOpt(PreviousAddressInfo.apiReads)
        }
      }

      "we don't have a previousAddressDetails" in {
        forAll(companyOrOrganisationDetailsGenerator()) { companyDetails =>
          val inputWithoutPreviousAddressDetails = companyDetails - "previousAddressDetails"

          inputWithoutPreviousAddressDetails.as(CompanyDetails.apiReads).previousAddress mustBe None
        }
      }

      "we have directorsDetails" in {
        forAll(companyOrOrganisationDetailsGenerator()) { companyDetails =>
          companyDetails.as(CompanyDetails.apiReads).directorsDetails mustBe (companyDetails \ "directorsDetails").as(
            Reads.seq(IndividualInfo.apiReads))
          companyDetails.as(CompanyDetails.apiReads).directorsDetails.length mustBe 1
        }
      }

      "we have a multiple directorsDetails" in {
        forAll(companyOrOrganisationDetailsGenerator(noOfElements = 2)) { companyDetails =>
          companyDetails.as(CompanyDetails.apiReads).directorsDetails mustBe (companyDetails \ "directorsDetails").as(
            Reads.seq(IndividualInfo.apiReads))
          companyDetails.as(CompanyDetails.apiReads).directorsDetails.length mustBe 2
        }
      }

      "we don't have a directorsDetails" in {
        forAll(companyOrOrganisationDetailsGenerator()) { companyDetails =>
          val inputWithoutDirectorsDetails = companyDetails - "directorsDetails"

          inputWithoutDirectorsDetails.as(CompanyDetails.apiReads).directorsDetails mustBe Nil
        }
      }
    }
  }

}
