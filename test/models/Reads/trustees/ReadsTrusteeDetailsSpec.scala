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

package models.Reads.trustees

import models.Reads.establishers.EstablishersTestJson.{ninoJson, utrJson}
import models._
import models.userAnswersToEtmp.ReadsTrustees
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._

class ReadsTrusteeDetailsSpec extends WordSpec with MustMatchers with OptionValues {

  import ReadsTrusteeDetailsSpec._

  "ReadsTrusteeDetails" must {

    "read one trustee individual details" when {
      val result = trusteeInputJson(Seq(trusteeIndividualJson)).as[TrusteeDetails](ReadsTrustees.readsTrusteeDetails).individualTrusteeDetail.head

      "we have valid person details" in {
        result.personalDetails mustEqual trusteeIndividualData.personalDetails
      }

      "we have valid nino" in {
        result.referenceOrNino mustEqual trusteeIndividualData.referenceOrNino
      }

      "we don't have nino but a valid no nino reason" in {
        val inputJson = trusteeInputJson(Seq(trusteeIndividualJson - "trusteeNino" + ("noNinoReason" -> JsString("No Nino"))))
        val result = inputJson.as[TrusteeDetails](ReadsTrustees.readsTrusteeDetails).individualTrusteeDetail.head
        result.referenceOrNino mustBe None
        result.noNinoReason.value mustEqual "No Nino"
      }

      "we have valid utr" in {
        result.utr mustEqual trusteeIndividualData.utr
      }

      "we don't have utr but a valid no utr reason" in {
        val inputJson = trusteeInputJson(Seq(trusteeIndividualJson - "utr" + ("noUtrReason" -> JsString("No Utr"))))
        val result = inputJson.as[TrusteeDetails](ReadsTrustees.readsTrusteeDetails).individualTrusteeDetail.head
        result.utr mustBe None
        result.noUtrReason.value mustEqual "No Utr"
      }

      "we have valid UK address" in {
        result.correspondenceAddressDetails mustEqual trusteeIndividualData.correspondenceAddressDetails
      }

      "we have address years less than 12 months with previous address" in {
        result.previousAddressDetails mustEqual trusteeIndividualData.previousAddressDetails
      }

      "we have address years more than 12 months without UK previous address" in {
        val inputJson = trusteeInputJson(Seq(trusteeIndividualJson + ("trusteeAddressYears" -> JsString("over_a_year"))))
        val result = inputJson.as[TrusteeDetails](ReadsTrustees.readsTrusteeDetails).individualTrusteeDetail.head
        result.previousAddressDetails mustBe None
      }

      "we have valid contact details" in {
        result.correspondenceContactDetails mustEqual trusteeIndividualData.correspondenceContactDetails
      }
    }

    "read one trustee partnership details" when {
      val result = trusteeInputJson().as[TrusteeDetails](ReadsTrustees.readsTrusteeDetails).partnershipTrusteeDetail.head

      "we have valid organisation name" in {
        result.organizationName mustEqual trusteePartnershipData.organizationName
      }

        "have valid Vat number" in {
          val updatedJson = trusteeInputJson(Seq(trusteePartnershipJson + ("partnershipVat" -> Json.obj("value" -> "Vat12345"))))
          val result = updatedJson.as[TrusteeDetails](ReadsTrustees.readsTrusteeDetails).partnershipTrusteeDetail.head
          result.vatRegistrationNumber mustEqual trusteePartnershipData.copy(vatRegistrationNumber = Some("Vat12345")).vatRegistrationNumber
        }

        "not have vat number" in {
          val updatedJson = trusteeInputJson(Seq(trusteePartnershipJson - "partnershipVat"))
          val result = updatedJson.as[TrusteeDetails](ReadsTrustees.readsTrusteeDetails).partnershipTrusteeDetail.head
          result.vatRegistrationNumber mustBe None
        }

        "have valid paye number" in {
          val updatedJson = trusteeInputJson(Seq(trusteePartnershipJson + ("partnershipPaye" ->
            Json.obj("value" -> "123AB56789"))))
          val result = updatedJson.as[TrusteeDetails](ReadsTrustees.readsTrusteeDetails).partnershipTrusteeDetail.head
          result.payeReference mustEqual trusteePartnershipData.copy(payeReference = Some("123AB56789")).payeReference
        }

        "not have paye number" in {
          val updatedJson = trusteeInputJson(Seq(trusteePartnershipJson - "partnershipPaye"))
          val result = updatedJson.as[TrusteeDetails](ReadsTrustees.readsTrusteeDetails).partnershipTrusteeDetail.head
          result.payeReference mustBe None
        }

      "we have valid utr" in {
        result.utr mustEqual trusteePartnershipData.utr
      }

      "we don't have utr but a valid no utr reason" in {
        val inputJson = trusteeInputJson(Seq(trusteePartnershipJson - "utr" + ("noUtrReason" -> JsString("No Utr"))))
        val result = inputJson.as[TrusteeDetails](ReadsTrustees.readsTrusteeDetails).partnershipTrusteeDetail.head
        result.utr mustBe None
        result.noUtrReason.value mustEqual "No Utr"
      }

      "we have valid UK address" in {
        result.correspondenceAddressDetails mustEqual trusteePartnershipData.correspondenceAddressDetails
      }

      "we have address years less than 12 months with previous address" in {
        result.previousAddressDetails mustEqual trusteePartnershipData.previousAddressDetails
      }

      "we have address years more than 12 months without UK previous address" in {
        val inputJson = trusteeInputJson(Seq(trusteePartnershipJson + ("partnershipAddressYears" -> JsString("over_a_year"))))
        val result = inputJson.as[TrusteeDetails](ReadsTrustees.readsTrusteeDetails).partnershipTrusteeDetail.head
        result.previousAddressDetails mustBe None
      }

      "we have valid contact details" in {
        result.correspondenceContactDetails mustEqual trusteePartnershipData.correspondenceContactDetails
      }
    }

    "read multiple trustees " when {

      "we have two trustee partnerships" in {
        val inputJson = trusteeInputJson(Seq(trusteePartnershipJson, trusteePartnershipJson ++ Json.obj("partnershipDetails" -> Json.obj(
          "name" -> "test partnership two", "isDeleted" -> JsBoolean(false)))))
        val result = inputJson.as[TrusteeDetails](ReadsTrustees.readsTrusteeDetails).partnershipTrusteeDetail
        result mustEqual Seq(trusteePartnershipData, trusteePartnershipData.copy(organizationName = "test partnership two"))
      }

      "we have two trustee partnerships one of them is deleted" in {
        val inputJson = trusteeInputJson(Seq(trusteePartnershipJson, trusteePartnershipJson ++ Json.obj("partnershipDetails" -> Json.obj(
          "name" -> "test partnership two", "isDeleted" -> JsBoolean(true)))))
        val result = inputJson.as[TrusteeDetails](ReadsTrustees.readsTrusteeDetails).partnershipTrusteeDetail
        result mustEqual Seq(trusteePartnershipData)
      }

      "we have two trustee individuals" in {
        val inputJson = trusteeInputJson(Seq(trusteeIndividualJson, trusteeIndividualJson ++ Json.obj("trusteeDetails" -> Json.obj(
          "firstName" -> "second",
          "lastName" -> "trustee",
          "date" -> JsString("2019-01-31"), "isDeleted" -> JsBoolean(false)))))
        val result = inputJson.as[TrusteeDetails](ReadsTrustees.readsTrusteeDetails).individualTrusteeDetail
        result mustEqual Seq(trusteeIndividualData, trusteeIndividualData.copy(
          personalDetails = PersonalDetails(None, "second", None, "trustee", "2019-01-31")))
      }

      "we have two trustee individuals one of them is deleted" in {
        val inputJson = trusteeInputJson(Seq(trusteeIndividualJson, trusteeIndividualJson ++ Json.obj("trusteeDetails" -> Json.obj(
          "firstName" -> "second",
          "lastName" -> "trustee",
          "date" -> JsString("2019-01-31"), "isDeleted" -> JsBoolean(true)))))
        val result = inputJson.as[TrusteeDetails](ReadsTrustees.readsTrusteeDetails).individualTrusteeDetail
        result mustEqual Seq(trusteeIndividualData)
      }

      "we have one trustee individual, one company and one partnership" in {
        val inputJson = trusteeInputJson(Seq(trusteePartnershipJson, trusteeIndividualJson, trusteeCompanyJson))
        val result = inputJson.as[TrusteeDetails](ReadsTrustees.readsTrusteeDetails)
        result mustEqual TrusteeDetails(Seq(trusteeIndividualData), Seq(trusteeCompanyData), Seq(trusteePartnershipData))
      }

      "we have one trustee individual, one company and one partnership but partnership is deleted" in {
        val inputJson = trusteeInputJson(Seq(trusteePartnershipJson ++ Json.obj("partnershipDetails" -> Json.obj(
          "name" -> "test partnership two", "isDeleted" -> JsBoolean(true))), trusteeIndividualJson, trusteeCompanyJson))
        val result = inputJson.as[TrusteeDetails](ReadsTrustees.readsTrusteeDetails)
        result mustEqual TrusteeDetails(Seq(trusteeIndividualData), Seq(trusteeCompanyData), Nil)
      }
    }
  }
}

