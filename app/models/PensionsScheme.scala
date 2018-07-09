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

package models

import models.enumeration.{Benefits, SchemeMembers, SchemeType}
import play.api.libs.json.{Format, JsPath, Json, Reads, Writes}
import play.api.libs.functional.syntax._
import utils.Lens

case class AddressAndContactDetails(addressDetails: Address, contactDetails: ContactDetails)

object AddressAndContactDetails {
  implicit val formats: Format[AddressAndContactDetails] = Json.format[AddressAndContactDetails]
}

case class PersonalDetails(title: Option[String] = None, firstName: String, middleName: Option[String] = None,
                           lastName: String, dateOfBirth: String)

object PersonalDetails {
  implicit val formats: Format[PersonalDetails] = Json.format[PersonalDetails]
}

case class PreviousAddressDetails(isPreviousAddressLast12Month: Boolean,
                                  previousAddressDetails: Option[Address] = None)

object PreviousAddressDetails {
  implicit val formats: Format[PreviousAddressDetails] = Json.format[PreviousAddressDetails]

  val psaSubmissionWrites : Writes[PreviousAddressDetails] = (
    (JsPath \ "isPreviousAddressLast12Month").write[Boolean] and
      (JsPath \ "previousAddressDetail").writeNullable[Address]
    )(previousAddress => (previousAddress.isPreviousAddressLast12Month,previousAddress.previousAddressDetails))

  def apiReads(typeOfAddressDetail: String): Reads[PreviousAddressDetails] = (
    (JsPath \ s"${typeOfAddressDetail}AddressYears").read[String] and
      (JsPath \ s"${typeOfAddressDetail}PreviousAddress").readNullable[Address]
    ) ((addressLast12Months, address) => {
    val isAddressLast12Months = if (addressLast12Months == "under_a_year") true else false
    PreviousAddressDetails(isAddressLast12Months, address)
  })
}

case class CorrespondenceAddressDetails(addressDetails: Address)

object CorrespondenceAddressDetails {
  implicit val formats: Format[CorrespondenceAddressDetails] = Json.format[CorrespondenceAddressDetails]
}

case class CorrespondenceContactDetails(contactDetails: ContactDetails)

object CorrespondenceContactDetails {
  implicit val formats: Format[CorrespondenceContactDetails] = Json.format[CorrespondenceContactDetails]
}

case class CustomerAndSchemeDetails(schemeName: String, isSchemeMasterTrust: Boolean, schemeStructure: Option[String],
                                    otherSchemeStructure: Option[String] = None, haveMoreThanTenTrustee: Option[Boolean] = None,
                                    currentSchemeMembers: String, futureSchemeMembers: String, isReguledSchemeInvestment: Boolean,
                                    isOccupationalPensionScheme: Boolean, areBenefitsSecuredContractInsuranceCompany: Boolean,
                                    doesSchemeProvideBenefits: String, schemeEstablishedCountry: String, haveInvalidBank: Boolean,
                                    insuranceCompanyName: Option[String] = None, policyNumber: Option[String] = None,
                                    insuranceCompanyAddress: Option[Address] = None)

object CustomerAndSchemeDetails {
  implicit val formats: Format[CustomerAndSchemeDetails] = Json.format[CustomerAndSchemeDetails]

  def insurerReads: Reads[(Option[String], Option[String])] = (
    ((JsPath \ "companyName").readNullable[String] and
      (JsPath \ "policyNumber").readNullable[String])
      ((companyName, policyNumber) => (companyName, policyNumber))
    )

  def schemeTypeReads : Reads[(Option[String],Option[String])] = (
    (JsPath \ "name").readNullable[String] and
      (JsPath \ "schemeTypeDetails").readNullable[String]
  )((name,schemeDetails)=>(name,schemeDetails))


