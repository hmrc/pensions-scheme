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

import play.api.libs.functional.syntax._
import play.api.libs.json
import play.api.libs.json.{JsPath, JsResult, JsSuccess, JsValue, Json, Reads, Writes}

trait PSADetail

object PSADetail {
  val companyDetailsReads : Reads[PSADetail] = (JsPath).read[OrganisationDetailType](OrganisationDetailType.apiReads).map(c=>c.asInstanceOf[PSADetail])
  val individualDetailsReads : Reads[PSADetail] = (JsPath).read[IndividualDetailType](IndividualDetailType.apiReads("individual")).map(
    c=>c.asInstanceOf[PSADetail])

  val apiReads : Reads[PSADetail] = companyDetailsReads orElse individualDetailsReads
}

case class OrganisationDetailType(name: Option[String] = None, crnNumber: Option[String] = None,
                                  vatRegistrationNumber: Option[String] = None, payeReference: Option[String] = None) extends PSADetail

object OrganisationDetailType {
  implicit val formats = Json.format[OrganisationDetailType]

  val companyDetailsReads: Reads[Option[(Option[String], Option[String])]] = (
    (JsPath \ "vatRegistrationNumber").readNullable[String] and
      (JsPath \ "payeEmployerReferenceNumber").readNullable[String]
    ) ((vatRegistrationNumber, payeEmployerReferenceNumber) => {
    (vatRegistrationNumber, payeEmployerReferenceNumber) match {
      case (None, None) => None
      case _ => Some((vatRegistrationNumber, payeEmployerReferenceNumber))
    }
  })

  val apiReads: Reads[OrganisationDetailType] = (
    (JsPath \ "businessDetails" \ "companyName").readNullable[String] and
      (JsPath \ "companyDetails").readNullable(companyDetailsReads) and
      (JsPath \ "companyRegistrationNumber").readNullable[String]
    ) ((name, companyDetails: Option[Option[(Option[String], Option[String])]], crnNumber) =>
    OrganisationDetailType(name, vatRegistrationNumber = companyDetails.flatMap(c => c.flatMap(c => c._1)),
      payeReference = companyDetails.flatMap(c => c.flatMap(c => c._2)),
      crnNumber = crnNumber))
}

case class IndividualDetailType(title: Option[String] = None, firstName: String, middleName: Option[String] = None,
                                lastName: String, dateOfBirth: LocalDate) extends PSADetail

object IndividualDetailType {
  implicit val formats = Json.format[IndividualDetailType]

  def apiReads(individualType: String): Reads[IndividualDetailType] = (
    (JsPath \ s"${individualType}Details" \ "firstName").read[String] and
      (JsPath \ s"${individualType}Details" \ "lastName").read[String] and
      (JsPath \ s"${individualType}Details" \ "middleName").readNullable[String] and
      ((JsPath \ "individualDateOfBirth").read[LocalDate] orElse (JsPath \ s"${individualType}Details" \ "dateOfBirth").read[LocalDate])
    ) ((name, lastName, middleName, dateOfBirth) => IndividualDetailType(None,name, middleName, lastName, dateOfBirth))
}

case class PensionSchemeAdministratorIdentifierStatusType(isExistingPensionSchemaAdministrator: Boolean,
                                                          existingPensionSchemaAdministratorReference: Option[String] = None)

object PensionSchemeAdministratorIdentifierStatusType {
  implicit val formats = Json.format[PensionSchemeAdministratorIdentifierStatusType]

  val apiReads : Reads[PensionSchemeAdministratorIdentifierStatusType] = (
    (JsPath \ "isExistingPSA").read[Boolean] and
      (JsPath \ "existingPSAId").readNullable[String]
    )((isExistingPSA,existingPSAId) => PensionSchemeAdministratorIdentifierStatusType(isExistingPSA,existingPSAId))
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