object ReadsTrusteeDetailsSpec extends Samples {

  private def trusteeInputJson(trusteesJson: Seq[JsObject] = Seq(trusteePartnershipJson)) = Json.obj(
    "trustees" -> trusteesJson.foldLeft(JsArray())((acc, x) => acc ++ Json.arr(x))
  )

  private def trusteePartnershipJson = {
    val trusteeJson = Json.obj(
      "partnershipDetails" -> Json.obj(
        "name" -> "test partnership",
        "isDeleted" -> JsBoolean(false)
      ),
      "partnershipAddress" -> address,
      "partnershipAddressYears" -> "under_a_year",
      "partnershipPreviousAddress" -> address,
      "partnershipContactDetails" -> Json.obj(
        "phoneNumber" -> "07592113",
        "emailAddress" -> "test@test.com"
      )
    )
    trusteeJson ++
    Json.obj("hasUtr" -> JsBoolean(true), "utr" -> Json.obj("value" -> "1111111111"))
  }

  private def trusteeCompanyJson = {
   val trusteeJson = Json.obj(
      "companyDetails" -> Json.obj(
        "companyName" -> "test company",
        "isDeleted" -> JsBoolean(false)
      ),
      "companyAddress" -> address,
      "trusteesCompanyAddressYears" -> "under_a_year",
      "companyPreviousAddress" -> address,
      "companyContactDetails" -> Json.obj(
        "phoneNumber" -> "07592113",
        "emailAddress" -> "test@test.com"
      )
    )
    trusteeJson ++
        Json.obj("companyRegistrationNumber" -> Json.obj("value" -> "crn1234")) ++
        Json.obj("utr" -> Json.obj("value" -> "1111111111"))
  }

