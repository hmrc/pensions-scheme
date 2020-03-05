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

package models.userAnswersToEtmp.establisher

import models.userAnswersToEtmp.Individual
import models.userAnswersToEtmp.ReadsHelper.readsFiltered
import play.api.libs.functional.syntax._
import play.api.libs.json.Writes.seq
import play.api.libs.json._

case class EstablisherDetails(
                               individual: Seq[Individual],
                               companyOrOrganization: Seq[CompanyEstablisher],
                               partnership: Seq[Partnership]
                             )

object EstablisherDetails {
  implicit val formats: Format[EstablisherDetails] = Json.format[EstablisherDetails]

  val readsEstablisherDetails: Reads[EstablisherDetails] = (
    (JsPath \ "establishers").readNullable(
      readsFiltered(_ \ "establisherDetails", Individual.readsEstablisherIndividual, "establisherDetails")
    ) and
      (JsPath \ "establishers").readNullable(
        readsFiltered(_ \ "companyDetails", CompanyEstablisher.readsEstablisherCompany, "companyDetails")
      ) and
      (JsPath \ "establishers").readNullable(
        readsFiltered(_ \ "partnershipDetails", Partnership.readsEstablisherPartnership, "partnershipDetails"))
    ) ((establisherIndividuals, establisherCompanies, establisherPartnerships) =>
    EstablisherDetails(
      individual = establisherIndividuals.getOrElse(Nil),
      companyOrOrganization = establisherCompanies.getOrElse(Nil),
      partnership = establisherPartnerships.getOrElse(Nil)
    )
  )

  val updateWrites: Writes[EstablisherDetails] = (
    (JsPath \ "individualDetails").writeNullable(seq(Individual.individualUpdateWrites)) and
      (JsPath \ "companyOrOrganisationDetails").writeNullable(seq(CompanyEstablisher.updateWrites)) and
      (JsPath \ "partnershipDetails").writeNullable(seq(Partnership.updateWrites))
    ) (establishers => (
    if (establishers.individual.nonEmpty) Some(establishers.individual) else None,
    if (establishers.companyOrOrganization.nonEmpty) Some(establishers.companyOrOrganization) else None,
    if (establishers.partnership.nonEmpty) Some(establishers.partnership) else None)
  )
}
