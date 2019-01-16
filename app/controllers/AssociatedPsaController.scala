/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import utils.ErrorHandler
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

class AssociatedPsaController @Inject()(schemeConnector: SchemeConnector,
                                        cc: ControllerComponents)(implicit ec: ExecutionContext) extends BackendController(cc) with ErrorHandler {
  def isPsaAssociated: Action[AnyContent] = Action.async {
    implicit request => {
      val psaId = request.headers.get("psaId")
      val srn = request.headers.get("schemeReferenceNumber")
      val srnRequest = "srn"

      (srn,psaId) match {
        case (Some(schemeReferenceNumber),Some(id)) =>
          schemeConnector.getSchemeDetails(id, srnRequest, schemeReferenceNumber).map {
            case Right(schemeDetails) =>
              val isAssociated = schemeDetails.psaDetails.exists(psa => psa.id == id)
              Ok(Json.toJson(isAssociated))
            case Left(e) => result(e)
          }
        case _ => Future.failed(new BadRequestException("Bad Request with missing parameters PSA Id or SRN"))
      }
    } recoverWith recoverFromError
  }
}
