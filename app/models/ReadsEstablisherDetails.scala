/*
 * Copyright 2019 HM Revenue & Customs
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

  def previousAddressDetails(addressYears: String, previousAddress: Option[Address],
                                     tradingTime: Option[Boolean] = None): Option[PreviousAddressDetails] = {

    val tradingTimeAnswer = tradingTime.getOrElse(true)
    if (addressYears == "under_a_year" && tradingTimeAnswer) {
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

  private def readsEstablisherIndividual(isToggleOn: Boolean): Reads[Individual] = (
    readsPersonDetailsHnS(isToggleOn, "establisherDetails") and
      (JsPath \ "address").read[Address] and
      (JsPath \ "contactDetails").read[ContactDetails] and
      (JsPath \ "establisherNino").readNullable[String]((__ \ "value").read[String]).
            orElse((JsPath \ "establisherNino" \ "nino").readNullable[String]) and

      (if(isToggleOn) (JsPath \ "noNinoReason").readNullable[String]
      else (JsPath \ "establisherNino" \ "reason").readNullable[String]) and

      (if(isToggleOn) (JsPath \ "utr").readNullable[String]((__ \ "value").read[String])
        else (JsPath \ "uniqueTaxReference" \ "utr").readNullable[String]) and

      (if(isToggleOn) (JsPath \ "noUtrReason").readNullable[String]
       else (JsPath \ "uniqueTaxReference" \ "reason").readNullable[String]) and

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

  private def readsCompanyDirector: Reads[Individual] = (
    readsPersonDetailsHnS(true, "directorDetails") and
      (JsPath \ "directorAddressId").read[Address] and
      (JsPath \ "directorContactDetails").read[ContactDetails] and
      (JsPath \ "directorNino").readNullable[String]((__ \ "value").read[String]) and
      (JsPath \ "noNinoReason").readNullable[String] and
      (JsPath \ "utr").readNullable[String]((__ \ "value").read[String]) and
      (JsPath \ "noUtrReason").readNullable[String] and
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

  private def readsPersonDetailsHnS(isToggleOn: Boolean, userAnswersBase: String): Reads[PersonalDetails] =
    if(isToggleOn) {
    (
      (JsPath \ userAnswersBase \ "firstName").read[String] and
        (JsPath \ userAnswersBase \ "lastName").read[String] and
        (JsPath \ "dateOfBirth").read[String]
      ) ((firstName, lastName, date) => PersonalDetails(None, firstName, None, lastName, date))
  }
  else{
    (JsPath \ userAnswersBase).read[PersonalDetails]
  }

  private def readsPartner(isToggleOn: Boolean): Reads[Individual] = (
    readsPersonDetailsHnS(isToggleOn, "partnerDetails") and
      (JsPath \ "partnerAddressId").read[Address] and
      (JsPath \ "partnerContactDetails").read[ContactDetails] and
      (JsPath \ "partnerNino").readNullable[String]((__ \ "value").read[String]).
            orElse((JsPath \ "partnerNino" \ "nino").readNullable[String]) and

      (if(isToggleOn) (JsPath \ "noNinoReason").readNullable[String]
      else (JsPath \ "partnerNino" \ "reason").readNullable[String]) and

      (if(isToggleOn) (JsPath \ "utr").readNullable[String]((__ \ "value").read[String])
        else (JsPath \ "partnerUniqueTaxReference" \ "utr").readNullable[String]) and

      (if(isToggleOn) (JsPath \ "noUtrReason").readNullable[String]
      else (JsPath \ "partnerUniqueTaxReference" \ "reason").readNullable[String]) and

      (JsPath \ "partnerAddressYears").read[String] and
      (JsPath \ "partnerPreviousAddress").readNullable[Address]
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

  private def readsCompanyDirectors: Reads[Seq[Individual]] =
    readsFiltered(_ \ "directorDetails", readsCompanyDirector, "directorDetails")

  private def readsPartners(isToggleOn: Boolean): Reads[Seq[Individual]] =
    readsFiltered(_ \ "partnerDetails", readsPartner(isToggleOn), "partnerDetails")

  case class Company(name: String, vatNumber: Option[String], payeNumber: Option[String], utr: Option[String],
                     noUtrReason: Option[String], crn: Option[String], noCrnReason: Option[String], address: Address,
                     contactDetails: ContactDetails, tradingTime: Option[Boolean], previousAddress: Option[Address], addressYears: String)

  private def companyReads: Reads[Company] = (
    (JsPath \ "companyDetails" \ "companyName").read[String] and
    (JsPath \ "companyVat").readNullable[String]((__ \ "value").read[String]) and
    (JsPath \ "companyPaye").readNullable[String]((__ \ "value").read[String]) and
    (JsPath \ "utr").readNullable[String]((__ \ "value").read[String]) and
    (JsPath \ "noUtrReason").readNullable[String] and
    (JsPath \ "companyRegistrationNumber").readNullable[String]((__ \ "value").read[String]) and
    (JsPath \ "noCrnReason").readNullable[String] and
    (JsPath \ "companyAddress").read[Address] and
    (JsPath \ "companyContactDetails").read[ContactDetails] and
    (JsPath \ "hasBeenTrading").readNullable[Boolean] and
    (JsPath \ "companyPreviousAddress").readNullable[Address] and
    ((JsPath \ "companyAddressYears").read[String] orElse (JsPath \ "trusteesCompanyAddressYears").read[String])
    ) (Company.apply _)

  private def readsEstablisherCompany: Reads[CompanyEstablisher] = (
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
      previousAddressDetails = previousAddressDetails(company.addressYears, company.previousAddress, company.tradingTime),
      directorDetails = directors.getOrElse(Nil)
    )
  )

  case class PartnershipDetail(name: String, vat: Option[String], paye: Option[String], utr: Option[String], utrReason: Option[String],
                               address: Address, contact: ContactDetails, addressYears: String, previousAddress: Option[Address])

  private def partnershipHnSReads(isToggleOn: Boolean): Reads[PartnershipDetail] = (
    (JsPath \ "partnershipDetails" \ "name").read[String] and
      (JsPath \ "partnershipVat").readNullable[String]((__ \ "value").read[String]).
        orElse((JsPath \ "partnershipVat" \ "vat").readNullable[String]) and
      (JsPath \ "partnershipPaye").readNullable[String]((__ \ "value").read[String]).
        orElse((JsPath \ "partnershipPaye" \ "paye").readNullable[String]) and
      (if (isToggleOn)
        (JsPath \ "utr").readNullable[String]((__ \ "value").read[String])
      else
        (JsPath \ "partnershipUniqueTaxReference" \ "utr").readNullable[String]) and
      (if (isToggleOn)
        (JsPath \ "noUtrReason").readNullable[String]
      else
        (JsPath \ "partnershipUniqueTaxReference" \ "reason").readNullable[String]) and
      (JsPath \ "partnershipAddress").read[Address] and
      (JsPath \ "partnershipContactDetails").read[ContactDetails] and
      (JsPath \ "partnershipAddressYears").read[String] and
      (JsPath \ "partnershipPreviousAddress").readNullable[Address]
    ) (PartnershipDetail.apply _)

  private def readsEstablisherPartnership(isToggleOn: Boolean): Reads[Partnership] = (
    JsPath.read(partnershipHnSReads(isToggleOn)) and
      (JsPath \ "otherPartners").readNullable[Boolean] and
      (JsPath \ "partner").readNullable(readsPartners(isToggleOn))
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

  private def readsTrusteeIndividual: Reads[Individual] = (
    readsPersonDetailsHnS(true, "trusteeDetails") and
      (JsPath \ "trusteeAddressId").read[Address] and
      (JsPath \ "trusteeContactDetails").read[ContactDetails] and
      (JsPath \ "trusteeNino").readNullable[String]((__ \ "value").read[String]) and
      (JsPath \ "noNinoReason").readNullable[String] and
      (JsPath \ "utr").readNullable[String]((__ \ "value").read[String]) and
      (JsPath \ "noUtrReason").readNullable[String] and
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

  private def readsTrusteeCompany: Reads[CompanyTrustee] = JsPath.read(companyReads).map(test => CompanyTrustee(
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

  private def readsTrusteePartnership: Reads[PartnershipTrustee] =
    JsPath.read(partnershipHnSReads(true)).map(partnership =>
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

  private def readsEstablisherIndividuals(isToggleOn: Boolean): Reads[Seq[Individual]] =
    readsFiltered(_ \ "establisherDetails", readsEstablisherIndividual(isToggleOn), "establisherDetails")

  private def readsEstablisherCompanies: Reads[Seq[CompanyEstablisher]] =
    readsFiltered(_ \ "companyDetails", readsEstablisherCompany, "companyDetails")

  private def readsEstablisherPartnerships(isToggleOn: Boolean): Reads[Seq[Partnership]] =
    readsFiltered(_ \ "partnershipDetails", readsEstablisherPartnership(isToggleOn), "partnershipDetails")

  private def readsTrusteeIndividuals: Reads[Seq[Individual]] =
    readsFiltered(_ \ "trusteeDetails", readsTrusteeIndividual, "trusteeDetails")

  private def readsTrusteeCompanies: Reads[Seq[CompanyTrustee]] =
    readsFiltered(_ \ "companyDetails", readsTrusteeCompany, "companyDetails")

  private def readsTrusteePartnerships: Reads[Seq[PartnershipTrustee]] =
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

  def readsEstablisherDetails(isToggleOn: Boolean = false): Reads[EstablisherDetails] = (
    (JsPath \ "establishers").readNullable(readsEstablisherIndividuals(isToggleOn)) and
      (JsPath \ "establishers").readNullable(readsEstablisherCompanies) and
      (JsPath \ "establishers").readNullable(readsEstablisherPartnerships(isToggleOn))
    ) ((establisherIndividuals, establisherCompanies, establisherPartnerships) =>
    EstablisherDetails(
      individual = establisherIndividuals.getOrElse(Nil),
      companyOrOrganization = establisherCompanies.getOrElse(Nil),
      partnership = establisherPartnerships.getOrElse(Nil)
    )
  )

  def readsTrusteeDetails: Reads[TrusteeDetails] = (
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
