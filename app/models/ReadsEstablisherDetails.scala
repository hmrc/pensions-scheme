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
        PreviousAddressDetails(isPreviousAddressLast12Month = true,previousAddress)
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
  )((personalDetails, address, contactDetails, nino, noNinoReason, utr, noUtrReason, addressYears, previousAddress) =>
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
  )((personalDetails, address, contactDetails, nino, noNinoReason, utr, noUtrReason, addressYears, previousAddress) =>
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

  private val readsEstablisherCompany: Reads[CompanyEstablisher] = (
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
    CompanyEstablisher(
      organizationName = companyName,
      utr = utr,
      noUtrReason = noUtrReason,
      crnNumber = crn,
      noCrnReason = noCrnReason,
      vatRegistrationNumber = vat,
      payeReference = paye,
      haveMoreThanTenDirectorOrPartner = otherDirectors.getOrElse(false),
      correspondenceAddressDetails = CorrespondenceAddressDetails(address),
      correspondenceContactDetails = CorrespondenceContactDetails(contactDetails),
      previousAddressDetails = previousAddressDetails(addressYears, previousAddress),
      directorDetails = directors.getOrElse(Nil)
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
  )((personalDetails, address, contactDetails, nino, noNinoReason, utr, noUtrReason, addressYears, previousAddress) =>
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

  private val readsTrusteeCompany: Reads[CompanyTrustee] = (
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
    CompanyTrustee(
      organizationName = companyName,
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

  private val readsEstablisherIndividuals: Reads[Seq[Individual]] =
    readsFiltered(_ \ "establisherDetails", readsEstablisherIndividual, "establisherDetails")

  private val readsEstablisherCompanies: Reads[Seq[CompanyEstablisher]] =
    readsFiltered(_ \ "companyDetails", readsEstablisherCompany, "companyDetails")

  private val readsTrusteeIndividuals: Reads[Seq[Individual]] =
    readsFiltered(_ \ "trusteeDetails", readsTrusteeIndividual, "trusteeDetails")

  private val readsTrusteeCompanies: Reads[Seq[CompanyTrustee]] =
    readsFiltered(_ \ "companyDetails", readsTrusteeCompany, "companyDetails")

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
    jsValueSeq.filterNot{json =>
      (json \ detailsType \ "isDeleted").validate[Boolean] match {
        case JsSuccess(e, _) => e
        case _ => false
      }
    }
  }

  @tailrec
  private def readFilteredSeq[T](result: JsResult[Seq[T]], js: Seq[JsValue], isA: JsValue => JsLookupResult, reads: Reads[T]): JsResult[Seq[T]] = {
    js match {
      case Seq(h, t @ _*) =>
        isA(h) match {
          case JsDefined(_) =>
            reads.reads(h) match {
              case JsSuccess(individual, _) => readFilteredSeq(JsSuccess(result.get :+ individual), t, isA, reads)
              case error @ JsError(_) => error
            }
          case _ => readFilteredSeq(result, t, isA, reads)
        }
      case Nil => result
    }
  }

  val readsEstablisherDetails: Reads[EstablisherDetails] = (
    (JsPath \ "establishers").readNullable(readsEstablisherIndividuals) and
    (JsPath \ "establishers").readNullable(readsEstablisherCompanies)
  )((establisherIndividuals, establisherCompanies) =>
    EstablisherDetails(
      individual = establisherIndividuals.getOrElse(Nil),
      companyOrOrganization = establisherCompanies.getOrElse(Nil)
    )
  )

  val readsTrusteeDetails: Reads[TrusteeDetails] = (
    (JsPath \ "trustees").readNullable(readsTrusteeIndividuals) and
    (JsPath \ "trustees").readNullable(readsTrusteeCompanies)
  )((trusteeIndividuals, trusteeCompanies) =>
    TrusteeDetails(
      individualTrusteeDetail = trusteeIndividuals.getOrElse(Nil),
      companyTrusteeDetail = trusteeCompanies.getOrElse(Nil)
    )
  )

}
