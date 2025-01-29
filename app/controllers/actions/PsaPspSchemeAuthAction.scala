/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.actions

import models.SchemeReferenceNumber
import play.api.Logging
import play.api.mvc.Results.Forbidden
import play.api.mvc.{ActionFunction, Result}
import service.SchemeService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider
import utils.ErrorHandler

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PsaPspSchemeActionImpl (srn:SchemeReferenceNumber, schemeService: SchemeService, loggedInAsPsa: Boolean)
                          (implicit val executionContext: ExecutionContext)
  extends ActionFunction[PsaPspAuthRequest, PsaPspAuthRequest] with BackendHeaderCarrierProvider with Logging with ErrorHandler {


  override def invokeBlock[A](request: PsaPspAuthRequest[A], block: PsaPspAuthRequest[A] => Future[Result]): Future[Result] = {

    val id = if(loggedInAsPsa) {
      request.psaId.flatMap { psaId =>
        Some(Left(psaId))
      }
    } else {
      request.pspId.flatMap { pspId =>
        Some(Right(pspId))
      }
    }

    val isAssociatedOpt = id.map { id =>
      schemeService.isAssociated(
        srn,
        id
      )(hc(request), request)
    }

    isAssociatedOpt match {
      case Some(isAssociated) =>
        isAssociated.flatMap {
          case Right(true) => block(request)
          case Right(false) =>
            logger.warn("Potentially prevented unauthorised access")
            Future.successful(Forbidden("PSA is not associated with pension scheme"))
          case Left(e) =>
            logger.error("Is associated call failed", e)
            recoverFromError(e)
        }
      case None =>
        logger.warn("No valid enrolment")
        Future.successful(Forbidden("No valid enrolment"))
    }
  }
}




class PsaPspSchemeAuthAction @Inject()(schemeService: SchemeService)(implicit ec: ExecutionContext){
  def apply(srn: SchemeReferenceNumber, loggedInAsPsa: Boolean): ActionFunction[PsaPspAuthRequest, PsaPspAuthRequest] =
    new PsaPspSchemeActionImpl(srn, schemeService, loggedInAsPsa)
}
