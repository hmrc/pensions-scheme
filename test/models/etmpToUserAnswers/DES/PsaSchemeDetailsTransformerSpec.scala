/*
 * Copyright 2021 HM Revenue & Customs
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

package models.etmpToUserAnswers.DES

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.libs.json.JsValue

class PsaSchemeDetailsTransformerSpec extends TransformationSpec {

  private def addressTransformer = new AddressTransformer

  private def directorOrPartnerTransformer =
    new DirectorsOrPartnersTransformer(addressTransformer)

  private def schemeDetailsTransformer =
    new SchemeDetailsTransformer(addressTransformer)

  private def establisherTransformer =
    new EstablisherDetailsTransformer(addressTransformer, directorOrPartnerTransformer)

  private def trusteesTransformer =
    new TrusteeDetailsTransformer(addressTransformer)

  private def transformer = new SchemeSubscriptionDetailsTransformer(
    schemeDetailsTransformer, establisherTransformer,
    trusteesTransformer)

  private val desResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsResponseDES.json")
  private val userAnswersResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsUserAnswers.json")

  "A DES payload with full scheme subscription details " must {
    "have the details transformed correctly to valid user answers format" which {

      s"uses generators" in {
        forAll(getSchemeDetailsGen) {
          case (desScheme, uaScheme) =>

            val result = desScheme.transform(transformer.transformToUserAnswers).get
            result mustBe uaScheme
        }
      }

      s"uses request/response json" in {
        val result = desResponse.transform(transformer.transformToUserAnswers).get
        result mustBe userAnswersResponse
      }
    }
  }
}
