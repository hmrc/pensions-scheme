/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class AppConfig @Inject()(configuration: Configuration, servicesConfig: ServicesConfig) {
  lazy val underlying: Config = configuration.underlying
  lazy val defaultDataExpireAfterDays: Int = underlying.getInt("defaultDataExpireInDays")
  lazy val ifURL: String = servicesConfig.baseUrl(serviceName = "if-hod")
  lazy val barsBaseUrl: String = servicesConfig.baseUrl("bank-account-reputation")
  lazy val appName: String = underlying.getString("appName")

  lazy val integrationframeworkEnvironment: String = configuration.getOptional[String](
    path = "microservice.services.if-hod.env").getOrElse("local")
  lazy val integrationframeworkAuthorization: String = "Bearer " + configuration.getOptional[String](
    path = "microservice.services.if-hod.authorizationToken").getOrElse("local")

  lazy val schemeRegistrationIFUrl: String = s"$ifURL${underlying.getString("serviceUrls.if.scheme.register")}"
  lazy val listOfSchemesUrl: String = s"$ifURL${underlying.getString("serviceUrls.if.list.of.schemes")}"
  lazy val schemeDetailsUrl: String = s"$ifURL${underlying.getString("serviceUrls.if.scheme.details")}"
  lazy val pspSchemeDetailsUrl: String = s"$ifURL${underlying.getString("serviceUrls.if.psp.scheme.details")}"
  lazy val updateSchemeUrl: String = s"$ifURL${underlying.getString("serviceUrls.if.update.scheme")}"
}
