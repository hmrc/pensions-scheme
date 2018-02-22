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
import connector.EtmpConnector
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import models.{PensionsScheme, Registrant}

import scala.concurrent.Future
import utils.ErrorHandler

import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationController @Inject()(etmpConnector: EtmpConnector) extends BaseController with ErrorHandler {

  def registrationNoIdIndividual: Action[AnyContent] = Action.async { implicit request => {

    request.body.asJson match {
      case (Some(jsValue)) => {
        val registrationIndividual = Json.toJson(jsValue.as[Registrant])
        etmpConnector.registrationNoIdIndividual(registrationIndividual).map { httpResponse =>
          Ok(httpResponse.body)
        }
      }
      case _ => Future.failed(new BadRequestException("Bad Request without request body"))
    }

  } recoverWith recoverFromError
  }
}
