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

import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.annotation.tailrec

object ReadsEstablisherDetails {

  private def previousAddressDetails(addressYears: String, previousAddress: Option[Address]): Option[PreviousAddressDetails] = {
    if (addressYears == "under_a_year") {
      Some(
        PreviousAddressDetails(isPreviousAddressLast12Month = true, previousAddress)
      )
    }
    else {
      None
    }
  }

  private implicit val readsContactDetails: Reads[ContactDetails] = (
    (JsPath \ "emailAddress").read[String] and
      (JsPath \ "phoneNumber").read[String]
    ) ((email, phone) => ContactDetails(telephone = phone, email = email))

  private implicit val readsPersonalDetails: Reads[PersonalDetails] = (
    (JsPath \ "firstName").read[String] and
      (JsPath \ "middleName").readNullable[String] and
      (JsPath \ "lastName").read[String] and
      (JsPath \ "date").read[String]
    ) ((firstName, middleName, lastName, dateOfBirth) =>
    PersonalDetails(
      None,
      firstName,
      middleName,
      lastName,
      dateOfBirth
    )
  )

  private val readsEstablisherIndividual: Reads[Individual] = (
    (JsPath \ "establisherDetails").read[PersonalDetails] and
      (JsPath \ "address").read[Address] and
      (JsPath \ "contactDetails").read[ContactDetails] and
      (JsPath \ "establisherNino" \ "nino").readNullable[String] and
      (JsPath \ "establisherNino" \ "reason").readNullable[String] and
      (JsPath \ "uniqueTaxReference" \ "utr").readNullable[String] and
      (JsPath \ "uniqueTaxReference" \ "reason").readNullable[String] and
      (JsPath \ "addressYears").read[String] and
      (JsPath \ "previousAddress").readNullable[Address]
    ) ((personalDetails, address, contactDetails, nino, noNinoReason, utr, noUtrReason, addressYears, previousAddress) =>
    Individual(
      personalDetails = personalDetails,
      referenceOrNino = nino,
      noNinoReason = noNinoReason,
      utr = utr,
      noUtrReason = noUtrReason,
      correspondenceAddressDetails = CorrespondenceAddressDetails(address),
      correspondenceContactDetails = CorrespondenceContactDetails(contactDetails),
      previousAddressDetails = previousAddressDetails(addressYears, previousAddress)
    )
  )

  private val readsCompanyDirector: Reads[Individual] = (
    (JsPath \ "directorDetails").read[PersonalDetails] and
      (JsPath \ "directorAddressId").read[Address] and
      (JsPath \ "directorContactDetails").read[ContactDetails] and
      (JsPath \ "directorNino" \ "nino").readNullable[String] and
      (JsPath \ "directorNino" \ "reason").readNullable[String] and
      (JsPath \ "directorUniqueTaxReference" \ "utr").readNullable[String] and
      (JsPath \ "directorUniqueTaxReference" \ "reason").readNullable[String] and
      (JsPath \ "companyDirectorAddressYears").read[String] and
      (JsPath \ "previousAddress").readNullable[Address]
    ) ((personalDetails, address, contactDetails, nino, noNinoReason, utr, noUtrReason, addressYears, previousAddress) =>
    Individual(
      personalDetails = personalDetails,
      referenceOrNino = nino,
      noNinoReason = noNinoReason,
      utr = utr,
      noUtrReason = noUtrReason,
      correspondenceAddressDetails = CorrespondenceAddressDetails(address),
      correspondenceContactDetails = CorrespondenceContactDetails(contactDetails),
      previousAddressDetails = previousAddressDetails(addressYears, previousAddress)
    )
  )

