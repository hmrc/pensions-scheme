package audit

import play.api.libs.json.{JsValue, Json}

case class SchemeDetailsAuditEvent(psaId: String,  status: Int, payload: Option[JsValue]) extends AuditEvent {

  override def auditType: String = "GetSchemeDetails"

  override def details: Map[String, String] = Map(
    "psaId" -> psaId,
    "status" -> status.toString,
    "payload" -> payload.fold("")(Json.stringify)
  )

}
