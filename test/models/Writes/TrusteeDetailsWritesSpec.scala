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
import models.{EstablisherDetails, TrusteeDetails}
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsValue, Json}
import utils.PensionSchemeGenerators

class TrusteeDetailsWritesSpec extends WordSpec with MustMatchers with OptionValues with PensionSchemeGenerators {

  "An trustee details object" should {

    "map correctly to an update payload for for trusteeDetailsType API 1468" when {

      "validate trusteeDetails write with schema" in {
        forAll(trusteeDetailsGen) {
          trustee => {

            val rootSchema = JsonSource.schemaFromUrl(getClass.getResource("/schemas/api1468_schema.json")).get

            val validator = SchemaValidator().addSchema("/schemas/api1468_schema.json", rootSchema)

            val schema = JsonSource.schemaFromString(
              """{
                |  "additionalProperties": {
                |  "$ref": "/schemas/api1468_schema.json#/properties/establisherAndTrustDetailsType/trusteeDetailsType" }
                |}""".stripMargin).get

            val mappedTrustee: JsValue = Json.toJson(trustee)(TrusteeDetails.updateWrites)

            val valid = Json.obj("trusteeDetailsType" -> mappedTrustee)

            validator.validate(schema, valid).isSuccess mustBe true
          }
        }
      }

      "invalidate companyTrusteeDetails write with schema when" in {

        forAll(trusteeDetailsGen) {
          trustee => {

            val localEstablisher = if(trustee.individualTrusteeDetail.nonEmpty) {
              trustee.copy(individualTrusteeDetail = Seq(trustee.individualTrusteeDetail(0).copy(utr = Some("12313123213123123123"))))
            } else if (trustee.companyTrusteeDetail.nonEmpty) {
              trustee.copy(companyTrusteeDetail = Seq(trustee.companyTrusteeDetail(0).copy(utr = Some("12313123213123123123"))))
            } else if (trustee.partnershipTrusteeDetail.nonEmpty) {
              trustee.copy(partnershipTrusteeDetail = Seq(trustee.partnershipTrusteeDetail(0).copy(utr = Some("12313123213123123123"))))
            } else trustee.copy(individualTrusteeDetail = Seq(individualGen.sample.get.copy(utr = Some("12313123213123123123"))))

            val mappedTrustee: JsValue = Json.toJson(localEstablisher)(TrusteeDetails.updateWrites)

            val rootSchema = JsonSource.schemaFromUrl(getClass.getResource("/schemas/api1468_schema.json")).get

            val validator = SchemaValidator().addSchema("/schemas/api1468_schema.json", rootSchema)

            val schema = JsonSource.schemaFromString(
              """{
                |  "additionalProperties": {
                |  "$ref": "/schemas/api1468_schema.json#/properties/establisherAndTrustDetailsType/trusteeDetailsType" }
                |}""".stripMargin).get

            val inValid = Json.obj("trusteeDetailsType" -> mappedTrustee)

            validator.validate(schema, inValid).isError mustBe true
          }
        }
      }
    }
  }
}