  private def trusteeIndividualJson = {
    val trusteeJson = Json.obj(
      "trusteeAddressId" -> Json.obj(
        "addressLine1" -> "line1",
        "addressLine2" -> "line2",
        "addressLine3" -> "line3",
        "addressLine4" -> "line4",
        "postcode" -> "NE1",
        "country" -> "GB"
      ),
      "trusteeAddressYears" -> "under_a_year",
      "trusteePreviousAddress" -> address,
      "trusteeContactDetails" -> Json.obj(
        "phoneNumber" -> "07592113",
        "emailAddress" -> "test@test.com"
      )
    )
    trusteeJson ++
      trusteeDetails ++
      utrJson(Some("1111111111"), None, "uniqueTaxReference") ++
      ninoJson(Some("nino1234"), None, "trusteeNino")
  }

  private def trusteeDetails =
      Json.obj("trusteeDetails" -> Json.obj(
        "firstName" -> "John",
        "lastName" -> "Doe",
        "isDeleted" -> JsBoolean(false)
      ),
        "dateOfBirth" -> JsString("2019-01-31"))

  val address: JsObject = Json.obj(
    "addressLine1" -> "line1",
    "addressLine2" -> "line2",
    "addressLine3" -> "line3",
    "addressLine4" -> "line4",
    "postcode" -> "NE1",
    "country" -> "GB"
  )
}