  val psaSubmissionWrites : Writes[DirectorOrPartnerDetailTypeItem] = (
    (JsPath \ "sequenceId").write[String] and
      (JsPath \ "entityType").write[String] and
      (JsPath \ "title").writeNullable[String] and
      (JsPath \ "firstName").write[String] and
      (JsPath \ "middleName").writeNullable[String] and
      (JsPath \ "lastName").write[String] and
      (JsPath \ "dateOfBirth").write[LocalDate] and
      (JsPath \ "referenceOrNino").writeNullable[String] and
      (JsPath \ "noNinoReason").writeNullable[String] and
      (JsPath \ "utr").writeNullable[String] and
      (JsPath \ "noUtrReason").writeNullable[String] and
      (JsPath \ "correspondenceCommonDetail").write[CorrespondenceCommonDetail] and
      (JsPath \ "previousAddressDetail").write(PreviousAddressDetails.psaSubmissionWrites)
  )(directorOrPartner => (directorOrPartner.sequenceId,
    directorOrPartner.entityType,
    directorOrPartner.title,
    directorOrPartner.firstName,
    directorOrPartner.middleName,
    directorOrPartner.lastName,
    directorOrPartner.dateOfBirth,
    directorOrPartner.referenceOrNino,
    directorOrPartner.noNinoReason,
    directorOrPartner.utr,
    directorOrPartner.noUtrReason,
    directorOrPartner.correspondenceCommonDetail,
    directorOrPartner.previousAddressDetail))

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

  def directorReferenceReads(referenceFlag: String, referenceName: String): Reads[(Option[String], Option[String])] = (
    (JsPath \ referenceName).readNullable[String] and
      (JsPath \ "reason").readNullable[String]
    ) ((referenceNumber, reason) => (referenceNumber, reason))

