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
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}

class PsaDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues with PSASchemeDetailsGenerator {

  "A JSON payload containing psa details" should {

    "read into a valid PSA Details object " when {

      "we have a psa id" in {
        forAll(psaDetailsGenerator()) { actualOutput =>
          actualOutput.as(PsaDetails.apiReads).id mustBe (actualOutput \ "psaid").as[String]
        }
      }

      "we have an organisation name" in {
        forAll(psaDetailsGenerator()) { actualOutput =>
          actualOutput.as(PsaDetails.apiReads).organisationOrPartnershipName mustBe (actualOutput \ "organizationOrPartnershipName").asOpt[String]
        }
      }

      "we don't have an organisation name" in {
        forAll(psaDetailsGenerator()) { actualOutput =>
          val inputWithoutOrg = actualOutput - "organizationOrPartnershipName"
          inputWithoutOrg.as(PsaDetails.apiReads).organisationOrPartnershipName mustBe None
        }
      }

      "we have a firstName" in {
        forAll(psaDetailsGenerator(enforceProperty = true)) { actualOutput =>
          actualOutput.as(PsaDetails.apiReads).individual.value.firstName mustBe (actualOutput \ "firstName").asOpt[String]
        }
      }

      "we don't have a firstName" in {
        forAll(psaDetailsGenerator(enforceProperty = true)) { actualOutput =>
          val inputWithoutFirstName = actualOutput - "firstName"

          inputWithoutFirstName.as(PsaDetails.apiReads).individual.value.firstName mustBe None
        }
      }

      "we have a middleName" in {
        forAll(psaDetailsGenerator(enforceProperty = true)) { actualOutput =>
          actualOutput.as(PsaDetails.apiReads).individual.value.middleName mustBe (actualOutput \ "middleName").asOpt[String]
        }
      }

      "we don't have a middleName" in {
        forAll(psaDetailsGenerator(enforceProperty = true)) { actualOutput =>
          val inputWithoutMiddleName = actualOutput - "middleName"

          inputWithoutMiddleName.as(PsaDetails.apiReads).individual.value.middleName mustBe None
        }
      }

      "we have a lastName" in {
        forAll(psaDetailsGenerator(enforceProperty = true)) { actualOutput =>
          actualOutput.as(PsaDetails.apiReads).individual.value.lastName mustBe (actualOutput \ "lastName").asOpt[String]
        }
      }

      "we don't have a lastName" in {
        forAll(psaDetailsGenerator(enforceProperty = true)) { actualOutput =>
          val inputWithoutLastName = actualOutput - "lastName"

          inputWithoutLastName.as(PsaDetails.apiReads).individual.value.lastName mustBe None
        }
      }

      "we don't have an individual" in {
        forAll(psaDetailsGenerator()) { actualOutput =>
          val inputWithoutLastName = actualOutput - "firstName" - "middleName" - "lastName"

          inputWithoutLastName.as(PsaDetails.apiReads).individual mustBe None
        }
      }
    }
  }
}
