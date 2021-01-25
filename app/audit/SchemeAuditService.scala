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

package audit

import play.api.Logger
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HttpResponse

import scala.util.{Failure, Success, Try}

class SchemeAuditService {

  private val logger = Logger(classOf[SchemeAuditService])

  def sendSchemeDetailsEvent(userIdNumber: String)
                            (sendEvent: SchemeDetailsAuditEvent => Unit): PartialFunction[Try[Either[HttpResponse, JsValue]], Unit] = {


    case Success(Right(psaSubscription)) =>
      sendEvent(
        SchemeDetailsAuditEvent(
          userIdNumber = userIdNumber,
          status = Status.OK,
          payload = Some(psaSubscription)
        )
      )
    case Success(Left(e)) =>
      sendEvent(
        SchemeDetailsAuditEvent(
          userIdNumber = userIdNumber,
          status = e.status,
          payload = Some(Json.parse(e.body))
        )
      )
    case Failure(t) =>
      logger.error("Error in sending audit event for get PSA details", t)

  }

  def sendPspSchemeDetailsEvent(pspId: String)
                               (sendEvent: PspSchemeDetailsAuditEvent => Unit): PartialFunction[Try[Either[HttpResponse, JsValue]], Unit] = {
    case Success(Right(pspSchemeSubscription)) =>
      sendEvent(
        PspSchemeDetailsAuditEvent(
          pspId = pspId,
          status = Status.OK,
          payload = Some(pspSchemeSubscription)
        )
      )
    case Success(Left(e)) =>
      sendEvent(
        PspSchemeDetailsAuditEvent(
          pspId = pspId,
          status = e.status,
          payload = Some(Json.parse(e.body))
        )
      )
    case Failure(t) =>
      logger.error("Error in sending audit event for get psp scheme details", t)
  }

}
