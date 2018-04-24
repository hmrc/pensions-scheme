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

trait Samples {


  val nonUkAddressSample = ForeignAddress("line1",Some("line2"), Some("line3"),Some("line4"),"IT",Some("NE1"))
  val ukAddressSample = UkAddress("line1",Some("line2"), Some("line3"),Some("line4"),"GB","NE1")
  val numberOfDirectorOrPartnersSample = NumberOfDirectorOrPartnersType(isMorethanTenDirectors = Some(true))
  val previousAddressDetailsSample = PreviousAddressDetails(isPreviousAddressLast12Month = false)
  val contactDetailsSample = ContactDetails("07592113",email="test@test.com")
  val pensionSchemeAdministratorSample = PensionSchemeAdministrator(customerType = "TestCustomer",
    legalStatus = "Individual",
    sapNumber = "NumberTest",
    noIdentifier = true,
    idType = Some("TestId"),
    idNumber = Some("TestIdNumber"),
    organisationDetail = None,
    individualDetail = None,
    pensionSchemeAdministratoridentifierStatus = PensionSchemeAdministratorIdentifierStatusType(true),
    correspondenceAddressDetail = ukAddressSample,
    correspondenceContactDetail = contactDetailsSample,
    previousAddressDetail = previousAddressDetailsSample,
    numberOfDirectorOrPartners = Some(numberOfDirectorOrPartnersSample),
    directorOrPartnerDetail = None)
  val correspondenceCommonDetails = CorrespondenceCommonDetail(nonUkAddressSample,contactDetailsSample)


}
