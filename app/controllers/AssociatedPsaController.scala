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
import connector.SchemeDetailsConnector
import models.SchemeWithId
import play.api.Logging
import play.api.libs.json._
import play.api.mvc._
import repositories.SchemeDetailsWithIdCacheRepository
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ErrorHandler

import scala.concurrent.{ExecutionContext, Future}

class AssociatedPsaController @Inject()(
                                         schemeDetailsConnector: SchemeDetailsConnector,
                                         cc: ControllerComponents,
                                         schemeDetailsCache: SchemeDetailsWithIdCacheRepository
                                       )(
                                         implicit ec: ExecutionContext
                                       )
  extends BackendController(cc)
    with ErrorHandler with Logging {
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
          val schemeWithId = SchemeWithId(schemeReferenceNumber, userId)
          fetchFromCacheOrApiForPsa(schemeWithId, srnRequest, None).map {
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

  private def fetchFromCacheOrApiForPsa(id: SchemeWithId, schemeIdType: String, refreshData: Option[Boolean])
                                       (implicit hc: HeaderCarrier, request: RequestHeader): Future[Either[HttpException, JsObject]] =
    schemeDetailsCache.get(id).flatMap {
      case Some(json) if !refreshData.contains(true) =>
        logger.info("Retrieving scheme details from cache")
        Future.successful(Right(json.as[JsObject]))
      case _ => schemeDetailsConnector.getSchemeDetails(id.userId, schemeIdType, id.schemeId).flatMap {
        case Right(json) =>
          logger.info("Retrieving scheme details from API")
          schemeDetailsCache.upsert(id, json).map { _ => Right(json) }
        case e => Future.successful(e)
      }
    }
}
