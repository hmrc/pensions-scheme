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

package models.userAnswersToEtmp

import models.{Address, CompanyEstablisher, ContactDetails, CorrespondenceAddressDetails, CorrespondenceContactDetails, Individual, PreviousAddressDetails}
import ReadsCommon.{companyReads, previousAddressDetails, readsFiltered, readsPersonDetails, readsContactDetails}
import play.api.libs.json.{JsPath, Reads}
import play.api.libs.functional.syntax._
import play.api.libs.json.Writes.seq
import play.api.libs.json._

object ReadsEstablisherCompany {

  def readsEstablisherCompanies: Reads[Seq[CompanyEstablisher]] =
    readsFiltered(_ \ "companyDetails", readsEstablisherCompany, "companyDetails")

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

  private def readsCompanyDirectors: Reads[Seq[Individual]] =
    readsFiltered(_ \ "directorDetails", readsCompanyDirector, "directorDetails")

  private def readsCompanyDirector: Reads[Individual] = (
    readsPersonDetails(userAnswersBase = "directorDetails") and
      (JsPath \ "directorAddressId").read[Address] and
      (JsPath \ "directorContactDetails").read[ContactDetails](readsContactDetails) and
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

}
