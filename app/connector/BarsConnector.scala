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

package connector

import audit.{AuditService, BarsCheck}
import com.google.inject.{ImplementedBy, Inject, Singleton}
import config.AppConfig
import models.userAnswersToEtmp.{BankAccount, ValidateBankDetailsRequest, ValidateBankDetailsResponse}
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.JsSuccess
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[BarsConnectorImpl])
trait BarsConnector {
  def invalidBankAccount(bankAccount: BankAccount,
                         psaId: String
                        )(implicit ec: ExecutionContext, hc: HeaderCarrier, rh: RequestHeader): Future[Boolean]
}

@Singleton
class BarsConnectorImpl @Inject()(httpClientV2: HttpClientV2, appConfig: AppConfig, auditService: AuditService)
  extends BarsConnector {

  private val logger = Logger(classOf[BarsConnectorImpl])

  private val barsBaseUrl: String = appConfig.barsBaseUrl

  def invalidBankAccount(bankAccount: BankAccount,
                         psaId: String
                        )(implicit ec: ExecutionContext, hc: HeaderCarrier, rh: RequestHeader): Future[Boolean] = {

    val request = ValidateBankDetailsRequest(bankAccount)
    val url = url"$barsBaseUrl/validate/bank-details"

    httpClientV2.post(url)
      .withBody(request)
      .execute[HttpResponse] map {
      httpResponse =>
        auditService.sendEvent(
          BarsCheck(
            psaIdentifier = psaId,
            status = httpResponse.status,
            request = request,
            response = if (httpResponse.status == OK) Some(httpResponse.json) else None
          )
        )

        if (httpResponse.status == OK)
          httpResponse.json.validate[ValidateBankDetailsResponse] match {
            case JsSuccess(ValidateBankDetailsResponse(false, false), _) => true
            case _ => false
          }
        else
          false
    } recoverWith {
      case t =>
        logger.error("Exception calling bank reputation service", t)
        Future.successful(false)
    }
  }

}
