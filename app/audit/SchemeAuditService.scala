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

package audit

import play.api.Logger
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class SchemeAuditService {

  def sendSchemeDetailsEvent(psaId: String)(sendEvent: SchemeDetailsAuditEvent => Unit)
                         (implicit rh: RequestHeader, ec: ExecutionContext): PartialFunction[Try[Either[HttpResponse, JsValue]], Unit] = {



    case Success(Right(psaSubscription)) =>
      sendEvent(
        SchemeDetailsAuditEvent(
          psaId = psaId,
          status = Status.OK,
          payload = Some(psaSubscription)
        )
      )
    case Success(Left(e)) =>
      sendEvent(
        SchemeDetailsAuditEvent(
          psaId = psaId,
          status = e.status,
          payload = Some(Json.parse(e.body))
        )
      )
    case Failure(t) =>
      Logger.error("Error in sending audit event for get PSA details", t)

  }

  def sendPspSchemeDetailsEvent(pspId: String)(sendEvent: PspSchemeDetailsAuditEvent => Unit)
                            (implicit rh: RequestHeader, ec: ExecutionContext): PartialFunction[Try[Either[HttpResponse, JsValue]], Unit] = {



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
      Logger.error("Error in sending audit event for get psp scheme details", t)

  }

}
