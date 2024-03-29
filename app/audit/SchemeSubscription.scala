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

package audit

import play.api.libs.json.{Format, JsValue, Json}

case class SchemeSubscription(
                               psaIdentifier: String,
                               schemeType: Option[SchemeType.Value],
                               hasIndividualEstablisher: Boolean,
                               hasCompanyEstablisher: Boolean,
                               hasPartnershipEstablisher: Boolean,
                               hasDormantCompany: Boolean,
                               hasBankDetails: Boolean,
                               hasValidBankDetails: Boolean,
                               status: Int,
                               request: JsValue,
                               response: Option[JsValue]
                             ) extends AuditEvent {
  override def auditType: String = "SchemeSubscription"

  override def details: Map[String, String] = {
    val details = Map(
      "psaIdentifier" -> psaIdentifier,
      "hasIndividualEstablisher" -> hasIndividualEstablisher.toString,
      "hasCompanyEstablisher" -> hasCompanyEstablisher.toString,
      "hasPartnershipEstablisher" -> hasPartnershipEstablisher.toString,
      "hasDormantCompany" -> hasDormantCompany.toString,
      "hasBankDetails" -> hasBankDetails.toString,
      "hasValidBankDetails" -> hasValidBankDetails.toString,
      "status" -> status.toString,
      "request" -> Json.stringify(request),
      "response" -> {
        response match {
          case Some(json) => Json.stringify(json)
          case _ => ""
        }
      }
    )

    schemeType.fold(details)(schemeType => details + ("schemeType" -> schemeType.toString))
  }
}

object SchemeSubscription {
  implicit val formatsSchemeSubscription: Format[SchemeSubscription] = Json.format[SchemeSubscription]
}
