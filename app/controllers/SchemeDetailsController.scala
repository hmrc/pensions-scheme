/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.mvc._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import utils.ErrorHandler

import scala.concurrent.{ExecutionContext, Future}

class SchemeDetailsController @Inject()(
                                         schemeConnector: SchemeConnector,
                                         cc: ControllerComponents
                                       )(implicit ec: ExecutionContext)
  extends BackendController(cc)
    with ErrorHandler {

  def getSchemeDetails: Action[AnyContent] = Action.async {
    implicit request => {
      val idType = request.headers.get("schemeIdType")
      val id = request.headers.get("idNumber")
      val idPsa = request.headers.get("PSAId")

      (idType, id, idPsa) match {
        case (Some(schemeIdType), Some(idNumber), Some(psaId)) =>
          schemeConnector.getSchemeDetails(psaId, schemeIdType, idNumber).map {
            case Right(psaSchemeDetails) => Ok(psaSchemeDetails)
            case Left(e) => result(e)
          }
        case _ =>
          Future.failed(new BadRequestException("Bad Request with missing parameters idType, idNumber or PSAId"))
      }
    } recoverWith recoverFromError
  }

  def getPspSchemeDetails: Action[AnyContent] = Action.async {
    implicit request => {
      val srnOpt = request.headers.get("srn")
      val pspIdOpt = request.headers.get("pspId")

      (srnOpt, pspIdOpt) match {
        case (Some(srn), Some(pspId)) =>
         schemeService.getPstrFromSrn(srn, "pspid", pspId).flatMap { pstr =>

           schemeConnector.getPspSchemeDetails(pspId, pstr).map {
             case Right(psaSchemeDetails) => Ok(psaSchemeDetails)
             case Left(e) => result(e)
           }
         }
        case _ => Future.failed(new BadRequestException("Bad Request with missing parameters idType, idNumber or PSAId"))
      }
    } recoverWith recoverFromError
  }
}
