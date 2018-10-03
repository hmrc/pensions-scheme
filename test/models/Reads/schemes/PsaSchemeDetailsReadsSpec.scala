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

import models.schemes.PsaSchemeDetails
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._

class PsaSchemeDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues {
  "A JSON payload containing a PsaSchemeDetails" should {
    "Parse correctly to a PsaSchemeDetails object" when {
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
      val psaDetail1 = Json.obj("psaid" -> "2432374232", "organizationOrPartnershipName" -> "org name test", "firstName" -> "Mickey", "middleName" -> "m", "lastName" -> "Mouse")
      val psaDetail2 = Json.obj("psaid" -> "1234444444", "organizationOrPartnershipName" -> "org name test", "firstName" -> "Mickey", "middleName" -> "m", "lastName" -> "Mouse")

      val psaDetails = Json.arr(psaDetail1,psaDetail2)
      val psaSchemeDetails = Json.obj("psaSchemeDetails" -> Json.obj("schemeDetails" -> schemeDetails, "psaDetails" -> psaDetails))

      val output = psaSchemeDetails.as[PsaSchemeDetails]

      "we have a valid Scheme Details object within it" in {
        output.schemeDetails.srn.value mustBe (psaSchemeDetails \ "psaSchemeDetails" \ "schemeDetails" \ "srn").as[String]
      }

      "we have a valid list of psa details within it" in {
        output.psaDetails.value.head.id mustBe (psaSchemeDetails \ "psaSchemeDetails" \ "psaDetails" \ 0 \ "psaid").as[String]
        output.psaDetails.value(1).id mustBe (psaSchemeDetails \ "psaSchemeDetails" \ "psaDetails" \ 1 \ "psaid").as[String]
      }

      "we don't have psa details" in {
        val psaSchemeDetails = Json.obj("psaSchemeDetails" -> Json.obj("schemeDetails" -> schemeDetails))

        val output = psaSchemeDetails.as[PsaSchemeDetails]
        output.psaDetails mustBe None
      }
    }
  }
}