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

import models.{ContactDetails, CorrespondenceAddress}
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, OFormat, Reads}


case class IndividualDetails(personalDetails: PersonalDetails,
                             nino: Option[String],
                             utr: Option[String],
                             address: CorrespondenceAddress,
                             contact: ContactDetails,
                             previousAddress: PreviousAddressDetails)

object IndividualDetails {

  val apiReads: Reads[IndividualDetails] = (
    (JsPath \ "personDetails").read(PersonalDetails.apiReads) and
      (JsPath \ "nino").readNullable[String] and
      (JsPath \ "utr").readNullable[String] and
      (JsPath \ "correspondenceAddressDetails").read(CorrespondenceAddress.reads) and
      (JsPath \ "correspondenceContactDetails").read(ContactDetails.apiReads) and
      (JsPath \ "previousAddressDetails").read(PreviousAddressDetails.apiReads)
    ) ((personal, nino, utr, address, contact, previousAddress) =>
    IndividualDetails(personal, nino, utr, address, contact, previousAddress))


  implicit val formats: OFormat[IndividualDetails] = Json.format[IndividualDetails]

}

case class PersonalDetails(name: IndividualName, dateOfBirth: String)

object PersonalDetails {

  val apiReads: Reads[PersonalDetails] = (
    (JsPath \ "firstName").read[String] and
      (JsPath \ "middleName").readNullable[String] and
      (JsPath \ "lastName").read[String] and
      (JsPath \ "dateOfBirth").read[String]
    ) ((firstName, middleName, lastName, dateOfBirth) =>
    PersonalDetails(IndividualName(firstName, middleName, lastName), dateOfBirth))


  implicit val formats: OFormat[PersonalDetails] = Json.format[PersonalDetails]

}

case class IndividualName(firstName: String, middleName: Option[String], lastName: String)

object IndividualName {
  implicit val formats : OFormat[IndividualName] = Json.format[IndividualName]
}

