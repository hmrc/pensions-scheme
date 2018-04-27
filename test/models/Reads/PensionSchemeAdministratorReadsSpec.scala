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
      val input = Json.obj(
        "legalStatus" -> "Individual",
        "sapNumber" -> "NumberTest",
        "noIdentifier" -> JsBoolean(true),
        "customerType" -> "TestCustomer",
        "contactDetails" -> Json.obj("phone" -> "07592113", "email" -> "test@test.com"),
        "companyAddressYears" -> JsString("over_a_year"),
        "companyAddressId" -> JsObject(Map("addressLine1" -> JsString("line1"),"addressLine2" -> JsString("line2"),"addressLine3" -> JsString("line3"),"addressLine4" -> JsString("line4"),"postalCode" -> JsString("NE1"),"countryCode" -> JsString("GB"))))

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
        val result = Json.fromJson[PensionSchemeAdministrator](input + ("idType" -> JsString("TestId")))(apiReads).asOpt.value

        result.idType mustEqual pensionSchemeAdministratorSample.idType
      }

      "We have a valid idNumber" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input + ("idNumber" -> JsString("TestIdNumber")))(apiReads).asOpt.value

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
        ) + ("directorContactDetails" -> Json.obj("email" -> "test@test.com", "phone" -> "07592113")) + ("directorAddress"->
          Json.obj("lines" -> JsArray(Seq(JsString("line1"),JsString("line2"))),
            "country" -> JsObject(Map("name" -> JsString("IT")))))

        val directors = JsArray(Seq(director,director))
        val pensionSchemeAdministrator = input + ("directors" -> directors)
        val result = Json.fromJson[PensionSchemeAdministrator](pensionSchemeAdministrator)(apiReads).asOpt.value

        result.directorOrPartnerDetail.value.head.sequenceId mustBe directorSample.sequenceId
      }
    }
  }

  val apiReads: Reads[PensionSchemeAdministrator] = (
    (JsPath \ "legalStatus").read[String] and //TODO: Missing mapping
      (JsPath \ "sapNumber").read[String] and //TODO: Missing mapping
      (JsPath \ "noIdentifier").read[Boolean] and //TODO: Missing mapping
      (JsPath \ "customerType").read[String] and //TODO: Missing mapping
      (JsPath \ "idType").readNullable[String] and //TODO: Missing mapping
      (JsPath \ "idNumber").readNullable[String] and //TODO: Missing mapping
      (JsPath \ "moreThanTenDirectors").readNullable[Boolean] and
      (JsPath \ "contactDetails").read(ContactDetails.apiReads) and
      (JsPath).read(PreviousAddressDetails.apiReads("company")) and
      (JsPath \ "companyAddressId").read[Address] and
      (JsPath \ "directors").readNullable(DirectorOrPartnerDetailTypeItem.apiReads)
    ) ((legalStatus, sapNumber, noIdentifier, customerType, idType,
        idNumber, isThereMoreThanTenDirectors, contactDetails,previousAddressDetails, correspondenceAddress, directors) => PensionSchemeAdministrator(
    customerType = customerType,
    legalStatus = legalStatus,
    sapNumber = sapNumber,
    noIdentifier = noIdentifier,
    idType = idType,
    idNumber = idNumber,
    numberOfDirectorOrPartners = isThereMoreThanTenDirectors.map(c=>NumberOfDirectorOrPartnersType(isMorethanTenDirectors = Some(c))),
    pensionSchemeAdministratoridentifierStatus = PensionSchemeAdministratorIdentifierStatusType(isExistingPensionSchemaAdministrator = false),
    correspondenceAddressDetail = correspondenceAddress,
    correspondenceContactDetail = contactDetails,
    previousAddressDetail = previousAddressDetails,
    directorOrPartnerDetail = directors))
}