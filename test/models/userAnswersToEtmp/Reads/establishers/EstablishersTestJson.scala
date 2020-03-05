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

package models.userAnswersToEtmp.Reads.establishers

import models._
import models.userAnswersToEtmp._
import models.userAnswersToEtmp.establisher.{CompanyEstablisher, Partnership}
import models.userAnswersToEtmp.trustee.CompanyTrustee
import org.scalatest.OptionValues
import play.api.libs.json._

object EstablishersTestJson extends OptionValues {

  def establisherIndividualJson(individual: Individual, isDeleted: Boolean = false): JsObject =
    Json.obj(
      "address" -> addressJson(individual.correspondenceAddressDetails.addressDetails),
      "contactDetails" -> contactDetailsJson(individual.correspondenceContactDetails.contactDetails),
      "addressYears" -> addressYearsJson(individual.previousAddressDetails),
      "previousAddress" -> previousAddressJson(individual.previousAddressDetails)) ++
      personJson(individual, isDeleted, "establisherDetails") ++
        ninoJson(individual.referenceOrNino, individual.noNinoReason, "establisherNino") ++
        utrJson(individual.utr, individual.noUtrReason, "uniqueTaxReference")

  def establisherCompany(company: CompanyEstablisher, isDeleted: Boolean = false): JsObject = {

    var json = Json.obj(
      "companyDetails" -> companyDetailsJson(company.organizationName, isDeleted),
      "companyVat" -> vatJson(company.vatRegistrationNumber),
      "companyPaye" -> payeJson(company.payeReference),
      "companyAddress" -> addressJson(company.correspondenceAddressDetails.addressDetails),
      "companyContactDetails" -> contactDetailsJson(company.correspondenceContactDetails.contactDetails),
      "companyAddressYears" -> addressYearsJson(company.previousAddressDetails),
      "companyPreviousAddress" -> previousAddressJson(company.previousAddressDetails)
    ) ++ utrJson(company.utr, company.noUtrReason, "companyUniqueTaxReference") ++
      crnJson(company.crnNumber, company.noCrnReason, "companyRegistrationNumber")

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
      "directorAddressId" -> addressJson(director.correspondenceAddressDetails.addressDetails),
      "directorContactDetails" -> contactDetailsJson(director.correspondenceContactDetails.contactDetails),
      "companyDirectorAddressYears" -> addressYearsJson(director.previousAddressDetails),
      "previousAddress" -> previousAddressJson(director.previousAddressDetails)
    ) ++ personJson(director, isDeleted, "directorDetails") ++
      utrJson(director.utr, director.noUtrReason, "directorUniqueTaxReference") ++
      ninoJson(director.referenceOrNino, director.noNinoReason, "directorNino")

  def partnership(partnership: Partnership, isDeleted: Boolean = false): JsObject = {
    var json = Json.obj(
      "partnershipDetails" -> partnershipDetailsJson(partnership.organizationName, isDeleted),
      "partnershipVat" -> vatJson(partnership.vatRegistrationNumber),
      "partnershipPaye" -> payeJson(partnership.payeReference),
      "partnershipAddress" -> addressJson(partnership.correspondenceAddressDetails.addressDetails),
      "partnershipContactDetails" -> contactDetailsJson(partnership.correspondenceContactDetails.contactDetails),
      "partnershipAddressYears" -> addressYearsJson(partnership.previousAddressDetails),
      "partnershipPreviousAddress" -> previousAddressJson(partnership.previousAddressDetails)
    ) ++
      utrJson(partnership.utr, partnership.noUtrReason, "partnershipUniqueTaxReference")

    //scalastyle:off magic.number
    if (partnership.haveMoreThanTenDirectorOrPartner || partnership.partnerDetails.lengthCompare(10) >= 0) {
      json = json +
        (("otherPartners", JsBoolean(partnership.haveMoreThanTenDirectorOrPartner)))
    }
    //scalastyle:on magic.number

    if (partnership.partnerDetails.nonEmpty) {
      json = json +
        (("partner", partnership.partnerDetails.foldLeft(Json.arr())((json, partner) => json :+ partnerJson(partner))))
    }

    json
  }

