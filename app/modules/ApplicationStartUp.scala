/*
 * Copyright 2026 HM Revenue & Customs
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

package modules

import models.{Api1359HipMigrationToggle, Api1360HipMigrationToggle, Api1444HipMigrationToggle, Api1468HipMigrationToggle, Api1660HipMigrationToggle}
import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlagNamesLibrary

import javax.inject.Singleton

@Singleton
class ApplicationStartUp {
  FeatureFlagNamesLibrary.addFlags(
    List(
      Api1359HipMigrationToggle,
      Api1360HipMigrationToggle,
      Api1444HipMigrationToggle,
      Api1468HipMigrationToggle,
      Api1660HipMigrationToggle,
    )
  )
}
