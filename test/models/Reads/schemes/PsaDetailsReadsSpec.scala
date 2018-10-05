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

import models.schemes.PsaDetails
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._


class PsaDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues {

  "A JSON payload containing psa details" should {
    "read into a valid PSA Details object " when {
      val actualOutput = Json.obj("psaid" -> "2432374232", "organizationOrPartnershipName" -> "org name test",
        "firstName" -> "Mickey", "middleName" -> "m", "lastName" -> "Mouse")

      "we have a psa id" in {
        actualOutput.as(PsaDetails.apiReads).id mustBe (actualOutput \ "psaid").as[String]
      }

      "we have an organisation name" in {
        actualOutput.as(PsaDetails.apiReads).organisationOrPartnershipName.value mustBe (actualOutput \ "organizationOrPartnershipName").as[String]
      }

      "we don't have an organisation name" in {
        val inputWithoutOrg = actualOutput - "organizationOrPartnershipName"
        inputWithoutOrg.as(PsaDetails.apiReads).organisationOrPartnershipName mustBe None
      }

      "we have a firstName" in {
        actualOutput.as(PsaDetails.apiReads).individual.value.firstName.value mustBe (actualOutput \ "firstName").as[String]
      }

      "we don't have a firstName" in {
        val inputWithoutFirstName = actualOutput - "firstName"

        inputWithoutFirstName.as(PsaDetails.apiReads).individual.value.firstName mustBe None
      }

      "we have a middleName" in {
        actualOutput.as(PsaDetails.apiReads).individual.value.middleName.value mustBe (actualOutput \ "middleName").as[String]
      }

      "we don't have a middleName" in {
        val inputWithoutMiddleName = actualOutput - "middleName"

        inputWithoutMiddleName.as(PsaDetails.apiReads).individual.value.middleName mustBe None
      }

      "we have a lastName" in {
        actualOutput.as(PsaDetails.apiReads).individual.value.lastName.value mustBe (actualOutput \ "lastName").as[String]
      }

      "we don't have a lastName" in {
        val inputWithoutLastName = actualOutput - "lastName"

        inputWithoutLastName.as(PsaDetails.apiReads).individual.value.lastName mustBe None
      }

      "we don't have an individual" in {
        val inputWithoutLastName = actualOutput - "firstName" - "middleName" - "lastName"

        inputWithoutLastName.as(PsaDetails.apiReads).individual mustBe None
      }
    }
  }
}
