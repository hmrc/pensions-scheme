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

package models.etmpToUseranswers

import base.JsonFileReader
import config.FeatureSwitchManagementServiceTestImpl
import models.etmpToUserAnswers._
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsValue
import play.api.{Configuration, Environment}
import utils.PensionSchemeJsValueGenerators

class SchemeSubscriptionDetailsTransformerSpec extends TransformationSpec {

  private def addressTransformer = new AddressTransformer(fs)

  private def directorOrPartnerTransformer =
    new DirectorsOrPartnersTransformer(addressTransformer, fs)

  private def schemeDetailsTransformer =
    new SchemeDetailsTransformer(addressTransformer, fs)

  private def establisherTransformer =
    new EstablisherDetailsTransformer(addressTransformer, directorOrPartnerTransformer, fs)

  private def trusteesTransformer =
    new TrusteeDetailsTransformer(addressTransformer, fs)

  private def transformer = new SchemeSubscriptionDetailsTransformer(
    schemeDetailsTransformer, establisherTransformer,
    trusteesTransformer)

  private val desResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsResponse.json")
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
