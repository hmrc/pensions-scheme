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

class PensionSchemeAdministratorReadsSpec extends WordSpec with MustMatchers with OptionValues {
  "JSON Payload of a PSA" should {
    "Map to a valid PensionSchemeAdministrator object" when {
      val input = Json.obj(
        "legalStatus" -> "Individual",
        "sapNumber" -> "NumberTest",
        "noIdentifier" -> JsBoolean(true),
        "customerType" -> "TestCustomer",
        "contactDetails" -> Json.obj("phone" -> "07592113", "email" -> "test@test.com")
      )

      "We have a valid legalStatus" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(apiReads).asOpt.value

        result.legalStatus mustEqual "Individual"
      }

      "We have a valid sapNumber" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(apiReads).asOpt.value

        result.sapNumber mustEqual "NumberTest"
      }

      "We have a valid noIdentifier" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(apiReads).asOpt.value

        result.noIdentifier mustEqual true
      }

      "We have valid customerType" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(apiReads).asOpt.value

        result.customerType mustEqual "TestCustomer"
      }

      "We have a valid idType" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input + ("idType" -> JsString("TestId")))(apiReads).asOpt.value

        result.idType mustEqual Some("TestId")
      }

      "We have a valid idNumber" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input + ("idNumber" -> JsString("TestIdNumber")))(apiReads).asOpt.value

        result.idNumber mustEqual Some("TestIdNumber")
      }

      "We have a moreThanTenDirectors flag" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input + ("moreThanTenDirectors" -> JsBoolean(true)))(apiReads).asOpt.value

        result.numberOfDirectorOrPartners.value.isMorethanTenDirectors.value mustEqual true
      }

      "We don't have moreThanTenDirectors flag" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(apiReads).asOpt.value

        result.numberOfDirectorOrPartners mustEqual None
      }

      "We have contact details" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(apiReads).asOpt.value

        result.correspondenceContactDetail.telephone mustBe "07592113"
      }
    }
  }

  val apiReads: Reads[PensionSchemeAdministrator] = (
    (JsPath \ "legalStatus").read[String] and
      (JsPath \ "sapNumber").read[String] and
      (JsPath \ "noIdentifier").read[Boolean] and
      (JsPath \ "customerType").read[String] and
      (JsPath \ "idType").readNullable[String] and
      (JsPath \ "idNumber").readNullable[String] and
      (JsPath \ "moreThanTenDirectors").readNullable[Boolean] and
      (JsPath \ "contactDetails").read(ContactDetails.apiReads)
    ) ((legalStatus, sapNumber, noIdentifier, customerType, idType, idNumber, isThereMoreThanTenDirectors, contactDetails) => PensionSchemeAdministrator(
    customerType = customerType,
    legalStatus = legalStatus,
    sapNumber = sapNumber,
    noIdentifier = noIdentifier,
    idType = idType,
    idNumber = idNumber,
    numberOfDirectorOrPartners = isThereMoreThanTenDirectors.map(c=>NumberOfDirectorOrPartnersType(isMorethanTenDirectors = Some(c))),
    pensionSchemeAdministratoridentifierStatus = PensionSchemeAdministratorIdentifierStatusType(isExistingPensionSchemaAdministrator = false),
    correspondenceAddressDetail = UkAddressType(addressType = "", line1 = "", line2 = "", countryCode = "", postalCode = ""),
    correspondenceContactDetail = contactDetails,
    previousAddressDetail = PreviousAddressDetails(isPreviousAddressLast12Month = false)
  ))
}
