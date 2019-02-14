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

package JsonTransformations

import base.JsonFileReader
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._
import utils.JsonTransformations.EstablisherDetailsTransformer


class EstablisherDetailsTransformerSpec extends WordSpec with MustMatchers with OptionValues with JsonFileReader {
  private val desSchemeDetailsJsValue1: JsValue = readJsonFromFile("/data/validGetSchemeDetails1.json")
  private val desSchemeDetailsJsValue2: JsValue = readJsonFromFile("/data/validGetSchemeDetails2.json")

  private val desPersonDetailsArrayElementReads: Reads[JsArray] = (__ \ 'psaSchemeDetails \ 'establisherDetails \ 'individualDetails).json.pick[JsArray]
  // private val desPersonDetailsElementReads: Reads[JsValue] = (__ \ 'personDetails).json.pick
  private val IndividualDetailsElementReads: Reads[JsValue] = (__).json.pick

  private val transformer = new EstablisherDetailsTransformer()

  private def desIndividualDetailsSeqJsValue(jsValue: JsValue) = jsValue.transform(desPersonDetailsArrayElementReads).asOpt.get.value

  "A DES payload containing establisher details" must {
    "Map correctly to valid establisher details in user answers for first json file" when {
      desIndividualDetailsSeqJsValue(desSchemeDetailsJsValue1).zipWithIndex.foreach {
        case (desIndividualDetailsJsValue, index) =>
          val desIndividualDetailsElementJsValue = desIndividualDetailsJsValue.transform(IndividualDetailsElementReads).asOpt.get
          s"we have establisher details for element $index in establishers array" in {

            val actual = desIndividualDetailsElementJsValue
              .transform(transformer.transformPersonDetailsToUserAnswersReads).asOpt.get


            (actual \ "establisherDetails" \ "firstName").as[String] mustBe (desIndividualDetailsJsValue \ "personDetails" \ "firstName").as[String]
            (actual \ "establisherDetails" \ "middleName").asOpt[String] mustBe (desIndividualDetailsJsValue \ "personDetails" \ "middleName").asOpt[String]
            (actual \ "establisherDetails" \ "lastName").as[String] mustBe (desIndividualDetailsJsValue \ "personDetails" \ "lastName").as[String]
            (actual \ "establisherDetails" \ "date").as[String] mustBe (desIndividualDetailsJsValue \ "personDetails" \ "dateOfBirth").as[String]
          }

          s"we have nino details for element $index in establishers array" in {

            val actual = desIndividualDetailsElementJsValue
              .transform(transformer.transformNinoDetailsToUserAnswersReads).asOpt.get

            (actual \ "establisherNino" \ "hasNino").as[Boolean] mustBe true
            (actual \ "establisherNino" \ "nino").asOpt[String] mustBe (desIndividualDetailsJsValue \ "nino").asOpt[String]
          }

          s"we have utr details for element $index in establishers array" in {

            val actual = desIndividualDetailsElementJsValue
              .transform(transformer.transformUtrDetailsToUserAnswersReads).asOpt.get

            (actual \ "uniqueTaxReference" \ "hasUtr").as[Boolean] mustBe true
            (actual \ "uniqueTaxReference" \ "utr").asOpt[String] mustBe (desIndividualDetailsJsValue \ "utr").asOpt[String]

          }

      }

    }


    "Map correctly to valid establisher details in user answers for second json file" when {
      desIndividualDetailsSeqJsValue(desSchemeDetailsJsValue2).zipWithIndex.foreach {
        case (desIndividualDetailsJsValue, index) =>
          val desIndividualDetailsElementJsValue = desIndividualDetailsJsValue.transform(IndividualDetailsElementReads).asOpt.get

          s"we have nino details for element $index in establishers array" in {

            val actual = desIndividualDetailsElementJsValue
              .transform(transformer.transformNinoDetailsToUserAnswersReads).asOpt.get

            (actual \ "establisherNino" \ "hasNino").as[Boolean] mustBe false
            (actual \ "establisherNino" \ "reason").asOpt[String] mustBe (desIndividualDetailsJsValue \ "noNinoReason").asOpt[String]
          }

          s"we have utr details for element $index in establishers array" in {

            val actual = desIndividualDetailsElementJsValue
              .transform(transformer.transformUtrDetailsToUserAnswersReads).asOpt.get

            (actual \ "uniqueTaxReference" \ "hasUtr").as[Boolean] mustBe false
            (actual \ "uniqueTaxReference" \ "reason").asOpt[String] mustBe (desIndividualDetailsJsValue \ "noUtrReason").asOpt[String]
          }

      }

    }

  }
}
