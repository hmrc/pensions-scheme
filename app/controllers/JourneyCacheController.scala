/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.Configuration
import play.api.mvc.{Action, AnyContent, RawBuffer}
import repositories.JourneyCacheRepository
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

abstract class JourneyCacheController (
                                        config: Configuration,
                                        repository: JourneyCacheRepository,
                                        val authConnector: AuthConnector
                                      ) extends BaseController with AuthorisedFunctions {

  private val maxSize: Int = config.underlying.getInt("mongodb.journey-cache.maxSize")

  def save(id: String): Action[RawBuffer] = Action.async(parse.raw(maxSize, maxSize)) {
    implicit request =>
      authorised() {
        request.body.asBytes().map {
          bytes =>
            repository.upsert(id, bytes.toArray[Byte])
              .map(_ => Ok)
        } getOrElse Future.successful(EntityTooLarge)
      }
  }

  def get(id: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        repository.get(id).map {
          _.map(Ok(_))
            .getOrElse(NotFound)
        }
      }
  }
}