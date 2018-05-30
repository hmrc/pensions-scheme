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

package service
import audit.{AuditService, PSASubscription, SchemeList}
import connector.SchemeConnector
import models.PensionSchemeAdministrator
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpException, HttpResponse}
import utils.validationUtils._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

abstract class SchemeServiceImpl(schemeConnector: SchemeConnector, auditService: AuditService) extends SchemeService {

  override def listOfSchemes(psaId: String)
                   (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[HttpResponse] = {

    schemeConnector.listOfSchemes(psaId) andThen {
      case Success(httpResponse) =>
        sendSchemeListEvent(psaId, Status.OK, Some(httpResponse.json))
      case Failure(error: HttpException) =>
        sendSchemeListEvent(psaId, error.responseCode, None)
    }

  }

  override def registerPSA(json: JsValue)
                          (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, rh: RequestHeader): Future[HttpResponse] = {

    Try(json.convertTo[PensionSchemeAdministrator](PensionSchemeAdministrator.apiReads)) match {
      case Success(pensionSchemeAdministrator) =>
        val psaJsValue = Json.toJson(pensionSchemeAdministrator)(PensionSchemeAdministrator.psaSubmissionWrites)
        Logger.debug(s"[PSA-Registration-Outgoing-Payload]$psaJsValue")

        schemeConnector.registerPSA(psaJsValue) andThen {
          case Success(httpResponse) =>
            sendPSASubscriptionEvent(pensionSchemeAdministrator, Status.OK, psaJsValue, Some(httpResponse.json))
          case Failure(error: HttpException) =>
            sendPSASubscriptionEvent(pensionSchemeAdministrator, error.responseCode, psaJsValue, None)
        }

      case Failure(e) =>
        Logger.warn(s"Bad Request returned from frontend for PSA $e")
        Future.failed(new BadRequestException(s"Bad Request returned from frontend for PSA $e"))
    }

  }

  private def sendSchemeListEvent(psaId: String, status: Int, response: Option[JsValue])(implicit request: RequestHeader, ec: ExecutionContext): Unit = {

    auditService.sendEvent(SchemeList(psaId, status, response))

  }

  private def sendPSASubscriptionEvent(psa: PensionSchemeAdministrator, status: Int, request: JsValue, response: Option[JsValue])
                                      (implicit rh: RequestHeader, ec: ExecutionContext): Unit = {

    auditService.sendEvent(
      PSASubscription(
        existingUser = psa.pensionSchemeAdministratoridentifierStatus.isExistingPensionSchemaAdministrator,
        legalStatus = psa.legalStatus,
        status = status,
        request = request,
        response = response
      )
    )

  }

}
