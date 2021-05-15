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

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

import scala.language.postfixOps

case class PspSchemeDetailsAuditEvent(
                                       pspId: String,
                                       status: Int,
                                       payload: Option[JsValue]
                                     ) extends ExtendedAuditEvent {

  override def auditType: String = "GetPensionSchemePractitionerSchemeDetails"

  override def details: JsObject = Json.obj(
    "pensionSchemePractitionerId" -> pspId,
    "status" -> status.toString,
    "payload" -> {
      payload match {
        case Some(json) => expandAcronymTransformer(json)
        case _ => Json.obj()
      }
    }
  )

  val doNothing: Reads[JsObject] = {
    __.json.put(Json.obj())
  }

  private val expandAcronymTransformer: JsValue => JsObject =
    json => json.as[JsObject].transform(
      __.json.update(
        (
          (__ \ "pensionSchemePractitionerDetails").json.copyFrom(
            (__ \ "pspDetails").json.pick
          ) and
            (__ \ "pensionSchemeTaxReference").json.copyFrom(
              (__ \ "pstr").json.pick
            ) and
            ((__ \ "schemeReferenceNumber").json.copyFrom(
              (__ \ "srn").json.pick
            ) orElse doNothing) and
            (__ \ "pensionSchemePractitionerDetails" \ "authorisingPensionSchemeAdministratorID").json.copyFrom(
              (__ \ "pspDetails" \ "authorisingPSAID").json.pick
            ) and
            (__ \ "pensionSchemePractitionerDetails" \ "authorisingPensionSchemeAdministrator").json.copyFrom(
              (__ \ "pspDetails" \ "authorisingPSA").json.pick
            ) and
            ((__ \ "pensionSchemePractitionerDetails" \ "pensionSchemePractitionerClientReference").json.copyFrom(
              (__ \ "pspDetails" \ "pspClientReference").json.pick
            ) orElse doNothing)
          ) reduce
      ) andThen
        (__ \ "pspDetails").json.prune andThen
        (__ \ "pensionSchemePractitionerDetails" \ "authorisingPSAID").json.prune andThen
        (__ \ "pensionSchemePractitionerDetails" \ "authorisingPSA").json.prune andThen
        (__ \ "pensionSchemePractitionerDetails" \ "pspClientReference").json.prune andThen
        (__ \ "pstr").json.prune andThen
        (__ \ "srn").json.prune
    ).getOrElse(throw ExpandAcronymTransformerFailed)

  case object ExpandAcronymTransformerFailed extends Exception

}
