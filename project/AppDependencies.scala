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
    "uk.gov.hmrc"             %% "simple-reactivemongo"       % "8.0.0-play-28",
    "com.typesafe.play"       %% "play-json"                  % "2.9.2",
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"  % "5.16.0",
    "com.networknt"           %  "json-schema-validator"      % "1.0.3",
    "com.eclipsesource"       %% "play-json-schema-validator" % "0.9.4",
    "uk.gov.hmrc"             %% "domain"                     % "6.2.0-play-28",
    ehcache
  )

  def test(scope: String = "test, it"): Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                 %% "bootstrap-test-play-28"       % "5.16.0"            % Test,
    "uk.gov.hmrc"                 %% "reactivemongo-test"           % "5.0.0-play-28"     % Test,
    "com.github.simplyscala"      %% "scalatest-embedmongo"         % "0.2.4"             % Test,
    "org.pegdown"                  % "pegdown"                      % "1.6.0"             % scope,
    "org.scalacheck"              %% "scalacheck"                   % "1.15.2"            % scope,
    "com.typesafe.play"           %% "play-test"                    % PlayVersion.current % scope,
    "com.github.tomakehurst"       % "wiremock-jre8"                % "2.26.0"            % scope,
    "wolfendale"                  %% "scalacheck-gen-regexp"        % "0.1.1"             % scope,
    "org.scalatestplus"           %% "scalatestplus-scalacheck"     % "3.1.0.0-RC2"       % "test",
    "org.mockito"                  % "mockito-core"                 % "4.0.0"             % "test",
    "org.mockito"                 %% "mockito-scala"                % "1.16.42"           % "test",
    "org.mockito"                 %% "mockito-scala-scalatest"      % "1.16.42"           % "test",
  )
}