  private val readsPartner: Reads[Individual] = (
    (JsPath \ "partnerDetails").read[PersonalDetails] and
      (JsPath \ "partnerAddressId").read[Address] and
      (JsPath \ "partnerContactDetails").read[ContactDetails] and
      (JsPath \ "partnerNino" \ "nino").readNullable[String] and
      (JsPath \ "partnerNino" \ "reason").readNullable[String] and
      (JsPath \ "partnerUniqueTaxReference" \ "utr").readNullable[String] and
      (JsPath \ "partnerUniqueTaxReference" \ "reason").readNullable[String] and
      (JsPath \ "partnerAddressYears").read[String] and
      (JsPath \ "previousAddress").readNullable[Address]
    ) ((personalDetails, address, contactDetails, nino, noNinoReason, utr, noUtrReason, addressYears, previousAddress) =>
    Individual(
      personalDetails = personalDetails,
      referenceOrNino = nino,
      noNinoReason = noNinoReason,
      utr = utr,
      noUtrReason = noUtrReason,
      correspondenceAddressDetails = CorrespondenceAddressDetails(address),
      correspondenceContactDetails = CorrespondenceContactDetails(contactDetails),
      previousAddressDetails = previousAddressDetails(addressYears, previousAddress)
    )
  )

  private val readsCompanyDirectors: Reads[Seq[Individual]] =
    readsFiltered(_ \ "directorDetails", readsCompanyDirector, "directorDetails")

  private val readsPartners: Reads[Seq[Individual]] =
    readsFiltered(_ \ "partnerDetails", readsPartner, "partnerDetails")

  case class Company(name: String, vatNumber: Option[String], payeNumber: Option[String], utr: Option[String],
                     noUtrReason: Option[String], crn: Option[String], noCrnReason: Option[String], address: Address,
                     contactDetails: ContactDetails, previousAddress: Option[Address], addressYears: String)

  private val companyReads: Reads[Company] = (
    (JsPath \ "companyDetails" \ "companyName").read[String] and
      (JsPath \ "companyDetails" \ "vatNumber").readNullable[String] and
      (JsPath \ "companyDetails" \ "payeNumber").readNullable[String] and
      (JsPath \ "companyUniqueTaxReference" \ "utr").readNullable[String] and
      (JsPath \ "companyUniqueTaxReference" \ "reason").readNullable[String] and
      (JsPath \ "companyRegistrationNumber" \ "crn").readNullable[String] and
      (JsPath \ "companyRegistrationNumber" \ "reason").readNullable[String] and
      (JsPath \ "companyAddress").read[Address] and
      (JsPath \ "companyContactDetails").read[ContactDetails] and
      (JsPath \ "companyPreviousAddress").readNullable[Address] and
      ((JsPath \ "companyAddressYears").read[String] orElse (JsPath \ "trusteesCompanyAddressYears").read[String])
    ) (Company.apply _)

  private val readsEstablisherCompany: Reads[CompanyEstablisher] = (
    JsPath.read(companyReads) and
      (JsPath \ "otherDirectors").readNullable[Boolean] and
      (JsPath \ "director").readNullable(readsCompanyDirectors)
    ) ((company, otherDirectors, directors) =>
    CompanyEstablisher(
      organizationName = company.name,
      utr = company.utr,
      noUtrReason = company.noUtrReason,
      crnNumber = company.crn,
      noCrnReason = company.noCrnReason,
      vatRegistrationNumber = company.vatNumber,
      payeReference = company.payeNumber,
      haveMoreThanTenDirectorOrPartner = otherDirectors.getOrElse(false),
      correspondenceAddressDetails = CorrespondenceAddressDetails(company.address),
      correspondenceContactDetails = CorrespondenceContactDetails(company.contactDetails),
      previousAddressDetails = previousAddressDetails(company.addressYears, company.previousAddress),
      directorDetails = directors.getOrElse(Nil)
    )
  )

  case class PartnershipDetail(name: String, vat: Option[String], paye: Option[String], utr: Option[String], utrReason: Option[String],
                               address: Address, contact: ContactDetails, addressYears: String, previousAddress: Option[Address])

