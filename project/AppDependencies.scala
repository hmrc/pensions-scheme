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

import play.core.PlayVersion
import play.sbt.PlayImport.{ehcache, ws}
import sbt._

object AppDependencies {

  val appName = "pensions-scheme"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"             %% "simple-reactivemongo"       % "8.0.0-play-27",
    "com.typesafe.play"       %% "play-json"                  % "2.6.10",
    "uk.gov.hmrc"             %% "bootstrap-backend-play-27"  % "5.3.0",
    "com.networknt"           %  "json-schema-validator"      % "1.0.3",
    "com.eclipsesource"       %% "play-json-schema-validator" % "0.9.4",
    "uk.gov.hmrc"             %% "domain"                     % "5.11.0-play-27",
    ehcache
  )

  def test(scope: String = "test, it"): Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                 %% "reactivemongo-test"           % "5.0.0-play-27"     % Test,
    "org.scalatest"               %% "scalatest"                    % "3.0.8"             % scope,
    "org.pegdown"                  % "pegdown"                      % "1.6.0"             % scope,
    "org.scalacheck"              %% "scalacheck"                   % "1.14.0"            % scope,
    "com.typesafe.play"           %% "play-test"                    % PlayVersion.current % scope,
    "org.scalatestplus.play"      %% "scalatestplus-play"           % "4.0.2"             % scope,
    "org.mockito"                  % "mockito-all"                  % "1.10.19"           % scope,
    "com.github.tomakehurst"       % "wiremock-jre8"                % "2.26.0"            % scope,
    "wolfendale"                  %% "scalacheck-gen-regexp"        % "0.1.1"             % scope
  )
}
