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

package models.Reads.schemes

import models.Reads.{CommonAddressTest, CommonContactDetailsReads}
import models.schemes._
import models.{ContactDetails, CorrespondenceAddress}
import play.api.libs.json._


class CommonDetailsReadsSpec extends CommonAddressTest with CommonContactDetailsReads{
  import CommonDetailsReadsSpec._

  behave like commonContactDetails(fullContactDetails)

  behave like correspondenceAddressDetails(addressDetails)

  "A JSON payload containing personal details" should {

    "read into a valid personal details object" when {

      var result = personalDetails.as[PersonalDetails](PersonalDetails.apiReads)

      "we have a firstName" in {
        result.name.value.firstName.value mustBe (personalDetails \ "firstName").as[String]
      }

      "we don't have a firstName" in {
        val inputWithoutFirstName = personalDetails - "firstName"

        inputWithoutFirstName.as[PersonalDetails](PersonalDetails.apiReads).name.value.firstName mustBe None
      }

      "we have a middleName" in {
        result.name.value.middleName.value mustBe (personalDetails \ "middleName").as[String]
      }

      "we don't have a middleName" in {
        val inputWithoutMiddleName = personalDetails - "middleName"

        inputWithoutMiddleName.as[PersonalDetails](PersonalDetails.apiReads).name.value.middleName mustBe None
      }

      "we have a lastName" in {
        result.name.value.lastName.value mustBe (personalDetails \ "lastName").as[String]
      }

      "we don't have a lastName" in {
        val inputWithoutLastName = personalDetails - "lastName"

        inputWithoutLastName.as[PersonalDetails](PersonalDetails.apiReads).name.value.lastName mustBe None
      }

      "we have a dateOfBirth" in {
        result.dateOfBirth.value mustBe (personalDetails \ "dateOfBirth").as[String]
      }

      "we don't have a dateOfBirth" in {
        val inputWithoutDateOfBirth = personalDetails - "dateOfBirth"

        inputWithoutDateOfBirth.as[PersonalDetails].dateOfBirth mustBe None
      }

      "we don't have an firstName, middleName, lastName" in {
        val inputWithoutPersonDetails = Json.obj()

        inputWithoutPersonDetails.as[PersonalDetails].name mustBe None
      }
    }
  }

  "A Json payload containing previous address details" should {

    "read into a valid previous address details object" when {

      "we have a isPreviousAddressLast12Month" in {
        previousAddressDetails.as[PreviousAddressDetails].isPreviousAddressLast12Month mustBe (previousAddressDetails \ "isPreviousAddressLast12Month").as[Boolean]
      }

      "we have a previousAddress" in {
        previousAddressDetails.as[PreviousAddressDetails].previousAddress.value mustBe (previousAddressDetails \ "previousAddress").as[CorrespondenceAddress]
      }

      "we don't have a previousAddress" in {
        val inputWithoutPreviousAddress = previousAddressDetails - "previousAddress"

        inputWithoutPreviousAddress.as[PreviousAddressDetails].previousAddress mustBe None
      }
    }
  }

  "A JSON payload containing individuals details" should {

    "read into a valid individuals details object" when {

      val result = individualDetails.as[IndividualDetails](IndividualDetails.apiReads)

      "we have a personalDetails" in {
        result.personalDetails.value mustBe (individualDetails \ "personDetails").as[PersonalDetails](PersonalDetails.apiReads)
      }

      "we don't have a personalDetails" in {
        val inputWithoutPersonalDetails = individualDetails - "personDetails"

        inputWithoutPersonalDetails.as[IndividualDetails].personalDetails mustBe None
      }

      "we have a nino" in {
        result.nino.value mustBe (individualDetails \ "nino").as[String]
      }

      "we don't have a nino" in {
        val inputWithoutNino = individualDetails - "nino"

        inputWithoutNino.as[IndividualDetails].nino mustBe None
      }

      "we have a utr" in {
        result.utr.value mustBe (individualDetails \ "utr").as[String]
      }

      "we don't have a utr" in {
        val inputWithoutUTR = individualDetails - "utr"

        inputWithoutUTR.as[IndividualDetails].utr mustBe None
      }

      "we have a correspondenceAddressDetails" in {
        result.address.value mustBe (individualDetails \ "correspondenceAddressDetails").as[CorrespondenceAddress]
      }

      "we don't have a correspondenceAddressDetails" in {
        val inputWithoutCorrespondenceAddressDetails = individualDetails - "correspondenceAddressDetails"

        inputWithoutCorrespondenceAddressDetails.as[IndividualDetails].address mustBe None
      }

      "we have a correspondenceContactDetails" in {
        result.contact.value mustBe (individualDetails \ "correspondenceContactDetails").as[ContactDetails](ContactDetails.apiReads)
      }

      "we don't have a correspondenceContactDetails" in {
        val inputWithoutCorrespondenceContactDetails = individualDetails - "correspondenceContactDetails"

        inputWithoutCorrespondenceContactDetails.as[IndividualDetails].contact mustBe None
      }

      "we have a previousAddressDetails" in {
        result.previousAddress.value mustBe (individualDetails \ "previousAddressDetails").as[PreviousAddressDetails]
      }

      "we don't have a previousAddressDetails" in {
        val inputWithoutPreviousAddressDetails = individualDetails - "previousAddressDetails"

        inputWithoutPreviousAddressDetails.as[IndividualDetails].previousAddress mustBe None
      }
    }
  }


}


object CommonDetailsReadsSpec {

  val personalDetails = Json.obj("firstName" -> "abcdef", "middleName" -> "fdgdgfggfdg", "lastName" -> "dfgfdgdfg", "dateOfBirth" -> "1955-03-29")

  val addressDetails = Json.obj("line1" -> JsString("line1"), "line2" -> JsString("line2"), "line3" -> JsString("line3"), "line4" -> JsString("line4"),
    "postalCode" -> JsString("NE1"), "countryCode" -> JsString("GB"))

  val fullContactDetails = Json.obj("phone" -> "0758237281", "email" -> "test@test.com", "mobileNumber" -> "4564564664", "fax" -> "4654654313")

  val previousAddressDetails = Json.obj("isPreviousAddressLast12Month"->JsBoolean(true), "previousAddress"-> addressDetails)

  val individualDetails = Json.obj("personDetails" -> personalDetails, "nino"-> "AA999999A", "utr"-> "1234567892",
    "correspondenceAddressDetails"-> addressDetails, "correspondenceContactDetails" -> fullContactDetails, "previousAddressDetails" -> previousAddressDetails)

}