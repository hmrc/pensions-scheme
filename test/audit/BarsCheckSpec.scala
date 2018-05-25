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

package audit

import models.{BankAccount, ValidateBankDetailsRequest}
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json

class BarsCheckSpec extends FlatSpec with Matchers {

  "BarsCheck.details" should "output the correct map of data" in {

    val psaId = "test-psa-id"
    val status = 1
    val bankAccount = BankAccount("123456", "12345678")
    val request = ValidateBankDetailsRequest(bankAccount)
    val response = Json.obj("field" -> "value")

    val event = BarsCheck(
      psaIdentifier = psaId,
      status = status,
      request = request,
      response = Some(response)
    )

    val expected: Map[String, String] = Map(
      "psaIdentifier" -> psaId,
      "status" -> status.toString,
      "request" -> Json.stringify(Json.toJson(request)),
      "response" -> Json.stringify(response)
    )

    event.details shouldBe expected

  }

}
