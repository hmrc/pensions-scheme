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

package models.Reads

import models.{Reads => _, _}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{Json, _}

class PensionSchemeAdministratorReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples {
  "JSON Payload of a PSA" should {
    "Map to a valid PensionSchemeAdministrator object" when {
      val input = Json.obj("registrationInfo" -> Json.obj("legalStatus" -> "Limited Company",
        "sapNumber" -> "NumberTest",
        "noIdentifier" -> JsBoolean(true),
        "customerType" -> "TestCustomer",
        "idType" -> JsString("TestId"),
        "idNumber" -> JsString("TestIdNumber"),
        "isExistingPSA" -> JsBoolean(false)),
        "contactDetails" -> Json.obj("phone" -> "07592113", "email" -> "test@test.com"),
        "companyAddressYears" -> JsString("over_a_year"),
        "companyAddressId" -> JsObject(Map("addressLine1" -> JsString("line1"), "addressLine2" -> JsString("line2"), "addressLine3" -> JsString("line3"),
          "addressLine4" -> JsString("line4"), "postalCode" -> JsString("NE1"), "countryCode" -> JsString("GB"))),
        "companyDetails" -> Json.obj("vatRegistrationNumber" -> JsString("VAT11111"), "payeEmployerReferenceNumber" -> JsString("PAYE11111")),
        "companyRegistrationNumber" -> JsString("CRN11111"),
        "businessDetails" -> Json.obj("companyName" -> JsString("Company Test")))

      "We have a valid legalStatus" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(apiReads).asOpt.value

        result.legalStatus mustEqual pensionSchemeAdministratorSample.legalStatus
      }

      "We have a valid sapNumber" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(apiReads).asOpt.value

        result.sapNumber mustEqual pensionSchemeAdministratorSample.sapNumber
      }

      "We have a valid noIdentifier" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(apiReads).asOpt.value

