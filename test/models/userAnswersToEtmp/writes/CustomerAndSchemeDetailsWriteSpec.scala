/*
 * Copyright 2020 HM Revenue & Customs
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

package models.userAnswersToEtmp.writes

import models.enumeration.{Benefits, SchemeType}
import models.userAnswersToEtmp.CustomerAndSchemeDetails
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.Json
import utils.{PensionSchemeGenerators, SchemaValidatorForTests}

class CustomerAndSchemeDetailsWriteSpec extends WordSpec with MustMatchers with OptionValues with PensionSchemeGenerators with SchemaValidatorForTests {

  "CustomerAndSchemeDetails object" should {

    "map correctly to an update payload for schemeDetails API 1468" when {

      "validate schemeDetails write with schema" in {

        forAll(CustomerAndSchemeDetailsGen) {
          schemeDetails => {

            val mappedSchemeDetails = Json.toJson(schemeDetails)(CustomerAndSchemeDetails.updateWrites(psaid = "A0012221"))
            val valid = Json.obj("schemeDetails" -> mappedSchemeDetails)

            val result = validateJson(elementToValidate = valid,
              schemaFileName = "api1468_schema.json",
              schemaNodePath = "#/properties/schemeDetails")

            result.isSuccess mustBe true
          }
        }
      }

      "invalidate schemeDetails write with schema" in {

        val details = CustomerAndSchemeDetails(
          schemeName = "test-pensions-scheme",
          isSchemeMasterTrust = false,
          schemeStructure = Some(SchemeType.single.value),
          currentSchemeMembers = "INVALID",
          futureSchemeMembers = "INVALID",
          isReguledSchemeInvestment = false,
          isOccupationalPensionScheme = false,
          areBenefitsSecuredContractInsuranceCompany = false,
          doesSchemeProvideBenefits = Benefits.opt1.value,
          schemeEstablishedCountry = "INVALID",
          haveInvalidBank = false
        )

        val mappedSchemeDetails = Json.toJson(details)(CustomerAndSchemeDetails.updateWrites(psaid = "INVALID"))
        val valid = Json.obj("schemeDetails" -> mappedSchemeDetails)

        val result = validateJson(elementToValidate = valid,
          schemaFileName = "api1468_schema.json",
          schemaNodePath = "#/properties/schemeDetails")

        result.isError mustBe true
        result.asEither.left.toOption.toSeq.flatten.size mustBe 4
      }
    }
  }
}
