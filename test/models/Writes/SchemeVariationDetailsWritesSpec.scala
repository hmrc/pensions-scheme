/*
 * Copyright 2019 HM Revenue & Customs
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

package models.Writes

import models._
import models.enumeration.SchemeType
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsValue, Json}
import utils.{PensionSchemeGenerators, SchemaValidatorForTests}

class SchemeVariationDetailsWritesSpec extends WordSpec with MustMatchers with OptionValues with PensionSchemeGenerators with SchemaValidatorForTests {

  "An PensionsScheme object" should {

    "map correctly to an update payload for scheme variation details API 1468" when {

      "validate scheme variation details write with schema" in {

        val pensionsScheme = PensionsScheme(
          CustomerAndSchemeDetails(
            schemeName = "test-pensions-scheme",
            isSchemeMasterTrust = false,
            schemeStructure = Some(SchemeType.single.value),
            currentSchemeMembers = "test-current-scheme-members",
            futureSchemeMembers = "test-future-scheme-members",
            isReguledSchemeInvestment = false,
            isOccupationalPensionScheme = false,
            areBenefitsSecuredContractInsuranceCompany = false,
            doesSchemeProvideBenefits = "test-does-scheme-provide-benefits",
            schemeEstablishedCountry = "test-scheme-established-country",
            haveInvalidBank = false
          ),
          PensionSchemeDeclaration(
            box1 = false,
            box2 = false,
            box6 = false,
            box7 = false,
            box8 = false,
            box9 = false
          ),
          EstablisherDetails(
            Nil,
            Nil,
            Nil
          ),
          TrusteeDetails(
            Nil,
            Nil,
            Nil
          )
        )

        val mappedSchemeDetails: JsValue = Json.toJson(pensionsScheme)(PensionsScheme.updateWrite("A0123456"))

        validateJson(elementToValidate = mappedSchemeDetails, schemaFileName = "api1468_schema.json").isSuccess mustBe true
      }

      /*"invalidate establisherAndTrustDetailsType write with schema for incorrect json" in {

        forAll(establisherAndTrustDetailsGen) {
          element => {
            val establisher =  element._3
            val invalidEstablisher = establisher.copy(individual = Seq(individualGen.sample.get.copy(utr = Some("12313123213123123123"))))

            val mappedEstablisherAndTrustDetails: JsValue = Json.toJson((element._1, element._2, invalidEstablisher, element._4)
            )(PensionsScheme.updateWriteEstablisherAndTrustDetails)

            val valid = Json.obj("establisherAndTrustDetailsType" -> mappedEstablisherAndTrustDetails)

            validateJson(elementToValidate = valid,
              schemaFileName = "api1468_schema.json",
              schemaNodePath = "#/properties/establisherAndTrustDetailsType").isError mustBe true
          }
        }
      }*/
    }
  }
}
