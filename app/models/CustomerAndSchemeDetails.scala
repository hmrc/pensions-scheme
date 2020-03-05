/*
 * Copyright 2020 HM Revenue & Customs
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

package models

import models.enumeration.{Benefits, SchemeMembers, SchemeType}
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class CustomerAndSchemeDetails(schemeName: String, isSchemeMasterTrust: Boolean, schemeStructure: Option[String],
                                    otherSchemeStructure: Option[String] = None, haveMoreThanTenTrustee: Option[Boolean] = None,
                                    currentSchemeMembers: String, futureSchemeMembers: String, isReguledSchemeInvestment: Boolean,
                                    isOccupationalPensionScheme: Boolean, areBenefitsSecuredContractInsuranceCompany: Boolean,
                                    doesSchemeProvideBenefits: String, schemeEstablishedCountry: String, haveInvalidBank: Boolean,
                                    insuranceCompanyName: Option[String] = None, policyNumber: Option[String] = None,
                                    insuranceCompanyAddress: Option[Address] = None, isInsuranceDetailsChanged: Option[Boolean] = None)

object CustomerAndSchemeDetails {
  implicit val formats: Format[CustomerAndSchemeDetails] = Json.format[CustomerAndSchemeDetails]

  def insurerReads: Reads[(Option[String], Option[String])] = (
    ((JsPath \ "companyName").readNullable[String] and
      (JsPath \ "policyNumber").readNullable[String])
      ((companyName, policyNumber) => (companyName, policyNumber))
    )

  def schemeTypeReads: Reads[(String, Option[String])] = (
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
      (JsPath \ "benefits").read[String] and
      (JsPath \ "schemeEstablishedCountry").read[String] and
      (JsPath \ "insuranceCompanyName").readNullable[String] and
      (JsPath \ "insurancePolicyNumber").readNullable[String] and
      (JsPath \ "insurerAddress").readNullable[Address] and
      (JsPath \ "isInsuranceDetailsChanged").readNullable[Boolean]
    ) (
    (name, schemeType, moreThanTenTrustees, membership, membershipFuture, investmentRegulated,
     occupationalPension, securedBenefits, benefits, country, insuranceCompanyName, insurancePolicyNumber, insurerAddress, isInsuranceDetailsChanged) => {

      val isMasterTrust = schemeType._1 == "master"

      val schemeTypeName = if (isMasterTrust) None else Some(SchemeType.valueWithName(schemeType._1))

      CustomerAndSchemeDetails(
        schemeName = name,
        isSchemeMasterTrust = isMasterTrust,
        schemeStructure = schemeTypeName,
        otherSchemeStructure = schemeType._2,
        haveMoreThanTenTrustee = moreThanTenTrustees,
        currentSchemeMembers = SchemeMembers.valueWithName(membership),
        futureSchemeMembers = SchemeMembers.valueWithName(membershipFuture),
        isReguledSchemeInvestment = investmentRegulated,
        isOccupationalPensionScheme = occupationalPension,
        areBenefitsSecuredContractInsuranceCompany = securedBenefits,
        doesSchemeProvideBenefits = Benefits.valueWithName(benefits),
        schemeEstablishedCountry = country,
        haveInvalidBank = false,
        insuranceCompanyName = insuranceCompanyName,
        policyNumber = insurancePolicyNumber,
        insuranceCompanyAddress = insurerAddress,
        isInsuranceDetailsChanged = isInsuranceDetailsChanged)
    }
  )

  private val insuranceCompanyWrite: Writes[(Boolean, Boolean, Option[String], Option[String], Option[Address])] ={
    ((JsPath \ "isInsuranceDetailsChanged").write[Boolean] and
      (JsPath \ "isSchemeBenefitsInsuranceCompany").write[Boolean] and
      (JsPath \ "insuranceCompanyName").writeNullable[String] and
      (JsPath \ "policyNumber").writeNullable[String] and
      (JsPath \ "insuranceCompanyAddressDetails").writeNullable[Address](Address.updateWrites)
      )(element => element)
  }

  def updateWrites(psaid: String): Writes[CustomerAndSchemeDetails] = (
    (JsPath \ "psaid").write[String] and
      (JsPath \ "schemeName").write[String] and
      (JsPath \ "schemeStatus").write[String] and
      (JsPath \ "isSchemeMasterTrust").write[Boolean] and
      (JsPath \ "pensionSchemeStructure").writeNullable[String] and
      (JsPath \ "otherPensionSchemeStructure").writeNullable[String] and
      (JsPath \ "currentSchemeMembers").write[String] and
      (JsPath \ "futureSchemeMembers").write[String] and
      (JsPath \ "isReguledSchemeInvestment").write[Boolean] and
      (JsPath \ "isOccupationalPensionScheme").write[Boolean] and
      (JsPath \ "schemeProvideBenefits").write[String] and
      (JsPath \ "schemeEstablishedCountry").write[String] and
      (JsPath \ "insuranceCompanyDetails").write(insuranceCompanyWrite)
    ) (scheme => (psaid,
    scheme.schemeName,
    "Open",
    scheme.isSchemeMasterTrust,
    scheme.schemeStructure,
    scheme.otherSchemeStructure,
    scheme.currentSchemeMembers,
    scheme.futureSchemeMembers,
    scheme.isReguledSchemeInvestment,
    scheme.isOccupationalPensionScheme,
    scheme.doesSchemeProvideBenefits,
    scheme.schemeEstablishedCountry,
    (scheme.isInsuranceDetailsChanged.getOrElse(false),
      scheme.areBenefitsSecuredContractInsuranceCompany,
      scheme.insuranceCompanyName,
      scheme.policyNumber,
      scheme.insuranceCompanyAddress)
  ))
}
