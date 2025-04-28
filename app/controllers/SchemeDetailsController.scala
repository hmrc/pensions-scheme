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

package controllers

import com.google.inject.Inject
import connector.SchemeDetailsConnector
import controllers.actions.{PsaEnrolmentAuthAction, PsaPspEnrolmentAuthAction, PsaPspSchemeAuthAction, PsaSchemeAuthAction}
import models.{PsaInvitationInfoResponse, SchemeReferenceNumber, SchemeWithId}
import play.api.libs.json.{JsError, JsObject, JsSuccess, JsValue, Json}
import play.api.mvc.*
import repositories.SchemeDetailsWithIdCacheRepository
import service.SchemeService
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ErrorHandler

import scala.concurrent.{ExecutionContext, Future}

class SchemeDetailsController @Inject()(
                                         schemeDetailsConnector: SchemeDetailsConnector,
                                         schemeDetailsCache: SchemeDetailsWithIdCacheRepository,
                                         schemeService: SchemeService,
                                         cc: ControllerComponents,
                                         psaEnrolmentAuthAction: PsaEnrolmentAuthAction,
                                         psaPspEnrolmentAuthAction: PsaPspEnrolmentAuthAction,
                                         psaSchemeAuthAction: PsaSchemeAuthAction,
                                         psaPspSchemeAuthAction: PsaPspSchemeAuthAction
                                       )(implicit ec: ExecutionContext)
  extends BackendController(cc)
    with ErrorHandler {

  def getSchemeDetails: Action[AnyContent] = Action.async {
    implicit request => {
      val idType = request.headers.get("schemeIdType")
      val id = request.headers.get("idNumber")
      val idPsa = request.headers.get("PSAId")
      val refreshDataOpt = request.headers.get("refreshData").map(_.toBoolean)

      (idType, id, idPsa) match {
        case (Some(schemeIdType), Some(idNumber), Some(psaId)) =>
          fetchFromCacheOrApiForPsa(SchemeWithId(idNumber, psaId), schemeIdType, refreshDataOpt)
        case _ =>
          Future.failed(new BadRequestException("Bad Request with missing parameters idType, idNumber or PSAId"))
      }
    } recoverWith recoverFromError
  }

  def getSchemeDetailsSrn(srn: SchemeReferenceNumber): Action[AnyContent] = (psaEnrolmentAuthAction andThen psaSchemeAuthAction(srn)).async {
    implicit request => {
      val idType = request.headers.get("schemeIdType")
      val id = request.headers.get("idNumber")
      val idPsa = request.psaId.value
      val refreshDataOpt = request.headers.get("refreshData").map(_.toBoolean)

      (idType, id) match {
        case (Some(schemeIdType), Some(idNumber)) =>
          fetchFromCacheOrApiForPsa(SchemeWithId(idNumber, idPsa), schemeIdType, refreshDataOpt)
        case _ =>
          Future.failed(new BadRequestException("Bad Request with missing parameters idType, idNumber or PSAId"))
      }
    } recoverWith recoverFromError
  }

  def getSchemePsaInvitationInfo: Action[AnyContent] = psaEnrolmentAuthAction.async {
    implicit request => {

      val idType = request.headers.get("schemeIdType")
      val id = request.headers.get("idNumber")
      val idPsa = request.psaId.value
      val refreshDataOpt = request.headers.get("refreshData").map(_.toBoolean)

      (idType, id) match {
        case (Some(schemeIdType), Some(idNumber)) =>
          fetchFromCacheOrApiForPsaNoResult(SchemeWithId(idNumber, idPsa), schemeIdType, refreshDataOpt).map {
            case Left(e) => result(e)
            case Right(json) =>
              Json.fromJson[PsaInvitationInfoResponse](json) match {
                case JsSuccess(value, _) => Ok(Json.toJson(value)(PsaInvitationInfoResponse.psaInvitationInfoResponseWrites))
                case JsError(errors) => InternalServerError(errors.toString)
              }
          }
        case _ =>
          Future.failed(new BadRequestException("Bad Request with missing parameters schemeIdType, idNumber"))
      }
    } recoverWith recoverFromError
  }

  def getPspSchemeDetails: Action[AnyContent] = Action.async {
    implicit request => {
      val srnOpt = request.headers.get("srn")
      val pstrOpt = request.headers.get("pstr")
      val pspIdOpt = request.headers.get("pspId")
      val refreshDataOpt = request.headers.get("refreshData").map(_.toBoolean)

      (srnOpt, pstrOpt, pspIdOpt) match {
        case (Some(srn), None, Some(pspId)) =>
          schemeService.getPstrFromSrn(srn, "pspid", pspId).flatMap { pstr =>
            fetchFromCacheOrApiForPsp(SchemeWithId(pstr, pspId), refreshDataOpt)
          }
        case (None, Some(pstr), Some(pspId)) =>
            fetchFromCacheOrApiForPsp(SchemeWithId(pstr, pspId), refreshDataOpt)

        case _ => Future.failed(new BadRequestException("Bad Request with missing parameters idType, idNumber or PSAId"))
      }
    } recoverWith recoverFromError
  }

  def getPspSchemeDetailsSrn(srn: SchemeReferenceNumber): Action[AnyContent] = {
    (psaPspEnrolmentAuthAction andThen psaPspSchemeAuthAction(srn, loggedInAsPsa = false)).async {
      implicit request => {
        val srnOpt = request.headers.get("srn")
        val pstrOpt = request.headers.get("pstr")
        val pspIdOpt = request.pspId.map(_.value)
        val refreshDataOpt = request.headers.get("refreshData").map(_.toBoolean)

        (srnOpt, pstrOpt, pspIdOpt) match {
          case (Some(srn), None, Some(pspId)) =>
            schemeService.getPstrFromSrn(srn, "pspid", pspId).flatMap { pstr =>
              fetchFromCacheOrApiForPsp(SchemeWithId(pstr, pspId), refreshDataOpt)
            }
          case (None, Some(pstr), Some(pspId)) =>
            fetchFromCacheOrApiForPsp(SchemeWithId(pstr, pspId), refreshDataOpt)

          case _ => Future.failed(new BadRequestException("Bad Request with missing parameters idType, idNumber or PSAId"))
        }
      } recoverWith recoverFromError
    }
  }

  private def fetchFromCacheOrApiForPsa(id: SchemeWithId, schemeIdType: String, refreshData: Option[Boolean])
                                       (implicit hc: HeaderCarrier, request: RequestHeader): Future[Result] =
    fetchFromCacheOrApiForPsaNoResult(id, schemeIdType, refreshData).map {
      case Left(e) => result(e)
      case Right(json) => Ok(json)
    }

  private def fetchFromCacheOrApiForPsaNoResult(id: SchemeWithId, schemeIdType: String, refreshData: Option[Boolean])
                                               (implicit hc: HeaderCarrier, request: RequestHeader)= {
    schemeDetailsCache.get(id).flatMap {
      case Some(json) if !refreshData.contains(true) => Future.successful(Right(json.as[JsObject]))
      case _ => schemeDetailsConnector.getSchemeDetails(id.userId, schemeIdType, id.schemeId).flatMap {
        case Right(json) => schemeDetailsCache.upsert(id, json).map { _ => Right(json) }
        case Left(e) => Future.successful(Left(e))
      }
    }
  }

  private def fetchFromCacheOrApiForPsp(id: SchemeWithId, refreshData: Option[Boolean])
                                       (implicit hc: HeaderCarrier, request: RequestHeader): Future[Result] =
    schemeDetailsCache.get(id).flatMap {
      case Some(json) if !refreshData.contains(true) => Future.successful(Ok(json.as[JsObject]))
      case _ => schemeDetailsConnector.getPspSchemeDetails(id.userId, id.schemeId).flatMap {
        case Right(json) => schemeDetailsCache.upsert(id, json).map {
          _ => Ok(json)
        }
        case Left(e) => Future.successful(result(e))
      }
    }
}