  private val partnershipDetailReads: Reads[PartnershipDetail] = (
    (JsPath \ "partnershipDetails" \ "name").read[String] and
      (JsPath \ "partnershipVat" \ "vat").readNullable[String] and
      (JsPath \ "partnershipPaye" \ "paye").readNullable[String] and
      (JsPath \ "partnershipUniqueTaxReference" \ "utr").readNullable[String] and
      (JsPath \ "partnershipUniqueTaxReference" \ "reason").readNullable[String] and
      (JsPath \ "partnershipAddress").read[Address] and
      (JsPath \ "partnershipContactDetails").read[ContactDetails] and
      (JsPath \ "partnershipAddressYears").read[String] and
      (JsPath \ "partnershipPreviousAddress").readNullable[Address]
    ) (PartnershipDetail.apply _)

  private val readsEstablisherPartnership: Reads[Partnership] = (
    JsPath.read(partnershipDetailReads) and
      (JsPath \ "otherPartners").readNullable[Boolean] and
      (JsPath \ "partner").readNullable(readsPartners)
    ) ((partnership, otherPartners, partners) =>
    Partnership(
      organizationName = partnership.name,
      utr = partnership.utr,
      noUtrReason = partnership.utrReason,
      vatRegistrationNumber = partnership.vat,
      payeReference = partnership.paye,
      haveMoreThanTenDirectorOrPartner = otherPartners.getOrElse(false),
      correspondenceAddressDetails = CorrespondenceAddressDetails(partnership.address),
      correspondenceContactDetails = CorrespondenceContactDetails(partnership.contact),
      previousAddressDetails = previousAddressDetails(partnership.addressYears, partnership.previousAddress),
      partnerDetails = partners.getOrElse(Nil)
    )
  )

  private val readsTrusteeIndividual: Reads[Individual] = (
    (JsPath \ "trusteeDetails").read[PersonalDetails] and
      (JsPath \ "trusteeAddressId").read[Address] and
      (JsPath \ "trusteeContactDetails").read[ContactDetails] and
      (JsPath \ "trusteeNino" \ "nino").readNullable[String] and
      (JsPath \ "trusteeNino" \ "reason").readNullable[String] and
      (JsPath \ "uniqueTaxReference" \ "utr").readNullable[String] and
      (JsPath \ "uniqueTaxReference" \ "reason").readNullable[String] and
      (JsPath \ "trusteeAddressYears").read[String] and
      (JsPath \ "trusteePreviousAddress").readNullable[Address]
    ) ((personalDetails, address, contactDetails, nino, noNinoReason, utr, noUtrReason, addressYears, previousAddress) =>
    Individual(
      personalDetails = personalDetails,
      referenceOrNino = nino,
      noNinoReason = noNinoReason,
      utr = utr,
      noUtrReason = noUtrReason,
      correspondenceAddressDetails = CorrespondenceAddressDetails(address),
      correspondenceContactDetails = CorrespondenceContactDetails(contactDetails),
      previousAddressDetails = previousAddressDetails(addressYears, previousAddress)
    )
  )

  private val readsTrusteeCompany: Reads[CompanyTrustee] = JsPath.read(companyReads).map(test => CompanyTrustee(
    organizationName = test.name,
    utr = test.utr,
    noUtrReason = test.noUtrReason,
    crnNumber = test.crn,
    noCrnReason = test.noCrnReason,
    vatRegistrationNumber = test.vatNumber,
    payeReference = test.payeNumber,
    correspondenceAddressDetails = CorrespondenceAddressDetails(test.address),
    correspondenceContactDetails = CorrespondenceContactDetails(test.contactDetails),
    previousAddressDetails = previousAddressDetails(test.addressYears, test.previousAddress)))

  private val readsTrusteePartnership: Reads[PartnershipTrustee] = JsPath.read(partnershipDetailReads).map(partnership =>
    PartnershipTrustee(
      organizationName = partnership.name,
      utr = partnership.utr,
      noUtrReason = partnership.utrReason,
      vatRegistrationNumber = partnership.vat,
      payeReference = partnership.paye,
      correspondenceAddressDetails = CorrespondenceAddressDetails(partnership.address),
      correspondenceContactDetails = CorrespondenceContactDetails(partnership.contact),
      previousAddressDetails = previousAddressDetails(partnership.addressYears, partnership.previousAddress)
    )
  )

