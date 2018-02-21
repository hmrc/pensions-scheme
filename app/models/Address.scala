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

package models

import play.api.libs.json._

sealed trait Address {

}

object Address {
  implicit val reads: Reads[Address] = {
    import play.api.libs.json._
    (__ \ "countryCode").read[String].flatMap {
      case "GB" =>
        UkAddress.format.map[Address](identity)
      case _ =>
        ForeignAddress.format.map[Address](identity)
    }
  }

  implicit val writes: Writes[Address] = Writes {
    case address: UkAddress =>
      UkAddress.writes.writes(address)
    case address: ForeignAddress =>
      ForeignAddress.format.writes(address)
  }
}

case class UkAddress(addressLine1: String, addressLine2: Option[String] = None, addressLine3: Option[String] = None,
                     addressLine4: Option[String] = None, countryCode: String, postalCode: String) extends Address

object UkAddress {
  implicit val format: Reads[UkAddress] = Json.reads[UkAddress]

  implicit val writes: Writes[UkAddress] = Writes {
    address =>
      Json.writes[UkAddress].writes(address) ++ Json.obj("countryCode" -> "GB")
  }
}

case class ForeignAddress(addressLine1: String, addressLine2: Option[String] = None, addressLine3: Option[String] = None,
                          addressLine4: Option[String] = None, countryCode: String, postalCode: Option[String] = None) extends Address

object ForeignAddress {
  implicit val format: OFormat[ForeignAddress] = Json.format[ForeignAddress]
}

