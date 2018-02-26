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

    val foreignAddress = ForeignAddress(
      "31 Myers Street",
      "Haddonfield",
      Some("Illinois"),
      Some("USA"),
      None, "US"
    )

    val organisationData = Organisation(organisationName = "John")

    val contactDetailsData = ContactDetailsType(
      phoneNumber = Some("01332752856"),
      emailAddress = None
    )

    val organisationRegistrantCaseClass = OrganisationRegistrant(
      acknowledgementReference = "12345678901234567890123456789012",
      organisation = organisationData,
      address = foreignAddress,
      contactDetails = contactDetailsData
    )

    "successfully read a OrganisationRegistrant" in {
      val json = readJsonFromFile("/data/validRegistrationNoIDOrganisationFE.json")

      Json.fromJson[OrganisationRegistrant](json).get mustEqual organisationRegistrantCaseClass
    }

    "Writes for Registrant" must {

      "succesfully write a json schema from a Organisation Registrant" in {
        val json = readJsonFromFile("/data/validRegistrationNoIDOrganisationToEMTP.json")
        Json.toJson[OrganisationRegistrant](organisationRegistrantCaseClass) mustEqual json
      }
    }

    "succesfully read an input Json and convert to an Output json" in {
      val inputJson = readJsonFromFile("/data/validRegistrationNoIDOrganisationFE.json")
      val outputJson = readJsonFromFile("/data/validRegistrationNoIDOrganisationToEMTP.json")

      val caseClass = Json.fromJson[OrganisationRegistrant](inputJson).get
      caseClass mustEqual organisationRegistrantCaseClass
      Json.toJson[OrganisationRegistrant](caseClass) mustEqual outputJson
    }
  }
}