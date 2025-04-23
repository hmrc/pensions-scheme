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

import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {

  private val excludedPackages: Seq[String] = Seq(
    "<empty>",
    "Reverse.*",
    ".*filters.*",
    ".*handlers.*",
    ".*components.*",
    ".*repositories.*",
    ".*FeatureSwitchModule.*",
    ".*BuildInfo.*",
    ".*javascript.*",
    ".*Routes.*",
    ".*GuiceInjector",
    ".*\\$anon.*"
  )

  // case classes with no added functionality so no requirement to test
  // other than default Reads, Writes or Format
  private val implicitOFormatObjects: Seq[String] = Seq(
    ".*ListOfSchemesAudit.*",
    ".*SchemeSubscription.*",
    ".*CompanyTrustee.*",
    ".*CompanyEstablisher.*",
    ".*PartnershipTrustee.*",
    ".*Partnership.*",
    ".*InternationalAddress.*",
    ".*AddressAndContactDetails.*",
    ".*Address.*",
    ".*UkAddress.*",
    ".*CorrespondenceAddress.*",
    ".*RACDACPensionsScheme.*",
    ".*RACDACDeclaration.*",
    ".*RACDACSubmissionEmailEvent.*",
    ".*SchemeJourneyType.*",
    ".*BankAccount.*",
    ".*ValidateBankDetailsRequest.*",
    ".*ValidateBankDetailsResponse.*",
    ".*SchemeVariance.*"
  )
  val settings: Seq[Setting[?]] = Seq(
    ScoverageKeys.coverageExcludedPackages := (excludedPackages ++ implicitOFormatObjects).mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}