  def directorReads(index: Int): Reads[DirectorOrPartnerDetailTypeItem] = (
    (JsPath).read(IndividualDetailType.apiReads("director")) and
      (JsPath \ "directorNino").readNullable(directorReferenceReads("hasNino", "nino")) and
      (JsPath \ "directorUtr").readNullable(directorReferenceReads("hasUtr", "utr")) and
      (JsPath).read(PreviousAddressDetails.apiReads("director")) and
      (JsPath).read(CorrespondenceCommonDetail.apiReads)
    ) (
    (directorPersonalDetails, ninoDetails, utrDetails, previousAddress, addressCommonDetails) =>
      DirectorOrPartnerDetailTypeItem(sequenceId = f"${index}%03d",
      entityType = "Director",
      title = None,
      firstName = directorPersonalDetails.firstName,
      middleName = directorPersonalDetails.middleName,
      lastName = directorPersonalDetails.lastName,
      dateOfBirth = directorPersonalDetails.dateOfBirth,
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
                                      directorOrPartnerDetail: Option[List[DirectorOrPartnerDetailTypeItem]] = None,
                                      declaration: PensionSchemeAdministratorDeclarationType)

object PensionSchemeAdministrator {
  implicit val formats = Json.format[PensionSchemeAdministrator]

  val psaSubmissionWrites : Writes[PensionSchemeAdministrator] = (
    (JsPath \ "customerType").write[String] and
      (JsPath \ "legalStatus").write[String] and
      (JsPath \ "idType").writeNullable[String] and
      (JsPath \ "idNumber").writeNullable[String] and
      (JsPath \ "sapNumber").write[String] and
      (JsPath \ "noIdentifier").write[Boolean] and
      (JsPath \ "organisationDetail").writeNullable[OrganisationDetailType] and
      (JsPath \ "individualDetail").writeNullable[IndividualDetailType] and
      (JsPath \ "pensionSchemeAdministratoridentifierStatus").write[PensionSchemeAdministratorIdentifierStatusType] and
      (JsPath \ "correspondenceAddressDetail").write[Address] and
      (JsPath \ "correspondenceContactDetail").write[ContactDetails] and
      (JsPath \ "previousAddressDetail").write(PreviousAddressDetails.psaSubmissionWrites) and
      (JsPath \ "numberOfDirectorOrPartners").writeNullable[NumberOfDirectorOrPartnersType] and
      (JsPath \ "directorOrPartnerDetail").writeNullable[List[JsValue]] and
      (JsPath \ "declaration").write[PensionSchemeAdministratorDeclarationType]
  )(psaSubmission => (psaSubmission.customerType,
  psaSubmission.legalStatus,
  psaSubmission.idType,
  psaSubmission.idNumber,
  psaSubmission.sapNumber,
  psaSubmission.noIdentifier,
  psaSubmission.organisationDetail,
  psaSubmission.individualDetail,
  psaSubmission.pensionSchemeAdministratoridentifierStatus,
  psaSubmission.correspondenceAddressDetail,
  psaSubmission.correspondenceContactDetail,
  psaSubmission.previousAddressDetail,
  psaSubmission.numberOfDirectorOrPartners,
  psaSubmission.directorOrPartnerDetail.map(directors => directors.map(director=>Json.toJson(director)(DirectorOrPartnerDetailTypeItem.psaSubmissionWrites))),
  psaSubmission.declaration))

  val registrationInfoReads: Reads[(String, String, Boolean, String, Option[String], Option[String])] = (
    (JsPath \ "legalStatus").read[String] and
      (JsPath \ "sapNumber").read[String] and
      (JsPath \ "noIdentifier").read[Boolean] and
      (JsPath \ "customerType").read[String] and
      (JsPath \ "idType").readNullable[String] and
      (JsPath \ "idNumber").readNullable[String]
    ) ((legalStatus, sapNumber, noIdentifier, customerType, idType, idNumber) => (legalStatus, sapNumber, noIdentifier, customerType, idType, idNumber))

  def apiReads(implicit contactAddressEnabled: Boolean): Reads[PensionSchemeAdministrator] = (
    (JsPath \ "registrationInfo").read(registrationInfoReads) and
      (JsPath \ "moreThanTenDirectors").readNullable[Boolean] and
      ((JsPath \ "contactDetails").read(ContactDetails.apiReads) orElse (JsPath \ "individualContactDetails").read(ContactDetails.apiReads)) and
      (JsPath.read(PreviousAddressDetails.apiReads("company")) orElse JsPath.read(PreviousAddressDetails.apiReads("individual"))) and
      (if(contactAddressEnabled){
        (JsPath \ "companyContactAddressId").read[Address] orElse (JsPath \ "individualContactAddress").read[Address]
      } else {
        (JsPath \ "companyAddressId").read[Address] orElse (JsPath \ "individualAddress").read[Address]
      }) and
      (JsPath \ "directors").readNullable(DirectorOrPartnerDetailTypeItem.apiReads) and
      JsPath.read(PSADetail.apiReads) and
      (JsPath \ "existingPSA").read(PensionSchemeAdministratorIdentifierStatusType.apiReads) and
      JsPath.read(PensionSchemeAdministratorDeclarationType.apiReads)
    ) ((registrationInfo, isThereMoreThanTenDirectors, contactDetails, previousAddressDetails, correspondenceAddress, directors,
        transactionDetails, isExistingPSA, declaration) =>
    PensionSchemeAdministrator(
      customerType = registrationInfo._4,
      legalStatus = registrationInfo._1,
      sapNumber = registrationInfo._2,
      noIdentifier = registrationInfo._3,
      idType = registrationInfo._5,
      idNumber = registrationInfo._6,
      numberOfDirectorOrPartners = isThereMoreThanTenDirectors.map(isMoreThanTenDirectors =>
        NumberOfDirectorOrPartnersType(isMorethanTenDirectors = Some(isMoreThanTenDirectors))),
      pensionSchemeAdministratoridentifierStatus = isExistingPSA,
      correspondenceAddressDetail = correspondenceAddress,
      correspondenceContactDetail = contactDetails,
      previousAddressDetail = previousAddressDetails,
      directorOrPartnerDetail = directors,
      organisationDetail = if (registrationInfo._1 == "Limited Company") Some(transactionDetails.asInstanceOf[OrganisationDetailType]) else None,
      individualDetail = if (registrationInfo._1 == "Individual") Some(transactionDetails.asInstanceOf[IndividualDetailType]) else None,
      declaration = declaration))
}
