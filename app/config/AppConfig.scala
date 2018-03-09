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
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.ServicesConfig

class AppConfig @Inject()(override val runModeConfiguration: Configuration, environment: Environment) extends ServicesConfig {
  override protected def mode = environment.mode

  lazy val baseURL: String = baseUrl("des-hod")

  lazy val schemeRegistrationUrl: String = s"$baseURL${runModeConfiguration.underlying.getString("serviceUrls.scheme.register")}"
  lazy val schemeAdminRegistrationUrl: String = s"$baseURL${runModeConfiguration.underlying.getString("serviceUrls.scheme.administrator.register")}"
  lazy val registerWithoutIdOrganisationUrl:String=s"$baseURL${runModeConfiguration.underlying.getString("serviceUrls.register.without.id.organisation")}"
  lazy val registerWithIdIndividualUrl: String = s"$baseURL${runModeConfiguration.underlying.getString("serviceUrls.register.with.id.individual")}"
  lazy val registerWithIdOrganisationUrl: String = s"$baseURL${runModeConfiguration.underlying.getString("serviceUrls.register.with.id.organisation")}"
  lazy val listOfSchemesUrl: String = s"$baseURL${runModeConfiguration.underlying.getString("serviceUrls.list.of.schemes")}"
  lazy val desEnvironment: String = runModeConfiguration.getString(s"$services.des-hod.env").getOrElse("local")
  lazy val authorization: String = "Bearer " + runModeConfiguration.getString(s"$services.des-hod.authorizationToken").getOrElse("local")
}