        result.noIdentifier mustEqual pensionSchemeAdministratorSample.noIdentifier
      }

      "We have valid customerType" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(apiReads).asOpt.value

        result.customerType mustEqual pensionSchemeAdministratorSample.customerType
      }

      "We have a valid idType" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(apiReads).asOpt.value

        result.idType mustEqual pensionSchemeAdministratorSample.idType
      }

      "We have a valid idNumber" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(apiReads).asOpt.value

        result.idNumber mustEqual pensionSchemeAdministratorSample.idNumber
      }

      "We have a moreThanTenDirectors flag" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input + ("moreThanTenDirectors" -> JsBoolean(true)))(apiReads).asOpt.value

        result.numberOfDirectorOrPartners.value.isMorethanTenDirectors mustEqual pensionSchemeAdministratorSample.numberOfDirectorOrPartners.value.isMorethanTenDirectors
      }

      "We don't have moreThanTenDirectors flag" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(apiReads).asOpt.value

        result.numberOfDirectorOrPartners mustEqual None
      }

      "We have contact details" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(apiReads).asOpt.value

        result.correspondenceContactDetail.telephone mustBe pensionSchemeAdministratorSample.correspondenceContactDetail.telephone
      }

      "We have previous address details" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(apiReads).asOpt.value

        result.previousAddressDetail.isPreviousAddressLast12Month mustBe pensionSchemeAdministratorSample.previousAddressDetail.isPreviousAddressLast12Month
      }

      "We have correspondence address" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(apiReads).asOpt.value

        result.correspondenceAddressDetail mustBe pensionSchemeAdministratorSample.correspondenceAddressDetail
      }

      "We have a director" in {
        val director = Json.obj("directorDetails" -> Json.obj("firstName" -> JsString("John"),
          "lastName" -> JsString("Doe"),
          "middleName" -> JsString("Does Does"),
          "dateOfBirth" -> JsString("2019-01-31")),
          "directorNino" -> Json.obj("hasNino" -> JsBoolean(true), "nino" -> JsString("SL211111A")),
          "directorUtr" -> Json.obj("hasUtr" -> JsBoolean(true), "utr" -> JsString("123456789")),
          "directorAddressYears" -> JsString("over_a_year")
        ) + ("directorContactDetails" -> Json.obj("email" -> "test@test.com", "phone" -> "07592113")) + ("directorAddress" ->
          Json.obj("lines" -> JsArray(Seq(JsString("line1"), JsString("line2"))),
            "country" -> JsObject(Map("name" -> JsString("IT")))))

        val directors = JsArray(Seq(director, director))
        val pensionSchemeAdministrator = input + ("directors" -> directors)
        val result = Json.fromJson[PensionSchemeAdministrator](pensionSchemeAdministrator)(apiReads).asOpt.value

        result.directorOrPartnerDetail.value.head.sequenceId mustBe directorSample.sequenceId
      }

      "We have organisation details" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(apiReads).asOpt.value

        result.organisationDetail.value.crnNumber mustBe companySample.crnNumber
      }

      "We have individual details" in {
        val inputWithIndividualDetails = input + ("individualDetails" -> Json.obj("firstName" -> JsString("John"),
          "lastName" -> JsString("Doe"),
          "middleName" -> JsString("Does Does"),
          "dateOfBirth" -> JsString("2019-01-31"))) + ("registrationInfo" -> Json.obj("legalStatus" -> "Individual",
          "sapNumber" -> "NumberTest",
          "noIdentifier" -> JsBoolean(true),
          "customerType" -> "TestCustomer",
          "idType" -> JsString("TestId"),
          "idNumber" -> JsString("TestIdNumber"), "isExistingPSA" -> JsBoolean(false))) - "businessDetails" - "companyDetails" - "companyRegistrationNumber"

        val result = Json.fromJson[PensionSchemeAdministrator](inputWithIndividualDetails)(apiReads).asOpt.value

        result.individualDetail.value.dateOfBirth mustBe individualSample.dateOfBirth
      }

      "We have organisation details but no individual details" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(apiReads).asOpt.value

        result.individualDetail mustBe None
      }

      "We have individual details but no organisation details" in {
        val inputWithIndividualDetails = input + ("individualDetails" -> Json.obj("firstName" -> JsString("John"),
          "lastName" -> JsString("Doe"),
          "middleName" -> JsString("Does Does"),
          "dateOfBirth" -> JsString("2019-01-31"))) + ("registrationInfo" -> Json.obj("legalStatus" -> "Individual",
          "sapNumber" -> "NumberTest",
          "noIdentifier" -> JsBoolean(true),
          "customerType" -> "TestCustomer",
          "idType" -> JsString("TestId"),
          "idNumber" -> JsString("TestIdNumber"),"isExistingPSA" -> JsBoolean(false))) - "businessDetails" - "companyDetails" - "companyRegistrationNumber"

        val result = Json.fromJson[PensionSchemeAdministrator](inputWithIndividualDetails)(apiReads).asOpt.value

        result.organisationDetail mustBe None
      }

      "We have individual with Individual Contact Details" in {
        val expectedContactDetails = contactDetailsSample.copy(telephone = "11111")
        val individiualContactDetails = "individualContactDetails" -> Json.obj("phone" -> "11111", "email" -> "test@test.com")
        val result = Json.fromJson[PensionSchemeAdministrator](input + individiualContactDetails - "contactDetails")(apiReads).asOpt.value

        result.correspondenceContactDetail.telephone mustBe expectedContactDetails.telephone
      }

      "We have an individual address" in {
        val expectedIndividualAddress = ukAddressSample.copy(addressLine1 = "Test 123 St")
        val individualCorrespondenceAddress = "individualAddress" -> JsObject(Map("addressLine1" -> JsString("Test 123 St"), "addressLine2" -> JsString("line2"), "addressLine3" -> JsString("line3"),
          "addressLine4" -> JsString("line4"), "postalCode" -> JsString("NE1"), "countryCode" -> JsString("GB")))
        val result = Json.fromJson[PensionSchemeAdministrator](input + individualCorrespondenceAddress - "companyAddressId")(apiReads).asOpt.value

        result.correspondenceAddressDetail.asInstanceOf[UkAddress].addressLine1 mustBe expectedIndividualAddress.addressLine1
      }

      "We have an individual previous address" in {
        val expectedIndividualPreviousAddress = previousAddressDetailsSample.copy(isPreviousAddressLast12Month = false, None)
        val individualPreviousAddress = "individualAddressYears" -> JsString("over_a_year")
        val result = Json.fromJson[PensionSchemeAdministrator](input + individualPreviousAddress - "companyAddressYears")(apiReads).asOpt.value

        result.previousAddressDetail.isPreviousAddressLast12Month mustBe expectedIndividualPreviousAddress.isPreviousAddressLast12Month
      }

      "The user is not an existing PSA user" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(apiReads).asOpt.value

        result.pensionSchemeAdministratoridentifierStatus.isExistingPensionSchemaAdministrator mustBe pensionSchemeAdministratorSample.pensionSchemeAdministratoridentifierStatus.isExistingPensionSchemaAdministrator
      }

      "The user is an existing PSA user with no previous reference" in {
         val registrationInfo = "registrationInfo" -> Json.obj("legalStatus" -> "Limited Company",
           "sapNumber" -> "NumberTest",
           "noIdentifier" -> JsBoolean(true),
           "customerType" -> "TestCustomer",
           "idType" -> JsString("TestId"),
           "idNumber" -> JsString("TestIdNumber"),
           "isExistingPSA" -> JsBoolean(true))

        val result = Json.fromJson[PensionSchemeAdministrator](input + registrationInfo)(apiReads).asOpt.value

        result.pensionSchemeAdministratoridentifierStatus.isExistingPensionSchemaAdministrator mustBe true
      }

      "The user is an existing PSA user with previous reference number" in {
        val registrationInfo = "registrationInfo" -> Json.obj("legalStatus" -> "Limited Company",
          "sapNumber" -> "NumberTest",
          "noIdentifier" -> JsBoolean(true),
          "customerType" -> "TestCustomer",
          "idType" -> JsString("TestId"),
          "idNumber" -> JsString("TestIdNumber"),
          "isExistingPSA" -> JsBoolean(true),
          "existingPSAId" -> JsString("TestId"))

        val result = Json.fromJson[PensionSchemeAdministrator](input + registrationInfo)(apiReads).asOpt.value

        result.pensionSchemeAdministratoridentifierStatus.existingPensionSchemaAdministratorReference mustBe Some("TestId")
      }
    }
  }

  val registrationInfoReads: Reads[(String, String, Boolean, String, Option[String], Option[String], PensionSchemeAdministratorIdentifierStatusType)] = (
    (JsPath \ "legalStatus").read[String] and
      (JsPath \ "sapNumber").read[String] and
      (JsPath \ "noIdentifier").read[Boolean] and
      (JsPath \ "customerType").read[String] and
      (JsPath \ "idType").readNullable[String] and
      (JsPath \ "idNumber").readNullable[String] and
      (JsPath \ "isExistingPSA").read[Boolean] and
      (JsPath \ "existingPSAId").readNullable[String]
    ) ((legalStatus, sapNumber, noIdentifier, customerType, idType, idNumber, isExistingPSA, existingPSAId) => (legalStatus, sapNumber, noIdentifier, customerType, idType, idNumber, PensionSchemeAdministratorIdentifierStatusType(isExistingPSA,existingPSAId)))

  val apiReads: Reads[PensionSchemeAdministrator] = (
    (JsPath \ "registrationInfo").read(registrationInfoReads) and
      (JsPath \ "moreThanTenDirectors").readNullable[Boolean] and
      ((JsPath \ "contactDetails").read(ContactDetails.apiReads) orElse (JsPath \ "individualContactDetails").read(ContactDetails.apiReads)) and
      ((JsPath).read(PreviousAddressDetails.apiReads("company")) orElse (JsPath).read(PreviousAddressDetails.apiReads("individual"))) and
      ((JsPath \ "companyAddressId").read[Address] orElse (JsPath \ "individualAddress").read[Address]) and
      (JsPath \ "directors").readNullable(DirectorOrPartnerDetailTypeItem.apiReads) and
      (JsPath).read(PSADetail.apiReads)
    ) ((registrationInfo, isThereMoreThanTenDirectors, contactDetails, previousAddressDetails, correspondenceAddress, directors, transactionDetails) => PensionSchemeAdministrator(
    customerType = registrationInfo._4,
    legalStatus = registrationInfo._1,
    sapNumber = registrationInfo._2,
    noIdentifier = registrationInfo._3,
    idType = registrationInfo._5,
    idNumber = registrationInfo._6,
    numberOfDirectorOrPartners = isThereMoreThanTenDirectors.map(isMoreThanTenDirectors => NumberOfDirectorOrPartnersType(isMorethanTenDirectors = Some(isMoreThanTenDirectors))),
    pensionSchemeAdministratoridentifierStatus = registrationInfo._7,
    correspondenceAddressDetail = correspondenceAddress,
    correspondenceContactDetail = contactDetails,
    previousAddressDetail = previousAddressDetails,
    directorOrPartnerDetail = directors,
    organisationDetail = if (registrationInfo._1 == "Limited Company") Some(transactionDetails.asInstanceOf[OrganisationDetailType]) else None,
    individualDetail = if (registrationInfo._1 == "Individual") Some(transactionDetails.asInstanceOf[IndividualDetailType]) else None))
}
