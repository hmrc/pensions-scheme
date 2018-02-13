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

import play.api.libs.json.Json

case class ContactDetails(telephone: String, mobileNumber: Option[String] = None, fax: Option[String] = None, email: String)

object ContactDetails {
  implicit val formats = Json.format[ContactDetails]
}

case class AddressDetails(addressType: String, line1: String, line2: String, line3: Option[String],
                          line4: Option[String], postalCode: Option[String], countryCode: String)

object AddressDetails {
  implicit val formats = Json.format[AddressDetails]
}

case class AddressAndContactDetails(addressDetails: AddressDetails, contactDetails: ContactDetails)

object AddressAndContactDetails {
  implicit val formats = Json.format[AddressAndContactDetails]
}

case class PersonalDetails(title: Option[String] = None, firstName: String, middleName: Option[String] = None,
                           lastName: String, dateOfBirth: String)

object PersonalDetails {
  implicit val formats = Json.format[PersonalDetails]
}

case class CorrespondenceAddressDetails(addressDetails: AddressDetails)

object CorrespondenceAddressDetails {
  implicit val formats = Json.format[CorrespondenceAddressDetails]
}

case class CorrespondenceContactDetails(contactDetails: ContactDetails)

object CorrespondenceContactDetails {
  implicit val formats = Json.format[CorrespondenceContactDetails]
}

case class PreviousAddressDetails(isPreviousAddressLast12Month: Boolean,
                                  previousAddressDetails: Option[AddressDetails] = None)

object PreviousAddressDetails {
  implicit val formats = Json.format[PreviousAddressDetails]
}

case class CustomerAndSchemeDetails(schemeName: String, isSchemeMasterTrust: Boolean, schemeStructure: Option[String] = None,
                                    otherSchemeStructure: Option[String] = None, haveMoreThanTenTrustee: Option[Boolean] = None,
                                    currentSchemeMembers: String, futureSchemeMembers: String, isReguledSchemeInvestment: Boolean,
                                    isOccupationalPensionScheme: Boolean, areBenefitsSecuredContractInsuranceCompany: Boolean,
                                    doesSchemeProvideBenefits: String, schemeEstablishedCountry: String, haveValidBank: Boolean,
                                    insuranceCompanyName: Option[String] = None, policyNumber: Option[String] = None,
                                    insuranceCompanyAddress: Option[AddressDetails] = None)

object CustomerAndSchemeDetails {
  implicit val formats = Json.format[CustomerAndSchemeDetails]
}

case class PensionSchemeDeclaration(box1: Boolean, box2: Boolean, box3: Option[Boolean] = None, box4: Option[Boolean] = None,
                                    box5: Option[Boolean] = None, box6: Boolean, box7: Boolean, box8: Boolean, box9: Boolean,
                                    box10: Option[Boolean] = None, box11: Option[Boolean] = None, pensionAdviserName: Option[String] = None,
                                    addressAndContactDetails: Option[AddressAndContactDetails] = None)

object PensionSchemeDeclaration {
  implicit val formats = Json.format[PensionSchemeDeclaration]
}

case class EstablisherDetails(`type`: String, organisationName: Option[String] = None,
                              personalDetails: Option[PersonalDetails] = None,
                              referenceOrNino: Option[String] = None, noNinoReason: Option[String] = None,
                              utr: Option[String] = None, noUtrReason: Option[String] = None, crnNumber: Option[String] = None,
                              noCrnReason: Option[String] = None, vatRegistrationNumber: Option[String] = None,
                              payeReference: Option[String] = None, haveMoreThanTenDirectorOrPartner: Option[Boolean] = None,
                              correspondenceAddressDetails: CorrespondenceAddressDetails,
                              correspondenceContactDetails: CorrespondenceContactDetails,
                              previousAddressDetails: Option[PreviousAddressDetails] = None)

object EstablisherDetails {
  implicit val formats = Json.format[EstablisherDetails]
}

case class PensionsScheme(customerAndSchemeDetails: CustomerAndSchemeDetails, pensionSchemeDeclaration: PensionSchemeDeclaration,
                          establisherDetails: List[EstablisherDetails])

object PensionsScheme {
  implicit val formats = Json.format[PensionsScheme]
}
