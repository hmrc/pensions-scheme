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

import play.api.libs.json._

trait SchemeDetailsStubJsonData {

  val personalDetails : JsObject = Json.obj("firstName" -> "fName", "middleName" -> "mName",
    "lastName" -> "lName", "dateOfBirth" -> "1955-03-29")

  val addressDetails : JsObject = Json.obj("nonUKAddress" -> JsBoolean(false),
    "line1" -> "line1", "line2" -> "line2", "line3" -> JsString("line3"), "line4" -> JsString("line4"),
    "postalCode" -> JsString("NE1"), "countryCode" -> JsString("GB"))

  val fullContactDetails : JsObject = Json.obj("telephone" -> "07592113", "email" -> "test@test.com", "mobileNumber" -> "4564564664", "fax" -> "4654654313")

  val previousAddressDetails : JsObject = Json.obj("isPreviousAddressLast12Month" -> JsBoolean(true), "previousAddress" -> addressDetails)

  val individualDetails : JsObject = Json.obj("personDetails" -> personalDetails, "nino" -> "AA999999A", "utr" -> "1234567892",
    "correspondenceAddressDetails" -> addressDetails, "correspondenceContactDetails" -> fullContactDetails,
    "previousAddressDetails" -> previousAddressDetails)

  val companyOrOrganisationDetails : JsObject = Json.obj("organisationName" -> "abc organisation", "utr"-> "7897700000",
    "crnNumber"-> "AA999999A", "vatRegistrationNumber"-> "789770000", "payeReference" -> "9999",
    "correspondenceAddressDetails"-> addressDetails, "correspondenceContactDetails" -> fullContactDetails,
    "previousAddressDetails" -> previousAddressDetails, "directorsDetails" -> Json.arr(individualDetails))

  val establisherPartnershipDetails : JsObject = Json.obj("partnershipName" -> "abc partnership", "utr"-> "7897700000",
    "vatRegistrationNumber"-> "789770000", "payeReference" -> "9999", "correspondenceAddressDetails"-> addressDetails,
    "correspondenceContactDetails" -> fullContactDetails, "previousAddressDetails" -> previousAddressDetails,
    "partnerDetails" -> Json.arr(individualDetails))

  val establisherDetails : JsObject = Json.obj("individualDetails" -> Json.arr(individualDetails),
    "companyOrOrganisationDetails" -> Json.arr(companyOrOrganisationDetails),
    "partnershipTrusteeDetail" -> Json.arr(establisherPartnershipDetails))

  val establisherDetailsWithMultipleData : JsObject = Json.obj("individualDetails" -> Json.arr(individualDetails, individualDetails),
    "companyOrOrganisationDetails" -> Json.arr(companyOrOrganisationDetails, companyOrOrganisationDetails),
    "partnershipTrusteeDetail" -> Json.arr(establisherPartnershipDetails, establisherPartnershipDetails))

  val trusteePartnershipDetails : JsObject = Json.obj("partnershipName" -> "abc partnership", "utr"-> "7897700000",
    "vatRegistrationNumber"-> "789770000", "payeReference" -> "9999", "correspondenceAddressDetails"-> addressDetails,
    "correspondenceContactDetails" -> fullContactDetails, "previousAddressDetails" -> previousAddressDetails)

  val trusteeDetails : JsObject = Json.obj("individualTrusteeDetails" -> Json.arr(individualDetails),
    "companyTrusteeDetails" -> Json.arr(companyOrOrganisationDetails),
    "partnershipTrusteeDetails" -> Json.arr(trusteePartnershipDetails))

  val trusteeDetailsWithMultipleData : JsObject = Json.obj("individualTrusteeDetails" -> Json.arr(individualDetails, individualDetails),
    "companyTrusteeDetails" -> Json.arr(companyOrOrganisationDetails, companyOrOrganisationDetails),
    "partnershipTrusteeDetails" -> Json.arr(trusteePartnershipDetails, trusteePartnershipDetails))

  val schemeDetails : JsObject = Json.obj("srn" -> JsString("AAABA932JASDA"),
    "pstr" -> JsString("A3DCADAA"),
    "schemeStatus" -> "Pending",
    "schemeName" -> "Test Scheme",
    "isSchemeMasterTrust" -> JsBoolean(true),
    "pensionSchemeStructure" -> "Other",
    "otherPensionSchemeStructure" -> "Other type",
    "hasMoreThanTenTrustees" -> JsBoolean(true),
    "currentSchemeMembers" -> "1",
    "futureSchemeMembers" -> "2",
    "isReguledSchemeInvestment" -> JsBoolean(true),
    "isOccupationalPensionScheme" -> JsBoolean(true),
    "schemeProvideBenefits" -> "Defined Benefits only",
    "schemeEstablishedCountry" -> "GB",
    "isSchemeBenefitsInsuranceCompany" -> JsBoolean(true),
    "insuranceCompanyName" -> "Test Insurance",
    "policyNumber" -> "ADN3JDA",
    "insuranceCompanyAddressDetails" -> Json.obj("line1" -> JsString("line1"),
      "line2" -> JsString("line2"),
      "line3" -> JsString("line3"),
      "line4" -> JsString("line4"),
      "postalCode" -> JsString("NE1"),
      "countryCode" -> JsString("GB")))

  val psaDetail1 : JsObject = Json.obj("psaid" -> "A0000001", "organizationOrPartnershipName" -> "org name test",
    "firstName" -> "Mickey", "middleName" -> "m", "lastName" -> "Mouse")

  val psaDetail2 : JsObject = Json.obj("psaid" -> "1234444444", "organizationOrPartnershipName" -> "org name test",
    "firstName" -> "Mickey", "middleName" -> "m", "lastName" -> "Mouse")

  val psaDetails : JsArray = Json.arr(psaDetail1,psaDetail2)

  val psaSchemeDetails : JsObject = Json.obj("psaSchemeDetails" -> Json.obj("schemeDetails" -> schemeDetails,
    "establisherDetails" -> establisherDetails, "trusteeDetails" -> trusteeDetails,
    "psaDetails" -> psaDetails))
}
