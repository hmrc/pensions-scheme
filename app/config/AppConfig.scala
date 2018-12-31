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

package config

import com.google.inject.Inject
import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.ServicesConfig

class AppConfig @Inject()(override val runModeConfiguration: Configuration, environment: Environment) extends ServicesConfig {
  override protected def mode: Mode = environment.mode

  lazy val baseURL: String = baseUrl("des-hod")
  lazy val barsBaseUrl: String = baseUrl("bank-account-reputation")
  lazy val appName: String = runModeConfiguration.underlying.getString("appName")

  lazy val schemeRegistrationUrl: String = s"$baseURL${runModeConfiguration.underlying.getString("serviceUrls.scheme.register")}"
  lazy val listOfSchemesUrl: String = s"$baseURL${runModeConfiguration.underlying.getString("serviceUrls.list.of.schemes")}"
  lazy val schemeDetailsUrl: String = s"$baseURL${runModeConfiguration.underlying.getString("serviceUrls.scheme.details")}"
  lazy val desEnvironment: String = runModeConfiguration.getString("microservice.services.des-hod.env").getOrElse("local")
  lazy val authorization: String = "Bearer " + runModeConfiguration.getString("microservice.services.des-hod.authorizationToken").getOrElse("local")
}
