package audit

import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.Json

class SchemeDetailsAuditEventSpec extends WordSpec with MustMatchers {

  private val psaId = "A2500001"
  private val status = 200
  private val payload = Json.toJson(Json.obj("name"->"abc"))


  val event = SchemeDetailsAuditEvent(psaId, status, Some(payload))

  val expectedDetails = Map(
    "psaId" -> psaId,
    "status" -> status.toString,
    "payload" -> payload.toString
  )

  "calling SchemeDetailsAuditEvent" must {

    " returns correct event object" in {

      event.auditType mustBe "GetSchemeDetails"

      event.details mustBe expectedDetails
    }
  }
}
