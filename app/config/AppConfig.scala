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

package config

import com.google.inject.Inject
import com.typesafe.config.Config
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class AppConfig @Inject()(runModeConfiguration: Configuration, environment: Environment, servicesConfig: ServicesConfig) {
  lazy val underlying: Config = runModeConfiguration.underlying
  lazy val defaultDataExpireAfterDays: Int = underlying.getInt("defaultDataExpireInDays")
  lazy val baseURL: String = servicesConfig.baseUrl("des-hod")
  lazy val barsBaseUrl: String = servicesConfig.baseUrl("bank-account-reputation")
  lazy val appName: String = underlying.getString("appName")

  lazy val schemeRegistrationUrl: String = s"$baseURL${underlying.getString("serviceUrls.scheme.register")}"
  lazy val listOfSchemesUrl: String = s"$baseURL${underlying.getString("serviceUrls.list.of.schemes")}"
  lazy val schemeDetailsUrl: String = s"$baseURL${underlying.getString("serviceUrls.scheme.details")}"
  lazy val updateSchemeUrl: String = s"$baseURL${underlying.getString("serviceUrls.update.scheme")}"
  lazy val desEnvironment: String = runModeConfiguration.getOptional[String]("microservice.services.des-hod.env").getOrElse("local")
  lazy val authorization: String = "Bearer " + runModeConfiguration.getOptional[String]("microservice.services.des-hod.authorizationToken").getOrElse("local")
}
