/*
 * Copyright 2019 HM Revenue & Customs
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

import models.CorrespondenceAddress
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads.seq
import play.api.libs.json.{JsPath, Json, OFormat, Reads}

case class CompanyDetails(organizationName: String,
                          utr: Option[String],
                          crn: Option[String],
                          vatRegistration: Option[String],
                          payeRef: Option[String],
                          address: CorrespondenceAddress,
                          contact: IndividualContactDetails,
                          previousAddress: Option[PreviousAddressInfo],
                          directorsDetails: Seq[IndividualInfo])

object CompanyDetails {

  val apiReads: Reads[CompanyDetails] = (
    (JsPath \ "organisationName").read[String] and
      (JsPath \ "utr").readNullable[String] and
      (JsPath \ "crnNumber").readNullable[String] and
      (JsPath \ "vatRegistrationNumber").readNullable[String] and
      (JsPath \ "payeReference").readNullable[String] and
      (JsPath \ "correspondenceAddressDetails").read[CorrespondenceAddress] and
      (JsPath \ "correspondenceContactDetails").read(IndividualContactDetails.apiReads) and
      (JsPath \ "previousAddressDetails").readNullable(PreviousAddressInfo.apiReads) and
      (JsPath \ "directorsDetails").readNullable(seq(IndividualInfo.apiReads))
    ) ((orgName, utr, crn, vatRegistration, payeRef, address, contact, previousAddress, directorsDetails) =>
    CompanyDetails(orgName, utr, crn, vatRegistration, payeRef, address, contact, previousAddress, directorsDetails.getOrElse(Nil)))

  implicit val formats: OFormat[CompanyDetails] = Json.format[CompanyDetails]

}
