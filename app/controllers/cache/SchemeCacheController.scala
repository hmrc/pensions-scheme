/*
 * Copyright 2021 HM Revenue & Customs
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

import org.joda.time.DateTime
import play.api.libs.json.JodaWrites._
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import play.api.{Configuration, Logger}
import repositories.SchemeCacheRepository
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

abstract class SchemeCacheController(
                                      config: Configuration,
                                      repository: SchemeCacheRepository,
                                      val authConnector: AuthConnector,
                                      cc: ControllerComponents
                                    )(implicit ec: ExecutionContext) extends BackendController(cc) with AuthorisedFunctions {

  private val maxSize: Int = config.underlying.getInt("mongodb.pensions-scheme-cache.maxSize")

  def save(id: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        request.body.asJson.map {
          jsValue =>
            repository.upsert(id, jsValue)
              .map(_ => Ok)
        } getOrElse Future.successful(EntityTooLarge)
      }
  }

  def get(id: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        Logger.debug("controllers.PensionsSchemeCacheController.get: Authorised Request " + id)
        repository.get(id).map { response =>
          Logger.debug(s"controllers.PensionsSchemeCacheController.get: Response for request Id $id is $response")
          response.map {
            Ok(_)
          }
            .getOrElse(NotFound)
        }
      }
  }

  def lastUpdated(id: String): Action[AnyContent] = Action.async {
    implicit request =>
      //This should be removed when manage-pensions-frontend is upgraded to Play 2.6
      val jodaTimeNumberWrites: Writes[DateTime] = Writes(JodaDateTimeNumberWrites.writes(_))
      authorised() {
        Logger.debug("controllers.PensionsSchemeCacheController.get: Authorised Request " + id)
        repository.getLastUpdated(id).map { response =>
          Logger.debug("controllers.PensionsSchemeCacheController.get: Response " + response)
          response.map { date => Ok(Json.toJson(date)(jodaTimeNumberWrites)) }
            .getOrElse(NotFound)
        }
      }
  }

  def remove(id: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        repository.remove(id).map(_ => Ok)
      }
  }

  def dropCollection(): Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        repository.dropCollection.map(_ => Ok)
      }
  }
}
