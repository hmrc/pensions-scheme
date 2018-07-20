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
  val companyReads: Reads[PSADetail] = (JsPath).read[OrganisationDetailType](OrganisationDetailType.CompanyApiReads).map(c => c.asInstanceOf[PSADetail])
  val individualDetailsReads: Reads[PSADetail] = (JsPath).read[IndividualDetailType](IndividualDetailType.apiReads("individual")).map(
    c => c.asInstanceOf[PSADetail])
  val partnershipReads: Reads[PSADetail] = (JsPath).read[OrganisationDetailType](
    OrganisationDetailType.partnershipApiReads).map(c => c.asInstanceOf[PSADetail])

  val apiReads: Reads[PSADetail] = companyReads orElse individualDetailsReads orElse partnershipReads
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
    ) ((name, lastName, middleName, dateOfBirth) => IndividualDetailType(None, name, middleName, lastName, dateOfBirth))
}

case class PensionSchemeAdministratorIdentifierStatusType(isExistingPensionSchemaAdministrator: Boolean,
                                                          existingPensionSchemaAdministratorReference: Option[String] = None)

object PensionSchemeAdministratorIdentifierStatusType {
  implicit val formats = Json.format[PensionSchemeAdministratorIdentifierStatusType]

  val apiReads: Reads[PensionSchemeAdministratorIdentifierStatusType] = (
    (JsPath \ "isExistingPSA").read[Boolean] and
      (JsPath \ "existingPSAId").readNullable[String]
    ) ((isExistingPSA, existingPSAId) => PensionSchemeAdministratorIdentifierStatusType(isExistingPSA, existingPSAId))
}

case class NumberOfDirectorOrPartnersType(isMorethanTenDirectors: Option[Boolean] = None,
                                          isMorethanTenPartners: Option[Boolean] = None)

object NumberOfDirectorOrPartnersType {
  implicit val formats = Json.format[NumberOfDirectorOrPartnersType]
}

case class CorrespondenceCommonDetail(addressDetail: Address, contactDetail: ContactDetails)

object CorrespondenceCommonDetail {
  implicit val formats = Json.format[CorrespondenceCommonDetail]

