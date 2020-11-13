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

package models.etmpToUseranswers.DES

import base.JsonFileReader
import org.scalatest.MustMatchers
import org.scalatest.OptionValues
import org.scalatest.WordSpec
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Configuration
import play.api.Environment
import utils.DESPensionSchemeJsValueGenerators

trait TransformationSpec extends WordSpec with MustMatchers with OptionValues with JsonFileReader with DESPensionSchemeJsValueGenerators {

  val injector: Injector = new GuiceApplicationBuilder().build().injector
  val config: Configuration = injector.instanceOf[Configuration]
  val environment: Environment = injector.instanceOf[Environment]
}
