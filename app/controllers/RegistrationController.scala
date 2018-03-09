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
import connector.RegistrationConnector
import models.{Organisation, OrganisationRegistrant, SuccessResponse}
import play.api.libs.json.{JsObject, JsPath, JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, AuthorisedFunctions, ConfidenceLevel}
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.http.{BadRequestException, Upstream4xxResponse}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import utils.ErrorHandler

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RegistrationController @Inject()(override val authConnector: AuthConnector,
                                       registerConnector: RegistrationConnector
                                      ) extends BaseController with ErrorHandler with AuthorisedFunctions {

  def registerWithIdIndividual: Action[AnyContent] = Action.async {
    implicit request => {

      authorised(ConfidenceLevel.L200 and AffinityGroup.Individual).retrieve(Retrievals.nino) {
        case Some(nino) =>
          registerConnector.registerWithIdIndividual(nino, mandatoryPODSData()).map { httpResponse =>
            val response = httpResponse.json.as[SuccessResponse]
            Ok(Json.toJson[SuccessResponse](response))
          }
        case _ =>
          Future.failed(new Upstream4xxResponse("Nino not found in auth record", UNAUTHORIZED, UNAUTHORIZED))
      } recoverWith recoverFromError
    }
  }

  def registerWithIdOrganisation: Action[AnyContent] = Action.async {
    implicit request => {

      request.body.asJson match {
        case Some(jsBody) =>
          val utr = (jsBody \ "utr").as[String]
          val registerWithIdData = mandatoryPODSData(true).as[JsObject] ++
            Json.obj("organisation" -> Json.toJson(jsBody.as[Organisation]))

          registerConnector.registerWithIdOrganisation(utr, registerWithIdData).map { httpResponse =>
            val response = httpResponse.json.as[SuccessResponse]
            Ok(Json.toJson[SuccessResponse](response))
          }
        case _ =>
          Future.failed(new BadRequestException("No request body received for Organisation"))
      }
    } recoverWith recoverFromError
  }

  private def mandatoryPODSData(requiresNameMatch: Boolean = false): JsValue = {
    Json.obj("regime" -> "PODS", "requiresNameMatch" -> requiresNameMatch, "isAnAgent" -> false)
  }

  def registrationNoIdOrganisation: Action[OrganisationRegistrant] =
    Action.async(parse.json[OrganisationRegistrant]) {
      implicit request => {
        registerConnector.registrationNoIdOrganisation(request.body).map { httpResponse =>
          Ok(httpResponse.body)

        }
      } recoverWith recoverFromError
    }
}
