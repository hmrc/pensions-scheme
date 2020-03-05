/*
 * Copyright 2020 HM Revenue & Customs
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

package models

import models.userAnswersToEtmp.ReadsTrusteeCompany.readsTrusteeCompanies
import models.userAnswersToEtmp.ReadsTrusteeIndividual.readsTrusteeIndividuals
import models.userAnswersToEtmp.ReadsTrusteePartnership.readsTrusteePartnerships
import play.api.libs.json._
import play.api.libs.json.Writes.seq
import play.api.libs.functional.syntax._

case class TrusteeDetails(
                           individualTrusteeDetail: Seq[Individual],
                           companyTrusteeDetail: Seq[CompanyTrustee],
                           partnershipTrusteeDetail: Seq[PartnershipTrustee]
                         )
object TrusteeDetails {
  implicit val formats : Format[TrusteeDetails] = Json.format[TrusteeDetails]

  val readsTrusteeDetails: Reads[TrusteeDetails] = (
    (JsPath \ "trustees").readNullable(readsTrusteeIndividuals) and
      (JsPath \ "trustees").readNullable(readsTrusteeCompanies) and
      (JsPath \ "trustees").readNullable(readsTrusteePartnerships)
    ) ((trusteeIndividuals, trusteeCompanies, trusteePartnerships) =>
    TrusteeDetails(
      individualTrusteeDetail = trusteeIndividuals.getOrElse(Nil),
      companyTrusteeDetail = trusteeCompanies.getOrElse(Nil),
      partnershipTrusteeDetail = trusteePartnerships.getOrElse(Nil)
    )
  )

  val updateWrites : Writes[TrusteeDetails] = (
    (JsPath \ "individualDetails").writeNullable(seq(Individual.individualUpdateWrites)) and
      (JsPath \ "companyTrusteeDetailsType").writeNullable(seq(CompanyTrustee.updateWrites)) and
      (JsPath \ "partnershipTrusteeDetails").writeNullable(seq(PartnershipTrustee.updateWrites))
    )(trustee => (
    if (trustee.individualTrusteeDetail.nonEmpty) Some(trustee.individualTrusteeDetail) else None,
    if (trustee.companyTrusteeDetail.nonEmpty) Some(trustee.companyTrusteeDetail) else None,
    if (trustee.partnershipTrusteeDetail.nonEmpty) Some(trustee.partnershipTrusteeDetail) else None)
  )
}
