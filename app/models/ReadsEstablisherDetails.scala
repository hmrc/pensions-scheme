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

import play.api.libs.json.{JsPath, Reads}
import play.api.libs.functional.syntax._

object ReadsEstablisherDetails {

  private def previousAddressDetails(addressYears: String, previousAddress: Option[Address]): Option[PreviousAddressDetails] = {
    if (addressYears == "under_a_year") {
      Some(
        PreviousAddressDetails(true,previousAddress)
      )
    }
    else {
      None
    }
  }

  private implicit val readsContactDetails: Reads[ContactDetails] = (
    (JsPath \ "emailAddress").read[String] and
      (JsPath \ "phoneNumber").read[String]
    )((email, phone) => ContactDetails(telephone = phone, email = email))

  private implicit val readsAddress: Reads[Address] = (
    (JsPath \ "addressLine1").read[String] and
    (JsPath \ "addressLine2").read[String] and
    (JsPath \ "addressLine3").readNullable[String] and
    (JsPath \ "addressLine4").readNullable[String] and
    (JsPath \ "country").read[String] and
    (JsPath \ "postcode").readNullable[String]
  )((addressLine1, addressLine2, addressLine3, addressLine4, countryCode, postalCode) => {
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

  private implicit val readsPersonalDetails: Reads[PersonalDetails] = (
    (JsPath \ "firstName").read[String] and
    (JsPath \ "middleName").readNullable[String] and
    (JsPath \ "lastName").read[String] and
    (JsPath \ "date").read[String]
  )((firstName, middleName, lastName, dateOfBirth) =>
    PersonalDetails(
      None,
      firstName,
      middleName,
      lastName,
      dateOfBirth
    )
  )

  private val readsEstablisherIndividual: Reads[EstablisherDetails] = (
    (JsPath \ "establisherDetails").read[PersonalDetails] and
    (JsPath \ "address").read[Address] and
    (JsPath \ "contactDetails").read[ContactDetails] and
    (JsPath \ "establisherNino" \ "nino").readNullable[String] and
    (JsPath \ "establisherNino" \ "reason").readNullable[String] and
    (JsPath \ "uniqueTaxReference" \ "utr").readNullable[String] and
    (JsPath \ "uniqueTaxReference" \ "reason").readNullable[String] and
    (JsPath \ "addressYears").read[String] and
    (JsPath \ "previousAddress").readNullable[Address]
  )((personalDetails, address, contactDetails, nino, noNinoReason, utr, noUtrReason, addressYears, previousAddress) =>
    EstablisherDetails(
      `type` = "Individual",
      personalDetails = Some(personalDetails),
      referenceOrNino = nino,
      noNinoReason = noNinoReason,
      utr = utr,
      noUtrReason = noUtrReason,
      correspondenceAddressDetails = CorrespondenceAddressDetails(address),
      correspondenceContactDetails = CorrespondenceContactDetails(contactDetails),
      previousAddressDetails = previousAddressDetails(addressYears, previousAddress)
    )
  )

  private val readsCompanyDirector: Reads[EstablisherDetails] = (
    (JsPath \ "directorDetails").read[PersonalDetails] and
    (JsPath \ "directorAddressId").read[Address] and
    (JsPath \ "directorContactDetails").read[ContactDetails] and
    (JsPath \ "directorNino" \ "nino").readNullable[String] and
    (JsPath \ "directorNino" \ "reason").readNullable[String] and
    (JsPath \ "directorUniqueTaxReference" \ "utr").readNullable[String] and
    (JsPath \ "directorUniqueTaxReference" \ "reason").readNullable[String] and
    (JsPath \ "companyDirectorAddressYears").read[String] and
    (JsPath \ "previousAddress").readNullable[Address]
  )((personalDetails, address, contactDetails, nino, noNinoReason, utr, noUtrReason, addressYears, previousAddress) =>
    EstablisherDetails(
      `type` = "Director",
      personalDetails = Some(personalDetails),
      referenceOrNino = nino,
      noNinoReason = noNinoReason,
      utr = utr,
      noUtrReason = noUtrReason,
      correspondenceAddressDetails = CorrespondenceAddressDetails(address),
      correspondenceContactDetails = CorrespondenceContactDetails(contactDetails),
      previousAddressDetails = previousAddressDetails(addressYears, previousAddress)
    )
  )

  private val readsCompanyDirectors: Reads[Seq[EstablisherDetails]] =
    Reads.seq(readsCompanyDirector)

  private val readsEstablisherCompany: Reads[Seq[EstablisherDetails]] = (
    (JsPath \ "companyDetails" \ "companyName").read[String] and
    (JsPath \ "companyDetails" \ "vatNumber").readNullable[String] and
    (JsPath \ "companyDetails" \ "payeNumber").readNullable[String] and
    (JsPath \ "companyUniqueTaxReference" \ "utr").readNullable[String] and
    (JsPath \ "companyUniqueTaxReference" \ "reason").readNullable[String] and
    (JsPath \ "companyRegistrationNumber" \ "crn").readNullable[String] and
    (JsPath \ "companyRegistrationNumber" \ "reason").readNullable[String] and
    (JsPath \ "otherDirectors").readNullable[Boolean] and
    (JsPath \ "companyAddress").read[Address] and
    (JsPath \ "companyContactDetails").read[ContactDetails] and
    (JsPath \ "companyAddressYears").read[String] and
    (JsPath \ "companyPreviousAddress").readNullable[Address] and
    (JsPath \ "director").readNullable(readsCompanyDirectors)
  )((companyName, vat, paye, utr, noUtrReason, crn, noCrnReason, otherDirectors, address, contactDetails, addressYears, previousAddress, directors) =>
    EstablisherDetails(
      `type` = "Company/Org",
      organisationName = Some(companyName),
      utr = utr,
      noUtrReason = noUtrReason,
      crnNumber = crn,
      noCrnReason = noCrnReason,
      vatRegistrationNumber = vat,
      payeReference = paye,
      haveMoreThanTenDirectorOrPartner = otherDirectors,
      correspondenceAddressDetails = CorrespondenceAddressDetails(address),
      correspondenceContactDetails = CorrespondenceContactDetails(contactDetails),
      previousAddressDetails = previousAddressDetails(addressYears, previousAddress)
    ) +: directors.getOrElse(Seq.empty[EstablisherDetails])
  )

  private val readsTrusteeCompany: Reads[EstablisherDetails] = (
    (JsPath \ "companyDetails" \ "companyName").read[String] and
    (JsPath \ "companyDetails" \ "vatNumber").readNullable[String] and
    (JsPath \ "companyDetails" \ "payeNumber").readNullable[String] and
    (JsPath \ "companyUniqueTaxReference" \ "utr").readNullable[String] and
    (JsPath \ "companyUniqueTaxReference" \ "reason").readNullable[String] and
    (JsPath \ "companyRegistrationNumber" \ "crn").readNullable[String] and
    (JsPath \ "companyRegistrationNumber" \ "reason").readNullable[String] and
    (JsPath \ "companyAddress").read[Address] and
    (JsPath \ "companyContactDetails").read[ContactDetails] and
    (JsPath \ "trusteesCompanyAddressYears").read[String] and
    (JsPath \ "companyPreviousAddress").readNullable[Address]
  )((companyName, vat, paye, utr, noUtrReason, crn, noCrnReason, address, contactDetails, addressYears, previousAddress) =>
    EstablisherDetails(
      `type` = "Company/Org Trustee",
      organisationName = Some(companyName),
      utr = utr,
      noUtrReason = noUtrReason,
      crnNumber = crn,
      noCrnReason = noCrnReason,
      vatRegistrationNumber = vat,
      payeReference = paye,
      correspondenceAddressDetails = CorrespondenceAddressDetails(address),
      correspondenceContactDetails = CorrespondenceContactDetails(contactDetails),
      previousAddressDetails = previousAddressDetails(addressYears, previousAddress)
    )
  )

  private val readsTrusteeIndividual: Reads[EstablisherDetails] = (
    (JsPath \ "trusteeDetails").read[PersonalDetails] and
    (JsPath \ "trusteeAddressId").read[Address] and
    (JsPath \ "trusteeContactDetails").read[ContactDetails] and
    (JsPath \ "trusteeNino" \ "nino").readNullable[String] and
    (JsPath \ "trusteeNino" \ "reason").readNullable[String] and
    (JsPath \ "uniqueTaxReference" \ "utr").readNullable[String] and
    (JsPath \ "uniqueTaxReference" \ "reason").readNullable[String] and
    (JsPath \ "trusteeAddressYears").read[String] and
    (JsPath \ "trusteePreviousAddress").readNullable[Address]
  )((personalDetails, address, contactDetails, nino, noNinoReason, utr, noUtrReason, addressYears, previousAddress) =>
    EstablisherDetails(
      `type` = "Individual Trustee",
      personalDetails = Some(personalDetails),
      referenceOrNino = nino,
      noNinoReason = noNinoReason,
      utr = utr,
      noUtrReason = noUtrReason,
      correspondenceAddressDetails = CorrespondenceAddressDetails(address),
      correspondenceContactDetails = CorrespondenceContactDetails(contactDetails),
      previousAddressDetails = previousAddressDetails(addressYears, previousAddress)
    )
  )

  private val readsEstablishers: Reads[Seq[EstablisherDetails]] =
    Reads.seq(readsEstablisherCompany orElse readsEstablisherIndividual.map(i => Seq(i))).map(_.flatten)

  private val readsTrustees: Reads[Seq[EstablisherDetails]] =
    Reads.seq(readsTrusteeCompany orElse readsTrusteeIndividual)

  implicit val readsEstablisherDetails: Reads[Seq[EstablisherDetails]] = (
    (JsPath \ "establishers").readNullable(readsEstablishers) and
    (JsPath \ "trustees").readNullable(readsTrustees)
  )((
      establisherCompanies,
      trusteeCompanies
    ) =>
      establisherCompanies.getOrElse(Seq.empty[EstablisherDetails]) ++
      trusteeCompanies.getOrElse(Seq.empty[EstablisherDetails])
  )

}
