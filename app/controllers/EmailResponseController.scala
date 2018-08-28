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

import audit.{AuditService, EmailAuditEvent}
import com.google.inject.Inject
import models.{EmailEventType, EmailEvents, Opened}
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.{Action, BodyParsers, Result}
import uk.gov.hmrc.crypto.{ApplicationCrypto, Crypted}
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global

class EmailResponseController @Inject()(
                                         auditService: AuditService,
                                         crypto: ApplicationCrypto
                                       ) extends BaseController {

  def retrieveStatus(requestType: String, id: String): Action[JsValue] = Action(BodyParsers.parse.tolerantJson) {
    implicit request =>

      validateEventType(requestType)
        .right
        .flatMap{ eventType =>
          validatePsaId(id)
            .right
            .flatMap(psaId => Right((eventType, psaId)))
        }
        .fold(result => result, { case (eventType, psaId) =>

          request.body.validate[EmailEvents].fold(
            _ => BadRequest("Bad request received for email call back event"),
            valid => {
              val events = valid.events.map(_.event)
              if(!(events contains Opened)) {
                auditService.sendEvent(EmailAuditEvent(eventType, psaId, events.last))
              }
              Ok
            }
          )

        }
      )
  }

  private def validatePsaId(id: String): Either[Result, PsaId] =
    try {
      Right(PsaId {
        crypto.QueryParameterCrypto.decrypt(Crypted(id)).value
      })
    } catch {
      case _: IllegalArgumentException => Left(Forbidden("Malformed PSAID"))
    }

  private def validateEventType(requestType: String): Either[Result, EmailEventType] =
    EmailEventType.enumerable.withName(requestType) match {
      case Some(eventType) => Right(eventType)
      case None => Left(Forbidden("Unknown Event Type"))
    }

}
