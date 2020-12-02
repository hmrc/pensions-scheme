/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.libs.json.{Format, JsPath, Json, Reads}
import play.api.libs.functional.syntax._

case class PSPDetails(pspid: String,
                      orgOrPartName: Option[String],
                      firstName: Option[String],
                      middleName: Option[String],
                      lastName: Option[String],
                      relationshipStartDate: String,
                      authorisedPSAID: String,
                      authorisedPSAOrgOrPartName: Option[String],
                      authorisedPSAFirstName: Option[String],
                      authorisedPSAMiddleName: Option[String],
                      authorisedPSALastName: Option[String])

object PSPDetails {
  implicit val format: Format[PSPDetails] = Json.format[PSPDetails]
}

case class SchemeDetails(name: String, referenceNumber: String, schemeStatus: String, openDate: Option[String], pstr: Option[String] = None,
                         relationship: Option[String], pspDetails: Option[List[PSPDetails]] = None, underAppeal: Option[String] = None)

object SchemeDetails {
  implicit val format: Format[SchemeDetails] = Json.format[SchemeDetails]

  val desReads: Reads[SchemeDetails] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "referenceNumber").read[String] and
      (JsPath \ "schemeStatus").read[String] and
      (JsPath \ "openDate").readNullable[String] and
      (JsPath \ "pstr").readNullable[String] and
      (JsPath \ "relationShip").readNullable[String] and
      (JsPath \ "underAppeal").readNullable[String]
    ) (
    (name, srn, status, openDate, pstr, relationship, underAppeal) =>
      SchemeDetails(name, srn, status, openDate, pstr, relationship, None, underAppeal)
  )
}

case class ListOfSchemes(processingDate: String, totalSchemesRegistered: String,
                         schemeDetails: Option[List[SchemeDetails]] = None)

object ListOfSchemes {
  implicit val format: Format[ListOfSchemes] = Json.format[ListOfSchemes]

  val desReads: Reads[ListOfSchemes] = (
    (JsPath \ "processingDate").read[String] and
      (JsPath \ "totalSchemesRegistered").read[String] and
      (JsPath \ "schemeDetail").readNullable[List[SchemeDetails]](Reads.list(SchemeDetails.desReads))
    ) (
    (processingDate, totalSchemesRegistered, schemeDetails) =>
      ListOfSchemes(processingDate, totalSchemesRegistered, schemeDetails)
  )
}
