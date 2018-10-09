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

import models.schemes.SchemeDetails
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}

class SchemeDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues with PSASchemeDetailsGenerator {

  "A JSON payload containing scheme details" should {

    "correctly parse to a model of SchemeDetails" when {
      "we have a srn" in {
        forAll(schemeDetailsGenerator) { schemeDetails =>
          schemeDetails.as(SchemeDetails.apiReads).srn mustBe (schemeDetails \ "srn").asOpt[String]
        }
      }

      "we don't have an srn" in {
        forAll(schemeDetailsGenerator) { schemeDetails =>
          val output = (schemeDetails - "srn").as(SchemeDetails.apiReads)

          output.srn mustBe None
        }
      }

      "we have a pstr" in {
        forAll(schemeDetailsGenerator) { schemeDetails =>
          schemeDetails.as(SchemeDetails.apiReads).pstr mustBe (schemeDetails \ "pstr").asOpt[String]
        }
      }

      "we don't have pstr" in {
        forAll(schemeDetailsGenerator) { schemeDetails =>
          val output = (schemeDetails - "pstr").as(SchemeDetails.apiReads)

          output.pstr mustBe None
        }
      }

      "we have a status" in {
        forAll(schemeDetailsGenerator) { schemeDetails =>
          schemeDetails.as(SchemeDetails.apiReads).status mustBe (schemeDetails \ "schemeStatus").as[String]
        }
      }

      "we have a name" in {
        forAll(schemeDetailsGenerator) { schemeDetails =>
          schemeDetails.as(SchemeDetails.apiReads).name mustBe (schemeDetails \ "schemeName").as[String]
        }
      }

      "we have a flag to say if it is a master trust" in {
        forAll(schemeDetailsGenerator) { schemeDetails =>
          schemeDetails.as(SchemeDetails.apiReads).isMasterTrust mustBe (schemeDetails \ "isSchemeMasterTrust").as[Boolean]
        }
      }

      "there is no flag to say it is a master trust so we assume it is not" in {
        forAll(schemeDetailsGenerator) { schemeDetails =>
          val output = (schemeDetails - "isSchemeMasterTrust").as(SchemeDetails.apiReads)

          output.isMasterTrust mustBe false
        }
      }

      "we have a type of scheme" in {
        forAll(schemeDetailsGenerator) { schemeDetails =>
          schemeDetails.as(SchemeDetails.apiReads).typeOfScheme mustBe (schemeDetails \ "pensionSchemeStructure").asOpt[String]
        }
      }

      "we don't have a type of scheme" in {
        forAll(schemeDetailsGenerator) { schemeDetails =>
          val output = (schemeDetails - "pensionSchemeStructure").as(SchemeDetails.apiReads)

          output.typeOfScheme mustBe None
        }
      }

      "we have other types of schemes" in {
        forAll(schemeDetailsGenerator) { schemeDetails =>
          schemeDetails.as(SchemeDetails.apiReads).otherTypeOfScheme mustBe (schemeDetails \ "otherPensionSchemeStructure").asOpt[String]
        }
      }

      "we don't have other types of scheme" in {
        forAll(schemeDetailsGenerator) { schemeDetails =>
          val output = (schemeDetails - "otherPensionSchemeStructure").as(SchemeDetails.apiReads)

          output.otherTypeOfScheme mustBe None
        }
      }

      "we have a flag that tells us if there is more than 10 trustees" in {
        forAll(schemeDetailsGenerator) { schemeDetails =>
          schemeDetails.as(SchemeDetails.apiReads).hasMoreThanTenTrustees mustBe (schemeDetails \ "hasMoreThanTenTrustees").as[Boolean]
        }
      }

      "we don't have a flag that tells us if there is more than 10 trustees so we assume we haven't" in {
        forAll(schemeDetailsGenerator) { schemeDetails =>
          val output = (schemeDetails - "hasMoreThanTenTrustees").as(SchemeDetails.apiReads)

          output.hasMoreThanTenTrustees mustBe false
        }
      }

      "we have current scheme members" in {
        forAll(schemeDetailsGenerator) { schemeDetails =>
          schemeDetails.as(SchemeDetails.apiReads).members.current mustBe (schemeDetails \ "currentSchemeMembers").as[String]
        }
      }

      "we have future scheme members" in {
        forAll(schemeDetailsGenerator) { schemeDetails =>
          schemeDetails.as(SchemeDetails.apiReads).members.future mustBe (schemeDetails \ "futureSchemeMembers").as[String]
        }
      }

      "we have an is regulated flag" in {
        forAll(schemeDetailsGenerator) { schemeDetails =>
          schemeDetails.as(SchemeDetails.apiReads).isInvestmentRegulated mustBe (schemeDetails \ "isReguledSchemeInvestment").as[Boolean]
        }
      }

      "we have an is occupational flag" in {
        forAll(schemeDetailsGenerator) { schemeDetails =>
          schemeDetails.as(SchemeDetails.apiReads).isOccupational mustBe (schemeDetails \ "isOccupationalPensionScheme").as[Boolean]
        }
      }

      "we have the way the scheme provides its benefits" in {
        forAll(schemeDetailsGenerator) { schemeDetails =>
          schemeDetails.as(SchemeDetails.apiReads).benefits mustBe (schemeDetails \ "schemeProvideBenefits").as[String]
        }
      }

      "we have a country" in {
        forAll(schemeDetailsGenerator) { schemeDetails =>
          schemeDetails.as(SchemeDetails.apiReads).country mustBe (schemeDetails \ "schemeEstablishedCountry").as[String]
        }
      }

      "we have a flag that tells us whether if the benefits are secured" in {
        forAll(schemeDetailsGenerator) { schemeDetails =>
          schemeDetails.as(SchemeDetails.apiReads).areBenefitsSecured mustBe (schemeDetails \ "isSchemeBenefitsInsuranceCompany").as[Boolean]
        }
      }

      "we have an insurance company name" in {
        forAll(schemeDetailsGenerator(withCompanyAddress = true)) { schemeDetails =>
          schemeDetails.as(SchemeDetails.apiReads).insuranceCompany.value.name.value mustBe (schemeDetails \ "insuranceCompanyName").as[String]
        }
      }

      "we don't have an insurance company name" in {
        forAll(schemeDetailsGenerator(withCompanyAddress = true)) { schemeDetails =>
          val output = (schemeDetails - "insuranceCompanyName").as(SchemeDetails.apiReads)

          output.insuranceCompany.value.name mustBe None
        }
      }

      "we have an insurance policu number" in {
        forAll(schemeDetailsGenerator(withCompanyAddress = true)) { schemeDetails =>
          schemeDetails.as(SchemeDetails.apiReads).insuranceCompany.value.policyNumber.value mustBe (schemeDetails \ "policyNumber").as[String]
        }
      }

      "we don't have an insurance policy number" in {
        forAll(schemeDetailsGenerator(withCompanyAddress = true)) { schemeDetails =>
          val output = (schemeDetails - "policyNumber").as(SchemeDetails.apiReads)

          output.insuranceCompany.value.policyNumber mustBe None
        }
      }

      "we have the address of the insurance company" in {
        forAll(schemeDetailsGenerator(withCompanyAddress = true)) { schemeDetails =>
          schemeDetails.as(SchemeDetails.apiReads).insuranceCompany.value.address.value.addressLine1 mustBe (schemeDetails \ "insuranceCompanyAddressDetails" \ "line1").as[String]
        }
      }

      "we don't have the address of the insurance company" in {
        forAll(schemeDetailsGenerator(withCompanyAddress = true)) { schemeDetails =>
          val output = (schemeDetails - "insuranceCompanyAddressDetails").as(SchemeDetails.apiReads)

          output.insuranceCompany.value.address mustBe None
        }
      }

      "we don't have policy number, address or name for an insurance company" in {
        forAll(schemeDetailsGenerator) { schemeDetails =>
          val output = (schemeDetails - "policyNumber" - "insuranceCompanyAddressDetails" - "insuranceCompanyName").as(SchemeDetails.apiReads)

          output.insuranceCompany mustBe None
        }
      }
    }
  }
}
