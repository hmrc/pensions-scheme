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

package models

import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlagName

trait HipMigrationToggle(api: String) extends FeatureFlagName:
  override val name: String = s"api-$api-hip-migration-toggle"
  override val description: Option[String] = Some(s"Migrate API $api to HIP")

case object Api1359HipMigrationToggle extends HipMigrationToggle("Api1359")
case object Api1360HipMigrationToggle extends HipMigrationToggle("Api1360")
case object Api1444HipMigrationToggle extends HipMigrationToggle("Api1444")
case object Api1468HipMigrationToggle extends HipMigrationToggle("Api1468")
case object Api1660HipMigrationToggle extends HipMigrationToggle("Api1660")
