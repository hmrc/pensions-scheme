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

package connector

import audit.{AuditService, BarsCheck}
import com.google.inject.{ImplementedBy, Inject, Singleton}
import config.AppConfig
import models.{BankAccount, ValidateBankDetailsRequest, ValidateBankDetailsResponse}
import play.api.Logger
import play.api.libs.json.JsSuccess
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

@ImplementedBy(classOf[BarsConnectorImpl])
trait BarsConnector {
  def invalidBankAccount(bankAccount: BankAccount, psaId: String)(implicit ec: ExecutionContext, hc: HeaderCarrier, rh: RequestHeader): Future[Boolean]
}

@Singleton
class BarsConnectorImpl @Inject()(http: HttpClient, appConfig: AppConfig, auditService: AuditService) extends BarsConnector {

  val barsBaseUrl: String = appConfig.barsBaseUrl

  val invalid = true
  val notInvalid = false

  def invalidBankAccount(bankAccount: BankAccount, psaId: String)(implicit ec: ExecutionContext, hc: HeaderCarrier, rh: RequestHeader): Future[Boolean] = {

    val request = ValidateBankDetailsRequest(bankAccount)

    http.POST(s"$barsBaseUrl/validateBankDetails", request).map {
      httpResponse =>
        require(httpResponse.status == 200)

        auditService.sendEvent(
          BarsCheck(
            psaId,
            httpResponse.status,
            request,
            Some(httpResponse.json)
          )
        )

        httpResponse.json.validate[ValidateBankDetailsResponse] match {
          case JsSuccess(ValidateBankDetailsResponse(false, false), _) => invalid
          case _ => notInvalid
        }
    } andThen {
      case Failure(t: HttpException) =>
        auditService.sendEvent(
          BarsCheck(
            psaId,
            t.responseCode,
            request,
            None
          )
        )
    } recoverWith {
      case t =>
        Logger.error("Exception calling bank reputation service", t)
        Future.successful(notInvalid)
    }
  }

}
