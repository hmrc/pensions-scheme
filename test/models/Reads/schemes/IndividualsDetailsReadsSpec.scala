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

import models.Reads.behaviours.ContactDetailsReadsBehaviours
import models.schemes._
import models.{ContactDetails, CorrespondenceAddress}
import play.api.libs.json._


class IndividualsDetailsReadsSpec extends ContactDetailsReadsBehaviours {

  import IndividualsDetailsReadsSpec._

  behave like commonContactDetails(fullContactDetails)

  "A JSON payload containing personal details" should {

    "read into a valid personal details object" when {

      var result = personalDetails.as(PersonalDetails.apiReads)

      "we have a firstName" in {
        result.name.firstName mustBe (personalDetails \ "firstName").as[String]
      }

      "we have a middleName" in {
        result.name.middleName.value mustBe (personalDetails \ "middleName").as[String]
      }

      "we don't have a middleName" in {
        val inputWithoutMiddleName = personalDetails - "middleName"

        inputWithoutMiddleName.as(PersonalDetails.apiReads).name.middleName mustBe None
      }

      "we have a lastName" in {
        result.name.lastName mustBe (personalDetails \ "lastName").as[String]
      }

      "we have a dateOfBirth" in {
        result.dateOfBirth mustBe (personalDetails \ "dateOfBirth").as[String]
      }

    }
  }

  "A JSON payload containing individuals details" should {

    "read into a valid individuals details object" when {

      val result = individualDetails.as(IndividualDetails.apiReads)

      "we have a personalDetails" in {
        result.personalDetails mustBe (individualDetails \ "personDetails").as(PersonalDetails.apiReads)
      }

      "we have a nino" in {
        result.nino.value mustBe (individualDetails \ "nino").as[String]
      }

      "we don't have a nino" in {
        val inputWithoutNino = individualDetails - "nino"

        inputWithoutNino.as(IndividualDetails.apiReads).nino mustBe None
      }

      "we have a utr" in {
        result.utr.value mustBe (individualDetails \ "utr").as[String]
      }

      "we don't have a utr" in {
        val inputWithoutUTR = individualDetails - "utr"

        inputWithoutUTR.as(IndividualDetails.apiReads).utr mustBe None
      }

      "we have a correspondenceAddressDetails" in {
        result.address mustBe (individualDetails \ "correspondenceAddressDetails").as(AddressDetails.apiReads)
      }

      "we have a correspondenceContactDetails" in {
        result.contact mustBe (individualDetails \ "correspondenceContactDetails").as(ContactDetails.apiReads)
      }

      "we have a previousAddressDetails" in {
        result.previousAddress mustBe (individualDetails \ "previousAddressDetails").as(PreviousAddressDetails.apiReads)
      }

    }
  }
}


object IndividualsDetailsReadsSpec {

  val personalDetails = Json.obj("firstName" -> "abcdef", "middleName" -> "fdgdgfggfdg", "lastName" -> "dfgfdgdfg", "dateOfBirth" -> "1955-03-29")

  val addressDetails = Json.obj("nonUKAddress" -> JsBoolean(false), "line1" -> "line1", "line2" -> "line2", "line3" -> JsString("line3"), "line4" -> JsString("line4"),
    "postalCode" -> JsString("NE1"), "countryCode" -> JsString("GB"))

  val fullContactDetails = Json.obj("phone" -> "0758237281", "email" -> "test@test.com", "mobileNumber" -> "4564564664", "fax" -> "4654654313")

  val previousAddressDetails = Json.obj("isPreviousAddressLast12Month" -> JsBoolean(true), "previousAddress" -> addressDetails)

  val individualDetails = Json.obj("personDetails" -> personalDetails, "nino" -> "AA999999A", "utr" -> "1234567892",
    "correspondenceAddressDetails" -> addressDetails, "correspondenceContactDetails" -> fullContactDetails, "previousAddressDetails" -> previousAddressDetails)
}
