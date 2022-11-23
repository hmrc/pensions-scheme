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

import play.sbt.PlayImport.{ehcache, ws}
import sbt._

object AppDependencies {

  val appName = "pensions-scheme"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"         % "0.74.0",
    "com.typesafe.play"       %% "play-json"                  % "2.9.3",
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"  % "7.12.0",
    "com.networknt"           %  "json-schema-validator"      % "1.0.73",
    "uk.gov.hmrc"             %% "domain"                     % "8.1.0-play-28",
    "com.typesafe.play"       %% "play-json-joda"             % "2.9.3",
    ehcache
  )

  def test(scope: String = "test, it"): Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                 %% "bootstrap-test-play-28"       % "7.12.0"            % Test,
    "uk.gov.hmrc.mongo"           %% "hmrc-mongo-test-play-28"      % "0.74.0"            % Test,
    "de.flapdoodle.embed"          % "de.flapdoodle.embed.mongo"    % "3.5.2"             % Test,
    "org.pegdown"                  % "pegdown"                      % "1.6.0"             % scope,
    "com.vladsch.flexmark"         % "flexmark-all"                 % "0.62.2"            % scope,
    "org.scalatest"               %% "scalatest"                    % "3.2.14"            % Test,
    "org.scalatestplus"           %% "scalacheck-1-17"              % "3.2.14.0"          % Test,
    "org.scalatestplus"           %% "mockito-4-6"                  % "3.2.14.0"          % Test,
    "org.scalatestplus.play"      %% "scalatestplus-play"           % "5.1.0"             % Test,
    "com.eclipsesource"           %% "play-json-schema-validator"   % "0.9.5"             % scope,
    "com.github.tomakehurst"       % "wiremock-jre8"                % "2.35.0"            % scope,
    "io.github.wolfendale"        %% "scalacheck-gen-regexp"        % "1.0.0"             % scope,
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"       % "2.14.0"            % scope
  )
}
