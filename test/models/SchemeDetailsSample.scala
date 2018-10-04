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

package models

import models.schemes.{PreviousAddressDetails => SchemePreviousAddressDetails, PersonalDetails => SchemePersonalDetails, _}
import models.schemes.{EstablisherDetails => SchemeEstablisherDetails}

trait SchemeDetailsSample {

  val personalName = IndividualName("fName", Some("mName"), "lName")
  val personalDetails =  SchemePersonalDetails(personalName, "1955-03-29")
  val correspondenceAddressDetails = CorrespondenceAddress("line1", "line2", Some("line3"), Some("line4"), "GB", Some("NE1"))
  val correspondenceContactDetails = ContactDetails(telephone="07592113", email = "test@test.com")
  val previousAddressDetails = SchemePreviousAddressDetails(true, Some(correspondenceAddressDetails))

  val inviduals = IndividualDetails(personalDetails, Some("AA999999A"), Some("1234567892"),
    correspondenceAddressDetails,  correspondenceContactDetails, previousAddressDetails)

  val companyDetails = CompanyDetails("abc organisation", Some("7897700000"), Some("AA999999A"), Some("789770000"), Some("9999"),
    correspondenceAddressDetails,  correspondenceContactDetails, Some(previousAddressDetails), Some(Seq(inviduals)))

  val partnershipDetails = PartnershipDetails("abc partnership", Some("7897700000"), Some("789770000"), Some("9999"),
    correspondenceAddressDetails,  correspondenceContactDetails, previousAddressDetails, Some(Seq(inviduals)))

  val establisherDetails = SchemeEstablisherDetails(Some(Seq(inviduals)), Some(Seq(companyDetails)), Some(Seq(partnershipDetails)))

  val psaSchemeDetailsSample = PsaSchemeDetails(
    SchemeDetails(
      Some("AAABA932JASDA"),
      Some("A3DCADAA"),
      "Pending",
      "Test Scheme",
      true,
      Some("Other"),
      Some("Other type"),
      true,
      SchemeMemberNumbers("1","2"),
      true,
      true,
      "Defined Benefits only",
      "GB",
      true,
      Some(InsuranceCompany(
        Some("Test Insurance"),
        Some("ADN3JDA"),
        Some(CorrespondenceAddress(
          "line1","line2",Some("line3"),Some("line4"),"GB",Some("NE1")))))),
    Some(List(establisherDetails)),
    Some(List(PsaDetails("A0000001",Some("org name test"),Some(Name(Some("Mickey"),Some("m"),Some("Mouse")))),
      PsaDetails("1234444444",Some("org name test"),Some(Name(Some("Mickey"),Some("m"),Some("Mouse")))))))
}
