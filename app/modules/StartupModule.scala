/*
 * Copyright 2025 HM Revenue & Customs
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

package modules

import com.google.inject.{AbstractModule, Inject}
import play.api.{Configuration, Environment}
import service.DropLockEntriesService

import scala.annotation.nowarn

@nowarn("msg=unused explicit parameter")
class StartupModule @Inject() (environment: Environment, configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = {
    if (configuration.underlying.getBoolean("mongodb.drop.collection.scheme_variation_lock.enable")) bind(classOf[DropLockEntriesService]).asEagerSingleton()
  }
}
