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


case class IndividualInfo(personalDetails: PersonalInfo,
                          nino: Option[String],
                          utr: Option[String],
                          address: CorrespondenceAddress,
                          contact: IndividualContactDetails,
                          previousAddress: PreviousAddressInfo)

object IndividualInfo {

  val apiReads: Reads[IndividualInfo] = (
    (JsPath \ "personDetails").read(PersonalInfo.apiReads) and
      (JsPath \ "nino").readNullable[String] and
      (JsPath \ "utr").readNullable[String] and
      (JsPath \ "correspondenceAddressDetails").read(CorrespondenceAddress.reads) and
      (JsPath \ "correspondenceContactDetails").read(IndividualContactDetails.apiReads) and
      (JsPath \ "previousAddressDetails").read(PreviousAddressInfo.apiReads)
    ) ((personal, nino, utr, address, contact, previousAddress) =>
    IndividualInfo(personal, nino, utr, address, contact, previousAddress))


  implicit val formats: OFormat[IndividualInfo] = Json.format[IndividualInfo]

}

case class IndividualContactDetails(telephone: String, email: String)

object IndividualContactDetails {

  val apiReads: Reads[IndividualContactDetails] = (
    (JsPath \ "telephone").read[String] and
      (JsPath \ "email").read[String]
    ) ((telephone, email) => IndividualContactDetails(telephone, email)
  )

  implicit val formats: OFormat[IndividualContactDetails] = Json.format[IndividualContactDetails]
}

case class PersonalInfo(name: IndividualName, dateOfBirth: String)

object PersonalInfo {

  val apiReads: Reads[PersonalInfo] = (
    (JsPath \ "firstName").read[String] and
      (JsPath \ "middleName").readNullable[String] and
      (JsPath \ "lastName").read[String] and
      (JsPath \ "dateOfBirth").read[String]
    ) ((firstName, middleName, lastName, dateOfBirth) =>
    PersonalInfo(IndividualName(firstName, middleName, lastName), dateOfBirth))


  implicit val formats: OFormat[PersonalInfo] = Json.format[PersonalInfo]

}

case class IndividualName(firstName: String, middleName: Option[String], lastName: String)

object IndividualName {
  implicit val formats : OFormat[IndividualName] = Json.format[IndividualName]
}
