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

package controllers


import com.google.inject.Inject
import connector.SchemeConnector
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import utils.ErrorHandler

import scala.concurrent.{ExecutionContext, Future}

class AssociatedPsaController @Inject()(
                                         schemeConnector: SchemeConnector,
                                         cc: ControllerComponents
                                       )(
                                         implicit ec: ExecutionContext
                                       )
  extends BackendController(cc)
    with ErrorHandler {
  def isPsaAssociated: Action[AnyContent] = Action.async {
    implicit request => {
      val (userId, jsonPath) =
        (request.headers.get("psaId"), request.headers.get("pspId")) match {
          case (Some(psaId), _) => (psaId, "psaDetails")
          case (_, Some(pspId)) => (pspId, "pspDetails")
          case _ => throw new Exception("Unable to retrieve either PSA or PSP from request")
        }

      val srn = request.headers.get("schemeReferenceNumber")
      val srnRequest = "srn"

      srn match {
        case Some(schemeReferenceNumber) =>
          schemeConnector.getSchemeDetails(
            userIdNumber = userId,
            schemeIdNumber = srnRequest,
            schemeIdType = schemeReferenceNumber
          ).map {
            case Right(json) =>

              val isAssociated =
                (json \ jsonPath)
                  .asOpt[JsArray]
                  .exists(_.value.map {
                    item => (item \ "id").as[String]
                  }.toList.contains(userId))

              Ok(Json.toJson(isAssociated))

            case Left(e) => result(e)
          }
        case _ =>
          Future.failed(new BadRequestException("Bad Request with missing parameters PSA Id or SRN"))
      }
    } recoverWith recoverFromError
  }
}
