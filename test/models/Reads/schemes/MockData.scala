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

import play.api.libs.json.{JsBoolean, JsString, Json}

trait MockData {

  val personalDetails = Json.obj("firstName" -> "abcdef", "middleName" -> "fdgdgfggfdg", "lastName" -> "dfgfdgdfg", "dateOfBirth" -> "1955-03-29")

  val addressDetails = Json.obj("nonUKAddress" -> JsBoolean(false), "line1" -> "line1", "line2" -> "line2", "line3" -> JsString("line3"), "line4" -> JsString("line4"),
    "postalCode" -> JsString("NE1"), "countryCode" -> JsString("GB"))

  val fullContactDetails = Json.obj("phone" -> "0758237281", "email" -> "test@test.com", "mobileNumber" -> "4564564664", "fax" -> "4654654313")

  val previousAddressDetails = Json.obj("isPreviousAddressLast12Month" -> JsBoolean(true), "previousAddress" -> addressDetails)

  val individualDetails = Json.obj("personDetails" -> personalDetails, "nino" -> "AA999999A", "utr" -> "1234567892",
    "correspondenceAddressDetails" -> addressDetails, "correspondenceContactDetails" -> fullContactDetails, "previousAddressDetails" -> previousAddressDetails)

  val companyOrOrganisationDetails = Json.obj("organisationName" -> "abc organisation", "utr"-> "7897700000",
    "crnNumber"-> "AA999999A", "vatRegistrationNumber"-> "789770000", "payeReference" -> "9999",
    "correspondenceAddressDetails"-> addressDetails, "correspondenceContactDetails" -> fullContactDetails,
    "previousAddressDetails" -> previousAddressDetails, "directorsDetails" -> Json.arr(individualDetails))
}
