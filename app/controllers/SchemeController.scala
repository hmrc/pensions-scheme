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
import models.PensionsScheme
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SchemeController @Inject()(schemeConnector: SchemeConnector) extends BaseController {

  def registerScheme(): Action[AnyContent] = Action.async { implicit request => {

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
  } recoverWith {
    case e: BadRequestException =>
      Future.failed(new BadRequestException(e.message))
    case e: NotFoundException =>
      Future.failed(new Upstream4xxResponse(e.message, NOT_FOUND, NOT_FOUND))
    case e: Upstream4xxResponse =>
      Future.failed(new Upstream4xxResponse(e.message, e.upstreamResponseCode, e.reportAs))
    case e: Upstream5xxResponse =>
      Future.failed(new Upstream5xxResponse(e.message, e.upstreamResponseCode, e.reportAs))
    case e: Exception =>
      Future.failed(new Exception(e.getMessage))
  }
  }
}