  def apiReads(personType: String): Reads[CorrespondenceCommonDetail] = (
    (JsPath \ s"${personType}ContactDetails").read(ContactDetails.apiReads) and
      (JsPath \ s"${personType}Address").read[Address]
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

  def apiReads(personType: String): Reads[List[DirectorOrPartnerDetailTypeItem]] = json.Reads {
    json =>
      println("\n\n 4..")
      json.validate[Seq[JsValue]].flatMap(elements => {
        val directorsOrPartners: Seq[JsResult[DirectorOrPartnerDetailTypeItem]] =
          filterDeletedDirectorOrPartner(personType, elements).zipWithIndex.map { directorOrPartner =>
            val (directorOrPartnerDetails, index) = directorOrPartner
            directorOrPartnerDetails.validate[DirectorOrPartnerDetailTypeItem](DirectorOrPartnerDetailTypeItem.directorOrPartnerReads(index, personType))
          }
        directorsOrPartners.foldLeft[JsResult[List[DirectorOrPartnerDetailTypeItem]]](JsSuccess(List.empty)) {
          (directors, currentDirector) => {
            for {
              sequenceOfDirectors <- directors
              director <- currentDirector
            } yield sequenceOfDirectors :+ director
          }
        }
      })
  }

  private def filterDeletedDirectorOrPartner(personType: String, jsValueSeq: Seq[JsValue]): Seq[JsValue] = {
    jsValueSeq.filterNot{json =>
      (json \ s"${personType}Details" \ "isDeleted").validate[Boolean] match {
        case JsSuccess(isDeleted, _) => isDeleted
        case _ => false
      }
    }
  }

  def directorOrPartnerReferenceReads(referenceFlag: String, referenceName: String): Reads[(Option[String], Option[String])] = (
    (JsPath \ referenceName).readNullable[String] and
      (JsPath \ "reason").readNullable[String]
    ) ((referenceNumber, reason) => (referenceNumber, reason))

  def directorOrPartnerReads(index: Int, personType: String): Reads[DirectorOrPartnerDetailTypeItem] = (
    (JsPath).read(IndividualDetailType.apiReads(personType)) and
      (JsPath \ s"${personType}Nino").readNullable(directorOrPartnerReferenceReads("hasNino", "nino")) and
      (JsPath \ s"${personType}Utr").readNullable(directorOrPartnerReferenceReads("hasUtr", "utr")) and
      (JsPath).read(PreviousAddressDetails.apiReads(personType)) and
      (JsPath).read(CorrespondenceCommonDetail.apiReads(personType))
    ) (
    (directorOrPartnerPersonalDetails, ninoDetails, utrDetails, previousAddress, addressCommonDetails) =>
      DirectorOrPartnerDetailTypeItem(sequenceId = f"${index}%03d",
      entityType = personType.capitalize,
      title = None,
      firstName = directorOrPartnerPersonalDetails.firstName,
      middleName = directorOrPartnerPersonalDetails.middleName,
      lastName = directorOrPartnerPersonalDetails.lastName,
      dateOfBirth = directorOrPartnerPersonalDetails.dateOfBirth,
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

  val psaSubmissionWrites: Writes[PensionSchemeAdministrator] = (
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
    ) (psaSubmission => (psaSubmission.customerType,
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
    psaSubmission.directorOrPartnerDetail.map(directors => directors.map(director =>
      Json.toJson(director)(DirectorOrPartnerDetailTypeItem.psaSubmissionWrites))),
    psaSubmission.declaration))

  val registrationInfoReads: Reads[(String, String, Boolean, String, Option[String], Option[String])] = (
    (JsPath \ "legalStatus").read[String] and
      (JsPath \ "sapNumber").read[String] and
      (JsPath \ "noIdentifier").read[Boolean] and
      (JsPath \ "customerType").read[String] and
      (JsPath \ "idType").readNullable[String] and
      (JsPath \ "idNumber").readNullable[String]
    ) ((legalStatus, sapNumber, noIdentifier, customerType, idType, idNumber) => (legalStatus, sapNumber, noIdentifier, customerType, idType, idNumber))

  private val contactDetailsReads: Reads[ContactDetails] = {
    ((JsPath \ "contactDetails").read(ContactDetails.apiReads) orElse (JsPath \ "individualContactDetails").read(ContactDetails.apiReads)
      orElse (JsPath \ "partnershipContactDetails").read(ContactDetails.apiReads))
  }

  private val previousAddressReads: Reads[PreviousAddressDetails] = {
    (JsPath.read(PreviousAddressDetails.apiReads("company"))
      orElse JsPath.read(PreviousAddressDetails.apiReads("individual"))
      orElse JsPath.read(PreviousAddressDetails.apiReads("partnership")))
  }

  private val contactAddressReads: Reads[Address] = {
    (JsPath \ "companyContactAddress").read[Address] orElse
      (JsPath \ "individualContactAddress").read[Address] orElse
      (JsPath \ "partnershipContactAddress").read[Address]
  }

  private val directorsOrPartnersReads = {
    println("\n\n\n 1...")
    (JsPath \ "directors").readNullable(DirectorOrPartnerDetailTypeItem.apiReads("director")) orElse
      (JsPath \ "partners").readNullable(DirectorOrPartnerDetailTypeItem.apiReads("partner"))
  }

  private val organisationLegalStatus = Seq("Limited Company", "Partnership")

  private def numberOfDirectorsOrPartners(isThereMoreThanTenDirectors: Option[Boolean],
                                          isThereMoreThanTenPartners: Option[Boolean]): Option[NumberOfDirectorOrPartnersType] =
    (isThereMoreThanTenDirectors, isThereMoreThanTenPartners) match {
      case (None, None) => None
      case _ => Some(NumberOfDirectorOrPartnersType(isThereMoreThanTenDirectors, isThereMoreThanTenPartners))
    }

  val apiReads: Reads[PensionSchemeAdministrator] = (
    (JsPath \ "registrationInfo").read(registrationInfoReads) and
      (JsPath \ "moreThanTenDirectors").readNullable[Boolean] and
      (JsPath \ "moreThanTenPartners").readNullable[Boolean] and
      contactDetailsReads and
      previousAddressReads and
      contactAddressReads and
      ((JsPath \ "directors").readNullable(DirectorOrPartnerDetailTypeItem.apiReads("director")) orElse
      (JsPath \ "partners").readNullable(DirectorOrPartnerDetailTypeItem.apiReads("partner"))) and
      JsPath.read(PSADetail.apiReads) and
      (JsPath \ "existingPSA").read(PensionSchemeAdministratorIdentifierStatusType.apiReads) and
      JsPath.read(PensionSchemeAdministratorDeclarationType.apiReads)
    ) ((registrationInfo,
        isThereMoreThanTenDirectors,
        isThereMoreThanTenPartners,
        contactDetails,
        previousAddressDetails,
        correspondenceAddress,
        directors,
        transactionDetails,
        isExistingPSA,
        declaration) =>

    PensionSchemeAdministrator(
      customerType = registrationInfo._4,
      legalStatus = registrationInfo._1,
      sapNumber = registrationInfo._2,
      noIdentifier = registrationInfo._3,
      idType = registrationInfo._5,
      idNumber = registrationInfo._6,
      numberOfDirectorOrPartners = numberOfDirectorsOrPartners(isThereMoreThanTenDirectors, isThereMoreThanTenPartners),
      pensionSchemeAdministratoridentifierStatus = isExistingPSA,
      correspondenceAddressDetail = correspondenceAddress,
      correspondenceContactDetail = contactDetails,
      previousAddressDetail = previousAddressDetails,
      directorOrPartnerDetail = directors,
      organisationDetail = if (organisationLegalStatus.contains(registrationInfo._1)) Some(transactionDetails.asInstanceOf[OrganisationDetailType]) else None,
      individualDetail = if (registrationInfo._1 == "Individual") Some(transactionDetails.asInstanceOf[IndividualDetailType]) else None,
      declaration = declaration))
}
