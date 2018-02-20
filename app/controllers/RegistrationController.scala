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
import models.{IndividualOrOrganisation, SuccessResponse}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import utils.ErrorHandler

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RegistrationController @Inject()(schemeConnector: SchemeConnector) extends BaseController with ErrorHandler {

  def registerWithId: Action[AnyContent] = Action.async {
    implicit request => {

      val idType = request.headers.get("idType")
      val idNumber = request.headers.get("idNumber")

      val feJson = request.body.asJson

      (idType, idNumber, feJson) match {
        case (Some(id), Some(number), Some(jsValue)) =>
          val registerWithIdData = Json.toJson(jsValue.as[IndividualOrOrganisation])
          schemeConnector.registerWithId(id, number, registerWithIdData).map { httpResponse =>
            val response = httpResponse.json.as[SuccessResponse]
            Ok(Json.toJson[SuccessResponse](response))
          }
        case _ => Future.failed(new BadRequestException("Bad Request without proper Id or request body"))
      }
    } recoverWith recoverFromError
  }
}
