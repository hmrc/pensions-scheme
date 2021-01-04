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
import play.Logger
import play.api.libs.json._
import play.api.mvc._
import service.SchemeService
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import utils.ErrorHandler
import utils.validationUtils._

import scala.concurrent.{ExecutionContext, Future}

class SchemeController @Inject()(schemeService: SchemeService,
                                 cc: ControllerComponents)(implicit ec: ExecutionContext) extends BackendController(cc) with ErrorHandler {

  def registerScheme: Action[AnyContent] = Action.async {
    implicit request => {
      val psaId = request.headers.get("psaId")
      val feJson = request.body.asJson
      Logger.debug(s"[PSA-Scheme-Incoming-Payload]$feJson")

      (psaId, feJson) match {
        case (Some(psa), Some(jsValue)) =>
          schemeService.registerScheme(psa, jsValue).map { response =>
            response.status match {
              case OK => Ok(response.body)
              case _ => result(response)
            }
          }

        case _ => Future.failed(new BadRequestException("Bad Request without PSAId or request body"))
      }
    } recoverWith recoverFromError
  }

  def listOfSchemes: Action[AnyContent] = Action.async {
    implicit request => {
      val psaId = request.headers.get("psaId")

      psaId match {
        case Some(psa) =>
          schemeService.listOfSchemes(psa).map { httpResponse =>
            httpResponse.status match {
              case OK =>
                Logger.debug(s"Call to list of schemes API on DES was successful with response ${httpResponse.json}")
                Ok(Json.toJson(httpResponse.json.convertTo[ListOfSchemes](ListOfSchemes.desReads)))
              case errorStatus =>
                Logger.error(s"List of schemes call to DES API failed with error $errorStatus and details ${httpResponse.body}")
                result(httpResponse)
            }
          }
        case _ => Future.failed(new BadRequestException("Bad Request with no Psa Id"))
      }
    } recoverWith recoverFromError
  }

  def updateScheme(): Action[AnyContent] = Action.async {
    implicit request => {
      val json = request.body.asJson
      Logger.debug(s"[Update-Scheme-Incoming-Payload]$json")
      (request.headers.get("pstr"), request.headers.get("psaId"), json) match {
        case (Some(pstr), Some(psaId), Some(jsValue)) =>
          schemeService.updateScheme(pstr, psaId, jsValue).map { response =>
            response.status match {
              case OK => Ok(response.body)
              case _ => result(response)
            }
          }

        case _ => Future.failed(new BadRequestException("Bad Request without PSTR or PSAId or request body"))
      }
    } recoverWith recoverFromError
  }

}
