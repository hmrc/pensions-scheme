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

package models.Reads.establishers

import models.Reads.establishers.EstablishersTestJson.{ninoJsonHnS, utrJsonHnS}
import models._
import org.scalatest.OptionValues
import play.api.libs.json._

object EstablishersTestJson extends OptionValues {

  def establisherIndividualJson(individual: Individual, isDeleted: Boolean = false, isToggleOn: Boolean): JsObject =
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

  def establisherCompany(company: CompanyEstablisher, isDeleted: Boolean = false, isToggleOn: Boolean): JsObject = {

    var json = Json.obj(
      "companyDetails" -> companyDetailsJson(company.organizationName, isDeleted),
      "companyVat" -> vatJson(company.vatRegistrationNumber, isToggleOn),
      "companyPaye" -> payeJson(company.payeReference, isToggleOn),
      "companyAddress" -> addressJson(company.correspondenceAddressDetails.addressDetails),
      "companyContactDetails" -> contactDetailsJson(company.correspondenceContactDetails.contactDetails),
      "companyAddressYears" -> addressYearsJson(company.previousAddressDetails),
      "companyPreviousAddress" -> previousAddressJson(company.previousAddressDetails)
    ) ++ utrJsonHnS(company.utr, company.noUtrReason, "companyUniqueTaxReference", isToggleOn) ++
      crnJsonHnS(company.crnNumber, company.noCrnReason, "companyRegistrationNumber", isToggleOn)

    //scalastyle:off magic.number
    if (company.haveMoreThanTenDirectorOrPartner || company.directorDetails.lengthCompare(10) >= 0) {
      json = json +
        (("otherDirectors", JsBoolean(company.haveMoreThanTenDirectorOrPartner)))
    }
    //scalastyle:on magic.number

    if (company.directorDetails.nonEmpty) {
      json = json +
        (("director", company.directorDetails.foldLeft(Json.arr())((json, director) => json :+ companyDirectorJson(director, isToggleOn = isToggleOn))))
    }

    json
  }

  def companyDirectorJson(director: Individual, isDeleted: Boolean = false, isToggleOn: Boolean): JsObject =
    Json.obj(
      "directorAddressId" -> addressJson(director.correspondenceAddressDetails.addressDetails),
      "directorContactDetails" -> contactDetailsJson(director.correspondenceContactDetails.contactDetails),
      "companyDirectorAddressYears" -> addressYearsJson(director.previousAddressDetails),
      "previousAddress" -> previousAddressJson(director.previousAddressDetails)
    ) ++ personJsonHnS(director, isDeleted, "directorDetails", isToggleOn) ++
      utrJsonHnS(director.utr, director.noUtrReason, "directorUniqueTaxReference", isToggleOn) ++
      ninoJsonHnS(director.referenceOrNino, director.noNinoReason, "directorNino", isToggleOn)

  def partnership(partnership: Partnership, isDeleted: Boolean = false, isToggleOn: Boolean): JsObject = {
    var json = Json.obj(
      "partnershipDetails" -> partnershipDetailsJson(partnership.organizationName, isDeleted),
      "partnershipVat" -> vatJson(partnership.vatRegistrationNumber, isToggleOn),
      "partnershipPaye" -> payeJson(partnership.payeReference, isToggleOn),
      "partnershipAddress" -> addressJson(partnership.correspondenceAddressDetails.addressDetails),
      "partnershipContactDetails" -> contactDetailsJson(partnership.correspondenceContactDetails.contactDetails),
      "partnershipUniqueTaxReference" -> utrJson(partnership.utr, partnership.noUtrReason),
      "partnershipAddressYears" -> addressYearsJson(partnership.previousAddressDetails),
      "partnershipPreviousAddress" -> previousAddressJson(partnership.previousAddressDetails)
    )

    //scalastyle:off magic.number
    if (partnership.haveMoreThanTenDirectorOrPartner || partnership.partnerDetails.lengthCompare(10) >= 0) {
      json = json +
        (("otherPartners", JsBoolean(partnership.haveMoreThanTenDirectorOrPartner)))
    }
    //scalastyle:on magic.number

    if (partnership.partnerDetails.nonEmpty) {
      json = json +
        (("partner", partnership.partnerDetails.foldLeft(Json.arr())((json, partner) => json :+ partnerJson(partner, isToggleOn = isToggleOn))))
    }

    json
  }

  def partnerJson(partner: Individual, isDeleted: Boolean = false, isToggleOn: Boolean): JsObject =
    Json.obj(
      "partnerDetails" -> Json.obj(
        "firstName" -> partner.personalDetails.firstName,
        "middleName" -> partner.personalDetails.middleName,
        "lastName" -> partner.personalDetails.lastName,
        "date" -> partner.personalDetails.dateOfBirth,
        "isDeleted" -> isDeleted
      ),
      "partnerNino" -> ninoJson(partner.referenceOrNino, partner.noNinoReason),
      "partnerUniqueTaxReference" -> utrJson(partner.utr, partner.noUtrReason),
      "partnerAddressId" -> addressJson(partner.correspondenceAddressDetails.addressDetails),
      "partnerContactDetails" -> contactDetailsJson(partner.correspondenceContactDetails.contactDetails),
      "partnerAddressYears" -> addressYearsJson(partner.previousAddressDetails),
      "partnerPreviousAddress" -> previousAddressJson(partner.previousAddressDetails)
    )

