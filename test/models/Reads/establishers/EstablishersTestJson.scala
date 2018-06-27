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

package models.Reads.establishers

import models._
import org.scalatest.OptionValues
import play.api.libs.json._

object EstablishersTestJson extends OptionValues {

  def establisherIndividualJson(individual: Individual, isDeleted: Boolean = false): JsObject =
    Json.obj(
      "establisherDetails" -> Json.obj(
        "firstName" -> individual.personalDetails.firstName,
        "middleName" -> individual.personalDetails.middleName,
        "lastName" -> individual.personalDetails.lastName,
        "date" -> individual.personalDetails.dateOfBirth,
        "isDeleted" -> isDeleted
      ),
      "establisherNino" -> ninoJson(individual.referenceOrNino, individual.noNinoReason),
      "uniqueTaxReference" -> utrJson(individual.utr, individual.noUtrReason),
      "address" -> addressJson(individual.correspondenceAddressDetails.addressDetails),
      "contactDetails" -> contactDetailsJson(individual.correspondenceContactDetails.contactDetails),
      "addressYears" -> addressYearsJson(individual.previousAddressDetails),
      "previousAddress" -> previousAddressJson(individual.previousAddressDetails)
    )

  def establisherCompany(company: CompanyEstablisher, isDeleted: Boolean = false): JsObject = {
    var json = Json.obj(
      "companyDetails" -> companyDetailsJson(company.organizationName, company.vatRegistrationNumber, company.payeReference, isDeleted),
      "companyAddress" -> addressJson(company.correspondenceAddressDetails.addressDetails),
      "companyContactDetails" -> contactDetailsJson(company.correspondenceContactDetails.contactDetails),
      "companyUniqueTaxReference" -> utrJson(company.utr, company.noUtrReason),
      "companyRegistrationNumber" -> crnJson(company.crnNumber, company.noCrnReason),
      "companyAddressYears" -> addressYearsJson(company.previousAddressDetails),
      "companyPreviousAddress" -> previousAddressJson(company.previousAddressDetails)
    )

    //scalastyle:off magic.number
    if (company.haveMoreThanTenDirectorOrPartner || company.directorDetails.lengthCompare(10) >= 0) {
      json = json +
        (("otherDirectors", JsBoolean(company.haveMoreThanTenDirectorOrPartner)))
    }
    //scalastyle:on magic.number

    if (company.directorDetails.nonEmpty) {
      json = json +
        (("director", company.directorDetails.foldLeft(Json.arr())((json, director) => json :+ companyDirectorJson(director))))
    }

    json
  }

  def companyDirectorJson(director: Individual, isDeleted: Boolean = false): JsObject =
    Json.obj(
      "directorDetails" -> Json.obj(
        "firstName" -> director.personalDetails.firstName,
        "middleName" -> director.personalDetails.middleName,
        "lastName" -> director.personalDetails.lastName,
        "date" -> director.personalDetails.dateOfBirth,
        "isDeleted" -> isDeleted
      ),
      "directorNino" -> ninoJson(director.referenceOrNino, director.noNinoReason),
      "directorUniqueTaxReference" -> utrJson(director.utr, director.noUtrReason),
      "directorAddressId" -> addressJson(director.correspondenceAddressDetails.addressDetails),
      "directorContactDetails" -> contactDetailsJson(director.correspondenceContactDetails.contactDetails),
      "companyDirectorAddressYears" -> addressYearsJson(director.previousAddressDetails),
      "previousAddress" -> previousAddressJson(director.previousAddressDetails)
    )

