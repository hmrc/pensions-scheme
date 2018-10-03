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

import models.schemes._

trait Samples {

  val ukAddressSampleWithTwoLines = UkAddress("line1", Some("line2"), None, None, "GB", "NE1")
  val nonUkAddressSample = InternationalAddress("line1", Some("line2"), Some("line3"), Some("line4"), "IT", Some("NE1"))
  val ukAddressSample = UkAddress("line1", Some("line2"), Some("line3"), Some("line4"), "GB", "NE1")
  val previousAddressDetailsSample = PreviousAddressDetails(isPreviousAddressLast12Month = false)
  val contactDetailsSample = ContactDetails("07592113", email = "test@test.com")

  val companySample = OrganisationDetailType("Company Test", vatRegistrationNumber = Some("VAT11111"),
    payeReference = Some("PAYE11111"), crnNumber = Some("CRN11111"))

  val pensionAdviserSample = PensionAdvisorDetail("John", ukAddressSample, contactDetailsSample)

  val trusteePartnershipData = PartnershipTrustee(
    organizationName = "test partnership",
    utr = Some("1111111111"),
    noUtrReason = None,
    vatRegistrationNumber = None,
    payeReference = None,
    correspondenceAddressDetails = CorrespondenceAddressDetails(ukAddressSample),
    correspondenceContactDetails = CorrespondenceContactDetails(contactDetailsSample),
    previousAddressDetails = Some(PreviousAddressDetails(isPreviousAddressLast12Month = true, Some(ukAddressSample))))

  val trusteeCompanyData = CompanyTrustee(
    organizationName = "test company",
    utr = Some("1111111111"),
    crnNumber = Some("crn1234"),
    noUtrReason = None,
    vatRegistrationNumber = None,
    payeReference = None,
    correspondenceAddressDetails = CorrespondenceAddressDetails(ukAddressSample),
    correspondenceContactDetails = CorrespondenceContactDetails(contactDetailsSample),
    previousAddressDetails = Some(PreviousAddressDetails(isPreviousAddressLast12Month = true, Some(ukAddressSample))))

  val trusteeIndividualData = Individual(
    personalDetails = PersonalDetails(firstName = "John",
      middleName = Some("William"),
      lastName = "Doe",
      dateOfBirth = "2019-01-31"),
    referenceOrNino = Some("nino1234"),
    noNinoReason = None,
    utr = Some("1111111111"),
    noUtrReason = None,
    correspondenceAddressDetails = CorrespondenceAddressDetails(ukAddressSample),
    correspondenceContactDetails = CorrespondenceContactDetails(contactDetailsSample),
    previousAddressDetails = Some(PreviousAddressDetails(isPreviousAddressLast12Month = true, Some(ukAddressSample))))

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
    Some(List(PsaDetails("A0000001",Some("org name test"),Some(Name(Some("Mickey"),Some("m"),Some("Mouse")))),
      PsaDetails("1234444444",Some("org name test"),Some(Name(Some("Mickey"),Some("m"),Some("Mouse")))))))
}
