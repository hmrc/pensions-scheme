/*
 * Copyright 2019 HM Revenue & Customs
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

import models._
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._

class ReadsTrusteeDetailsSpec extends WordSpec with MustMatchers with OptionValues {

  import ReadsTrusteeDetailsSpec._

  "ReadsTrusteeDetails" must {

    "read one trustee individual details" when {
      val result = trusteeInputJson(Seq(trusteeIndividualJson)).as[TrusteeDetails](ReadsEstablisherDetails.readsTrusteeDetails).individualTrusteeDetail.head

      "we have valid person details" in {
        result.personalDetails mustEqual trusteeIndividualData.personalDetails
      }

      "we have valid nino" in {
        result.referenceOrNino mustEqual trusteeIndividualData.referenceOrNino
      }

      "we don't have nino but a valid no nino reason" in {
        val inputJson = trusteeInputJson(Seq(trusteeIndividualJson + ("trusteeNino" ->
          Json.obj("hasNino" -> JsBoolean(false), "reason" -> "No Nino"))))
        val result = inputJson.as[TrusteeDetails](ReadsEstablisherDetails.readsTrusteeDetails).individualTrusteeDetail.head
        result.referenceOrNino mustBe None
        result.noNinoReason.value mustEqual "No Nino"
      }

      "we have valid utr" in {
        result.utr mustEqual trusteeIndividualData.utr
      }

      "we don't have utr but a valid no utr reason" in {
        val inputJson = trusteeInputJson(Seq(trusteeIndividualJson + ("uniqueTaxReference" ->
          Json.obj("hasUtr" -> JsBoolean(false), "reason" -> "No Utr"))))
        val result = inputJson.as[TrusteeDetails](ReadsEstablisherDetails.readsTrusteeDetails).individualTrusteeDetail.head
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
        val result = inputJson.as[TrusteeDetails](ReadsEstablisherDetails.readsTrusteeDetails).individualTrusteeDetail.head
        result.previousAddressDetails mustBe None
      }

      "we have valid contact details" in {
        result.correspondenceContactDetails mustEqual trusteeIndividualData.correspondenceContactDetails
      }
    }

    "read one trustee partnership details" when {
      val result = trusteeInputJson().as[TrusteeDetails](ReadsEstablisherDetails.readsTrusteeDetails).partnershipTrusteeDetail.head

      "we have valid organisation name" in {
        result.organizationName mustEqual trusteePartnershipData.organizationName
      }

      "we have valid Vat number" in {
        val updatedJson = trusteeInputJson(Seq(trusteePartnershipJson + ("partnershipVat" -> Json.obj("hasVat" -> JsBoolean(true), "vat" -> "123456789"))))
        val result = updatedJson.as[TrusteeDetails](ReadsEstablisherDetails.readsTrusteeDetails).partnershipTrusteeDetail.head
        result.vatRegistrationNumber mustEqual trusteePartnershipData.copy(vatRegistrationNumber = Some("123456789")).vatRegistrationNumber
      }

      "we don't have vat number" in {
        result.vatRegistrationNumber mustBe None
      }

      "we have valid paye number" in {
        val updatedJson = trusteeInputJson(Seq(trusteePartnershipJson + ("partnershipPaye" ->
          Json.obj("hasPaye" -> JsBoolean(true), "paye" -> "123AB56789"))))
        val result = updatedJson.as[TrusteeDetails](ReadsEstablisherDetails.readsTrusteeDetails).partnershipTrusteeDetail.head
        result.payeReference mustEqual trusteePartnershipData.copy(payeReference = Some("123AB56789")).payeReference
      }

      "we don't have paye number" in {
        result.payeReference mustBe None
      }

      "we have valid utr" in {
        result.utr mustEqual trusteePartnershipData.utr
      }

      "we don't have utr but a valid no utr reason" in {
        val inputJson = trusteeInputJson(Seq(trusteePartnershipJson + ("partnershipUniqueTaxReference" ->
          Json.obj("hasUtr" -> JsBoolean(false), "reason" -> "No Utr"))))
        val result = inputJson.as[TrusteeDetails](ReadsEstablisherDetails.readsTrusteeDetails).partnershipTrusteeDetail.head
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
        val result = inputJson.as[TrusteeDetails](ReadsEstablisherDetails.readsTrusteeDetails).partnershipTrusteeDetail.head
        result.previousAddressDetails mustBe None
      }

      "we have valid contact details" in {
        result.correspondenceContactDetails mustEqual trusteePartnershipData.correspondenceContactDetails
      }
    }

    "read one trustee company details" when {
      val result = trusteeInputJson(Seq(trusteeCompanyJson)).as[TrusteeDetails](ReadsEstablisherDetails.readsTrusteeDetails).companyTrusteeDetail.head

      "we have valid organisation name" in {
        result.organizationName mustEqual trusteeCompanyData.organizationName
      }

      "we have valid Vat number" in {
        val updatedJson = trusteeInputJson(Seq(trusteeCompanyJson + ("companyDetails" -> Json.obj("companyName" -> "test company", "vatNumber" -> "Vat12345"))))
        val result = updatedJson.as[TrusteeDetails](ReadsEstablisherDetails.readsTrusteeDetails).companyTrusteeDetail.head
        result.vatRegistrationNumber mustEqual trusteeCompanyData.copy(vatRegistrationNumber = Some("Vat12345")).vatRegistrationNumber
      }

      "we don't have vat number" in {
        result.vatRegistrationNumber mustBe None
      }

      "we have valid paye number" in {
        val updatedJson = trusteeInputJson(Seq(trusteeCompanyJson +
          ("companyDetails" -> Json.obj("companyName" -> "test company", "payeNumber" -> "Paye12345"))))
        val result = updatedJson.as[TrusteeDetails](ReadsEstablisherDetails.readsTrusteeDetails).companyTrusteeDetail.head
        result.payeReference mustEqual trusteeCompanyData.copy(payeReference = Some("Paye12345")).payeReference
      }

      "we don't have paye number" in {
        result.payeReference mustBe None
      }

      "we have valid utr" in {
        result.utr mustEqual trusteeCompanyData.utr
      }

      "we don't have utr but a valid no utr reason" in {
        val inputJson = trusteeInputJson(Seq(trusteeCompanyJson + ("companyUniqueTaxReference" ->
          Json.obj("hasUtr" -> JsBoolean(false), "reason" -> "No Utr"))))
        val result = inputJson.as[TrusteeDetails](ReadsEstablisherDetails.readsTrusteeDetails).companyTrusteeDetail.head
        result.utr mustBe None
        result.noUtrReason.value mustEqual "No Utr"
      }

      "we have valid crn" in {
        result.crnNumber mustEqual trusteeCompanyData.crnNumber
      }

      "we don't have crn but a valid no crn reason" in {
        val inputJson = trusteeInputJson(Seq(trusteeCompanyJson + ("companyRegistrationNumber" ->
          Json.obj("hasCrn" -> JsBoolean(false), "reason" -> "No Crn"))))
        val result = inputJson.as[TrusteeDetails](ReadsEstablisherDetails.readsTrusteeDetails).companyTrusteeDetail.head
        result.crnNumber mustBe None
        result.noCrnReason.value mustEqual "No Crn"
      }

      "we have valid UK address" in {
        result.correspondenceAddressDetails mustEqual trusteeCompanyData.correspondenceAddressDetails
      }

      "we have address years less than 12 months with previous address" in {
        result.previousAddressDetails mustEqual trusteeCompanyData.previousAddressDetails
      }

      "we have address years more than 12 months without UK previous address" in {
        val inputJson = trusteeInputJson(Seq(trusteeCompanyJson + ("trusteesCompanyAddressYears" -> JsString("over_a_year"))))
        val result = inputJson.as[TrusteeDetails](ReadsEstablisherDetails.readsTrusteeDetails).companyTrusteeDetail.head
        result.previousAddressDetails mustBe None
      }

      "we have valid contact details" in {
        result.correspondenceContactDetails mustEqual trusteeCompanyData.correspondenceContactDetails
      }
    }

    "read multiple trustees " when {

      "we have two trustee partnerships" in {
        val inputJson = trusteeInputJson(Seq(trusteePartnershipJson, trusteePartnershipJson ++ Json.obj("partnershipDetails" -> Json.obj(
          "name" -> "test partnership two", "isDeleted" -> JsBoolean(false)))))
        val result = inputJson.as[TrusteeDetails](ReadsEstablisherDetails.readsTrusteeDetails).partnershipTrusteeDetail
        result mustEqual Seq(trusteePartnershipData, trusteePartnershipData.copy(organizationName = "test partnership two"))
      }

      "we have two trustee partnerships one of them is deleted" in {
        val inputJson = trusteeInputJson(Seq(trusteePartnershipJson, trusteePartnershipJson ++ Json.obj("partnershipDetails" -> Json.obj(
          "name" -> "test partnership two", "isDeleted" -> JsBoolean(true)))))
        val result = inputJson.as[TrusteeDetails](ReadsEstablisherDetails.readsTrusteeDetails).partnershipTrusteeDetail
        result mustEqual Seq(trusteePartnershipData)
      }

      "we have two trustee individuals" in {
        val inputJson = trusteeInputJson(Seq(trusteeIndividualJson, trusteeIndividualJson ++ Json.obj("trusteeDetails" -> Json.obj(
          "firstName" -> "second",
          "lastName" -> "trustee",
          "date" -> JsString("2019-01-31"), "isDeleted" -> JsBoolean(false)))))
        val result = inputJson.as[TrusteeDetails](ReadsEstablisherDetails.readsTrusteeDetails).individualTrusteeDetail
        result mustEqual Seq(trusteeIndividualData, trusteeIndividualData.copy(
          personalDetails = PersonalDetails(None, "second", None, "trustee", "2019-01-31")))
      }

      "we have two trustee individuals one of them is deleted" in {
        val inputJson = trusteeInputJson(Seq(trusteeIndividualJson, trusteeIndividualJson ++ Json.obj("trusteeDetails" -> Json.obj(
          "firstName" -> "second",
          "lastName" -> "trustee",
          "date" -> JsString("2019-01-31"), "isDeleted" -> JsBoolean(true)))))
        val result = inputJson.as[TrusteeDetails](ReadsEstablisherDetails.readsTrusteeDetails).individualTrusteeDetail
        result mustEqual Seq(trusteeIndividualData)
      }

      "we have two trustee companies" in {
        val inputJson = trusteeInputJson(Seq(trusteeCompanyJson, trusteeCompanyJson ++ Json.obj("companyDetails" -> Json.obj(
          "companyName" -> "test company two", "isDeleted" -> JsBoolean(false)))))
        val result = inputJson.as[TrusteeDetails](ReadsEstablisherDetails.readsTrusteeDetails).companyTrusteeDetail
        result mustEqual Seq(trusteeCompanyData, trusteeCompanyData.copy(organizationName = "test company two"))
      }

      "we have two trustee companies one of them is deleted" in {
        val inputJson = trusteeInputJson(Seq(trusteeCompanyJson, trusteeCompanyJson ++ Json.obj("companyDetails" -> Json.obj(
          "companyName" -> "test company two", "isDeleted" -> JsBoolean(true)))))
        val result = inputJson.as[TrusteeDetails](ReadsEstablisherDetails.readsTrusteeDetails).companyTrusteeDetail
        result mustEqual Seq(trusteeCompanyData)
      }

      "we have one trustee individual, one company and one partnership" in {
        val inputJson = trusteeInputJson(Seq(trusteePartnershipJson, trusteeIndividualJson, trusteeCompanyJson))
        val result = inputJson.as[TrusteeDetails](ReadsEstablisherDetails.readsTrusteeDetails)
        result mustEqual TrusteeDetails(Seq(trusteeIndividualData), Seq(trusteeCompanyData), Seq(trusteePartnershipData))
      }

      "we have one trustee individual, one company and one partnership but partnership is deleted" in {
        val inputJson = trusteeInputJson(Seq(trusteePartnershipJson ++ Json.obj("partnershipDetails" -> Json.obj(
          "name" -> "test partnership two", "isDeleted" -> JsBoolean(true))), trusteeIndividualJson, trusteeCompanyJson))
        val result = inputJson.as[TrusteeDetails](ReadsEstablisherDetails.readsTrusteeDetails)
        result mustEqual TrusteeDetails(Seq(trusteeIndividualData), Seq(trusteeCompanyData), Nil)
      }
    }
  }
}

object ReadsTrusteeDetailsSpec extends Samples {

  private def trusteeInputJson(trusteesJson: Seq[JsObject] = Seq(trusteePartnershipJson)) = Json.obj(
    "trustees" -> trusteesJson.foldLeft(JsArray())((acc, x) => acc ++ Json.arr(x))
  )

  private val trusteePartnershipJson = Json.obj(
    "partnershipDetails" -> Json.obj(
      "name" -> "test partnership",
      "isDeleted" -> JsBoolean(false)
    ),
    "partnershipVat" -> Json.obj(
      "hasVat" -> JsBoolean(false)
    ),
    "partnershipPaye" -> Json.obj(
      "hasPaye" -> JsBoolean(false)
    ),
    "partnershipUniqueTaxReference" -> Json.obj(
      "hasUtr" -> JsBoolean(true),
      "utr" -> "1111111111"
    ),
    "partnershipAddress" -> Json.obj(
      "addressLine1" -> "line1",
      "addressLine2" -> "line2",
      "addressLine3" -> "line3",
      "addressLine4" -> "line4",
      "postcode" -> "NE1",
      "country" -> "GB"
    ),
    "partnershipAddressYears" -> "under_a_year",
    "partnershipPreviousAddress" -> Json.obj(
      "addressLine1" -> "line1",
      "addressLine2" -> "line2",
      "addressLine3" -> "line3",
      "addressLine4" -> "line4",
      "postcode" -> "NE1",
      "country" -> "GB"
    ),
    "partnershipContactDetails" -> Json.obj(
      "phoneNumber" -> "07592113",
      "emailAddress" -> "test@test.com"
    )
  )

  private val trusteeCompanyJson = Json.obj(
    "companyDetails" -> Json.obj(
      "companyName" -> "test company",
      "isDeleted" -> JsBoolean(false)
    ),
    "companyUniqueTaxReference" -> Json.obj(
      "hasUtr" -> JsBoolean(true),
      "utr" -> "1111111111"
    ),
    "companyRegistrationNumber" -> Json.obj(
      "hasCrn" -> JsBoolean(true),
      "crn" -> "crn1234"
    ),
    "companyAddress" -> Json.obj(
      "addressLine1" -> "line1",
      "addressLine2" -> "line2",
      "addressLine3" -> "line3",
      "addressLine4" -> "line4",
      "postcode" -> "NE1",
      "country" -> "GB"
    ),
    "trusteesCompanyAddressYears" -> "under_a_year",
    "companyPreviousAddress" -> Json.obj(
      "addressLine1" -> "line1",
      "addressLine2" -> "line2",
      "addressLine3" -> "line3",
      "addressLine4" -> "line4",
      "postcode" -> "NE1",
      "country" -> "GB"
    ),
    "companyContactDetails" -> Json.obj(
      "phoneNumber" -> "07592113",
      "emailAddress" -> "test@test.com"
    )
  )

  private val trusteeIndividualJson = Json.obj(
    "trusteeDetails" -> Json.obj(
      "firstName" -> "John",
      "middleName" -> "William",
      "lastName" -> "Doe",
      "date" -> JsString("2019-01-31"),
      "isDeleted" -> JsBoolean(false)
    ),
    "uniqueTaxReference" -> Json.obj(
      "hasUtr" -> JsBoolean(true),
      "utr" -> "1111111111"
    ),
    "trusteeNino" -> Json.obj(
      "hasNino" -> JsBoolean(true),
      "nino" -> "nino1234"
    ),
    "trusteeAddressId" -> Json.obj(
      "addressLine1" -> "line1",
      "addressLine2" -> "line2",
      "addressLine3" -> "line3",
      "addressLine4" -> "line4",
      "postcode" -> "NE1",
      "country" -> "GB"
    ),
    "trusteeAddressYears" -> "under_a_year",
    "trusteePreviousAddress" -> Json.obj(
      "addressLine1" -> "line1",
      "addressLine2" -> "line2",
      "addressLine3" -> "line3",
      "addressLine4" -> "line4",
      "postcode" -> "NE1",
      "country" -> "GB"
    ),
    "trusteeContactDetails" -> Json.obj(
      "phoneNumber" -> "07592113",
      "emailAddress" -> "test@test.com"
    )
  )
}
