/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.libs.json.{JsObject, JsValue, Json, Reads, __}
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class PspSchemeDetailsAuditEvent(
                                       pspId: String,
                                       status: Int,
                                       payload: Option[JsValue]
                                     ) extends AuditEvent {

  override def auditType: String = "GetPensionSchemePractitionerSchemeDetails"

  override def details: Map[String, String] = Map(
    "pensionSchemePractitionerId" -> pspId,
    "status" -> status.toString,
    "payload" -> payload.fold("")(Json.stringify)
  )

  val doNothing: Reads[JsObject] = {
    __.json.put(Json.obj())
  }

  private val expandAcronymTransformer: JsValue => JsObject =
    json => json.as[JsObject].transform(
      __.json.update(
        (
          (__ \ "pensionSchemePractitionerSchemeDetails").json.copyFrom(
            (__ \ "pspDetails").json.pick
          ) and
            (__ \ "pensionSchemeTaxReference").json.copyFrom(
              (__ \ "pstr").json.pick
            ) and
            (__ \ "schemeReferenceNumber").json.copyFrom(
              (__ \ "srn").json.pick
            ) and
            (__ \ "pensionSchemePractitionerSchemeDetails" \ "authorisingPensionSchemeAdministratorID").json.copyFrom(
              (__ \ "pspDetails" \ "authorisingPSAID").json.pick
            ) and
            (__ \ "pensionSchemePractitionerSchemeDetails" \ "authorisingPensionSchemeAdministrator").json.copyFrom(
              (__ \ "pspDetails" \ "authorisingPSA").json.pick
            ) and
            ((__ \ "pensionSchemePractitionerSchemeDetails" \ "pensionSchemePractitionerClientReference").json.copyFrom(
              (__ \ "pspDetails" \ "pspClientReference").json.pick) orElse doNothing)
          ) reduce
      ) andThen
        (__ \ "pspDetails").json.prune andThen
        (__ \ "pensionSchemePractitionerSchemeDetails" \ "authorisingPSAID").json.prune andThen
        (__ \ "pensionSchemePractitionerSchemeDetails" \ "authorisingPSA").json.prune andThen
        (__ \ "pensionSchemePractitionerSchemeDetails" \ "pspClientReference").json.prune andThen
        (__ \ "pstr").json.prune andThen
        (__ \ "srn").json.prune
    ).getOrElse(throw ExpandAcronymTransformerFailed)

  case object ExpandAcronymTransformerFailed extends Exception

  println(s"\n\n\n\n\n ${Json.prettyPrint(expandAcronymTransformer(payload.get))} \n\n\n\n")
}
