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

import controllers.actions.PsaPspEnrolmentAuthAction
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import repositories.SchemeCacheRepository
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

abstract class SchemeCacheController(
                                      repository: SchemeCacheRepository,
                                      val authConnector: AuthConnector,
                                      cc: ControllerComponents,
                                      psaPspEnrolmentAuthAction: PsaPspEnrolmentAuthAction
                                    )(
                                      implicit ec: ExecutionContext
                                    )
  extends BackendController(cc)
    with AuthorisedFunctions {

  private val logger = Logger(classOf[SchemeCacheController])

  def save(id: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        request.body.asJson.map {
          jsValue =>
            repository.upsert(id, jsValue).map(_ => Ok)
        } getOrElse Future.successful(BadRequest("Can't parse json"))
      }
  }

  def get(id: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        logger.debug("controllers.SchemeCacheController.get: Authorised Request " + id)
        repository.get(id).map { response =>
          logger.debug(s"controllers.SchemeCacheController.get: Response for request Id $id is $response")
          response.map(Ok(_)).getOrElse(NotFound)
        }
      }
  }

  def remove(id: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        repository.remove(id).map(_ => Ok)
      }
  }

  def lastUpdated(id: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        logger.debug("controllers.SchemeCacheController.lastUpdated: Authorised Request " + id)
        repository.getLastUpdated(id).map { response =>
          logger.debug("controllers.SchemeCacheController.lastUpdated: Response " + response)
          response.map {
            date => Ok(Json.toJson(date.toEpochMilli))
          } getOrElse NotFound
        }
      }
  }

  def saveSelf: Action[AnyContent] = psaPspEnrolmentAuthAction.async {
    implicit request =>
      request.body.asJson.map {
        jsValue =>
          repository.upsert(request.externalId, jsValue).map(_ => Ok)
      } getOrElse Future.successful(BadRequest("Can't parse json"))
  }

  def getSelf: Action[AnyContent] = psaPspEnrolmentAuthAction.async {
    implicit request =>
      val id = request.externalId
      logger.debug("controllers.SchemeCacheController.get: Authorised Request " + id)
      repository.get(id).map { response =>
        logger.debug(s"controllers.SchemeCacheController.get: Response for request Id $id is $response")
        response.map(Ok(_)).getOrElse(NotFound)
      }
  }

  def removeSelf: Action[AnyContent] = psaPspEnrolmentAuthAction.async {
    implicit request =>
      repository.remove(request.externalId).map(_ => Ok)
  }

  def lastUpdatedSelf: Action[AnyContent] = psaPspEnrolmentAuthAction.async {
    implicit request =>
      val id = request.externalId
      logger.debug("controllers.SchemeCacheController.lastUpdated: Authorised Request " + id)
      repository.getLastUpdated(id).map { response =>
        logger.debug("controllers.SchemeCacheController.lastUpdated: Response " + response)
        response.map {
          date => Ok(Json.toJson(date.toEpochMilli))
        } getOrElse NotFound
      }
  }
}
