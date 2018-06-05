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

import java.time.LocalDate

import org.joda.time.DateTime

trait Samples {

  val nonUkAddressSample = InternationalAddress("line1",Some("line2"), Some("line3"),Some("line4"),"IT",Some("NE1"))
  val ukAddressSample = UkAddress("line1",Some("line2"), Some("line3"),Some("line4"),"GB","NE1")
  val numberOfDirectorOrPartnersSample = NumberOfDirectorOrPartnersType(isMorethanTenDirectors = Some(true))
  val previousAddressDetailsSample = PreviousAddressDetails(isPreviousAddressLast12Month = false)
  val contactDetailsSample = ContactDetails("07592113",email="test@test.com")
  val declarationSample = PensionSchemeAdministratorDeclarationType(true,true,true,true,Some(true),None,true,None)
  val pensionSchemeAdministratorSample = PensionSchemeAdministrator(customerType = "TestCustomer",
    legalStatus = "Limited Company",
    sapNumber = "NumberTest",
    noIdentifier = true,
    idType = Some("TestId"),
    idNumber = Some("TestIdNumber"),
    organisationDetail = None,
    individualDetail = None,
    pensionSchemeAdministratoridentifierStatus = PensionSchemeAdministratorIdentifierStatusType(false),
    correspondenceAddressDetail = ukAddressSample,
    correspondenceContactDetail = contactDetailsSample,
    previousAddressDetail = previousAddressDetailsSample,
    numberOfDirectorOrPartners = Some(numberOfDirectorOrPartnersSample),
    directorOrPartnerDetail = None,declaration= declarationSample)
  val correspondenceCommonDetails = CorrespondenceCommonDetail(nonUkAddressSample,contactDetailsSample)
  val directorSample = DirectorOrPartnerDetailTypeItem(sequenceId = "000",
    entityType = "Director",
    title = None,
    firstName = "John",
    middleName = Some("Does Does"),
    lastName = "Doe",
    dateOfBirth = LocalDate.parse("2019-01-31"),
    referenceOrNino = Some("SL211111A"),
    noNinoReason = Some("he can't find it"),
    utr = Some("123456789"),
    noUtrReason = Some("he can't find it"),
    correspondenceCommonDetail = correspondenceCommonDetails,
    previousAddressDetail = PreviousAddressDetails(isPreviousAddressLast12Month = false))
  val companySample = OrganisationDetailType(Some("Company Test"),vatRegistrationNumber = Some("VAT11111"), payeReference = Some("PAYE11111"), crnNumber = Some("CRN11111"))
  val individualSample = IndividualDetailType(firstName = "John",middleName = Some("Does Does"), lastName = "Doe",dateOfBirth = LocalDate.parse("2019-01-31"))
  val pensionAdviserSample = PensionAdvisorDetail("John",ukAddressSample,contactDetailsSample)
}
