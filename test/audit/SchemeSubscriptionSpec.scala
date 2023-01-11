/*
 * Copyright 2023 HM Revenue & Customs
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

import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec
import play.api.libs.json.Json

class SchemeSubscriptionSpec extends AnyFlatSpec with Matchers {

  "SchemeSubscription.details" should "output the correct map of data" in {

    val psaIdentifier = "test-psa-id"
    val status = 1
    val request = Json.obj("name" -> "request")
    val response = Json.obj("name" -> "response")

    val event = SchemeSubscription(
      psaIdentifier = psaIdentifier,
      schemeType = Some(SchemeType.singleTrust),
      hasIndividualEstablisher = true,
      hasCompanyEstablisher = false,
      hasPartnershipEstablisher = true,
      hasDormantCompany = false,
      hasBankDetails = true,
      hasValidBankDetails = false,
      status = status,
      request = request,
      response = Some(response)
    )

    val expected: Map[String, String] = Map(
      "psaIdentifier" -> psaIdentifier,
      "schemeType" -> SchemeType.singleTrust.toString,
      "hasIndividualEstablisher" -> "true",
      "hasCompanyEstablisher" -> "false",
      "hasPartnershipEstablisher" -> "true",
      "hasDormantCompany" -> "false",
      "hasBankDetails" -> "true",
      "hasValidBankDetails" -> "false",
      "status" -> status.toString,
      "request" -> Json.stringify(request),
      "response" -> Json.stringify(response)
    )

    event.details shouldBe expected

  }

}
