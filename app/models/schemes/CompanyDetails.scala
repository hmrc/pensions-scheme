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

case class CompanyDetails(organizationName: String,
                          utr: Option[String],
                          crn: Option[String],
                          vatRegistration: Option[String],
                          payeRef: Option[String],
                          address: CorrespondenceAddress,
                          contact: ContactDetails,
                          previousAddress: Option[PreviousAddressInfo],
                          directorsDetails: Option[Seq[IndividualDetails]])

object CompanyDetails {

  def seq[A](implicit reads: Reads[A]): Reads[Seq[A]] = Reads.traversableReads[Seq, A]

  val apiReads: Reads[CompanyDetails] = (
    (JsPath \ "organisationName").read[String] and
      (JsPath \ "utr").readNullable[String] and
      (JsPath \ "crnNumber").readNullable[String] and
      (JsPath \ "vatRegistrationNumber").readNullable[String] and
      (JsPath \ "payeReference").readNullable[String] and
      (JsPath \ "correspondenceAddressDetails").read[CorrespondenceAddress] and
      (JsPath \ "correspondenceContactDetails").read(ContactDetails.apiReads) and
      (JsPath \ "previousAddressDetails").readNullable(PreviousAddressInfo.apiReads) and
      (JsPath \ "directorsDetails").readNullable(seq(IndividualDetails.apiReads))
    ) ((orgName, utr, crn, vatRegistration, payeRef, address, contact, previousAddress, directorsDetails) =>
    CompanyDetails(orgName, utr, crn, vatRegistration, payeRef, address, contact, previousAddress, directorsDetails))

  implicit val formats: OFormat[CompanyDetails] = Json.format[CompanyDetails]

}
