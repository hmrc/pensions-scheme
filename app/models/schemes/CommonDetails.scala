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

package models.schemes

import models.{ContactDetails, CorrespondenceAddress, Name}
import play.api.libs.json.{JsPath, Json, OFormat, Reads}
import play.api.libs.functional.syntax._


case class PersonalDetails(name: Option[Name], dateOfBirth: Option[String])

object PersonalDetails {

  val apiReads: Reads[PersonalDetails] = (
    (JsPath \ "firstName").readNullable[String] and
      (JsPath \ "middleName").readNullable[String] and
      (JsPath \ "lastName").readNullable[String] and
      (JsPath \ "dateOfBirth").readNullable[String]
    ) ((firstName, middleName, lastName, dateOfBirth) => PersonalDetails(getName(firstName, middleName, lastName), dateOfBirth))


  implicit val formats : OFormat[PersonalDetails] = Json.format[PersonalDetails]


  private def getName(firstName: Option[String], middleName: Option[String], lastName: Option[String]): Option[Name] = {
    (firstName, middleName, lastName) match {
      case (None, None, None) => None
      case _ => Some(Name(firstName, middleName, lastName))
    }
  }
}

case class IndividualDetails(personalDetails: Option[PersonalDetails],
                             nino: Option[String],
                             utr: Option[String],
                             address: Option[CorrespondenceAddress],
                             contact: Option[ContactDetails],
                             previousAddress: Option[PreviousAddressDetails])

object IndividualDetails {

  val apiReads: Reads[IndividualDetails] = (
    (JsPath \ "personDetails").readNullable(PersonalDetails.apiReads) and
      (JsPath \ "nino").readNullable[String] and
      (JsPath \ "utr").readNullable[String] and
      (JsPath \ "correspondenceAddressDetails").readNullable[CorrespondenceAddress] and
      (JsPath \ "correspondenceContactDetails").readNullable(ContactDetails.apiReads) and
      (JsPath \ "previousAddressDetails").readNullable(PreviousAddressDetails.apiReads)
    ) ((personal, nino, utr, address, contact, previousAddress) =>
    IndividualDetails(personal, nino, utr, address, contact, previousAddress))


  implicit val formats: OFormat[IndividualDetails] = Json.format[IndividualDetails]

}


case class PreviousAddressDetails(isPreviousAddressLast12Month: Boolean,
                                  previousAddress: Option[CorrespondenceAddress] = None)

object PreviousAddressDetails {

  val apiReads: Reads[PreviousAddressDetails] = (
    (JsPath \ s"isPreviousAddressLast12Month").read[Boolean] and
      (JsPath \ s"previousAddress").readNullable[CorrespondenceAddress]
    ) ((addressLast12Months, address) => {
    PreviousAddressDetails(addressLast12Months, address)
  })

  implicit val formats: OFormat[PreviousAddressDetails] = Json.format[PreviousAddressDetails]
}
