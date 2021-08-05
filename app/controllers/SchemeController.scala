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
import models.ListOfSchemes
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._
import service.SchemeService
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ErrorHandler
import utils.ValidationUtils.genResponse

import scala.concurrent.{ExecutionContext, Future}

class SchemeController @Inject()(
                                  schemeService: SchemeService,
                                  cc: ControllerComponents
                                )(
                                  implicit ec: ExecutionContext
                                )
  extends BackendController(cc)
    with ErrorHandler {

  private val logger = Logger(classOf[SchemeController])

  def listOfSchemes: Action[AnyContent] = Action.async {
    implicit request => {
      val idType = request.headers.get("idType")
      val idValue = request.headers.get("idValue")

      (idType, idValue) match {
        case (Some(typeOfId), Some(valueOfId)) =>
          schemeService.listOfSchemes(typeOfId, valueOfId).map {
            case Right(json) => Ok(Json.toJson(json.convertTo[ListOfSchemes]))
            case Left(e) => result(e)
          }
        case _ => Future.failed(new BadRequestException("Bad Request with no ID type or value"))
      }
    }
  }

  def registerScheme: Action[AnyContent] = Action.async {
    implicit request => {
      val psaId = request.headers.get("psaId")
      val feJson = request.body.asJson
      logger.debug(s"[PSA-Scheme-Incoming-Payload]$feJson")

      (psaId, feJson) match {
        case (Some(psa), Some(jsValue)) =>
          schemeService.registerScheme(psa, jsValue).map {
            case Right(json) => Ok(json)
            case Left(e) => result(e)
          }
        case _ => Future.failed(new BadRequestException("Bad Request without PSAId or request body"))
      }
    } recoverWith recoverFromError
  }

  def updateScheme(): Action[AnyContent] = Action.async {
    implicit request => {
      val json = request.body.asJson
      logger.debug(s"[Update-Scheme-Incoming-Payload]$json")
      (request.headers.get("pstr"), request.headers.get("psaId"), json) match {
        case (Some(pstr), Some(psaId), Some(jsValue)) =>
          schemeService.updateScheme(pstr, psaId, jsValue).map {
            case Right(json) => Ok(json)
            case Left(e) => result(e)
          }

        case _ => Future.failed(new BadRequestException("Bad Request without PSTR or PSAId or request body"))
      }
    } recoverWith recoverFromError
  }

}
