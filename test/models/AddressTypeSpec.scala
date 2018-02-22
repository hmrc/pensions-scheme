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

import base.SpecBase
import play.api.libs.json.Json

class AddressTypeSpec extends SpecBase {

  "Reads Address" must {
    "return Address for UK when the countryCode is GB" in {
      val json = Json.obj("addressType" -> "UK", "line1" -> "test line 1", "line2" -> "test line 2",
        "postalCode" -> "test post code", "countryCode" -> "GB")

      json.as[AddressType] mustEqual UkAddressType(addressType = "UK", line1 = "test line 1", line2 = "test line 2",
        postalCode = "test post code", countryCode = "GB")
    }

    "return Address for Non UK when the countryCode is not GB and no postal code" in {
      val json = Json.obj("addressType" -> "NON-UK", "line1" -> "test line 1", "line2" -> "test line 2", "countryCode" -> "IN")

      json.as[AddressType] mustEqual ForeignAddressType(addressType = "NON-UK", line1 = "test line 1", line2 = "test line 2",
        countryCode = "IN")
    }

    "return Address for Non UK when the countryCode is not GB and with postal code" in {
      val json = Json.obj("addressType" -> "NON-UK", "line1" -> "test line 1", "line2" -> "test line 2", "postalCode" -> "test post code",
        "countryCode" -> "IN")

      json.as[AddressType] mustEqual ForeignAddressType(addressType = "NON-UK", line1 = "test line 1", line2 = "test line 2",
        postalCode = Some("test post code"), countryCode = "IN")
    }
  }

  "Writes Address" must {
    "return json for UK Address" in {
      val ukAddressType = UkAddressType(addressType = "UK", line1 = "test line 1", line2 = "test line 2",
        postalCode = "test post code", countryCode = "GB")

      val jsonResult = Json.obj("addressType" -> "UK", "line1" -> "test line 1", "line2" -> "test line 2",
        "postalCode" -> "test post code", "countryCode" -> "GB")

      Json.toJson[AddressType](ukAddressType) mustEqual jsonResult
    }

    "return json for UK Address even if address type is other than UK" in {
      val ukAddressType = UkAddressType(addressType = "xyz", line1 = "test line 1", line2 = "test line 2",
        postalCode = "test post code", countryCode = "GB")

      val jsonResult = Json.obj("addressType" -> "UK", "line1" -> "test line 1", "line2" -> "test line 2",
        "postalCode" -> "test post code", "countryCode" -> "GB")

      Json.toJson[AddressType](ukAddressType) mustEqual jsonResult
    }

    "return json for Non UK Address with no postal code" in {
      val foreignAddressType = ForeignAddressType(addressType = "NON-UK", line1 = "test line 1", line2 = "test line 2",
        countryCode = "IN")

      val jsonResult = Json.obj("addressType" -> "NON-UK", "line1" -> "test line 1", "line2" -> "test line 2", "countryCode" -> "IN")

      Json.toJson[AddressType](foreignAddressType) mustEqual jsonResult
    }

    "return json for Non UK Address with postal code" in {
      val foreignAddressType = ForeignAddressType(addressType = "NON-UK", line1 = "test line 1", line2 = "test line 2",
        postalCode = Some("test post code"), countryCode = "IN")

      val jsonResult = Json.obj("addressType" -> "NON-UK", "line1" -> "test line 1", "line2" -> "test line 2",
        "postalCode" -> "test post code", "countryCode" -> "IN")

      Json.toJson[AddressType](foreignAddressType) mustEqual jsonResult
    }
  }
}
