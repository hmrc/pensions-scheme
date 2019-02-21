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
import models.jsonTransformations.{AddressTransformer, DirectorsOrPartnersTransformer, EstablisherDetailsTransformer}
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._
import utils.PensionSchemeJsValueGenerators


class SchemeSubscriptionDetailsTransformerSpec extends WordSpec with MustMatchers with OptionValues with JsonFileReader with PensionSchemeJsValueGenerators {

  private val addressTransformer = new AddressTransformer()
  private val directorOrPartnerTransformer = new DirectorsOrPartnersTransformer(addressTransformer)
  private val transformer = new EstablisherDetailsTransformer(addressTransformer, directorOrPartnerTransformer)

  "A DES payload with full scheme subscription details " must {
    "have the details transformed correctly to valid user answers format" which {

      s"uses generators " in {
        forAll(getSchemeDetailsGen) {
          schemeDetails => {
            val (desScheme, uaScheme) = schemeDetails

            //println("\n\n\n desScheme : "+desScheme)
          }
        }
      }
    }
  }
}
