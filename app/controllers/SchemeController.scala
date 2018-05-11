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

import com.google.inject.Inject
import connector.SchemeConnector
import models.{ListOfSchemes, PensionSchemeAdministrator}
import play.api.libs.json._
import play.api.mvc._
import service.SchemeService
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import utils.ErrorHandler
import play.api.Logger


import utils.validationUtils._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class SchemeController @Inject()(schemeConnector: SchemeConnector, schemeService: SchemeService) extends BaseController with ErrorHandler {

  def registerScheme: Action[AnyContent] = Action.async {
    implicit request => {
      val psaId = request.headers.get("psaId")
      val feJson = request.body.asJson

      (psaId, feJson) match {
        case (Some(psa), Some(jsValue)) =>
          schemeService.registerScheme(psa, jsValue).map {
            response =>
              Ok(response.body)
          }

        case _ => Future.failed(new BadRequestException("Bad Request without PSAId or request body"))
      }
    } recoverWith recoverFromError
  }

  def registerPSA: Action[AnyContent] = Action.async {
    implicit request => {
      val feJson = request.body.asJson
      Logger.info(s"Incoming payload ${feJson}")

      feJson match {
        case Some(jsValue) =>
          Try(jsValue.convertTo[PensionSchemeAdministrator](PensionSchemeAdministrator.apiReads)) match {
            case Success(pensionSchemeAdministrator) =>
              val psaJsValue = Json.toJson(pensionSchemeAdministrator)
              Logger.info(s"Outgoing payload ${psaJsValue}")

              schemeConnector.registerPSA(psaJsValue).map {
                httpResponse => Ok(httpResponse.body)
              }
            case Failure(e) =>
              Logger.warn(s"Bad Request returned from frontend for PSA $e")
              Future.failed(new BadRequestException(s"Bad Request returned from frontend for PSA $e"))
          }
        case _ => Future.failed(new BadRequestException("Bad Request with no request body"))
      }
    } recoverWith recoverFromError
  }

  def listOfSchemes: Action[AnyContent] = Action.async {
    implicit request => {
      val psaId = request.headers.get("psaId")

      psaId match {
        case Some(psa) =>
          schemeConnector.listOfSchemes(psa).map { httpResponse =>
            Ok(Json.toJson(httpResponse.json.convertTo[ListOfSchemes]))
          }
        case _ => Future.failed(new BadRequestException("Bad Request with no Psa Id"))
      }
    } recoverWith recoverFromError
  }
}
