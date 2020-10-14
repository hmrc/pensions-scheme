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

package models.userAnswersToEtmp.trustee

import models.userAnswersToEtmp.Individual
import models.userAnswersToEtmp.ReadsHelper.readsFiltered
import play.api.libs.functional.syntax._
import play.api.libs.json.Writes.seq
import play.api.libs.json._
import models.userAnswersToEtmp.ReadsHelper.readsFilteredBoolean

case class TrusteeDetails(
                           individualTrusteeDetail: Seq[Individual],
                           companyTrusteeDetail: Seq[CompanyTrustee],
                           partnershipTrusteeDetail: Seq[PartnershipTrustee]
                         )

object TrusteeDetails {
  implicit val formats: Format[TrusteeDetails] = Json.format[TrusteeDetails]

  private val isKindIndividual: JsValue => Boolean = js => (js \ "trusteeKind").asOpt[String].contains( "individual")
  private val isKindCompany: JsValue => Boolean = js => (js \ "trusteeKind").asOpt[String].contains("company")
  private val isKindPartnership: JsValue => Boolean = js => (js \ "trusteeKind").asOpt[String].contains("partnership")

  val readsTrusteeDetails: Reads[TrusteeDetails] = (
    (JsPath \ "trustees").readNullable(
      readsFilteredBoolean( isKindIndividual, Individual.readsTrusteeIndividual, "trusteeDetails")
      //readsFiltered(_ \ "trusteeDetails", Individual.readsTrusteeIndividual, "trusteeDetails")
    ) and
      (JsPath \ "trustees").readNullable(
        readsFilteredBoolean(isKindCompany, CompanyTrustee.readsTrusteeCompany, "companyDetails")
        //readsFiltered(_ \ "companyDetails", CompanyTrustee.readsTrusteeCompany, "companyDetails")
      ) and
      (JsPath \ "trustees").readNullable(
        readsFilteredBoolean(isKindPartnership, PartnershipTrustee.readsTrusteePartnership, "partnershipDetails")
        //readsFiltered(_ \ "partnershipDetails", PartnershipTrustee.readsTrusteePartnership, "partnershipDetails")
      )
    ) ((trusteeIndividuals, trusteeCompanies, trusteePartnerships) =>
    TrusteeDetails(
      individualTrusteeDetail = trusteeIndividuals.getOrElse(Nil),
      companyTrusteeDetail = trusteeCompanies.getOrElse(Nil),
      partnershipTrusteeDetail = trusteePartnerships.getOrElse(Nil)
    )
  )

  val updateWrites: Writes[TrusteeDetails] = (
    (JsPath \ "individualDetails").writeNullable(seq(Individual.individualUpdateWrites)) and
      (JsPath \ "companyTrusteeDetailsType").writeNullable(seq(CompanyTrustee.updateWrites)) and
      (JsPath \ "partnershipTrusteeDetails").writeNullable(seq(PartnershipTrustee.updateWrites))
    ) (trustee => (
    if (trustee.individualTrusteeDetail.nonEmpty) Some(trustee.individualTrusteeDetail) else None,
    if (trustee.companyTrusteeDetail.nonEmpty) Some(trustee.companyTrusteeDetail) else None,
    if (trustee.partnershipTrusteeDetail.nonEmpty) Some(trustee.partnershipTrusteeDetail) else None)
  )
}
