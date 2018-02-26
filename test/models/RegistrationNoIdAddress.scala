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

import org.scalatest.{MustMatchers, WordSpecLike}
import play.api.libs.json.{JsValue, Json}

import scala.io.Source

class RegistrationNoIdAddress extends WordSpecLike with MustMatchers {

  def readJsonFromFile(filePath: String): JsValue = {
    val path = Source.fromURL(getClass.getResource(filePath)).mkString
    Json.parse(path)
  }


  "Reads for Registrant" must {

    val individualData = Individual(
      firstName = "John",
      None,
      lastName = "Smith",
      dateOfBirth = "1990-04-03"
    )

    val foreignAddress=ForeignAddress(
      "31 Myers Street",
      "Haddonfield",
      Some("Illinois"),
      Some("USA"),
      None, "US"
    )

    val identificationData = Some(IdentificationType(
      idNumber = "123456",
      issuingInstitution = "France Institution",
      issuingCountryCode = "FR")
    )


    val organisationData = Organisation(organisationName = "John")

    val contactDetailsData = ContactDetailsType(
      phoneNumber = Some("01332752856"),
      mobileNumber = Some("07782565326"),
      faxNumber = Some("01332754256"),
      emailAddress = None
    )

    val organisationRegistrantCaseClass = OrganisationRegistrant(
      regime = "FHDDS",
      acknowledgementReference = "12345678901234567890123456789012",
      isAnAgent = false,
      isAGroup = false,
      identification = identificationData,
      organisation = organisationData,
      address = foreignAddress,
      contactDetails = contactDetailsData
    )

    "successfully read a OrganisationRegistrant" in {
      val json = readJsonFromFile("/data/validRegistrationNoIDOrganisation.json")

      Json.fromJson[OrganisationRegistrant](json).get mustEqual organisationRegistrantCaseClass
    }

    "Writes for Registrant" must {

      "succesfully write a json schema from a Organisation Registrant" in {
        val json = readJsonFromFile("/data/validRegistrationNoIDOrganisation.json")
        Json.toJson[OrganisationRegistrant](organisationRegistrantCaseClass) mustEqual json
      }
    }
  }
}
