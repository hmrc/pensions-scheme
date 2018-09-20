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

import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.functional.syntax._
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
      "policyNumber" -> "ADN3JDA")

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
        output.currentNumberOfMembers mustBe (schemeDetails \ "currentSchemeMembers").as[String]
      }

      "we have future scheme members" in {
        output.futureNumberOfMembers mustBe (schemeDetails \ "futureSchemeMembers").as[String]
      }

      "we have an is regulated flag" in {
        output.isInvestmentedRegulated mustBe (schemeDetails \ "isReguledSchemeInvestment").as[Boolean]
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
        output.insuranceName.value mustBe (schemeDetails \ "insuranceCompanyName").as[String]
      }

      "we don't have an insurance company name" in {
        val output = (schemeDetails - "insuranceCompanyName").as[SchemeDetails]

        output.insuranceName mustBe None
      }

      "we have an insurance police number" in {
        output.policeNumber.value mustBe (schemeDetails \ "policyNumber").as[String]
      }

      "we don't have an insurance police number" in {
        val output = (schemeDetails - "policyNumber").as[SchemeDetails]

        output.policeNumber mustBe None
      }
    }
  }
}

case class SchemeDetails(srn: Option[String],
                         pstr: Option[String],
                         status: String,
                         name: String,
                         isMasterTrust: Boolean,
                         typeOfScheme: Option[String],
                         otherTypeOfScheme: Option[String],
                         hasMoreThanTenTrustees: Boolean,
                         currentNumberOfMembers: String,
                         futureNumberOfMembers: String,
                         isInvestmentedRegulated: Boolean,
                         isOccupational: Boolean,
                         benefits: String,
                         country: String,
                         areBenefitsSecured: Boolean,
                         insuranceName: Option[String],
                         policeNumber: Option[String])

object SchemeDetails {
  implicit val reads : Reads[SchemeDetails] = (
    (JsPath \ "srn").readNullable[String] and
      (JsPath \ "pstr").readNullable[String] and
    (JsPath \ "schemeStatus").read[String] and
      (JsPath \ "schemeName").read[String] and
      (JsPath \ "isSchemeMasterTrust").readNullable[Boolean] and
      (JsPath \ "pensionSchemeStructure").readNullable[String] and
      (JsPath \ "otherPensionSchemeStructure").readNullable[String] and
      (JsPath \ "hasMoreThanTenTrustees").readNullable[Boolean] and
      (JsPath \ "currentSchemeMembers").read[String] and
      (JsPath \ "futureSchemeMembers").read[String] and
      (JsPath \ "isReguledSchemeInvestment").read[Boolean] and
      (JsPath \ "isOccupationalPensionScheme").read[Boolean] and
      (JsPath \ "schemeProvideBenefits").read[String] and
      (JsPath \ "schemeEstablishedCountry").read[String] and
      (JsPath \ "isSchemeBenefitsInsuranceCompany").read[Boolean] and
      (JsPath \ "insuranceCompanyName").readNullable[String] and
      (JsPath \ "policyNumber").readNullable[String]
  )((srn,pstr,status,name,isMasterTrust,typeOfScheme,otherTypeOfScheme,moreThan10Trustees,members,futureMembers,isRegulated,isOccupational,benefits,country,benefitsSecured,insuranceName,policy) =>
    SchemeDetails(srn,pstr,status,name,isMasterTrust.getOrElse(false),typeOfScheme,otherTypeOfScheme,moreThan10Trustees.getOrElse(false),members,
      futureMembers,isRegulated,isOccupational,benefits,country,benefitsSecured,insuranceName,policy))
  implicit val writes : Writes[SchemeDetails] = Json.writes[SchemeDetails]
}
