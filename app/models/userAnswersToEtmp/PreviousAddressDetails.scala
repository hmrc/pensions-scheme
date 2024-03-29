/*
 * Copyright 2024 HM Revenue & Customs
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

package models.userAnswersToEtmp

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class PreviousAddressDetails(isPreviousAddressLast12Month: Boolean,
                                  previousAddressDetails: Option[Address] = None)

object PreviousAddressDetails {
  implicit val formats: Format[PreviousAddressDetails] = Json.format[PreviousAddressDetails]

  val psaSubmissionWrites: Writes[PreviousAddressDetails] = (
    (JsPath \ "isPreviousAddressLast12Month").write[Boolean] and
      (JsPath \ "previousAddressDetail").writeNullable[Address]
    ) (previousAddress => (previousAddress.isPreviousAddressLast12Month, previousAddress.previousAddressDetails))

  val psaUpdateWrites: Writes[PreviousAddressDetails] = (
    (JsPath \ "isPreviousAddressLast12Month").write[Boolean] and
      (JsPath \ "previousAddressDetails").writeNullable[Address](Address.updateWrites)
    ) (previousAddress => (previousAddress.isPreviousAddressLast12Month, previousAddress.previousAddressDetails))

  def apiReads(typeOfAddressDetail: String): Reads[PreviousAddressDetails] = (
    (JsPath \ s"${typeOfAddressDetail}AddressYears").read[String] and
      (JsPath \ s"${typeOfAddressDetail}PreviousAddress").readNullable[Address]
    ) ((addressLast12Months, address) => {
    val isAddressLast12Months = if (addressLast12Months == "under_a_year") true else false
    PreviousAddressDetails(isAddressLast12Months, address)
  })
}
