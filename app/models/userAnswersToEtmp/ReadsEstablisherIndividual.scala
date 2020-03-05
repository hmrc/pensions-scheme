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

package models.userAnswersToEtmp

import models.userAnswersToEtmp.ReadsCommon.{previousAddressDetails, readsFiltered, readsPersonDetails, readsContactDetails}
import models._
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, __}

object ReadsEstablisherIndividual {

  def readsEstablisherIndividuals: Reads[Seq[Individual]] =
    readsFiltered(_ \ "establisherDetails", readsEstablisherIndividual, "establisherDetails")

  def readsEstablisherIndividual: Reads[Individual] = (
    readsPersonDetails("establisherDetails") and
      (JsPath \ "address").read[Address] and
      (JsPath \ "contactDetails").read[ContactDetails](readsContactDetails) and
      (JsPath \ "establisherNino").readNullable[String]((__ \ "value").read[String]) and
      (JsPath \ "noNinoReason").readNullable[String] and
      (JsPath \ "utr").readNullable[String]((__ \ "value").read[String]) and
      (JsPath \ "noUtrReason").readNullable[String] and
      (JsPath \ "addressYears").read[String] and
      (JsPath \ "previousAddress").readNullable[Address]
    ) ((personalDetails, address, contactDetails, nino, noNinoReason, utr, noUtrReason, addressYears, previousAddress) =>
    Individual(
      personalDetails = personalDetails,
      referenceOrNino = nino,
      noNinoReason = noNinoReason,
      utr = utr,
      noUtrReason = noUtrReason,
      correspondenceAddressDetails = CorrespondenceAddressDetails(address),
      correspondenceContactDetails = CorrespondenceContactDetails(contactDetails),
      previousAddressDetails = previousAddressDetails(addressYears, previousAddress)
    )
  )

}
