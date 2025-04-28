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

package controllers


import com.google.inject.Inject
import models.SchemeReferenceNumber
import play.api.Logging
import play.api.libs.json._
import play.api.mvc._
import service.SchemeService
import uk.gov.hmrc.domain.{PsaId, PspId}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ErrorHandler

import scala.concurrent.{ExecutionContext, Future}

class AssociatedPsaController @Inject()(
                                         cc: ControllerComponents,
                                         schemeService: SchemeService
                                       )(
                                         implicit ec: ExecutionContext
                                       )
  extends BackendController(cc)
    with ErrorHandler with Logging {
  def isPsaAssociated: Action[AnyContent] = Action.async {
    implicit request => {
      request.headers.get("schemeReferenceNumber").map { SchemeReferenceNumber(_) }
        .map { srn =>
          def isAssociated(psaOrPsp: Either[PsaId, PspId]) = {
            schemeService.isAssociated(srn, psaOrPsp).map {
              case Left(e) =>
                logger.error("Is association check failed", e)
                result(e)
              case Right(value) =>
                Ok(Json.toJson(value))
            }
          }
          (request.headers.get("psaId"), request.headers.get("pspId")) match {
            case (Some(psaId), _) =>
              isAssociated(Left(PsaId(psaId)))
            case (None, Some(pspId)) =>
              isAssociated(Right(PspId(pspId)))
            case (None, None) => Future.successful(BadRequest("Missing headers: psaId or pspId"))
          }
        }.getOrElse(
          Future.successful(BadRequest("Missing header: schemeReferenceNumber"))
        )
    } recoverWith recoverFromError
  }

}
