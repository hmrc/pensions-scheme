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
  private val desSchemeDetailsJsValue: JsValue = readJsonFromFile("/data/validGetSchemeDetails.json")

  private val desPersonDetailsArrayElementReads: Reads[JsArray] = (__ \ 'psaSchemeDetails \ 'establisherDetails \ 'individualDetails).json.pick[JsArray]
  private val desPersonDetailsElementReads: Reads[JsValue] = (__ \ 'personDetails).json.pick

  private val transformer = new EstablisherDetailsTransformer()

  private val desIndividualDetailsSeqJsValue = desSchemeDetailsJsValue.transform(desPersonDetailsArrayElementReads).asOpt.get.value

  "A DES payload containing establisher details" must {
    "Map correctly to valid establisher details in user answers" when {
      "We have individual person details title" in {
        desIndividualDetailsSeqJsValue.map {
          desIndividualDetailsJsValue =>
            val desPersonDetailsElementJsValue = desIndividualDetailsJsValue.transform(desPersonDetailsElementReads).asOpt.get

            val actual = desPersonDetailsElementJsValue
              .transform(transformer.transformPersonDetailsToUserAnswersReads).asOpt.get


            (actual \ "establisherDetails" \ "firstName").as[String] mustBe (desIndividualDetailsJsValue \ "personDetails" \ "firstName").as[String]
            (actual \ "establisherDetails" \ "middleName").asOpt[String] mustBe (desIndividualDetailsJsValue \ "personDetails" \ "middleName").asOpt[String]
            (actual \ "establisherDetails" \ "lastName").as[String] mustBe (desIndividualDetailsJsValue \ "personDetails" \ "lastName").as[String]
            (actual \ "establisherDetails" \ "date").as[String] mustBe (desIndividualDetailsJsValue \ "personDetails" \ "dateOfBirth").as[String]
        }
      }
    }
  }
}
