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
import models.{ListOfSchemes, PensionSchemeAdministrator, PensionsScheme}
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import utils.ErrorHandler

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SchemeController @Inject()(schemeConnector: SchemeConnector) extends BaseController with ErrorHandler {

  def registerScheme: Action[AnyContent] = Action.async { implicit request => {
    val psaId = request.headers.get("psaId")
    val feJson = request.body.asJson

    (psaId, feJson) match {
      case (Some(psa), Some(jsValue)) =>
        val pensionSchemeData = Json.toJson(jsValue.as[PensionsScheme])
        schemeConnector.registerScheme(psa, pensionSchemeData).map { httpResponse =>
          Ok(httpResponse.body)
        }
      case _ => Future.failed(new BadRequestException("Bad Request without PSAId or request body"))
    }
  } recoverWith recoverFromError
  }

  def registerPSA: Action[AnyContent] = Action.async { implicit request => {
    val feJson = request.body.asJson

    feJson match {
      case Some(jsValue) =>
        val psa = Json.toJson(jsValue.as[PensionSchemeAdministrator])
        schemeConnector.registerPSA(psa).map {
          httpResponse => Ok(httpResponse.body)
        }
      case _ => Future.failed(new BadRequestException("Bad Request with no request body"))
    }
  } recoverWith recoverFromError
  }

  def listOfSchemes: Action[AnyContent] = Action.async { implicit request => {
    val psaId = request.headers.get("psaId")

    psaId match {
      case Some(psa) =>
        schemeConnector.listOfSchemes(psa).map { httpResponse =>
          Ok(Json.toJson(httpResponse.json.as[ListOfSchemes]))
        }
      case _ => Future.failed(new BadRequestException("Bad Request with no Psa Id"))
    }
  } recoverWith recoverFromError
  }
}
