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

package models

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class BankAccount(sortCode: String, accountNumber: String)

case class ValidateBankDetailsRequest(account: BankAccount)

case class ValidateBankDetailsResponse(accountNumberWithSortCodeIsValid: Boolean, sortCodeIsPresentOnEISCD: Boolean)

object BankAccount {
  implicit val formats = Json.format[BankAccount]

  implicit def apiReads: Reads[BankAccount] = (
    (JsPath \ "sortCode" \ "first").read[String] and
      (JsPath \ "sortCode" \ "second").read[String] and
      (JsPath \ "sortCode" \ "third").read[String] and
      (JsPath \ "accountNumber").read[String]) (
    (first, second, third, accountNo) => BankAccount(s"$first$second$third", accountNo)
  )
}

object ValidateBankDetailsRequest {
  implicit val formats = Json.format[ValidateBankDetailsRequest]
}

object ValidateBankDetailsResponse {

  implicit val reads: Reads[ValidateBankDetailsResponse] = {

    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    (
      (__ \ "accountNumberWithSortCodeIsValid").read[Boolean] and
        (__ \ "sortCodeIsPresentOnEISCD").read[String].map {
          case "yes" => true
          case _ => false
        }
      ) (ValidateBankDetailsResponse.apply _)
  }
}
