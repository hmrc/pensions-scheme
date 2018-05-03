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
import play.api.libs.json.{JsPath, Json, Reads}
import play.api.libs.functional.syntax._
import play.api.libs.json

case class AddressAndContactDetails(addressDetails: Address, contactDetails: ContactDetails)

object AddressAndContactDetails {
  implicit val formats = Json.format[AddressAndContactDetails]
}

case class PersonalDetails(title: Option[String] = None, firstName: String, middleName: Option[String] = None,
                           lastName: String, dateOfBirth: String)

object PersonalDetails {
  implicit val formats = Json.format[PersonalDetails]
}

case class PreviousAddressDetails(isPreviousAddressLast12Month: Boolean,
                                  previousAddressDetails: Option[Address] = None)

object PreviousAddressDetails {
  implicit val formats = Json.format[PreviousAddressDetails]

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
  implicit val formats = Json.format[CorrespondenceAddressDetails]
}

case class CorrespondenceContactDetails(contactDetails: ContactDetails)

object CorrespondenceContactDetails {
  implicit val formats = Json.format[CorrespondenceContactDetails]
}


case class CustomerAndSchemeDetails(schemeName: String, isSchemeMasterTrust: Boolean, schemeStructure: String,
                                    otherSchemeStructure: Option[String] = None, haveMoreThanTenTrustee: Option[Boolean] = None,
                                    currentSchemeMembers: String, futureSchemeMembers: String, isReguledSchemeInvestment: Boolean,
                                    isOccupationalPensionScheme: Boolean, areBenefitsSecuredContractInsuranceCompany: Boolean,
                                    doesSchemeProvideBenefits: String, schemeEstablishedCountry: String, haveInvalidBank: Boolean,
                                    insuranceCompanyName: Option[String] = None, policyNumber: Option[String] = None,
                                    insuranceCompanyAddress: Option[Address] = None)

object CustomerAndSchemeDetails {
  implicit val formats = Json.format[CustomerAndSchemeDetails]

  def insurerReads: Reads[(Option[String], Option[String])] = (
    ((JsPath \ "companyName").readNullable[String] and
      (JsPath \ "policyNumber").readNullable[String])
      ((companyName, policyNumber) => (companyName, policyNumber))
    )

  def apiReads: Reads[CustomerAndSchemeDetails] = (
    (JsPath \ "schemeDetails" \ "schemeName").read[String] and
      (JsPath \ "schemeDetails" \ "schemeType" \ "name").read[String] and
      (JsPath \ "schemeDetails" \ "schemeType" \ "schemeTypeDetails").readNullable[String] and
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
    (name, schemeType, schemeTypeDetails, moreThanTenTrustees, membership, membershipFuture, investmentRegulated,
     occupationalPension, securedBenefits, benefits, country, benefitsInsurer, insurerAddress) => {
      CustomerAndSchemeDetails(
        schemeName = name,
        isSchemeMasterTrust = false,
        schemeStructure = SchemeType.valueWithName(schemeType),
        otherSchemeStructure = schemeTypeDetails,
        haveMoreThanTenTrustee = moreThanTenTrustees,
        currentSchemeMembers = SchemeMembers.valueWithName(membership),
        futureSchemeMembers = SchemeMembers.valueWithName(membershipFuture),
        isReguledSchemeInvestment = investmentRegulated,
        isOccupationalPensionScheme = occupationalPension,
        areBenefitsSecuredContractInsuranceCompany = securedBenefits,
        doesSchemeProvideBenefits = Benefits.valueWithName(benefits),
        schemeEstablishedCountry = country,
        haveInvalidBank = true,
        insuranceCompanyName = benefitsInsurer.flatMap(_._1),
        policyNumber = benefitsInsurer.flatMap(_._2),
        insuranceCompanyAddress = insurerAddress)
    }
  )
}


case class PensionSchemeDeclaration(box1: Boolean, box2: Boolean, box3: Option[Boolean] = None, box4: Option[Boolean] = None,
                                    box5: Option[Boolean] = None, box6: Boolean, box7: Boolean, box8: Boolean, box9: Boolean,
                                    box10: Option[Boolean] = None, box11: Option[Boolean] = None, pensionAdviserName: Option[String] = None,
                                    addressAndContactDetails: Option[AddressAndContactDetails] = None)

case class AdviserDetails(adviserName: String, emailAddress: String, phoneNumber: String)

