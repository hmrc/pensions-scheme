/*
 * Copyright 2025 HM Revenue & Customs
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

import org.apache.pekko.stream.Materializer
import org.scalatest.Inside
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{ApplicationLifecycle, bind}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import repositories.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.*
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.compiletime.uninitialized
import scala.concurrent.{ExecutionContext, Future}

class AuditServiceSpec extends AsyncFlatSpec with Matchers with Inside {

  import AuditServiceSpec._

  "AuditServiceImpl" should "construct and send the correct event" in {

    implicit val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest()

    val event = TestAuditEvent("test-audit-payload")

    auditService().sendEvent(event)

    val sentEvent = FakeAuditConnector.lastSentEvent

    inside(sentEvent) {
      case DataEvent(auditSource, auditType, _, _, detail, _, _, _, _) =>
        auditSource shouldBe appName
        auditType shouldBe "TestAuditEvent"
        detail should contain("payload" -> "test-audit-payload")
    }

  }

}

object AuditServiceSpec extends MockitoSugar {

  private val app = new GuiceApplicationBuilder()
    .overrides(
      bind[AuditConnector].toInstance(FakeAuditConnector),
      bind[LockRepository].toInstance(mock[LockRepository]),
      bind[RacdacSchemeSubscriptionCacheRepository].toInstance(mock[RacdacSchemeSubscriptionCacheRepository]),
      bind[SchemeCacheRepository].toInstance(mock[SchemeCacheRepository]),
      bind[SchemeDetailsCacheRepository].toInstance(mock[SchemeDetailsCacheRepository]),
      bind[SchemeDetailsWithIdCacheRepository].toInstance(mock[SchemeDetailsWithIdCacheRepository]),
      bind[SchemeSubscriptionCacheRepository].toInstance(mock[SchemeSubscriptionCacheRepository]),
      bind[UpdateSchemeCacheRepository].toInstance(mock[UpdateSchemeCacheRepository])
    )
    .build()

  def fakeRequest(): FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  def auditService(): AuditService = app.injector.instanceOf[AuditService]

  def appName: String = app.configuration.underlying.getString("appName")

}

//noinspection ScalaDeprecation
object FakeAuditConnector extends AuditConnector {

  private var sentEvent: DataEvent = uninitialized

  override def auditingConfig: AuditingConfig =
    AuditingConfig(
      consumer = None,
      enabled = false,
      auditSource = "test audit source",
      auditSentHeaders = true
    )

  override def sendEvent(event: DataEvent)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuditResult] = {
    sentEvent = event
    super.sendEvent(event)
  }

  def lastSentEvent: DataEvent = sentEvent

  def materializer: Materializer = ???

  def lifecycle: ApplicationLifecycle = ???

  def auditChannel: AuditChannel = ???

  override def datastreamMetrics: DatastreamMetrics = ???
}

case class TestAuditEvent(payload: String) extends AuditEvent {

  override def auditType: String = "TestAuditEvent"

  override def details: Map[String, String] =
    Map(
      "payload" -> payload
    )

}
