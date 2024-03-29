/*
 * Copyright 2024 HM Revenue & Customs
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

package models.userAnswersToEtmp

import models.enumeration.{SchemeType, SchemeMembers, Benefits}
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class CustomerAndSchemeDetails(schemeName: String, isSchemeMasterTrust: Boolean, schemeStructure: Option[String],
                                    otherSchemeStructure: Option[String] = None, haveMoreThanTenTrustee: Option[Boolean] = None, currentSchemeMembers: String,
                                    futureSchemeMembers: String, isRegulatedSchemeInvestment: Boolean, isOccupationalPensionScheme: Boolean,
                                    areBenefitsSecuredContractInsuranceCompany: Boolean, doesSchemeProvideBenefits: String, tcmpBenefitType: Option[String],
                                    schemeEstablishedCountry: String, haveInvalidBank: Boolean, insuranceCompanyName: Option[String] = None,
                                    policyNumber: Option[String] = None, insuranceCompanyAddress: Option[Address] = None,
                                    isInsuranceDetailsChanged: Option[Boolean] = None, isTcmpChanged: Option[Boolean] = None)

object CustomerAndSchemeDetails {
  implicit val formats: Format[CustomerAndSchemeDetails] = Json.format[CustomerAndSchemeDetails]

  private val schemeTypeReads: Reads[(String, Option[String])] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "schemeTypeDetails").readNullable[String]
    ) ((name, schemeDetails) => (name, schemeDetails))

  def apiReads: Reads[CustomerAndSchemeDetails] = (
    (JsPath \ "schemeName").read[String] and
      (JsPath \ "schemeType").read[(String, Option[String])](schemeTypeReads) and
      (JsPath \ "moreThanTenTrustees").readNullable[Boolean] and
      (JsPath \ "membership").read[String] and
      (JsPath \ "membershipFuture").read[String] and
      (JsPath \ "investmentRegulated").read[Boolean] and
      (JsPath \ "occupationalPensionScheme").read[Boolean] and
      (JsPath \ "securedBenefits").read[Boolean] and
      (JsPath \ "schemeEstablishedCountry").read[String] and
      (JsPath \ "insuranceCompanyName").readNullable[String] and
      (JsPath \ "insurancePolicyNumber").readNullable[String] and
      (JsPath \ "insurerAddress").readNullable[Address] and
      (JsPath \ "isInsuranceDetailsChanged").readNullable[Boolean] and
      benefitsReads
    ) (
    (name, schemeType, moreThanTenTrustees, membership, membershipFuture, investmentRegulated, occupationalPension, securedBenefits, country,
     insuranceCompanyName, insurancePolicyNumber, insurerAddress, isInsuranceDetailsChanged, benefits) => {
      val (schemeName, otherScheme) = schemeType
      val isMasterTrust = schemeName == "master"

      val schemeTypeName = if (isMasterTrust) None else Some(SchemeType.valueWithName(schemeName))

      CustomerAndSchemeDetails(
        schemeName = name,
        isSchemeMasterTrust = isMasterTrust,
        schemeStructure = schemeTypeName,
        otherSchemeStructure = otherScheme,
        haveMoreThanTenTrustee = moreThanTenTrustees,
        currentSchemeMembers = SchemeMembers.valueWithName(membership),
        futureSchemeMembers = SchemeMembers.valueWithName(membershipFuture),
        isRegulatedSchemeInvestment = investmentRegulated,
        isOccupationalPensionScheme = occupationalPension,
        areBenefitsSecuredContractInsuranceCompany = securedBenefits,
        doesSchemeProvideBenefits = benefits._1,
        tcmpBenefitType = benefits._2,
        schemeEstablishedCountry = country,
        haveInvalidBank = false,
        insuranceCompanyName = insuranceCompanyName,
        policyNumber = insurancePolicyNumber,
        insuranceCompanyAddress = insurerAddress,
        isInsuranceDetailsChanged = isInsuranceDetailsChanged
      )
    }
  )

  def updateReads: Reads[CustomerAndSchemeDetails] = (
    (JsPath \ "schemeName").read[String] and
      (JsPath \ "schemeType").read[(String, Option[String])](schemeTypeReads) and
      (JsPath \ "moreThanTenTrustees").readNullable[Boolean] and
      (JsPath \ "membership").read[String] and
      (JsPath \ "membershipFuture").read[String] and
      (JsPath \ "investmentRegulated").read[Boolean] and
      (JsPath \ "occupationalPensionScheme").read[Boolean] and
      (JsPath \ "securedBenefits").read[Boolean] and
      (JsPath \ "schemeEstablishedCountry").read[String] and
      (JsPath \ "insuranceCompanyName").readNullable[String] and
      (JsPath \ "insurancePolicyNumber").readNullable[String] and
      (JsPath \ "insurerAddress").readNullable[Address] and
      (JsPath \ "isInsuranceDetailsChanged").readNullable[Boolean] and
      (JsPath \ "isTcmpChanged").readNullable[Boolean] and
      benefitsReads
    ) (
    (name, schemeType, moreThanTenTrustees, membership, membershipFuture, investmentRegulated, occupationalPension, securedBenefits, country,
     insuranceCompanyName, insurancePolicyNumber, insurerAddress, isInsuranceDetailsChanged, isTcmpChanged, benefits) => {
      val (schemeName, otherScheme) = schemeType
      val isMasterTrust = schemeName == "master"

      val schemeTypeName = if (isMasterTrust) None else Some(SchemeType.valueWithName(schemeName))

      CustomerAndSchemeDetails(
        schemeName = name,
        isSchemeMasterTrust = isMasterTrust,
        schemeStructure = schemeTypeName,
        otherSchemeStructure = otherScheme,
        haveMoreThanTenTrustee = moreThanTenTrustees,
        currentSchemeMembers = SchemeMembers.valueWithName(membership),
        futureSchemeMembers = SchemeMembers.valueWithName(membershipFuture),
        isRegulatedSchemeInvestment = investmentRegulated,
        isOccupationalPensionScheme = occupationalPension,
        areBenefitsSecuredContractInsuranceCompany = securedBenefits,
        doesSchemeProvideBenefits = benefits._1,
        tcmpBenefitType = benefits._2,
        schemeEstablishedCountry = country,
        haveInvalidBank = false,
        insuranceCompanyName = insuranceCompanyName,
        policyNumber = insurancePolicyNumber,
        insuranceCompanyAddress = insurerAddress,
        isInsuranceDetailsChanged = isInsuranceDetailsChanged,
        isTcmpChanged = isTcmpChanged
      )
    }
  )

  private def benefitsReads: Reads[(String, Option[String])] =
    (JsPath \ "benefits").read[String] flatMap {
      case benefits if !benefits.equalsIgnoreCase("opt2") =>
        moneyPurchaseBenefits(benefits)
      case _ =>
        (JsPath \ "benefits").read[String].map(benefits =>
          (Benefits.valueWithName(benefits), None: Option[String]))
    }

  private def moneyPurchaseBenefits(benefits: String): Reads[(String, Option[String])] =
    (JsPath \ "moneyPurchaseBenefits").read[String].map {
      moneyPurchaseBenefits =>
        (Benefits.valueWithName(benefits), Some(moneyPurchaseBenefits))
    }


  private val insuranceCompanyWrite: Writes[(Boolean, Boolean, Option[String], Option[String], Option[Address])] = {
    ((JsPath \ "isInsuranceDetailsChanged").write[Boolean] and
      (JsPath \ "isSchemeBenefitsInsuranceCompany").write[Boolean] and
      (JsPath \ "insuranceCompanyName").writeNullable[String] and
      (JsPath \ "policyNumber").writeNullable[String] and
      (JsPath \ "insuranceCompanyAddressDetails").writeNullable[Address](Address.updateWrites)
      ) (element => element)
  }

  def updateWrites(psaid: String): Writes[CustomerAndSchemeDetails] = (
    (JsPath \ "changeOfschemeDetails").write[Boolean] and
      (JsPath \ "psaid").write[String] and
      (JsPath \ "schemeName").write[String] and
      (JsPath \ "schemeStatus").write[String] and
      (JsPath \ "isSchemeMasterTrust").write[Boolean] and
      (JsPath \ "pensionSchemeStructure").writeNullable[String] and
      (JsPath \ "otherPensionSchemeStructure").writeNullable[String] and
      (JsPath \ "currentSchemeMembers").write[String] and
      (JsPath \ "futureSchemeMembers").write[String] and
      (JsPath \ "isRegulatedSchemeInvestment").write[Boolean] and
      (JsPath \ "isOccupationalPensionScheme").write[Boolean] and
      (JsPath \ "schemeProvideBenefits").write[String] and
      (JsPath \ "tcmpBenefitType").writeNullable[String] and
      (JsPath \ "schemeEstablishedCountry").write[String] and
      (JsPath \ "insuranceCompanyDetails").write(insuranceCompanyWrite)
    ) (
    scheme => (
      scheme.isTcmpChanged.getOrElse(false),
      psaid,
      scheme.schemeName,
      "Open",
      scheme.isSchemeMasterTrust,
      scheme.schemeStructure,
      scheme.otherSchemeStructure,
      scheme.currentSchemeMembers,
      scheme.futureSchemeMembers,
      scheme.isRegulatedSchemeInvestment,
      scheme.isOccupationalPensionScheme,
      scheme.doesSchemeProvideBenefits,
      scheme.tcmpBenefitType,
      scheme.schemeEstablishedCountry,
      (
        scheme.isInsuranceDetailsChanged.getOrElse(false),
        scheme.areBenefitsSecuredContractInsuranceCompany,
        scheme.insuranceCompanyName,
        scheme.policyNumber,
        scheme.insuranceCompanyAddress
      )
    )
  )
}
