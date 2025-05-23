/*
 * Copyright 2025 HM Revenue & Customs
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
import models.SchemeVariance
import play.api.libs.json.Json
import play.api.mvc.*
import repositories.LockRepository
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

class SchemeVarianceLockCacheController @Inject()(
                                                   repository: LockRepository,
                                                   cc: ControllerComponents,
                                                   psaPspEnrolmentAuthAction: PsaPspEnrolmentAuthAction
                                                 )(implicit ec: ExecutionContext) extends BackendController(cc) {

  def lock(): Action[AnyContent] = psaPspEnrolmentAuthAction.async {
    implicit request =>
      withIDs { (psaId, srn) =>
        repository.lock(SchemeVariance(psaId, srn))
          .map { lock => Ok(Json.toJson(lock.toString)) }
      }
  }

  def isLockByPsaIdOrSchemeId(): Action[AnyContent] = psaPspEnrolmentAuthAction.async {
    implicit request =>
      withIDs { (psaId, srn) =>
        repository.isLockByPsaIdOrSchemeId(psaId, srn)
          .map {
            case Some(lock) =>
              Ok(Json.toJson(lock.toString))
            case None =>
              NotFound
          }
      }
  }

  def getLock(): Action[AnyContent] = psaPspEnrolmentAuthAction.async {
    implicit request =>
      withIDs { (psaId, srn) =>
        repository.getExistingLock(SchemeVariance(psaId, srn))
          .map {
            case Some(schemeVariance) => Ok(Json.toJson(schemeVariance))
            case None => NotFound
          }
      }

  }

  def getLockByPsa(): Action[AnyContent] = psaPspEnrolmentAuthAction.async {
    implicit request =>
      request.headers.get("psaId") match {
        case Some(psaId) =>
          repository.getExistingLockByPSA(psaId).flatMap {
            case Some(schemeVariance) => Future.successful(Ok(Json.toJson(schemeVariance)))
            case None => Future.successful(NotFound)
          }
        case _ => Future.failed(new BadRequestException("Bad Request without psaId"))
      }
  }

  def getLockByScheme(): Action[AnyContent] = psaPspEnrolmentAuthAction.async {
    implicit request =>
      request.headers.get("srn") match {
        case Some(srn) =>
          repository.getExistingLockBySRN(srn)
            .map {
              case Some(schemeVariance) => Ok(Json.toJson(schemeVariance))
              case None => NotFound
            }
        case _ => Future.failed(new BadRequestException("Bad Request without srn"))
      }
  }

  def releaseLock(): Action[AnyContent] = psaPspEnrolmentAuthAction.async {
    implicit request =>
      withIDs { (psaId, srn) =>
        repository.releaseLock(SchemeVariance(psaId, srn)).map(_ => Ok)
      }
  }

  private def withIDs(block: (String, String) => Future[Result])(implicit request: Request[?]): Future[Result] = {
    (request.headers.get("psaId"), request.headers.get("srn")) match {
      case (Some(psaId), Some(srn)) => block(psaId, srn)
      case _ =>
        Future.failed(new BadRequestException("Bad Request without psaId and srn"))
    }
  }
}
