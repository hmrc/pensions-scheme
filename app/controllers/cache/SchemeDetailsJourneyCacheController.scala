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

package controllers.cache

import com.google.inject.Inject
import controllers.actions.PsaPspEnrolmentAuthAction
import play.api.mvc.ControllerComponents
import repositories.SchemeDetailsCacheRepository
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.ExecutionContext

class SchemeDetailsJourneyCacheController @Inject()(
                                                     repository: SchemeDetailsCacheRepository,
                                                     authConnector: AuthConnector,
                                                     cc: ControllerComponents,
                                                     psaPspEnrolmentAuthAction: PsaPspEnrolmentAuthAction
                                                   )(implicit val ec: ExecutionContext)
  extends SchemeCacheController(repository, authConnector, cc, psaPspEnrolmentAuthAction)
