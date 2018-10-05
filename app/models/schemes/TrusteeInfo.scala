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
import play.api.libs.json.{JsPath, Json, OFormat, Reads}
import play.api.libs.json.Reads.seq

case class TrusteeInfo(individual: Seq[IndividualInfo],
                       company: Seq[CompanyDetails],
                       partnership: Seq[PartnershipDetails])

object TrusteeInfo {

  val apiReads: Reads[TrusteeInfo] = (
    (JsPath \ "individualTrusteeDetails").readNullable(seq(IndividualInfo.apiReads)) and
      (JsPath \ "companyTrusteeDetails").readNullable(seq(CompanyDetails.apiReads)) and
      (JsPath \ "partnershipTrusteeDetails").readNullable(seq(PartnershipDetails.apiReads))
    ) ((individual, organization, partnership) =>
    TrusteeInfo(individual.getOrElse(Nil), organization.getOrElse(Nil), partnership.getOrElse(Nil)))

  implicit val formats: OFormat[TrusteeInfo] = Json.format[TrusteeInfo]

}