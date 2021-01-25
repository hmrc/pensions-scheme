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

package connector

import audit.{AuditService, BarsCheck}
import com.google.inject.{ImplementedBy, Inject, Singleton}
import config.AppConfig
import models.userAnswersToEtmp.{BankAccount, ValidateBankDetailsRequest, ValidateBankDetailsResponse}
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.JsSuccess
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[BarsConnectorImpl])
trait BarsConnector {
  def invalidBankAccount(bankAccount: BankAccount, psaId: String)
                        (implicit ec: ExecutionContext, hc: HeaderCarrier, rh: RequestHeader): Future[Boolean]
}

@Singleton
class BarsConnectorImpl @Inject()(http: HttpClient, appConfig: AppConfig, auditService: AuditService) extends BarsConnector {

  private val logger = Logger(classOf[BarsConnectorImpl])

  val barsBaseUrl: String = appConfig.barsBaseUrl

  def invalidBankAccount(bankAccount: BankAccount, psaId: String)
                        (implicit ec: ExecutionContext, hc: HeaderCarrier, rh: RequestHeader): Future[Boolean] = {

    val request = ValidateBankDetailsRequest(bankAccount)

    http.POST[ValidateBankDetailsRequest, HttpResponse](s"$barsBaseUrl/validateBankDetails", request).map {
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
