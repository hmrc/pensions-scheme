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

import models.Sent
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.domain.PsaId

class EmailAuditEventSpec extends AnyWordSpec with Matchers {
  val psaId = "A0000000"
  "EmailAuditEvent" must {
    "have correct audit type and details" in {
      val eae = EmailAuditEvent(psaId = PsaId(psaId), event = Sent)
      eae.auditType mustBe "SchemeEmailEvent"
      eae.details mustBe Map("psaId" -> psaId, "event" -> Sent.toString)
    }
  }

}
