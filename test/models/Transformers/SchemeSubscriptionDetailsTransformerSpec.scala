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

package models.Transformers

import base.JsonFileReader
import models.jsonTransformations._
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.JsValue
import utils.{FakeFeatureSwitchManagementService, PensionSchemeJsValueGenerators}

class SchemeSubscriptionDetailsTransformerSpec extends WordSpec with MustMatchers with OptionValues with JsonFileReader with PensionSchemeJsValueGenerators {
  private def addressTransformer(isToggleOn: Boolean) =
    new AddressTransformer(FakeFeatureSwitchManagementService(isToggleOn))

  private def directorOrPartnerTransformer(isToggleOn: Boolean) =
    new DirectorsOrPartnersTransformer(addressTransformer(isToggleOn), FakeFeatureSwitchManagementService(isToggleOn))

  private def schemeDetailsTransformer(isToggleOn: Boolean) =
    new SchemeDetailsTransformer(addressTransformer(isToggleOn), FakeFeatureSwitchManagementService(isToggleOn))

  private def establisherTransformer(isToggleOn: Boolean) =
    new EstablisherDetailsTransformer(addressTransformer(isToggleOn), directorOrPartnerTransformer(isToggleOn), FakeFeatureSwitchManagementService(isToggleOn))

  private def trusteesTransformer(isToggleOn: Boolean) =
    new TrusteeDetailsTransformer(addressTransformer(isToggleOn), FakeFeatureSwitchManagementService(isToggleOn))

  private def transformer(isToggleOn: Boolean = false) = new SchemeSubscriptionDetailsTransformer(
    schemeDetailsTransformer(isToggleOn), establisherTransformer(isToggleOn),
    trusteesTransformer(isToggleOn), FakeFeatureSwitchManagementService(isToggleOn))

  private val desResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsResponse.json")
  private val userAnswersResponseToggleOff: JsValue = readJsonFromFile("/data/validGetSchemeDetailsUserAnswersToggleOff.json")
  private val userAnswersResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsUserAnswers.json")

  "A DES payload with full scheme subscription details " must {
    "have the details transformed correctly to valid user answers format" which {

      s"uses generators when toggle(separate-ref-collection) is on" in {
        forAll(getSchemeDetailsGen(isToggleOn = true)) {
          schemeDetails => {
            val (desScheme, uaScheme) = schemeDetails

            val result = desScheme.transform(transformer(isToggleOn = true).transformToUserAnswers).get
            result mustBe uaScheme
          }
        }
      }

      s"uses generators when toggle(separate-ref-collection) is off" in {
        forAll(getSchemeDetailsGen()) {
          schemeDetails => {
            val (desScheme, uaScheme) = schemeDetails

            val result = desScheme.transform(transformer().transformToUserAnswers).get
            result mustBe uaScheme
          }
        }
      }

      s"uses request/response json when toggle(separate-ref-collection) is on" in {
        val result = desResponse.transform(transformer(true).transformToUserAnswers).get
        result mustBe userAnswersResponse
      }

      s"uses request/response json when toggle(separate-ref-collection) is off" in {
        val result = desResponse.transform(transformer().transformToUserAnswers).get
        result mustBe userAnswersResponseToggleOff
      }
    }
  }
}
