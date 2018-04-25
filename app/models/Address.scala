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

import play.api.libs.functional.syntax._
import play.api.libs.json._

sealed trait Address

object Address {
  val addressTypeOneReads: Reads[Address] = (__ \ "country" \ "name").read[String].flatMap(countryCode =>
    getReadsBasedOnCountry[UkAddress, InternationalAddress](UkAddress.apiAddressTypeOneReads, InternationalAddress.apiAddressTypeOneReads, countryCode))
  val addressTypeTwoReads: Reads[Address] = (__ \ "countryCode").read[String].flatMap(countryCode =>
    getReadsBasedOnCountry[UkAddress, InternationalAddress](UkAddress.apiAddressTypeTwoReads, InternationalAddress.apiAddressTypeTwoReads, countryCode))

  implicit val reads: Reads[Address] = JsPath.read(addressTypeOneReads) | JsPath.read(addressTypeTwoReads)

  implicit val writes: Writes[Address] = Writes {
    case address: UkAddress =>
      UkAddress.writes.writes(address)
    case address: InternationalAddress =>
      InternationalAddress.format.writes(address)
  }

  val commonTypeOneAddressElementsReads: Reads[(String, Option[String], Option[String], Option[String], String)] = (
    (JsPath \ "lines").read[List[String]] and
      (JsPath \ "country" \ "name").read[String]
    ) ((lines, countryCode) => {
    val addressLines = lines.size match {
      case 2 => List(Some(lines(0)), Some(lines(1)), None, None)
      case 3 => List(Some(lines(0)), Some(lines(1)), Some(lines(2)), None)
      case 4 => List(Some(lines(0)), Some(lines(1)), Some(lines(2)), Some(lines(3)))
    }
    (addressLines(0).get, addressLines(1), addressLines(2), addressLines(3), countryCode)
  })

  val commonTypeTwoAddressElementsReads: Reads[(String, Option[String], Option[String], Option[String], String)] = (
    (JsPath \ "addressLine1").read[String] and
      (JsPath \ "addressLine2").readNullable[String] and
      (JsPath \ "addressLine3").readNullable[String] and
      (JsPath \ "addressLine4").readNullable[String] and
      (JsPath \ "countryCode").read[String]
    ) ((line1, line2, line3, line4, countryCode) => (line1, line2, line3, line4, countryCode))

  private def getReadsBasedOnCountry[T, B](ukAddressReads: Reads[T], nonUkAddressReads: Reads[B], countryCode: String) = {
    if (countryCode == "GB") ukAddressReads.map(c => c.asInstanceOf[Address]) else nonUkAddressReads.map(c => c.asInstanceOf[Address])
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

  val apiAddressTypeOneReads: Reads[UkAddress] = (
    JsPath.read(Address.commonTypeOneAddressElementsReads) and
      (JsPath \ "postcode").read[String]
    ) ((common, postCode) => {
    UkAddress(common._1, common._2, common._3, common._4, common._5, postCode)
  })

  val apiAddressTypeTwoReads: Reads[UkAddress] = (
    JsPath.read(Address.commonTypeTwoAddressElementsReads) and
      (JsPath \ "postalCode").read[String]
    ) ((common, postalCode) => UkAddress(common._1, common._2, common._3, common._4, common._5, postalCode))
}

case class InternationalAddress(addressLine1: String, addressLine2: Option[String] = None, addressLine3: Option[String] = None,
                                addressLine4: Option[String] = None, countryCode: String, postalCode: Option[String] = None) extends Address

object InternationalAddress {
  implicit val format: Format[InternationalAddress] = Json.format[InternationalAddress]

  val apiAddressTypeTwoReads: Reads[InternationalAddress] = (
    JsPath.read(Address.commonTypeTwoAddressElementsReads) and
      (JsPath \ "postalCode").readNullable[String]
    ) ((common, postalCode) => InternationalAddress(common._1, common._2, common._3, common._4, common._5, postalCode))


  val apiAddressTypeOneReads: Reads[InternationalAddress] = (
    JsPath.read(Address.commonTypeOneAddressElementsReads) and
      (JsPath \ "postcode").readNullable[String]
    ) ((common, postCode) => {
    InternationalAddress(common._1, common._2, common._3, common._4, common._5, postCode)
  })
}

