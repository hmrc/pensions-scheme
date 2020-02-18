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

import sbt._
import play.sbt.PlayImport.{ws, _}
import play.core.PlayVersion

object AppDependencies {

  val appName = "pensions-scheme"

  val compile: Seq[ModuleID] = Seq(
      ws,
    "uk.gov.hmrc"             %% "simple-reactivemongo"       % "7.23.0-play-26",
    "com.typesafe.play"       %% "play-json"                  % "2.6.10",
    "com.typesafe.play"       %% "play-json-joda"             % "2.6.10",
    "uk.gov.hmrc"             %% "bootstrap-play-26"          % "1.3.0",
    "com.networknt"           %  "json-schema-validator"      % "1.0.3",
    "com.eclipsesource"       %% "play-json-schema-validator" % "0.9.4",
    "com.josephpconley"       %% "play-jsonpath"              % "2.6.0",
    "uk.gov.hmrc"             %% "domain"                     % "5.6.0-play-26"
  )

  def test(scope: String = "test,it"): Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                 %% "bootstrap-play-26"            % "1.3.0" % Test classifier "tests",
    "uk.gov.hmrc"                 %% "reactivemongo-test"           % "4.15.0-play-26" % Test,
    "uk.gov.hmrc"                 %% "hmrctest"                     % "3.9.0-play-26" % scope,
    "org.scalatest"               %% "scalatest"                    % "3.0.5" % scope,
    "org.pegdown"                  % "pegdown"                      % "1.6.0" % scope,
    "org.scalacheck"              %% "scalacheck"                   % "1.14.0" % scope,
    "com.typesafe.play"           %% "play-test"                    % PlayVersion.current % scope,
    "org.scalatestplus.play"      %% "scalatestplus-play"           % "3.1.2" % scope,
    "org.mockito"                  % "mockito-all"                  % "1.10.19" % scope,
    "com.github.tomakehurst"       % "wiremock"                     % "2.21.0" % scope,
    "wolfendale"                  %% "scalacheck-gen-regexp"        % "0.1.1" % scope
  )

  // Fixes a transitive dependency clash between wiremock and scalatestplus-play
  val overrides: Set[ModuleID] = {
    val jettyFromWiremockVersion = "9.2.24.v20180105"
    Set(
      "org.eclipse.jetty"           % "jetty-client"       % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-continuation" % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-http"         % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-io"           % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-security"     % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-server"       % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-servlet"      % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-servlets"     % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-util"         % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-webapp"       % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-xml"          % jettyFromWiremockVersion,
      "org.eclipse.jetty.websocket" % "websocket-api"      % jettyFromWiremockVersion,
      "org.eclipse.jetty.websocket" % "websocket-client"   % jettyFromWiremockVersion,
      "org.eclipse.jetty.websocket" % "websocket-common"   % jettyFromWiremockVersion
    )
  }
}