  def trusteeIndividualJson(individual: Individual, isDeleted: Boolean = false): JsObject =
    Json.obj(
      "trusteeDetails" -> Json.obj(
        "firstName" -> individual.personalDetails.firstName,
        "middleName" -> individual.personalDetails.middleName,
        "lastName" -> individual.personalDetails.lastName,
        "date" -> individual.personalDetails.dateOfBirth,
        "isDeleted" -> isDeleted
      ),
      "trusteeNino" -> ninoJson(individual.referenceOrNino, individual.noNinoReason),
      "uniqueTaxReference" -> utrJson(individual.utr, individual.noUtrReason),
      "trusteeAddressId" -> addressJson(individual.correspondenceAddressDetails.addressDetails),
      "trusteeContactDetails" -> contactDetailsJson(individual.correspondenceContactDetails.contactDetails),
      "trusteeAddressYears" -> addressYearsJson(individual.previousAddressDetails),
      "trusteePreviousAddress" -> previousAddressJson(individual.previousAddressDetails)
    )

  def trusteeCompanyJson(company: CompanyTrustee, isDeleted: Boolean = true): JsObject =
    Json.obj(
      "companyDetails" -> companyDetailsJson(company.organizationName, company.vatRegistrationNumber, company.payeReference, isDeleted),
      "companyAddress" -> addressJson(company.correspondenceAddressDetails.addressDetails),
      "companyContactDetails" -> contactDetailsJson(company.correspondenceContactDetails.contactDetails),
      "companyUniqueTaxReference" -> utrJson(company.utr, company.noUtrReason),
      "companyRegistrationNumber" -> crnJson(company.crnNumber, company.noCrnReason),
      "trusteesCompanyAddressYears" -> addressYearsJson(company.previousAddressDetails),
      "companyPreviousAddress" -> previousAddressJson(company.previousAddressDetails)
    )

  private def addressJson(address: Address) = {
    address match {
      case a: UkAddress =>
        Json.obj(
          "addressLine1" -> a.addressLine1,
          "addressLine2" -> a.addressLine2,
          "addressLine3" -> a.addressLine3,
          "addressLine4" -> a.addressLine4,
          "postcode" -> a.postalCode,
          "country" -> a.countryCode
        )
      case a: InternationalAddress =>
        Json.obj(
          "addressLine1" -> a.addressLine1,
          "addressLine2" -> a.addressLine2,
          "addressLine3" -> a.addressLine3,
          "addressLine4" -> a.addressLine4,
          "postcode" -> a.postalCode,
          "country" -> a.countryCode
        )
    }
  }

  private def addressYearsJson(previousAddressDetails: Option[PreviousAddressDetails]) = {
    previousAddressDetails match {
      case Some(PreviousAddressDetails(true, _)) => "under_a_year"
      case _ => "over_a_year"
    }
  }

  private def previousAddressJson(previousAddressDetails: Option[PreviousAddressDetails]) = {
    previousAddressDetails match {
      case Some(PreviousAddressDetails(true, Some(address))) => addressJson(address)
      case _ => JsNull
    }
  }

  private def contactDetailsJson(contactDetails: ContactDetails) =
    Json.obj(
      "emailAddress" -> contactDetails.email,
      "phoneNumber" -> contactDetails.telephone
    )

  private def companyDetailsJson(companyName: String, vat: Option[String], paye: Option[String], isDeleted: Boolean = false) =
    Json.obj(
      "companyName" -> companyName,
      "vatNumber" -> vat,
      "payeNumber" -> paye,
      "isDeleted" -> isDeleted
    )

  private def ninoJson(nino: Option[String], noNinoReason: Option[String]) =
    valueOrReason(nino, noNinoReason, "hasNino", "nino")

  private def utrJson(utr: Option[String], noUtrReason: Option[String]) =
    valueOrReason(utr, noUtrReason, "hasUtr", "utr")

  private def crnJson(crn: Option[String], noCrnReason: Option[String]) =
    valueOrReason(crn, noCrnReason, "hasCrn", "crn")

  private def valueOrReason(value: Option[String], reason: Option[String], hasField: String, valueField: String) =
    value match {
      case Some(v) =>
        Json.obj(
          hasField -> true,
          valueField -> v
        )
      case _ =>
        Json.obj(
          hasField -> false,
          "reason" -> reason
        )
    }

}
