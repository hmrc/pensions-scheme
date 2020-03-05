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

import models.userAnswersToEtmp.ReadsCommon.{partnershipReads, previousAddressDetails, readsContactDetails, readsFiltered, readsPersonDetails}
import models._
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, __}

object ReadsEstablisherPartnership {

  def readsEstablisherPartnerships: Reads[Seq[Partnership]] =
    readsFiltered(_ \ "partnershipDetails", readsEstablisherPartnership, "partnershipDetails")

  private def readsEstablisherPartnership: Reads[Partnership] = (
    JsPath.read(partnershipReads) and
      (JsPath \ "otherPartners").readNullable[Boolean] and
      (JsPath \ "partner").readNullable(readsPartners)
    ) ((partnership, otherPartners, partners) =>
    Partnership(
      organizationName = partnership.name,
      utr = partnership.utr,
      noUtrReason = partnership.utrReason,
      vatRegistrationNumber = partnership.vat,
      payeReference = partnership.paye,
      haveMoreThanTenDirectorOrPartner = otherPartners.getOrElse(false),
      correspondenceAddressDetails = CorrespondenceAddressDetails(partnership.address),
      correspondenceContactDetails = CorrespondenceContactDetails(partnership.contact),
      previousAddressDetails = previousAddressDetails(partnership.addressYears, partnership.previousAddress, partnership.tradingTime),
      partnerDetails = partners.getOrElse(Nil)
    )
  )

  private def readsPartners: Reads[Seq[Individual]] =
    readsFiltered(_ \ "partnerDetails", readsPartner, "partnerDetails")

  private def readsPartner: Reads[Individual] = (
    readsPersonDetails(userAnswersBase = "partnerDetails") and
      (JsPath \ "partnerAddressId").read[Address] and
      (JsPath \ "partnerContactDetails").read[ContactDetails](readsContactDetails) and
      (JsPath \ "partnerNino").readNullable[String]((__ \ "value").read[String]) and
      (JsPath \ "noNinoReason").readNullable[String] and
      (JsPath \ "utr").readNullable[String]((__ \ "value").read[String]) and
      (JsPath \ "noUtrReason").readNullable[String] and
      (JsPath \ "partnerAddressYears").read[String] and
      (JsPath \ "partnerPreviousAddress").readNullable[Address]
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
