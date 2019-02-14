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

  private val desPersonDetailsArrayElementReads: Reads[JsArray] = (__ \ 'psaSchemeDetails \ 'establisherDetails \ 'individualDetails).json.pick[JsArray]
  private val desPersonDetailsElementReads: Reads[JsValue] = (__ \ 'personDetails).json.pick
  private val desPersonDetailsTitleReads: JsPath = __ \ 'title

  val transformer = new EstablisherDetailsTransformer()

  "A DES payload containing establisher details" must {
    "Map correctly to valid establisher details in user answers" when {
      "We have individual person details title" in {
        desSchemeDetails.transform(desPersonDetailsArrayElementReads).asOpt.get.value
          .foreach { desPersonDetails =>
            val pd = desPersonDetails.transform(desPersonDetailsElementReads).asOpt.get
            println(">>>> Person details:" + pd)
            val expected = pd.transform(desPersonDetailsTitleReads.json.pick).asOpt.map( _.validate[String].asOpt.get)
            desPersonDetails.transform(transformer.transformToUserAnswersReads).asOpt mustBe expected
          }

        true mustBe true
      }
    }
  }

  private val desSchemeDetails: JsValue = readJsonFromFile("/data/validGetSchemeDetails.json")
}