  def apiReads: Reads[CustomerAndSchemeDetails] = (
    (JsPath \ "schemeDetails" \ "schemeName").read[String] and
      (JsPath \ "schemeDetails" \ "schemeType").readNullable[(Option[String],Option[String])](schemeTypeReads) and
      (JsPath \ "schemeDetails" \ "isSchemeMasterTrust").read[Boolean] and
      (JsPath \ "moreThanTenTrustees").readNullable[Boolean] and
      (JsPath \ "membership").read[String] and
      (JsPath \ "membershipFuture").read[String] and
      (JsPath \ "investmentRegulated").read[Boolean] and
      (JsPath \ "occupationalPensionScheme").read[Boolean] and
      (JsPath \ "securedBenefits").read[Boolean] and
      (JsPath \ "benefits").read[String] and
      (JsPath \ "schemeEstablishedCountry").read[String] and
      (JsPath \ "benefitsInsurer").readNullable(insurerReads) and
      (JsPath \ "insurerAddress").readNullable[Address]
    ) (
    (name, schemeDetails, isSchemeMasterTrust, moreThanTenTrustees, membership, membershipFuture, investmentRegulated,
     occupationalPension, securedBenefits, benefits, country, benefitsInsurer, insurerAddress) => {
      CustomerAndSchemeDetails(
        schemeName = name,
        isSchemeMasterTrust = isSchemeMasterTrust,
        schemeStructure = schemeDetails.flatMap(c=>c._1.map(z=>SchemeType.valueWithName(z))),
        otherSchemeStructure = schemeDetails.flatMap(c=>c._2.map(z=>z)),
        haveMoreThanTenTrustee = moreThanTenTrustees,
        currentSchemeMembers = SchemeMembers.valueWithName(membership),
        futureSchemeMembers = SchemeMembers.valueWithName(membershipFuture),
        isReguledSchemeInvestment = investmentRegulated,
        isOccupationalPensionScheme = occupationalPension,
        areBenefitsSecuredContractInsuranceCompany = securedBenefits,
        doesSchemeProvideBenefits = Benefits.valueWithName(benefits),
        schemeEstablishedCountry = country,
        haveInvalidBank = false,
        insuranceCompanyName = benefitsInsurer.flatMap(_._1),
        policyNumber = benefitsInsurer.flatMap(_._2),
        insuranceCompanyAddress = insurerAddress)
    }
  )
}

case class AdviserDetails(adviserName: String, emailAddress: String, phoneNumber: String)

object AdviserDetails {

  implicit val formats: Format[PensionSchemeDeclaration] = Json.format[PensionSchemeDeclaration]

  implicit val readsAdviserDetails: Reads[AdviserDetails] = (
    (JsPath \ "adviserName").read[String] and
      (JsPath \ "emailAddress").read[String] and
      (JsPath \ "phoneNumber").read[String]
    ) ((name, email, phone) => AdviserDetails(name, email, phone))
}


case class PensionSchemeDeclaration(box1: Boolean, box2: Boolean, box3: Option[Boolean] = None, box4: Option[Boolean] = None,
                                    box5: Option[Boolean] = None, box6: Boolean, box7: Boolean, box8: Boolean, box9: Boolean,
                                    box10: Option[Boolean] = None, box11: Option[Boolean] = None, pensionAdviserName: Option[String] = None,
                                    addressAndContactDetails: Option[AddressAndContactDetails] = None)
object PensionSchemeDeclaration {

  implicit val formats: Format[PensionSchemeDeclaration] = Json.format[PensionSchemeDeclaration]

  val apiReads: Reads[PensionSchemeDeclaration] = (
    (JsPath \ "declaration").read[Boolean] and
      (JsPath \ "isSchemeMasterTrust").readNullable[Boolean] and
      (JsPath \ "declarationDormant").readNullable[String] and
      (JsPath \ "declarationDuties").read[Boolean] and
      (JsPath \ "adviserDetails").readNullable[AdviserDetails] and
      (JsPath \ "adviserAddress").readNullable[Address]
    ) ((declaration, isSchemeMasterTrust, declarationDormant, declarationDuties, adviserDetails, adviserAddress) => {


    val basicDeclaration = PensionSchemeDeclaration(
      declaration,
      declaration,
      None, None, None,
      declaration,
      declaration,
      declaration,
      declaration,
      None, None,
      None)

    val dormant = (dec: PensionSchemeDeclaration) => {
      declarationDormant.fold(dec)(value => {
        if (value=="no") {
          dec.copy(box4 = Some(true))
        } else {
          dec.copy(box5 = Some(true))
        }
      }
      )
    }

    val isMasterTrust=(dec:PensionSchemeDeclaration) =>{
      isSchemeMasterTrust.fold(dec)(value =>{
        dec.copy(box3=Some(value))
      })
    }
    val decDuties = (dec: PensionSchemeDeclaration) => {

        if (declarationDuties) {
          dec.copy(box10 = Some(true))
        }
        else {
          dec.copy(
            box11 = Some(true),
            pensionAdviserName = adviserDetails.map(_.adviserName),
            addressAndContactDetails = {
              (adviserDetails, adviserAddress) match {
                case (Some(contact), Some(address)) =>
                  Some(AddressAndContactDetails(
                    address,
                    ContactDetails(contact.phoneNumber, None, None, contact.emailAddress)
                  ))
                case _ => None
              }
            }
          )
        }

    }

    val completedDeclaration = dormant andThen isMasterTrust andThen decDuties
    completedDeclaration(basicDeclaration)
  })
}

