/*
 * Copyright 2022 HM Revenue & Customs
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

import models.userAnswersToEtmp.PensionsScheme
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.OptionValues
import play.api.libs.json.{JsValue, Json}
import utils.{PensionSchemeGenerators, SchemaValidatorForTests}

class EstablisherAndTrusteeTypeWritesSpec extends AnyWordSpec with Matchers with OptionValues with PensionSchemeGenerators with SchemaValidatorForTests {

  "An PensionsScheme object" should {

    "map correctly to an update payload for establisherAndTrustDetailsType API 1468" when {

      "validate establisherAndTrustDetailsType write with schema" in {

        forAll(establisherAndTrustDetailsGen) { element =>

          val mappedEstablisherAndTrustDetails: JsValue = Json.toJson(element)(PensionsScheme.updateWriteEstablisherAndTrustDetails)

          val valid = Json.obj("establisherAndTrustDetailsType" -> mappedEstablisherAndTrustDetails)

          validateJson(elementToValidate = valid,
            schemaFileName = "api1468_schema.json",
            schemaNodePath = "#/properties/establisherAndTrustDetailsType").isSuccess mustBe true
        }
      }

      "invalidate establisherAndTrustDetailsType write with schema for incorrect json" in {

        val element = establisherAndTrustDetailsNonEmpty.retryUntil(_.nonEmpty).sample.get.head
        val establisher = element._3
        val invalidEstablisher = establisher.copy(individual = Seq(individualGen.suchThat(
          _.personalDetails.dateOfBirth.nonEmpty).sample.get.copy(utr = Some("12313123213123123123"))))

        val mappedEstablisherAndTrustDetails: JsValue = Json.toJson((element._1, element._2, invalidEstablisher, element._4)
        )(PensionsScheme.updateWriteEstablisherAndTrustDetails)

        val valid = Json.obj("establisherAndTrustDetailsType" -> mappedEstablisherAndTrustDetails)

        validateJson(elementToValidate = valid,
          schemaFileName = "api1468_schema.json",
          schemaNodePath = "#/properties/establisherAndTrustDetailsType").isError mustBe true
      }
    }
  }
}
