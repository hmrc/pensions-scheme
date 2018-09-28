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

package models.Reads.schemes

import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._
import play.api.libs.functional.syntax._


class PsaDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues {

  "A JSON payload containing psa details" should {
    "read into a valid PSA Details object " when {
      val input = Json.obj("psaid" -> "2432374232", "organizationOrPartnershipName" -> "org name test", "firstName" -> "Mickey", "middleName" -> "m", "lastName" -> "Mouse")

      "we have a psa id" in {
        input.as[PsaDetails].id mustBe (input \ "psaid").as[String]
      }

      "we have an organisation name" in {
        input.as[PsaDetails].organisationOrPartnershipName.value mustBe (input \ "organizationOrPartnershipName").as[String]
      }

      "we don't have an organisation name" in {
        val inputWithoutOrg = input - "organizationOrPartnershipName"
        inputWithoutOrg.as[PsaDetails].organisationOrPartnershipName mustBe None
      }

      "we have a firstName" in {
        input.as[PsaDetails].individual.value.firstName.value mustBe (input \ "firstName").as[String]
      }

      "we don't have a firstName" in {
        val inputWithoutFirstName = input - "firstName"

        inputWithoutFirstName.as[PsaDetails].individual.value.firstName mustBe None
      }

      "we have a middleName" in {
        input.as[PsaDetails].individual.value.middleName.value mustBe (input \ "middleName").as[String]
      }

      "we don't have a middleName" in {
        val inputWithoutMiddleName = input - "middleName"

        inputWithoutMiddleName.as[PsaDetails].individual.value.middleName mustBe None
      }

      "we have a lastName" in {
        input.as[PsaDetails].individual.value.lastName.value mustBe (input \ "lastName").as[String]
      }

      "we don't have a lastName" in {
        val inputWithoutLastName = input - "lastName"

        inputWithoutLastName.as[PsaDetails].individual.value.lastName mustBe None
      }

      "we don't have an individual" in {
        val inputWithoutLastName = input - "firstName" - "middleName" - "lastName"

        inputWithoutLastName.as[PsaDetails].individual mustBe None
      }
    }
  }

  case class PsaIndividual(firstName: Option[String], middleName: Option[String], lastName: Option[String])

  object PsaIndividual {
    implicit val formats : OFormat[PsaIndividual] = Json.format[PsaIndividual]
  }

  case class PsaDetails(id: String, organisationOrPartnershipName: Option[String], individual: Option[PsaIndividual])

  private def getPsaIndividual(firstName : Option[String], middleName : Option[String], lastName: Option[String]) : Option[PsaIndividual] = {
    (firstName,middleName,lastName) match {
      case (None,None,None) => None
      case  _ => Some(PsaIndividual(firstName,middleName,lastName))
    }
  }

  object PsaDetails {
    implicit val reads: Reads[PsaDetails] = (
      (JsPath \ "psaid").read[String] and
        (JsPath \ "organizationOrPartnershipName").readNullable[String] and
        (JsPath \ "firstName").readNullable[String] and
        (JsPath \ "middleName").readNullable[String] and
        (JsPath \ "lastName").readNullable[String]
    )((psaid,
       organizationOrPartnershipName,
       firstName,
       middleName,
       lastName) => PsaDetails(psaid, organizationOrPartnershipName, getPsaIndividual(firstName, middleName, lastName)))
    implicit val writes: Writes[PsaDetails] = Json.writes[PsaDetails]
  }

}
