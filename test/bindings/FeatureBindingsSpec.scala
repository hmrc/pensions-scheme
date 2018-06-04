/*
 * Copyright 2018 HM Revenue & Customs
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

package bindings

import org.scalatest.{FlatSpec, Matchers}
import play.api.inject.guice.GuiceApplicationBuilder
import service.{SchemeService, SchemeServiceV1, SchemeServiceV2}

class FeatureBindingsSpec extends FlatSpec with Matchers {

  "SchemeService" should "bind to SchemeServiceV1 when feature.registerSchemeJsonVersion is 'v1'" in {

    val app =
      new GuiceApplicationBuilder()
        .configure("feature.registerSchemeJsonVersion" -> "v1")
        .build()

    val schemeService = app.injector.instanceOf[SchemeService]

    schemeService shouldBe a[SchemeServiceV1]

  }

  it should "bind to SchemeServiceV2 when feature.registerSchemeJsonVersion is 'v2'" in {

    val app =
      new GuiceApplicationBuilder()
        .configure("feature.registerSchemeJsonVersion" -> "v2")
        .build()

    val schemeService = app.injector.instanceOf[SchemeService]

    schemeService shouldBe a[SchemeServiceV2]

  }

}
