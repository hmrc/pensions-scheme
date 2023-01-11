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

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsString, Json}

class SchemeUpdateSpec extends AnyWordSpec with Matchers {
  val psaId = "A0000000"
  val requestJsValue = JsString("test request")
  val responseJsValue = JsString("test response")
  val su = SchemeUpdate(
    psaIdentifier = psaId,
    schemeType = Some(SchemeType.singleTrust),
    status = 33,
    request = requestJsValue,
    response = None
  )
  "SchemeUpdate" must {
    "have correct details where there is a response" in {

      val su = SchemeUpdate(
        psaIdentifier = psaId,
        schemeType = Some(SchemeType.singleTrust),
        status = 33,
        request = requestJsValue,
        response = Some(responseJsValue)
      )
      su.details mustBe Map(
        "psaIdentifier" -> psaId,
        "status" -> "33",
        "request" -> Json.stringify(requestJsValue),
        "response" -> Json.stringify(responseJsValue),
        "schemeType" -> SchemeType.singleTrust.toString
      )
    }

    "have correct details where there is no response" in {
      su.details mustBe Map(
        "psaIdentifier" -> psaId,
        "status" -> "33",
        "request" -> Json.stringify(requestJsValue),
        "response" -> "",
        "schemeType" -> SchemeType.singleTrust.toString
      )
    }

    "have correct audit type" in {
      su.auditType mustBe "SchemeVariation"
    }
  }
}
