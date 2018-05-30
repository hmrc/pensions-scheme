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

package models.Reads

import models.{CustomerAndSchemeDetails, UkAddress}
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json._

class CustomerAndSchemeDetailsReadsSpec extends WordSpec with MustMatchers {

  import CustomerAndSchemeDetailsReadsSpec._

  "Json Payload containing Customer and scheme details" must {

    "correctly parse to the corresponding CustomerAndSchemeDetailsReads" when {

      "we have a valid scheme name" in {
        val result = dataJson.as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)
        result.schemeName mustBe customerDetails.schemeName
      }

      "we have a scheme type which is not master trust" in {
        val result = dataJson.as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)
        result.isSchemeMasterTrust mustBe customerDetails.isSchemeMasterTrust
      }

      "we have scheme structure" that {
        "is Single Trust with no other scheme structure" in {
          val result = dataJson.as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)
          result.schemeStructure mustBe customerDetails.schemeStructure
          result.otherSchemeStructure mustBe None
        }

        "is Group Life/Death" in {
          val result = (dataJson + ("schemeDetails" -> Json.obj(
            "schemeName" -> "test scheme name",
            "schemeType" -> Json.obj(
              "name" -> "group"
            )))).as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)

          result.schemeStructure mustBe customerDetails.copy(schemeStructure = "A group life/death in service scheme").schemeStructure
        }

        "is Body Corporate" in {
          val result = (dataJson + ("schemeDetails" -> Json.obj(
            "schemeName" -> "test scheme name",
            "schemeType" -> Json.obj(
              "name" -> "corp"
            )))).as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)

          result.schemeStructure mustBe customerDetails.copy(schemeStructure = "A body corporate").schemeStructure
        }

        "is Other with other Scheme structure" in {
          val result = (dataJson + ("schemeDetails" -> Json.obj(
            "schemeName" -> "test scheme name",
            "schemeType" -> Json.obj(
              "name" -> "other",
              "schemeTypeDetails" -> "other details"
            )))).as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)

          result.schemeStructure mustBe customerDetails.copy(schemeStructure = "Other").schemeStructure
        }
      }

      "we have a valid more than ten trustees flag" in {
        val result = (dataJson + ("moreThanTenTrustees" -> JsBoolean(true))).as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)
        result.haveMoreThanTenTrustee mustBe customerDetails.haveMoreThanTenTrustee
      }

      "we don't have more than ten trustees flag" in {
        val result = dataJson.as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)
        result.haveMoreThanTenTrustee mustBe None
      }

      "we have a valid membership" in {
        val result = dataJson.as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)
        result.currentSchemeMembers mustBe customerDetails.currentSchemeMembers
      }

      "we have a valid future membership" in {
        val result = dataJson.as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)
        result.futureSchemeMembers mustBe customerDetails.futureSchemeMembers
      }

      "we have a valid investment regulated flag" in {
        val result = dataJson.as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)
        result.isReguledSchemeInvestment mustBe customerDetails.isReguledSchemeInvestment
      }

      "we have a valid occupational pension scheme flag" in {
        val result = dataJson.as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)
        result.isOccupationalPensionScheme mustBe customerDetails.isOccupationalPensionScheme
      }

      "we have a valid secured benefits flag" in {
        val result = dataJson.as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)
        result.areBenefitsSecuredContractInsuranceCompany mustBe customerDetails.areBenefitsSecuredContractInsuranceCompany
      }

      "we have a valid scheme benefits" in {
        val result = dataJson.as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)
        result.doesSchemeProvideBenefits mustBe customerDetails.doesSchemeProvideBenefits
      }

      "we have a valid scheme established country" in {
        val result = dataJson.as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)
        result.schemeEstablishedCountry mustBe customerDetails.schemeEstablishedCountry
      }

      "we have a invalid bank account as false" in {
        val result = dataJson.as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)
        result.haveInvalidBank mustBe customerDetails.haveInvalidBank
      }

      "we have benefits insurer" that {
        "is with valid insurance company name but no policy number" in {
          val json = dataJson + ("benefitsInsurer" -> Json.obj(
            "companyName" -> "my insurance company"))
          val result = json.as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)
          result.insuranceCompanyName mustBe customerDetails.insuranceCompanyName
          result.policyNumber mustBe None
        }

        "is with valid policy number but no company name" in {
          val json = dataJson + ("benefitsInsurer" -> Json.obj(
            "policyNumber" -> "111"))
          val result = json.as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)
          result.policyNumber mustBe customerDetails.policyNumber
          result.insuranceCompanyName mustBe None
        }

        "is with valid policy number and insurance company name" in {
          val json = dataJson + ("benefitsInsurer" -> Json.obj(
            "companyName" -> "my insurance company",
            "policyNumber" -> "111"))
          val result = json.as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)
          result.insuranceCompanyName mustBe customerDetails.insuranceCompanyName
          result.policyNumber mustBe customerDetails.policyNumber
        }
      }

      "we don't have benefits insurer" in {
        val result = dataJson.as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)
        result.insuranceCompanyName mustBe customerDetails.copy(insuranceCompanyName = None, policyNumber = None).insuranceCompanyName
      }

      "we have benefits insurer address" in {
        val result = (dataJson + ("insurerAddress" -> Json.obj(
          "addressLine1" -> "ADDRESS LINE 1",
          "addressLine2" -> "ADDRESS LINE 2",
          "addressLine3" -> "ADDRESS LINE 3",
          "addressLine4" -> "ADDRESS LINE 4",
          "postcode" -> "ZZ1 1ZZ",
          "country" -> "GB"))).as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)
        result.insuranceCompanyAddress mustBe customerDetails.insuranceCompanyAddress
      }

      "we don't have benefits insurer address" in {
        val result = dataJson.as[CustomerAndSchemeDetails](CustomerAndSchemeDetails.apiReads)
        result.insuranceCompanyAddress mustBe None
      }
    }
  }
}

object CustomerAndSchemeDetailsReadsSpec {

  val dataJson: JsObject = Json.obj(
    "schemeDetails" -> Json.obj(
      "schemeName" -> "test scheme name",
      "schemeType" -> Json.obj(
        "name" -> "single"
      )
    ),
    "membership" -> "opt3",
    "membershipFuture" -> "opt1",
    "investmentRegulated" -> true,
    "occupationalPensionScheme" -> true,
    "securedBenefits" -> true,
    "benefits" -> "opt2",
    "schemeEstablishedCountry" -> "GB",
    "uKBankAccount" -> true
  )

  val customerDetails = CustomerAndSchemeDetails("test scheme name", false, "A single trust under which all" +
    " of the assets are held for the benefit of all members of the scheme", Some("other details"),
    Some(true), "2 to 11", "0", true, true, true, "Defined Benefits only", "GB", false,
    Some("my insurance company"), Some("111"), Some(UkAddress("ADDRESS LINE 1", Some("ADDRESS LINE 2"),
      Some("ADDRESS LINE 3"), Some("ADDRESS LINE 4"), "GB", "ZZ1 1ZZ")))

}
