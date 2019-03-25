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
import play.api.Configuration
import play.api.libs.json.{JsArray, Json, Writes}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import repositories.{LockRepository, SchemeVariance}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext

class SchemeVarianceLockCacheController @Inject()(
                                                   config: Configuration,
                                                   repository: LockRepository,
                                                   val authConnector: AuthConnector,
                                                   cc: ControllerComponents
                                                 )(implicit ec: ExecutionContext) extends BackendController(cc) with AuthorisedFunctions {

  implicit def tuple2Writes[A, B](implicit aWrites: Writes[A], bWrites: Writes[B]): Writes[Tuple2[A, B]] = new Writes[Tuple2[A, B]] {
    def writes(tuple: Tuple2[A, B]) = JsArray(Seq(aWrites.writes(tuple._1), bWrites.writes(tuple._2)))

  def lock(psaId: String, srn: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        repository.lock(SchemeVariance(psaId, srn))
          .map{ Ok(_)}
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