  def partnerJson(partner: Individual, isDeleted: Boolean = false): JsObject =
    Json.obj(
      "partnerAddressId" -> addressJson(partner.correspondenceAddressDetails.addressDetails),
      "partnerContactDetails" -> contactDetailsJson(partner.correspondenceContactDetails.contactDetails),
      "partnerAddressYears" -> addressYearsJson(partner.previousAddressDetails),
      "partnerPreviousAddress" -> previousAddressJson(partner.previousAddressDetails)) ++
      personJson(partner, isDeleted, "partnerDetails") ++
      ninoJson(partner.referenceOrNino, partner.noNinoReason, "partnerNino") ++
      utrJson(partner.utr, partner.noUtrReason, "partnerUniqueTaxReference")

  def trusteeIndividualJson(individual: Individual, isDeleted: Boolean = false): JsObject =
    Json.obj(
      "trusteeAddressId" -> addressJson(individual.correspondenceAddressDetails.addressDetails),
      "trusteeContactDetails" -> contactDetailsJson(individual.correspondenceContactDetails.contactDetails),
      "trusteeAddressYears" -> addressYearsJson(individual.previousAddressDetails),
      "trusteePreviousAddress" -> previousAddressJson(individual.previousAddressDetails)
    ) ++ personJson(individual, isDeleted, "trusteeDetails") ++
      ninoJson(individual.referenceOrNino, individual.noNinoReason, "trusteeNino") ++
      utrJson(individual.utr, individual.noUtrReason, "uniqueTaxReference")

  def trusteeCompanyJson(company: CompanyTrustee, isDeleted: Boolean = true): JsObject =
    Json.obj(
      "companyDetails" -> companyDetailsJson(company.organizationName, isDeleted),
      "companyVat" -> vatJson(company.vatRegistrationNumber),
      "companyPaye" -> payeJson(company.payeReference),
      "companyAddress" -> addressJson(company.correspondenceAddressDetails.addressDetails),
      "companyContactDetails" -> contactDetailsJson(company.correspondenceContactDetails.contactDetails),
      "trusteesCompanyAddressYears" -> addressYearsJson(company.previousAddressDetails),
      "companyPreviousAddress" -> previousAddressJson(company.previousAddressDetails)
    ) ++ utrJson(company.utr, company.noUtrReason, "companyUniqueTaxReference") ++
      crnJson(company.crnNumber, company.noCrnReason, "companyRegistrationNumber")

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

  private def companyDetailsJson(companyName: String, isDeleted: Boolean = false) =
    Json.obj(
      "companyName" -> companyName,
      "isDeleted" -> isDeleted
    )

  private def partnershipDetailsJson(name: String, isDeleted: Boolean = false) =
    Json.obj(
      "name" -> name,
      "isDeleted" -> isDeleted
    )

  def ninoJson(nino: Option[String], noNinoReason: Option[String], userAnswerBase: String): JsObject =
      nino match {
        case Some(no) => Json.obj(userAnswerBase -> Json.obj("value" -> no))
        case _ => Json.obj("noNinoReason" -> noNinoReason)
      }

  private def vatJson(vat: Option[String]): JsValue =
    vat match {
      case Some(vatNumber) => Json.obj("value" -> vatNumber)
      case _ => JsNull
    }

  private def payeJson(paye: Option[String]): JsValue =
    paye match {
      case Some(payeNumber) => Json.obj("value" -> payeNumber)
      case _ => JsNull
    }

  def utrJson(utr: Option[String], noUtrReason: Option[String], userAnswerBase: String): JsObject =
      (utr, noUtrReason) match {
        case (Some(utrValue), _) => Json.obj("utr" -> Json.obj("value" -> utrValue))
        case (_, Some(reason)) => Json.obj("noUtrReason" -> reason)
        case _ => Json.obj()
      }

  private def crnJson(crn: Option[String], noCrnReason: Option[String], userAnswerBase: String): JsObject =
      crn match {
        case Some(regNo) => Json.obj(userAnswerBase -> Json.obj("value" -> regNo))
        case _ => Json.obj("noCrnReason" -> noCrnReason)
      }


  private def valueOrReason(value: Option[String], reason: Option[String], hasField: String, valueField: String): JsObject =
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

  private def personJson(person: Individual, isDeleted: Boolean, userAnswersBase: String) =
      Json.obj(userAnswersBase -> Json.obj(
        "firstName" -> person.personalDetails.firstName,
        "lastName" -> person.personalDetails.lastName,
        "isDeleted" -> isDeleted
      ),
      "dateOfBirth" -> person.personalDetails.dateOfBirth)
}
