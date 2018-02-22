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
import models.Address._
import org.scalatest.{MustMatchers, WordSpecLike}
import play.api.libs.json.{JsValue, Json}

import scala.io.Source

class RegistrationNoIDAddress extends WordSpecLike with MustMatchers {
  def readJsonFromFile(filePath: String): JsValue = {
    val path = Source.fromURL(getClass.getResource(filePath)).mkString
    Json.parse(path)
  }

  "Reads for Address" must {
    "successfully read a UK address" in {
      val json = Json.obj("addressLine1" -> "myhouse",
        "addressLine2" -> "street1",
        "addressLine3" -> "street2",
        "addressLine4" -> "street3",
        "postalCode" -> "ZZ1 1ZZ",
        "countryCode" -> "GB")

      Json.fromJson[Address](json).get mustEqual UkAddress("myhouse", "street1", Some("street2"), Some("street3"), "ZZ1 1ZZ")
    }

    "successfully read a Foreign address" in {
      val json = Json.obj("addressLine1" -> "31 Myers Street",
        "addressLine2" -> "Haddonfield",
        "addressLine3" -> "Illinois",
        "addressLine4" -> "USA",
        "countryCode" -> "US")

      Json.fromJson[Address](json).get mustEqual ForeignAddress("31 Myers Street", "Haddonfield", Some("Illinois"), Some("USA"), None, "US")

    }
  }

  "Writes for Address" must {
    "successfully write a UK address to JSON" in {
      val json = Json.obj("addressLine1" -> "myhouse", "addressLine2" -> "street1", "addressLine3" -> "street2", "addressLine4" -> "street3", "postalCode" -> "ZZ1 1ZZ", "countryCode" -> "GB")
      val address = UkAddress("myhouse", "street1", Some("street2"), Some("street3"), "ZZ1 1ZZ")
      Json.toJson[Address](address) mustEqual json
    }

    "successfully write a Foreign address to JSON" in {
      val json = Json.obj("addressLine1" -> "myhouse", "addressLine2" -> "street1", "addressLine3" -> "street2", "addressLine4" -> "street3", "countryCode" -> "AB")
      val address = ForeignAddress("myhouse", "street1", Some("street2"), Some("street3"), None, "AB")
      Json.toJson[Address](address) mustEqual json
    }
  }

  "Reads for Registrant" must {
    "successfully read a OrganisationRegistrant" in {
      val json = readJsonFromFile("/data/validRegistrationNoIDOrganisation.json")

      val indentificationData = Some(IdentificationType(
        idNumber = "123456",
        issuingInstitution = "France Institution",
        issuingCountryCode = "FR")
      )

      val addressData = UkAddress(addressLine1 = "100, Sutton Street",
        addressLine2 = "Wokingham",
        addressLine3 = Some("Surrey"),
        addressLine4 = Some("London"),
        postalCode = "DH1 4EJ"
      )

      val organisationData = Organisation(organisationName = "John")

      val contactDetailsData = ContactDetailsType(
        phoneNumber = Some("01332752856"),
        mobileNumber = Some("07782565326"),
        faxNumber = Some("01332754256"),
        emailAddress = None
      )

      Json.fromJson[Registrant](json).get mustEqual OrganisationRegistrant(
        regime = "FHDDS",
        acknowledgementReference = "12345678901234567890123456789012",
        isAnAgent = false,
        isAGroup = false,
        identification = indentificationData,
        organisation = organisationData,
        address = addressData,
        contactDetails = contactDetailsData)
    }
  }

  "successfully read an IndividualRegistrant" in {
    val json= readJsonFromFile("/data/validRegistrationNoIDIndividual.json")
    val individualData = Individual(
      firstName = "John",
      None,
      lastName = "Smith",
      dateOfBirth = DateString("1990-04-03")
    )
    val indentificationData = Some(IdentificationType(
      idNumber = "123456",
      issuingInstitution = "France Institution",
      issuingCountryCode = "FR")
    )

    val addressData = UkAddress(addressLine1 = "100, Sutton Street",
      addressLine2 = "Wokingham",
      addressLine3 = Some("Surrey"),
      addressLine4 = Some("London"),
      postalCode = "DH1 4EJ"
    )

    val contactDetailsData = ContactDetailsType(
      phoneNumber = Some("01332752856"),
      mobileNumber = Some("07782565326"),
      faxNumber = Some("01332754256"),
      emailAddress = None
    )

    Json.fromJson[Registrant](json) mustEqual IndividualRegistrant(
      regime = "FHDDS",
      acknowledgementReference = "12345678901234567890123456789012",
      isAnAgent = false,
      isAGroup = false,
      individual = individualData,
      identification = indentificationData,
      address = addressData,
      contactDetails = contactDetailsData
    )
  }
}
