/*
 * Copyright 2020 HM Revenue & Customs
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

package repositories

import com.google.inject.Inject
import play.api.Configuration
import play.modules.reactivemongo.ReactiveMongoComponent

import scala.concurrent.ExecutionContext

class SchemeDetailsCacheRepository @Inject()(
                                             config: Configuration,
                                             component: ReactiveMongoComponent
                                           )(implicit val executionContext: ExecutionContext) extends SchemeCacheRepository(
  config.underlying.getString("mongodb.pensions-scheme-cache.scheme-details.name"),
  "scheme.json.encryption",
  Some(config.underlying.getInt("mongodb.pensions-scheme-cache.scheme-details.timeToLiveInSeconds")),
  config,
  component,
  None
)