case class Individual(
  personalDetails: PersonalDetails,
  referenceOrNino: Option[String] = None,
  noNinoReason: Option[String] = None,
  utr: Option[String] = None,
  noUtrReason: Option[String] = None,
  correspondenceAddressDetails: CorrespondenceAddressDetails,
  correspondenceContactDetails: CorrespondenceContactDetails,
  previousAddressDetails: Option[PreviousAddressDetails] = None
)

case class CompanyEstablisher (
  organizationName: String,
  utr: Option[String] = None,
  noUtrReason: Option[String] = None,
  crnNumber: Option[String] = None,
  noCrnReason: Option[String] = None,
  vatRegistrationNumber: Option[String] = None,
  payeReference: Option[String] = None,
  haveMoreThanTenDirectorOrPartner: Boolean,
  correspondenceAddressDetails: CorrespondenceAddressDetails,
  correspondenceContactDetails: CorrespondenceContactDetails,
  previousAddressDetails: Option[PreviousAddressDetails] = None,
  directorDetails: Seq[Individual]
)

case class CompanyTrustee(
  organizationName: String,
  utr: Option[String] = None,
  noUtrReason: Option[String] = None,
  crnNumber: Option[String] = None,
  noCrnReason: Option[String] = None,
  vatRegistrationNumber: Option[String] = None,
  payeReference: Option[String] = None,
  correspondenceAddressDetails: CorrespondenceAddressDetails,
  correspondenceContactDetails: CorrespondenceContactDetails,
  previousAddressDetails: Option[PreviousAddressDetails] = None
)

case class TrusteeDetails(
  individualTrusteeDetail: Seq[Individual],
  companyTrusteeDetail: Seq[CompanyTrustee]
)

case class EstablisherDetails(
  individual: Seq[Individual],
  companyOrOrganization: Seq[CompanyEstablisher]
)

case class PensionsScheme(customerAndSchemeDetails: CustomerAndSchemeDetails, pensionSchemeDeclaration: PensionSchemeDeclaration,
                          establisherDetails: EstablisherDetails, trusteeDetails: TrusteeDetails)

object PensionsScheme {

  implicit val formatsIndividual: Format[Individual] = Json.format[Individual]
  implicit val formatsCompanyEstablisher: Format[CompanyEstablisher] = Json.format[CompanyEstablisher]
  implicit val formatsCompanyTrustee: Format[CompanyTrustee] = Json.format[CompanyTrustee]
  implicit val formatsTrusteeDetails: Format[TrusteeDetails] = Json.format[TrusteeDetails]
  implicit val formatsEstablisherDetails: Format[EstablisherDetails] = Json.format[EstablisherDetails]
  implicit val formatsPensionsScheme: Format[PensionsScheme] = Json.format[PensionsScheme]

  val pensionSchemeHaveInvalidBank: Lens[PensionsScheme, Boolean] = new Lens[PensionsScheme, Boolean] {
    override def get: PensionsScheme => Boolean = pensionsScheme => pensionsScheme.customerAndSchemeDetails.haveInvalidBank

    override def set: (PensionsScheme, Boolean) => PensionsScheme =
      (pensionsScheme, haveInvalidBank) =>
        pensionsScheme.copy(
          customerAndSchemeDetails =
            pensionsScheme.customerAndSchemeDetails.copy(haveInvalidBank = haveInvalidBank)
        )
  }

}
