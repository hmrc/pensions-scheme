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

import com.google.inject.{ImplementedBy, Inject, Singleton}
import config.AppConfig
import models.{BankAccount, ValidateBankDetailsRequest, ValidateBankDetailsResponse}
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[BarsConnectorImpl])
trait BarsConnector {
  def invalidBankAccount(bankAccount: BankAccount)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Boolean]
}

@Singleton
class BarsConnectorImpl @Inject()(http: HttpClient, appConfig: AppConfig) extends BarsConnector {

  val barsBaseUrl: String = appConfig.barsBaseUrl

  val invalid = true
  val notInvalid = false

  def invalidBankAccount(bankAccount: BankAccount)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Boolean] = {

    val request = ValidateBankDetailsRequest(bankAccount)
    http.POST[ValidateBankDetailsRequest, ValidateBankDetailsResponse](s"$barsBaseUrl/validateBankDetails", request).map {
      case ValidateBankDetailsResponse(false, false) => invalid
      case _ => notInvalid
    } recoverWith {
      case t =>
        Logger.error("Exception calling bank reputation service", t)
        Future.successful(notInvalid)
    }
  }

}
