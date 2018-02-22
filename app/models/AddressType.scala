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

sealed trait AddressType

object AddressType {
  implicit val reads: Reads[AddressType] = {
    import play.api.libs.json._
    (__ \ "countryCode").read[String].flatMap {
      case "GB" =>
        UkAddressType.format.map[AddressType](identity)
      case _ =>
        ForeignAddressType.format.map[AddressType](identity)
    }
  }

  implicit val writes: Writes[AddressType] = Writes {
    case address: UkAddressType =>
      UkAddressType.writes.writes(address)
    case address: ForeignAddressType =>
      ForeignAddressType.format.writes(address)
  }
}

case class UkAddressType(addressType: String, line1: String, line2: String, line3: Option[String] = None,
                     line4: Option[String] = None, countryCode: String, postalCode: String) extends AddressType

object UkAddressType {
  implicit val format: Reads[UkAddressType] = Json.reads[UkAddressType]

  implicit val writes: Writes[UkAddressType] = Writes {
    address =>
      Json.writes[UkAddressType].writes(address) ++ Json.obj("countryCode" -> "GB", "addressType" -> "UK")
  }
}

case class ForeignAddressType(addressType: String, line1: String, line2: String, line3: Option[String] = None,
                          line4: Option[String] = None, postalCode: Option[String] = None, countryCode: String) extends AddressType

object ForeignAddressType {
  implicit val format: OFormat[ForeignAddressType] = Json.format[ForeignAddressType]

  implicit val writes: Writes[ForeignAddressType] = Writes {
    address =>
      Json.writes[ForeignAddressType].writes(address) ++ Json.obj("addressType" -> "NON-UK")
  }
}

