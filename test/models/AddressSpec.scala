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

class AddressSpec extends SpecBase  {

  "Reads Address" must {
    "return a UkAddress when given a Uk Address" in {
      val json = Json.obj("addressLine1" -> "address1", "addressLine2" -> "address2", "addressLine3" -> "address3",
        "addressLine4"->"address4","postalCode" -> "test post code", "countryCode" -> "GB")

      json.as[Address] mustEqual UkAddress(addressLine1 = "address1",addressLine2 = Some("address2"), addressLine3 = Some("address3"),
        addressLine4=Some("address4"),postalCode = "test post code", countryCode = "GB")
    }

    "return Foreign Address when the countryCode is not GB and no postal code" in {
      val json = Json.obj("addressLine1" -> "address1", "addressLine2" -> "address2", "addressLine3" -> "address3",
        "addressLine4"->"address4", "countryCode" -> "IN")

      json.as[Address] mustEqual ForeignAddress(addressLine1 = "address1",addressLine2 = Some("address2"), addressLine3 = Some("address3"),
        addressLine4=Some("address4"),countryCode = "IN")
    }

    "return Foreign Address when the countryCode is not GB and with postal code" in {
      val json = Json.obj("addressLine1" -> "address1", "addressLine2" -> "address2", "addressLine3" -> "address3",
        "addressLine4"->"address4", "postalCode"->"test post code", "countryCode" -> "IN")

      json.as[Address] mustEqual ForeignAddress(addressLine1 = "address1",addressLine2 = Some("address2"), addressLine3 = Some("address3"),
        addressLine4=Some("address4"),postalCode=Some("test post code"),countryCode = "IN")
    }
  }

  "Writes Address" must {
    "return json for UK Address" in {
      val ukAddress =UkAddress(addressLine1 = "address1",addressLine2 = Some("address2"), addressLine3 = Some("address3"),
        addressLine4=Some("address4"),postalCode = "test post code", countryCode = "GB")

      val jsonResult =  Json.obj("addressLine1" -> "address1", "addressLine2" -> "address2", "addressLine3" -> "address3",
        "addressLine4"->"address4","postalCode" -> "test post code", "countryCode" -> "GB")

      Json.toJson[Address](ukAddress) mustEqual jsonResult
    }

    "return json for UK Address ensuring country code is GB" in {
      val ukAddress =UkAddress(addressLine1 = "address1",addressLine2 = Some("address2"), addressLine3 = Some("address3"),
        addressLine4=Some("address4"),postalCode = "test post code", countryCode = "Non GB Code")

      val jsonResult =  Json.obj("addressLine1" -> "address1", "addressLine2" -> "address2", "addressLine3" -> "address3",
        "addressLine4"->"address4","postalCode" -> "test post code", "countryCode" -> "GB")

      Json.toJson[Address](ukAddress) mustEqual jsonResult
    }


    "return json for Foreign Address with no postal code" in {
      val foreignAddress =ForeignAddress(addressLine1 = "address1",addressLine2 = Some("address2"), addressLine3 = Some("address3"),
        addressLine4=Some("address4"),countryCode = "IN")

      val jsonResult = Json.obj("addressLine1" -> "address1", "addressLine2" -> "address2", "addressLine3" -> "address3",
        "addressLine4"->"address4", "countryCode" -> "IN")
      Json.toJson[Address](foreignAddress) mustEqual jsonResult
    }

    "return json for Foreign Address with postal code" in {
      val foreignAddress = ForeignAddress(addressLine1 = "address1",addressLine2 = Some("address2"), addressLine3 = Some("address3"),
        addressLine4=Some("address4"),postalCode=Some("test post code"),countryCode = "IN")

      val jsonResult= Json.obj("addressLine1" -> "address1", "addressLine2" -> "address2", "addressLine3" -> "address3",
        "addressLine4"->"address4", "postalCode"->"test post code", "countryCode" -> "IN")

      Json.toJson[Address](foreignAddress) mustEqual jsonResult
    }
  }
}
