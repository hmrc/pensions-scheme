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

import org.scalatest.{AsyncFlatSpec, Inside, Matchers}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{Format, JsDefined, JsString, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Disabled
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class AuditServiceSpec extends AsyncFlatSpec with Matchers with Inside {

  import AuditServiceSpec._

  "AuditServiceImpl" should "construct and send the correct event" in {

    implicit val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest()

    val event = TestAuditEvent("test-audit-payload")

    val result = auditService().sendEvent(event)
    val sentEvent = FakeAuditConnector.lastSentEvent

    result.map {
      auditResult =>
        auditResult shouldBe Disabled

        inside(sentEvent) {
          case ExtendedDataEvent(auditSource, auditType, _, _, detail, _) =>
            auditSource shouldBe appName
            auditType shouldBe "TestAuditEvent"
            (detail \ "data" \ "payload") shouldBe JsDefined(JsString("test-audit-payload"))
        }
    }

  }

}

object AuditServiceSpec {

  private val app = new GuiceApplicationBuilder()
    .overrides(
      bind[AuditConnector].toInstance(FakeAuditConnector)
    )
    .build()

  def fakeRequest(): FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  def auditService(): AuditService = app.injector.instanceOf[AuditService]

  def appName: String = app.configuration.underlying.getString("appName")

}

//noinspection ScalaDeprecation
object FakeAuditConnector extends AuditConnector {

  private var sentEvent:ExtendedDataEvent = _

  override def auditingConfig: AuditingConfig = AuditingConfig(None, false)

  override def sendExtendedEvent(event: ExtendedDataEvent)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuditResult] = {
    sentEvent = event
    super.sendExtendedEvent(event)
  }

  def lastSentEvent: ExtendedDataEvent = sentEvent

}

case class TestAuditEvent(payload: String) extends AuditEvent {
  override def auditType: String = "TestAuditEvent"
}

object TestAuditEvent {
  implicit val formatsTestAuditEvent: Format[TestAuditEvent] = Json.format[TestAuditEvent]
}