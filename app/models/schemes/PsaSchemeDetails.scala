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

package models.schemes

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads.seq
import play.api.libs.json._

case class PsaSchemeDetails(schemeDetails: SchemeDetails,
                            establisherDetails: Option[EstablisherInfo],
                            trusteeDetails: Option[TrusteeInfo],
                            psaDetails: Seq[PsaDetails])

object PsaSchemeDetails {

  val apiReads: Reads[PsaSchemeDetails] = (
    (JsPath \ "psaSchemeDetails" \ "schemeDetails").read(SchemeDetails.apiReads) and
      (JsPath \ "psaSchemeDetails" \ "establisherDetails").readNullable(EstablisherInfo.apiReads) and
      (JsPath \ "psaSchemeDetails" \ "trusteeDetails").readNullable(TrusteeInfo.apiReads) and
      (JsPath \ "psaSchemeDetails" \ "psaDetails").readNullable(seq(PsaDetails.apiReads))) (
    (schemeDetails, establisherDetails, trusteeDetails, psaDetails) =>
      PsaSchemeDetails(schemeDetails,
        validateEstablisher(establisherDetails),
        validateTrustee(trusteeDetails),
        psaDetails.getOrElse(Nil))
  )

  implicit val formats: OFormat[PsaSchemeDetails] = Json.format[PsaSchemeDetails]

  private def validateEstablisher(establisherDetails: Option[EstablisherInfo]): Option[EstablisherInfo] = {
    establisherDetails match {
      case Some(EstablisherInfo(Nil, Nil, Nil)) => None
      case _ => establisherDetails
    }
  }

  private def validateTrustee(trusteeDetails: Option[TrusteeInfo]): Option[TrusteeInfo] = {
    trusteeDetails match {
      case Some(TrusteeInfo(Nil, Nil, Nil)) => None
      case _ => trusteeDetails
    }
  }
}

