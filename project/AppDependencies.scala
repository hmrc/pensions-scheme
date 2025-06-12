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

import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {
  private val bootstrapVersion = "9.13.0"
  private val mongoVersion = "2.6.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-play-30"         % mongoVersion,
    "org.playframework"             %% "play-json"                  % "3.0.4",
    "uk.gov.hmrc"                   %% "bootstrap-backend-play-30"  % bootstrapVersion,
    "com.networknt"                 %  "json-schema-validator"      % "1.5.7",
    "uk.gov.hmrc"                   %% "domain-play-30"             % "10.0.0",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"       % "2.19.0"
  )

  def test(scope: String = "test"): Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                 %% "bootstrap-test-play-30"       % bootstrapVersion    % Test,
    "uk.gov.hmrc.mongo"           %% "hmrc-mongo-test-play-30"      % mongoVersion        % Test,
    "org.scalatestplus"           %% "scalacheck-1-18"              % "3.2.19.0"          % Test,
    "org.scalatestplus"           %% "mockito-4-6"                  % "3.2.15.0"          % Test,
    "org.scalatestplus.play"      %% "scalatestplus-play"           % "7.0.1"             % Test,
    "io.github.wolfendale"        %% "scalacheck-gen-regexp"        % "1.1.0"             % scope
  )
}