object AdviserDetails {

  implicit val formats = Json.format[PensionSchemeDeclaration]

  implicit val readsAdviserDetails: Reads[AdviserDetails] = (
    (JsPath \ "adviserName").read[String] and
      (JsPath \ "emailAddress").read[String] and
      (JsPath \ "phoneNumber").read[String]
    ) ((name, email, phone) => AdviserDetails(name, email, phone))
}

object PensionSchemeDeclaration {

  //TODO
  private implicit val readsAddress: Reads[Address] = (
    (JsPath \ "addressLine1").read[String] and
      (JsPath \ "addressLine2").read[String] and
      (JsPath \ "addressLine3").readNullable[String] and
      (JsPath \ "addressLine4").readNullable[String] and
      (JsPath \ "country").read[String] and
      (JsPath \ "postcode").readNullable[String]
    ) ((addressLine1, addressLine2, addressLine3, addressLine4, countryCode, postalCode) => {
    if (countryCode == "GB") {
      postalCode match {
        case Some(zip) =>
          UkAddress(
            addressLine1,
            Some(addressLine2),
            addressLine3,
            addressLine4,
            countryCode,
            zip
          )
        case _ =>
          throw new IllegalArgumentException("Null postcode in UK address")
      }
    }
    else {
      InternationalAddress(
        addressLine1,
        Some(addressLine2),
        addressLine3,
        addressLine4,
        countryCode,
        postalCode
      )
    }
  })


  implicit val formats = Json.format[PensionSchemeDeclaration]

  val apiReads: Reads[PensionSchemeDeclaration] = (
    (JsPath \ "declaration").read[Boolean] and
      (JsPath \ "declarationDormant").readNullable[Boolean] and
      (JsPath \ "declarationDuties").readNullable[Boolean] and
      (JsPath \ "adviserDetails").readNullable[AdviserDetails] and
      (JsPath \ "adviserAddress").readNullable[Address]
    ) ((declaration, declarationDormant, declarationDuties, adviserDetails, adviserAddress) => {


    val basicDeclaration = PensionSchemeDeclaration(
      declaration,
      declaration,
      None, None, None,
      declaration,
      declaration,
      declaration,
      declaration,
      None, None)

    val dormant = (dec: PensionSchemeDeclaration) => {
      declarationDormant match {
        case Some(value) => {
          if (value) {
            dec.copy(box4 = Some(true))
          }
          else {
            dec.copy(box5 = Some(true))
          }
        }
        case None => dec
      }
    }

    val decDuties = (dec: PensionSchemeDeclaration) => {
      declarationDuties match {
        case Some(value) => {
          if (value) {
            dec.copy(box10 = Some(true))
          }
          else {
            dec.copy(
              box11 = Some(true),
              pensionAdviserName = adviserDetails.map(_.adviserName),
              addressAndContactDetails = {
                (adviserDetails, adviserAddress) match {
                  case (Some(contact), Some(address)) =>
                    Some(AddressAndContactDetails(address, ContactDetails(contact.phoneNumber, None, None, contact.emailAddress)))
                  case _ => None
                }
              }
            )
          }
        }
        case None => dec
      }
    }
    val completedDeclaration = dormant andThen decDuties
    completedDeclaration(basicDeclaration)

  }
  )
}

case class EstablisherDetails(`type`: String, organisationName: Option[String] = None,
                              personalDetails: Option[PersonalDetails] = None,
                              referenceOrNino: Option[String] = None, noNinoReason: Option[String] = None,
                              utr: Option[String] = None, noUtrReason: Option[String] = None, crnNumber: Option[String] = None,
                              noCrnReason: Option[String] = None, vatRegistrationNumber: Option[String] = None,
                              payeReference: Option[String] = None, haveMoreThanTenDirectorOrPartner: Option[Boolean] = None,
                              correspondenceAddressDetails: CorrespondenceAddressDetails,
                              correspondenceContactDetails: CorrespondenceContactDetails,
                              previousAddressDetails: Option[PreviousAddressDetails] = None)

object EstablisherDetails {
  implicit val formats = Json.format[EstablisherDetails]
}

case class PensionsScheme(customerAndSchemeDetails: CustomerAndSchemeDetails, pensionSchemeDeclaration: PensionSchemeDeclaration,
                          establisherDetails: List[EstablisherDetails])

object PensionsScheme {
  implicit val formats = Json.format[PensionsScheme]
}
