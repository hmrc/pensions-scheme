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
  private val playVersion = "8.4.0"
  private val mongoVersion = "1.7.0"
  val appName = "pensions-scheme"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-play-30"         % mongoVersion,
    "com.typesafe.play"             %% "play-json"                  % "2.9.4",
    "uk.gov.hmrc"                   %% "bootstrap-backend-play-30"  % playVersion,
    "com.networknt"                 %  "json-schema-validator"      % "1.0.76",
    "uk.gov.hmrc"                   %% "domain-play-30"             % "9.0.0",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"       % "2.14.2",
    ehcache
  )

  def test(scope: String = "test, it"): Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                 %% "bootstrap-test-play-30"       % playVersion         % Test,
    "uk.gov.hmrc.mongo"           %% "hmrc-mongo-test-play-30"      % mongoVersion        % Test,
    "org.pegdown"                  % "pegdown"                      % "1.6.0"             % scope,
    "com.vladsch.flexmark"         % "flexmark-all"                 % "0.64.0"            % scope,
    "org.scalatest"               %% "scalatest"                    % "3.2.15"            % Test,
    "org.scalatestplus"           %% "scalacheck-1-17"              % "3.2.15.0"          % Test,
    "org.scalatestplus"           %% "mockito-4-6"                  % "3.2.15.0"          % Test,
    "org.scalatestplus.play"      %% "scalatestplus-play"           % "5.1.0"             % Test,
    "com.eclipsesource"           %% "play-json-schema-validator"   % "0.9.5"             % scope,
    "io.github.wolfendale"        %% "scalacheck-gen-regexp"        % "1.1.0"             % scope
  )
}
