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

import models.{BankAccount, PensionsScheme}
import play.api.libs.json.JsValue
import com.google.inject.Inject
import connector.BarsConnector
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.{ExecutionContext, Future}

class SchemeService @Inject()(barsConnector: BarsConnector) {

  def retrievePensionScheme(feJson: JsValue)(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[PensionsScheme] = {
    val bankDetails = (feJson \ "uKBankDetails").asOpt[BankAccount](BankAccount.apiReads)

    bankDetails match {
      case Some(details) =>
        barsConnector.invalidBankAccount(details).map { invalidBankAcct =>
          val pensionScheme = feJson.as[PensionsScheme]
          pensionScheme.copy(customerAndSchemeDetails = pensionScheme.customerAndSchemeDetails.copy(haveInvalidBank = invalidBankAcct))
        }
      case _ =>
        Future.successful(feJson.as[PensionsScheme])
    }
  }
}
