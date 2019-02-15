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

import com.eclipsesource.schema.{JsonSource, SchemaValidator}
import models.EstablisherDetails
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsValue, Json}
import utils.PensionSchemeGenerators

class EstablisherDetailsWritesSpec extends WordSpec with MustMatchers with OptionValues with PensionSchemeGenerators {

  "An establisher details object" should {

    "map correctly to an update payload for for establisherDetails API 1468" when {

      "validate establisherDetails write with schema" in {
        forAll(establisherDetailsGen) {
          establisher => {

            val rootSchema = JsonSource.schemaFromUrl(getClass.getResource("/schemas/api1468_schema.json")).get

            val validator = SchemaValidator().addSchema("/schemas/api1468_schema.json", rootSchema)

            val schema = JsonSource.schemaFromString(
              """{
                |  "additionalProperties": {
                |  "$ref": "/schemas/api1468_schema.json#/properties/establisherAndTrustDetailsType/establisherDetails" }
                |}""".stripMargin).get

            val mappedEstablisher: JsValue = Json.toJson(establisher)(EstablisherDetails.updateWrites)

            val valid = Json.obj("establisherDetails" -> mappedEstablisher)

            validator.validate(schema, valid).isSuccess mustBe true
          }
        }
      }

      "invalidate companyTrusteeDetails write with schema when" in {

        forAll(establisherDetailsGen) {
          establisher => {

            val localEstablisher = if(establisher.individual.nonEmpty) {
              establisher.copy(individual = Seq(establisher.individual(0).copy(utr = Some("12313123213123123123"))))
            } else if (establisher.companyOrOrganization.nonEmpty) {
              establisher.copy(companyOrOrganization = Seq(establisher.companyOrOrganization(0).copy(utr = Some("12313123213123123123"))))
            } else if (establisher.partnership.nonEmpty) {
              establisher.copy(partnership = Seq(establisher.partnership(0).copy(utr = Some("12313123213123123123"))))
            } else establisher.copy(individual = Seq(individualGen.sample.get.copy(utr = Some("12313123213123123123"))))

            val mappedEstablisher: JsValue = Json.toJson(localEstablisher)(EstablisherDetails.updateWrites)

            val rootSchema = JsonSource.schemaFromUrl(getClass.getResource("/schemas/api1468_schema.json")).get

            val validator = SchemaValidator().addSchema("/schemas/api1468_schema.json", rootSchema)

            val schema = JsonSource.schemaFromString(
              """{
                |  "additionalProperties": {
                |  "$ref": "/schemas/api1468_schema.json#/properties/establisherAndTrustDetailsType/establisherDetails" }
                |}""".stripMargin).get

            val inValid = Json.obj("establisherDetails" -> mappedEstablisher)

            validator.validate(schema, inValid).isError mustBe true
          }
        }
      }
    }
  }
}
