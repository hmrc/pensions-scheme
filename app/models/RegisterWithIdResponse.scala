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

case class OrganisationType(organisationName: String, isAGroup: Option[Boolean] = None, organisationType: Option[String] = None)

object OrganisationType {
  implicit val formats = Json.format[OrganisationType]
}

case class IndividualType(firstName: String, middleName: Option[String] = None, lastName: String, dateOfBirth: Option[String] = None)

object IndividualType {
  implicit val formats = Json.format[IndividualType]
}

case class AddressType(addressLine1: String, addressLine2: Option[String] = None, addressLine3: Option[String] = None,
                       addressLine4: Option[String] = None, countryCode: String, postalCode: Option[String] = None)

object AddressType {
  implicit val formats = Json.format[AddressType]
}

case class ContactCommDetailsType(primaryPhoneNumber: Option[String] = None, secondaryPhoneNumber: Option[String] = None,
                                  faxNumber: Option[String] = None, emailAddress: Option[String] = None)

object ContactCommDetailsType {
  implicit val formats = Json.format[ContactCommDetailsType]
}

case class SuccessResponse(safeId: String, sapNumber: String, agentReferenceNumber: String, isEditable: Boolean,
                           isAnAgent: Boolean, isAnASAgent: Boolean, isAnIndividual: Boolean, individual: Option[IndividualType] = None,
                           organisation: Option[OrganisationType] = None, address: AddressType, contactDetails: ContactCommDetailsType)

object SuccessResponse {
  implicit val formats = Json.format[SuccessResponse]
}

case class FailureResponseElement(code: String, reason: String)

object FailureResponseElement {
  implicit val formats = Json.format[FailureResponseElement]
}

case class FailureResponseArray(failures: List[FailureResponseElement])

object FailureResponseArray {
  implicit val formats = Json.format[FailureResponseArray]
}

case class FailureResponse(failureResponseElement: Option[FailureResponseElement] = None,
                           failureResponseArray: Option[FailureResponseArray] = None)

object FailureResponse {
  implicit val formats = Json.format[FailureResponse]
}
