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
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._

class SchemeDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues {
  "A JSON payload containing scheme details" should {

    val schemeDetails = Json.obj("srn" -> JsString("AAABA932JASDA"),
      "pstr" -> JsString("A3DCADAA"),
      "schemeStatus" -> "Pending",
      "schemeName" -> "Test Scheme",
      "isSchemeMasterTrust" -> JsBoolean(true),
      "pensionSchemeStructure" -> "Other",
      "otherPensionSchemeStructure" -> "Other type",
      "hasMoreThanTenTrustees" -> JsBoolean(true),
      "currentSchemeMembers" -> "1",
      "futureSchemeMembers" -> "2",
      "isReguledSchemeInvestment" -> JsBoolean(true),
      "isOccupationalPensionScheme" -> JsBoolean(true),
      "schemeProvideBenefits" -> "Defined Benefits only",
      "schemeEstablishedCountry" -> "GB",
      "isSchemeBenefitsInsuranceCompany" -> JsBoolean(true),
      "insuranceCompanyName" -> "Test Insurance",
      "policyNumber" -> "ADN3JDA",
      "insuranceCompanyAddressDetails" -> Json.obj("line1" -> JsString("line1"),
        "line2" -> JsString("line2"),
        "line3" -> JsString("line3"),
        "line4" -> JsString("line4"),
        "postalCode" -> JsString("NE1"),
        "countryCode" -> JsString("GB")))

    val output = schemeDetails.as[SchemeDetails]


    "correctly parse to a model of SchemeDetails" when {
      "we have a srn" in {
        output.srn.value mustBe (schemeDetails \ "srn").as[String]
      }

      "we don't have an srn" in {
        val output =  (schemeDetails - "srn").as[SchemeDetails]

        output.srn mustBe None
      }

      "we have a pstr" in {
        output.pstr.value mustBe (schemeDetails \ "pstr").as[String]
      }

      "we don't have pstr" in {
        val output = (schemeDetails - "pstr").as[SchemeDetails]

        output.pstr mustBe None
      }

      "we have a status" in {
        output.status mustBe (schemeDetails \ "schemeStatus").as[String]
      }

      "we have a name" in {
        output.name mustBe (schemeDetails \ "schemeName").as[String]
      }

      "we have a flag to say if it is a master trust" in {
        output.isMasterTrust mustBe (schemeDetails \ "isSchemeMasterTrust").as[Boolean]
      }

      "there is no flag to say it is a master trust so we assume it is not" in {
        val output = (schemeDetails - "isSchemeMasterTrust").as[SchemeDetails]

        output.isMasterTrust mustBe false
      }

      "we have a type of scheme" in {
        output.typeOfScheme.value mustBe (schemeDetails \ "pensionSchemeStructure").as[String]
      }

      "we don't have a type of scheme" in {
        val output = (schemeDetails - "pensionSchemeStructure").as[SchemeDetails]

        output.typeOfScheme mustBe None
      }

      "we have other types of schemes" in {
        output.otherTypeOfScheme.value mustBe (schemeDetails \ "otherPensionSchemeStructure").as[String]
      }

      "we don't have other types of scheme" in {
        val output = (schemeDetails - "otherPensionSchemeStructure").as[SchemeDetails]

        output.otherTypeOfScheme mustBe None
      }

      "we have a flag that tells us if there is more than 10 trustees" in {
        output.hasMoreThanTenTrustees mustBe (schemeDetails \ "hasMoreThanTenTrustees").as[Boolean]
      }

      "we don't have a flag that tells us if there is more than 10 trustees so we assume we haven't" in {
        val output = (schemeDetails - "hasMoreThanTenTrustees").as[SchemeDetails]

        output.hasMoreThanTenTrustees mustBe false
      }

      "we have current scheme members" in {
        output.members.current mustBe (schemeDetails \ "currentSchemeMembers").as[String]
      }

      "we have future scheme members" in {
        output.members.future mustBe (schemeDetails \ "futureSchemeMembers").as[String]
      }

      "we have an is regulated flag" in {
        output.isInvestmentRegulated mustBe (schemeDetails \ "isReguledSchemeInvestment").as[Boolean]
      }

      "we have an is occupational flag" in {
        output.isOccupational mustBe (schemeDetails \ "isOccupationalPensionScheme").as[Boolean]
      }

      "we have the way the scheme provides its benefits" in {
        output.benefits mustBe (schemeDetails \ "schemeProvideBenefits").as[String]
      }

      "we have a country" in {
        output.country mustBe (schemeDetails \ "schemeEstablishedCountry").as[String]
      }

      "we have a flag that tells us whether if the benefits are secured" in {
        output.areBenefitsSecured mustBe (schemeDetails \ "isSchemeBenefitsInsuranceCompany").as[Boolean]
      }

      "we have an insurance company name" in {
        output.insuranceCompany.value.name.value mustBe (schemeDetails \ "insuranceCompanyName").as[String]
      }

      "we don't have an insurance company name" in {
        val output = (schemeDetails - "insuranceCompanyName").as[SchemeDetails]

        output.insuranceCompany.value.name mustBe None
      }

      "we have an insurance policu number" in {
        output.insuranceCompany.value.policyNumber.value mustBe (schemeDetails \ "policyNumber").as[String]
      }

      "we don't have an insurance policy number" in {
        val output = (schemeDetails - "policyNumber").as[SchemeDetails]

        output.insuranceCompany.value.policyNumber mustBe None
      }

      "we have the address of the insurance company" in {
        output.insuranceCompany.value.address.value.addressLine1 mustBe (schemeDetails \ "insuranceCompanyAddressDetails" \ "line1").as[String]
      }

      "we don't have the address of the insurance company" in {
        val output = (schemeDetails - "insuranceCompanyAddressDetails").as[SchemeDetails]

        output.insuranceCompany.value.address mustBe None
      }

      "we don't have policy number, address or name for an insurance company" in {
        val output = (schemeDetails - "policyNumber" - "insuranceCompanyAddressDetails" - "insuranceCompanyName").as[SchemeDetails]

        output.insuranceCompany mustBe None
      }
    }
  }
}
