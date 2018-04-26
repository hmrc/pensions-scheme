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

import java.time.LocalDate

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json
import play.api.libs.json.{JsPath, JsResult, JsSuccess, JsValue, Json, Reads}

case class OrganisationDetailType(name: Option[String] = None, crnNumber: Option[String] = None,
                                  vatRegistrationNumber: Option[String] = None, payeReference: Option[String] = None)

object OrganisationDetailType {
  implicit val formats = Json.format[OrganisationDetailType]
}

case class IndividualDetailType(title: Option[String] = None, firstName: String, middleName: Option[String] = None,
                                lastName: String, dateOfBirth: String)

object IndividualDetailType {
  implicit val formats = Json.format[IndividualDetailType]
}

case class PensionSchemeAdministratorIdentifierStatusType(isExistingPensionSchemaAdministrator: Boolean,
                                                          existingPensionSchemaAdministratorReference: Option[String] = None)

object PensionSchemeAdministratorIdentifierStatusType {
  implicit val formats = Json.format[PensionSchemeAdministratorIdentifierStatusType]
}

case class NumberOfDirectorOrPartnersType(isMorethanTenDirectors: Option[Boolean] = None,
                                          isMorethanTenPartners: Option[Boolean] = None)

object NumberOfDirectorOrPartnersType {
  implicit val formats = Json.format[NumberOfDirectorOrPartnersType]
}

case class CorrespondenceCommonDetail(addressDetail: Address, contactDetail: ContactDetails)

object CorrespondenceCommonDetail {
  implicit val formats = Json.format[CorrespondenceCommonDetail]

  val apiReads: Reads[CorrespondenceCommonDetail] = (
    (JsPath \ "directorContactDetails").read(ContactDetails.apiReads) and
      (JsPath \ "directorAddress").read[Address]
    ) ((contactDetails, address) => CorrespondenceCommonDetail(address, contactDetails))
}

case class DirectorOrPartnerDetailTypeItem(sequenceId: String, entityType: String, title: Option[String] = None,
                                           firstName: String, middleName: Option[String] = None, lastName: String,
                                           dateOfBirth: LocalDate, referenceOrNino: Option[String] = None,
                                           noNinoReason: Option[String] = None, utr: Option[String] = None,
                                           noUtrReason: Option[String] = None,
                                           correspondenceCommonDetail: CorrespondenceCommonDetail,
                                           previousAddressDetail: PreviousAddressDetails)

object DirectorOrPartnerDetailTypeItem {
  implicit val formats = Json.format[DirectorOrPartnerDetailTypeItem]

  val apiReads: Reads[List[DirectorOrPartnerDetailTypeItem]] = json.Reads {
    json =>
      json.validate[Seq[JsValue]].flatMap(elements => {
        val directors: Seq[JsResult[DirectorOrPartnerDetailTypeItem]] = elements.zipWithIndex.map(director => director._1.
          validate[DirectorOrPartnerDetailTypeItem](DirectorOrPartnerDetailTypeItem.directorReads(director._2)))
        directors.foldLeft[JsResult[List[DirectorOrPartnerDetailTypeItem]]](JsSuccess(List.empty)) {
          (directors, currentDirector) => {
            for {
              sequenceOfDirectors <- directors
              director <- currentDirector
            } yield sequenceOfDirectors :+ director
          }
        }
      })
  }

  val directorPersonalDetailsReads: Reads[(String, String, Option[String], LocalDate)] = (
    (JsPath \ "firstName").read[String] and
      (JsPath \ "lastName").read[String] and
      (JsPath \ "middleName").readNullable[String] and
      (JsPath \ "dateOfBirth").read[LocalDate]
    ) ((name, lastName, middleName, dateOfBirth) => (name, lastName, middleName, dateOfBirth))

  def directorReferenceReads(referenceFlag: String, referenceName: String): Reads[(Option[String], Option[String])] = (
    (JsPath \ referenceName).readNullable[String] and
      (JsPath \ "reason").readNullable[String]
    ) ((referenceNumber, reason) => (referenceNumber, reason))

  def directorReads(index : Int): Reads[DirectorOrPartnerDetailTypeItem] = (
    (JsPath \ "directorDetails").read(directorPersonalDetailsReads) and
      (JsPath \ "directorNino").readNullable(directorReferenceReads("hasNino", "nino")) and
      (JsPath \ "directorUtr").readNullable(directorReferenceReads("hasUtr", "utr")) and
      (JsPath).read(PreviousAddressDetails.apiReads("director")) and
      (JsPath).read(CorrespondenceCommonDetail.apiReads)
    ) ((directorPersonalDetails, ninoDetails, utrDetails, previousAddress, addressCommonDetails) => DirectorOrPartnerDetailTypeItem(sequenceId = index.toString,
    entityType = "Director",
    title = None,
    firstName = directorPersonalDetails._1,
    middleName = directorPersonalDetails._3,
    lastName = directorPersonalDetails._2,
    dateOfBirth = directorPersonalDetails._4,
    referenceOrNino = ninoDetails.flatMap(_._1),
    noNinoReason = ninoDetails.flatMap(_._2),
    utr = utrDetails.flatMap(_._1),
    noUtrReason = utrDetails.flatMap(_._2),
    correspondenceCommonDetail = addressCommonDetails,
    previousAddressDetail = previousAddress))
}

case class PensionSchemeAdministrator(customerType: String, legalStatus: String, idType: Option[String] = None,
                                      idNumber: Option[String] = None, sapNumber: String, noIdentifier: Boolean,
                                      organisationDetail: Option[OrganisationDetailType] = None,
                                      individualDetail: Option[IndividualDetailType] = None,
                                      pensionSchemeAdministratoridentifierStatus: PensionSchemeAdministratorIdentifierStatusType,
                                      correspondenceAddressDetail: Address,
                                      correspondenceContactDetail: ContactDetails,
                                      previousAddressDetail: PreviousAddressDetails,
                                      numberOfDirectorOrPartners: Option[NumberOfDirectorOrPartnersType] = None,
                                      directorOrPartnerDetail: Option[List[DirectorOrPartnerDetailTypeItem]] = None)

object PensionSchemeAdministrator {
  implicit val formats = Json.format[PensionSchemeAdministrator]
}