  private val readsEstablisherIndividuals: Reads[Seq[Individual]] =
    readsFiltered(_ \ "establisherDetails", readsEstablisherIndividual, "establisherDetails")

  private val readsEstablisherCompanies: Reads[Seq[CompanyEstablisher]] =
    readsFiltered(_ \ "companyDetails", readsEstablisherCompany, "companyDetails")

  private val readsEstablisherPartnerships: Reads[Seq[Partnership]] =
    readsFiltered(_ \ "partnershipDetails", readsEstablisherPartnership, "partnershipDetails")

  private val readsTrusteeIndividuals: Reads[Seq[Individual]] =
    readsFiltered(_ \ "trusteeDetails", readsTrusteeIndividual, "trusteeDetails")

  private val readsTrusteeCompanies: Reads[Seq[CompanyTrustee]] =
    readsFiltered(_ \ "companyDetails", readsTrusteeCompany, "companyDetails")

  private val readsTrusteePartnerships: Reads[Seq[PartnershipTrustee]] =
    readsFiltered(_ \ "partnershipDetails", readsTrusteePartnership, "partnershipDetails")

  //noinspection ConvertExpressionToSAM
  private def readsFiltered[T](isA: JsValue => JsLookupResult, readsA: Reads[T], detailsType: String): Reads[Seq[T]] = new Reads[Seq[T]] {
    override def reads(json: JsValue): JsResult[Seq[T]] = {
      json match {
        case JsArray(establishers) =>
          readFilteredSeq(JsSuccess(Nil), filterDeleted(establishers, detailsType), isA, readsA)
        case _ => JsSuccess(Nil)
      }
    }
  }

  private def filterDeleted(jsValueSeq: Seq[JsValue], detailsType: String): Seq[JsValue] = {
    jsValueSeq.filterNot { json =>
      (json \ detailsType \ "isDeleted").validate[Boolean] match {
        case JsSuccess(e, _) => e
        case _ => false
      }
    }
  }

  @tailrec
  private def readFilteredSeq[T](result: JsResult[Seq[T]], js: Seq[JsValue], isA: JsValue => JsLookupResult, reads: Reads[T]): JsResult[Seq[T]] = {
    js match {
      case Seq(h, t@_*) =>
        isA(h) match {
          case JsDefined(_) =>
            reads.reads(h) match {
              case JsSuccess(individual, _) => readFilteredSeq(JsSuccess(result.get :+ individual), t, isA, reads)
              case error@JsError(_) => error
            }
          case _ => readFilteredSeq(result, t, isA, reads)
        }
      case Nil => result
    }
  }

  val readsEstablisherDetails: Reads[EstablisherDetails] = (
    (JsPath \ "establishers").readNullable(readsEstablisherIndividuals) and
      (JsPath \ "establishers").readNullable(readsEstablisherCompanies) and
      (JsPath \ "establishers").readNullable(readsEstablisherPartnerships)
    ) ((establisherIndividuals, establisherCompanies, establisherPartnerships) =>
    EstablisherDetails(
      individual = establisherIndividuals.getOrElse(Nil),
      companyOrOrganization = establisherCompanies.getOrElse(Nil),
      partnership = establisherPartnerships.getOrElse(Nil)
    )
  )

  val readsTrusteeDetails: Reads[TrusteeDetails] = (
    (JsPath \ "trustees").readNullable(readsTrusteeIndividuals) and
      (JsPath \ "trustees").readNullable(readsTrusteeCompanies) and
      (JsPath \ "trustees").readNullable(readsTrusteePartnerships)
    ) ((trusteeIndividuals, trusteeCompanies, trusteePartnerships) =>
    TrusteeDetails(
      individualTrusteeDetail = trusteeIndividuals.getOrElse(Nil),
      companyTrusteeDetail = trusteeCompanies.getOrElse(Nil),
      partnershipTrusteeDetail = trusteePartnerships.getOrElse(Nil)
    )
  )

}
