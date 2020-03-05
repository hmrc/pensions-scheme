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

import play.api.libs.json.{Format, JsPath, Json, Writes}
import play.api.libs.functional.syntax._

case class Individual(
                       personalDetails: PersonalDetails,
                       referenceOrNino: Option[String] = None,
                       noNinoReason: Option[String] = None,
                       utr: Option[String] = None,
                       noUtrReason: Option[String] = None,
                       correspondenceAddressDetails: CorrespondenceAddressDetails,
                       correspondenceContactDetails: CorrespondenceContactDetails,
                       previousAddressDetails: Option[PreviousAddressDetails] = None
                     )

object Individual {
  implicit val formats: Format[Individual] = Json.format[Individual]

  private val commonIndividualWrites: Writes[(Option[String], Option[String], Option[String],
    Option[String], CorrespondenceAddressDetails, CorrespondenceContactDetails, PreviousAddressDetails)] = (
    (JsPath \ "nino").writeNullable[String] and
      (JsPath \ "noNinoReason").writeNullable[String] and
      (JsPath \ "utr").writeNullable[String] and
      (JsPath \ "noUtrReason").writeNullable[String] and
      (JsPath \ "correspondenceAddressDetails").write[CorrespondenceAddressDetails](CorrespondenceAddressDetails.updateWrites) and
      (JsPath \ "correspondenceContactDetails").write[CorrespondenceContactDetails] and
      (JsPath \ "previousAddressDetails").write[PreviousAddressDetails](PreviousAddressDetails.psaUpdateWrites)
    ) (elements => elements)

  private def getIndividual(details: Individual) = {
    (details.personalDetails,
      (details.referenceOrNino,
        details.noNinoReason,
        details.utr,
        details.noUtrReason,
        details.correspondenceAddressDetails,
        details.correspondenceContactDetails,
        details.previousAddressDetails.fold(PreviousAddressDetails(isPreviousAddressLast12Month = false))(c => c)))
  }

  val individualUpdateWrites: Writes[Individual] = (
    (JsPath \ "personalDetails").write[PersonalDetails] and
      JsPath.write(commonIndividualWrites)
    ) (getIndividual(_))
}

case class PersonalDetails(title: Option[String] = None, firstName: String, middleName: Option[String] = None,
                           lastName: String, dateOfBirth: String)

object PersonalDetails {
  implicit val formats: Format[PersonalDetails] = Json.format[PersonalDetails]
}
