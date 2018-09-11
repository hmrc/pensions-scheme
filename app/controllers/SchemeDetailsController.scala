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
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import utils.ErrorHandler

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SchemeDetailsController @Inject()(schemeConnector: SchemeConnector) extends BaseController with ErrorHandler {

  def getSchemeDetails: Action[AnyContent] = Action.async {
    implicit request => {
      val idType = request.headers.get("schemeIdType")
      val id = request.headers.get("idNumber")

      (idType,id) match {
        case (Some(schemeIdType),Some(idNumber)) =>
          schemeConnector.getSchemeDetails(schemeIdType, idNumber).map {
            case Right(httpResponse) => Ok(httpResponse)
            case Left(e) => result(e)
          }
        case _ => Future.failed(new BadRequestException("Bad Request with missing parameters idType or idNumber"))
      }
    } recoverWith recoverFromError
  }

}
