/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers

import com.google.inject.Inject
import models.SchemeVariance
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import repositories.LockRepository
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext

class SchemeVarianceLockCacheController @Inject()(
                                                   config: Configuration,
                                                   repository: LockRepository,
                                                   val authConnector: AuthConnector,
                                                   cc: ControllerComponents
                                                 )(implicit ec: ExecutionContext) extends BackendController(cc) with AuthorisedFunctions {

  def lock(psaId: String, srn: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        repository.lock(SchemeVariance(psaId, srn))
          .map{ lock =>  Ok(Json.toJson(lock))}
      }
  }

  def getLock(psaId: String, srn: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        repository.getExistingLock(SchemeVariance(psaId, srn))
          .map{
            case Some(schemeVariance)=> Ok(Json.toJson(schemeVariance))
            case None=>  NotFound
          }
      }
  }

  def releaseLock(psaId: String, srn: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        repository.releaseLock(SchemeVariance(psaId, srn)).map(_ => Ok)
      }
  }
}