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

package utils

import play.api.libs.json.JsResultException
import play.api.mvc.Result
import uk.gov.hmrc.http._
import play.api.http.Status._
import scala.concurrent.Future

trait ErrorHandler {

  def recoverFromError: PartialFunction[Throwable, Future[Result]] = {
    case e: JsResultException =>
      Future.failed(new BadRequestException(e.getMessage))
    case e: BadRequestException =>
      Future.failed(new BadRequestException(e.message))
    case e: NotFoundException =>
      Future.failed(new NotFoundException(e.message))
    case e: Upstream4xxResponse =>
      Future.failed(throwAppropriateException(e))
    case e: Upstream5xxResponse =>
      Future.failed(new Upstream5xxResponse(e.message, e.upstreamResponseCode, e.reportAs))
    case e: Exception =>
      Future.failed(new Exception(e.getMessage))
  }

  def throwAppropriateException(e: Upstream4xxResponse): Exception = {
    e.upstreamResponseCode match {
      case (FORBIDDEN) if (e.message.contains("INVALID_BUSINESS_PARTNER")) =>
        new ForbiddenException(e.message)
      case CONFLICT if (e.message.contains("DUPLICATE_SUBMISSION")) =>
        new ConflictException(e.message)
      case _ =>
        new Upstream4xxResponse(e.message, e.upstreamResponseCode, e.reportAs)
    }
  }
}

