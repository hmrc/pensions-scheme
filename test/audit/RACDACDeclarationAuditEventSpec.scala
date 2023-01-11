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
import play.api.http.Status
import play.api.libs.json.Json

class RACDACDeclarationAuditEventSpec extends AnyWordSpec with Matchers {

  "RACDACDeclarationAuditEvent" must {

    "serialise and deserialise correctly" in {

      val auditEvent: RACDACDeclarationAuditEvent =
        RACDACDeclarationAuditEvent(psaIdentifier = "", status = Status.OK,
          request = Json.obj("requestKey" -> "requestValue"), response = Some(Json.obj("key" -> "value")))

      auditEvent.auditType mustBe "RACDACDeclaration"

      auditEvent.details mustBe
        Json.parse("""{"psaIdentifier":"","status":"200","request":{"requestKey":"requestValue"},"response":{"key":"value"}}""")
    }
  }
}
