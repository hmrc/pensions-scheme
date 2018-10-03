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

import play.api.libs.functional.syntax._
import play.api.libs.json._



case class AddressDetails(isNonUK: Boolean,
                          addressLine1: String,
                          addressLine2: String,
                          addressLine3: Option[String],
                          addressLine4: Option[String],
                          countryCode: String,
                          postalCode: Option[String])

object AddressDetails {

  val apiReads: Reads[AddressDetails] = (
    (JsPath \ "nonUKAddress").read[Boolean] and
    (JsPath \ "line1").read[String] and
      (JsPath \ "line2").read[String] and
      (JsPath \ "line3").readNullable[String] and
      (JsPath \ "line4").readNullable[String] and
      (JsPath \ "countryCode").read[String] and
      (JsPath \ "postalCode").readNullable[String]
    ) (AddressDetails.apply _)

  implicit val formats: OFormat[AddressDetails] = Json.format[AddressDetails]

}

case class PreviousAddressDetails(isPreviousAddressLast12Month: Boolean,
                                  previousAddress: Option[AddressDetails] = None)

object PreviousAddressDetails {

  val apiReads: Reads[PreviousAddressDetails] = (
    (JsPath \ s"isPreviousAddressLast12Month").read[Boolean] and
      (JsPath \ s"previousAddress").readNullable(AddressDetails.apiReads)
    ) ((addressLast12Months, address) => {
    PreviousAddressDetails(addressLast12Months, address)
  })

  implicit val formats: OFormat[PreviousAddressDetails] = Json.format[PreviousAddressDetails]
}
