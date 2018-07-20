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
import play.api.libs.json.{Json, _}

class PensionSchemeAdministratorPartnershipReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples {

  implicit val contactAddressEnabled: Boolean = true
  import PensionSchemeAdministratorPartnershipReadsSpec._

  "Json Payload of a PSA for partnership" must {
/*    "read correct registration info" when {
      "we have valid legal status" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value
        result.legalStatus mustEqual pensionSchemeAdministratorSample.copy(legalStatus = "Partnership").legalStatus
      }

      "We have a valid sapNumber" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value
        result.sapNumber mustEqual pensionSchemeAdministratorSample.sapNumber
      }

      "We have a valid noIdentifier" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value
        result.noIdentifier mustEqual pensionSchemeAdministratorSample.noIdentifier
      }

      "We have valid customerType" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value
        result.customerType mustEqual pensionSchemeAdministratorSample.customerType
      }

      "We have a valid idType" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value
        result.idType mustEqual pensionSchemeAdministratorSample.idType
      }

      "We have a valid idNumber" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value
        result.idNumber mustEqual pensionSchemeAdministratorSample.idNumber
      }
    }

    "read correct moreThanTenPartners" when {

      "We have a moreThanTenPartners flag" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input +
          ("moreThanTenPartners" -> JsBoolean(true)))(PensionSchemeAdministrator.apiReads).asOpt.value

        result.numberOfDirectorOrPartners.value.isMorethanTenPartners mustEqual
          pensionSchemeAdministratorSample.numberOfDirectorOrPartners.value.isMorethanTenPartners
      }

      "We don't have moreThanTenPartners flag" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.numberOfDirectorOrPartners mustBe None
      }
    }

    "read correct contact details" when {

      "we have telephone" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.correspondenceContactDetail.telephone mustBe pensionSchemeAdministratorSample.correspondenceContactDetail.telephone
      }

      "we have email" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.correspondenceContactDetail.email mustBe pensionSchemeAdministratorSample.correspondenceContactDetail.email
      }
    }

    "read correct previous address details" when {

      "we have address years last 12 months flag" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.previousAddressDetail.isPreviousAddressLast12Month mustBe pensionSchemeAdministratorSample.previousAddressDetail.isPreviousAddressLast12Month
      }

      "we have previous address" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.previousAddressDetail.previousAddressDetails mustBe pensionSchemeAdministratorSample.previousAddressDetail.previousAddressDetails
      }
    }

    "read correct contact address" when {

      "we have contact address" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.correspondenceAddressDetail mustBe ukAddressSample
      }
    }*/

    "read correct partners" when {

      "we have a partner" in {
        val partners = JsArray(Seq(testDirectorOrPartner("partner"), testDirectorOrPartner("partner")))
        val pensionSchemeAdministrator = input + ("partners" -> partners)
        val result = Json.fromJson[PensionSchemeAdministrator](pensionSchemeAdministrator)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.directorOrPartnerDetail.value.head.sequenceId mustBe directorOrPartnerSample("partner").sequenceId
      }

      /*"We have two partners one of which is deleted" in {
        val deletedDirector = testDirectorOrPartner("partner") ++ Json.obj("partnerDetails" -> Json.obj("firstName" -> JsString("Joe"),
          "lastName" -> JsString("Bloggs"),
          "dateOfBirth" -> JsString("2019-01-31"),
          "isDeleted" -> JsBoolean(true)))

        val partners = JsArray(Seq(testDirectorOrPartner("partner"), deletedDirector))
        val pensionSchemeAdministrator = input + ("partners" -> partners)
        val result = Json.fromJson[PensionSchemeAdministrator](pensionSchemeAdministrator)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.directorOrPartnerDetail.value.size mustEqual 1
        result.directorOrPartnerDetail.value.head.lastName mustEqual "Doe"
      }*/
    }

/*    "read correct partnership details" when {

      "We have organisation details but no individual details" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.individualDetail mustBe None
        result.organisationDetail mustBe (defined)
      }
    }

    "read correct psa identifier" when {

      "The user is not an existing PSA user" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.pensionSchemeAdministratoridentifierStatus.isExistingPensionSchemaAdministrator mustBe pensionSchemeAdministratorSample.pensionSchemeAdministratoridentifierStatus.isExistingPensionSchemaAdministrator
      }

      "The user is an existing PSA user with no previous reference" in {
        val existingPSA = "existingPSA" -> Json.obj("isExistingPSA" -> JsBoolean(true))
        val result = Json.fromJson[PensionSchemeAdministrator](input + existingPSA)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.pensionSchemeAdministratoridentifierStatus.isExistingPensionSchemaAdministrator mustBe true
      }

      "The user is an existing PSA user with previous reference number" in {
        val existingPSA = "existingPSA" -> Json.obj("isExistingPSA" -> JsBoolean(true),"existingPSAId" -> JsString("TestId"))
        val result = Json.fromJson[PensionSchemeAdministrator](input + existingPSA)(PensionSchemeAdministrator.apiReads).asOpt.value

        result.pensionSchemeAdministratoridentifierStatus.existingPensionSchemaAdministratorReference mustBe Some("TestId")
      }
    }

    "We have a declaration" in {
      val result = Json.fromJson[PensionSchemeAdministrator](input)(PensionSchemeAdministrator.apiReads).asOpt.value

      result.declaration mustBe pensionSchemeAdministratorSample.declaration
    }*/
  }
}

object PensionSchemeAdministratorPartnershipReadsSpec {
  val input = Json.obj(
    "existingPSA" -> Json.obj("isExistingPSA" -> JsBoolean(false)),
    "registrationInfo" ->
      Json.obj("legalStatus" -> "Partnership",
        "sapNumber" -> "NumberTest",
        "noIdentifier" -> JsBoolean(true),
        "customerType" -> "TestCustomer",
        "idType" -> JsString("TestId"),
        "idNumber" -> JsString("TestIdNumber")),
    "partnershipContactDetails" -> Json.obj("phone" -> "07592113", "email" -> "test@test.com"),
    "partnershipAddressYears" -> JsString("over_a_year"),
    "partnershipContactAddress" -> JsObject(Map("addressLine1" -> JsString("line1"), "addressLine2" -> JsString("line2"), "addressLine3" -> JsString("line3"),
      "addressLine4" -> JsString("line4"), "postalCode" -> JsString("NE1"), "countryCode" -> JsString("GB"))),
    "partnershipVat" -> Json.obj(
      "hasVat" -> JsBoolean(true),
      "vat" -> "1234567"
    ),
    "partnershipPaye" -> Json.obj(
      "hasPaye" -> JsBoolean(true),
      "paye" -> "1234567"
    ),
    "partnershipDetails" -> Json.obj("companyName" -> JsString("Company Test"), "uniqueTaxReferenceNumber" -> "1234567891"),
    "declaration" -> JsBoolean(true),
    "declarationFitAndProper" -> JsBoolean(true),
    "declarationWorkingKnowledge" -> "workingKnowledge")
}
