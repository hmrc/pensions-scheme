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

case class PartnershipDetails(partnershipName: String,
                              utr: Option[String],
                              vatRegistration: Option[String],
                              payeRef: Option[String],
                              address: CorrespondenceAddress,
                              contact: ContactDetails,
                              previousAddress: PreviousAddressDetails,
                              partnerDetails: Option[Seq[IndividualDetails]])

object PartnershipDetails {

  def seq[A](implicit reads: Reads[A]): Reads[Seq[A]] = Reads.traversableReads[Seq, A]

  val apiReads: Reads[PartnershipDetails] = (
    (JsPath \ "partnershipName").read[String] and
      (JsPath \ "utr").readNullable[String] and
      (JsPath \ "vatRegistrationNumber").readNullable[String] and
      (JsPath \ "payeReference").readNullable[String] and
      (JsPath \ "correspondenceAddressDetails").read[CorrespondenceAddress] and
      (JsPath \ "correspondenceContactDetails").read(ContactDetails.apiReads) and
      (JsPath \ "previousAddressDetails").read(PreviousAddressDetails.apiReads) and
      (JsPath \ "partnerDetails").readNullable(seq(IndividualDetails.apiReads))
    ) ((partnershipName, utr, vatRegistration, payeRef, address, contact, previousAddress, partnerDetails) =>
    PartnershipDetails(partnershipName, utr, vatRegistration, payeRef, address, contact, previousAddress, partnerDetails))


  implicit val formats: OFormat[PartnershipDetails] = Json.format[PartnershipDetails]

}