  def trusteeIndividualJson(individual: Individual, isDeleted: Boolean = false, isToggleOn: Boolean): JsObject =
    Json.obj(
      "trusteeAddressId" -> addressJson(individual.correspondenceAddressDetails.addressDetails),
      "trusteeContactDetails" -> contactDetailsJson(individual.correspondenceContactDetails.contactDetails),
      "trusteeAddressYears" -> addressYearsJson(individual.previousAddressDetails),
      "trusteePreviousAddress" -> previousAddressJson(individual.previousAddressDetails)
    ) ++ personJsonHnS(individual, isDeleted, "trusteeDetails", isToggleOn) ++
      ninoJsonHnS(individual.referenceOrNino, individual.noNinoReason, "trusteeNino", isToggleOn) ++
      utrJsonHnS(individual.utr, individual.noUtrReason, "uniqueTaxReference", isToggleOn)

  def trusteeCompanyJson(company: CompanyTrustee, isDeleted: Boolean = true, isToggleOn: Boolean): JsObject =
    Json.obj(
      "companyDetails" -> companyDetailsJson(company.organizationName, isDeleted),
      "companyVat" -> vatJson(company.vatRegistrationNumber, isToggleOn),
      "companyPaye" -> payeJson(company.payeReference, isToggleOn),
      "companyAddress" -> addressJson(company.correspondenceAddressDetails.addressDetails),
      "companyContactDetails" -> contactDetailsJson(company.correspondenceContactDetails.contactDetails),
      "trusteesCompanyAddressYears" -> addressYearsJson(company.previousAddressDetails),
      "companyPreviousAddress" -> previousAddressJson(company.previousAddressDetails)
    ) ++ utrJsonHnS(company.utr, company.noUtrReason, "companyUniqueTaxReference", isToggleOn) ++
      crnJsonHnS(company.crnNumber, company.noCrnReason, "companyRegistrationNumber", isToggleOn)

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

  private def ninoJson(nino: Option[String], noNinoReason: Option[String]) =
      nino match {
        case Some(no) => Json.obj("value" -> no)
        case _ =>
          Json.obj(
            "hasNino" -> false,
            "reason" -> noNinoReason
          )
      }

  def ninoJsonHnS(nino: Option[String], noNinoReason: Option[String], userAnswerBase: String, isToggleOn: Boolean) =
    if(isToggleOn) {
      nino match {
        case Some(no) => Json.obj(userAnswerBase -> Json.obj("value" -> no))
        case _ => Json.obj("noNinoReason" -> noNinoReason)
      }
    } else {
      nino match {
        case Some(no) => Json.obj(userAnswerBase -> Json.obj("value" -> no))
        case _ =>
          Json.obj(userAnswerBase -> Json.obj(
            "hasNino" -> false,
            "reason" -> noNinoReason
          ))
      }
    }

  private def vatJson(vat: Option[String], isToggleOn: Boolean) =
    vat match {
      case Some(vat) => if(isToggleOn) Json.obj("value" -> vat) else Json.obj("vat" -> vat)
      case _ => JsNull
    }

  private def payeJson(paye: Option[String], isToggleOn: Boolean) =
    paye match {
      case Some(paye) => if(isToggleOn)  Json.obj("value" -> paye) else Json.obj("paye" -> paye)
      case _ => JsNull
    }

  private def utrJson(utr: Option[String], noUtrReason: Option[String]) =
    valueOrReason(utr, noUtrReason, "hasUtr", "utr")

  def utrJsonHnS(utr: Option[String], noUtrReason: Option[String], userAnswerBase: String, isToggleOn: Boolean) =
    if(isToggleOn) {
      (utr, noUtrReason) match {
        case (Some(utrValue), _) => Json.obj("utr" -> Json.obj("value" -> utrValue))
        case (_, Some(reason)) => Json.obj("noUtrReason" -> reason)
        case _ => Json.obj()
      }
    } else {
      Json.obj(userAnswerBase -> valueOrReason(utr, noUtrReason, "hasUtr", "utr"))
    }

  private def crnJson(crn: Option[String], noCrnReason: Option[String]) =
      crn match {
        case Some(regNo) => Json.obj("value" -> regNo)
        case _ =>
          Json.obj(
            "hasCrn" -> false,
            "reason" -> noCrnReason
          )
      }

  private def crnJsonHnS(crn: Option[String], noCrnReason: Option[String], userAnswerBase: String, isToggleOn: Boolean) =
    if(isToggleOn)
      crn match {
        case Some(regNo) => Json.obj(userAnswerBase -> Json.obj("value" -> regNo))
        case _ => Json.obj("noCrnReason" -> noCrnReason)
      }
     else
      crn match {
        case Some(regNo) => Json.obj(userAnswerBase -> Json.obj("value" -> regNo))
        case _ =>
          Json.obj(userAnswerBase -> Json.obj(
            "hasCrn" -> false,
            "reason" -> noCrnReason
          ))
      }


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

  private def personJsonHnS(person: Individual, isDeleted: Boolean, userAnswersBase: String, isToggleOn: Boolean) = {
    if(isToggleOn)
      Json.obj(userAnswersBase -> Json.obj(
        "firstName" -> person.personalDetails.firstName,
        "lastName" -> person.personalDetails.lastName,
        "isDeleted" -> isDeleted
      ),
      "dateOfBirth" -> person.personalDetails.dateOfBirth)
      else
    Json.obj(userAnswersBase -> Json.obj(
      "firstName" -> person.personalDetails.firstName,
      "middleName" -> person.personalDetails.middleName,
      "lastName" -> person.personalDetails.lastName,
      "date" -> person.personalDetails.dateOfBirth,
      "isDeleted" -> isDeleted
    ))
  }

}
