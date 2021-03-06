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
import play.api.libs.json._
import play.api.mvc._
import service.SchemeService
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ErrorHandler
import utils.ValidationUtils.genResponse

import scala.concurrent.{ExecutionContext, Future}

class SchemeIFController @Inject()(
                                    schemeService: SchemeService,
                                    cc: ControllerComponents
                                  )(implicit ec: ExecutionContext)
  extends BackendController(cc)
    with ErrorHandler {

  private val logger = Logger(classOf[SchemeIFController])

  def listOfSchemes: Action[AnyContent] = Action.async {
    implicit request => {
      val idType = request.headers.get("idType")
      val idValue = request.headers.get("idValue")

      (idType, idValue) match {
        case (Some(typeOfId), Some(valueOfId)) =>
          schemeService.listOfSchemes(typeOfId, valueOfId).map { httpResponse =>
            httpResponse.status match {
              case OK =>
                logger.debug(s"Call to list of schemes API on IF was successful with response ${httpResponse.json}")
                Ok(Json.toJson(httpResponse.json.convertTo[ListOfSchemes]))
              case errorStatus =>
                logger.error(s"List of schemes call to IF API failed with error $errorStatus and details ${httpResponse.body}")
                result(httpResponse)
            }
          }
        case _ => Future.failed(new BadRequestException("Bad Request with no ID type or value"))
      }
    }
  }

}
