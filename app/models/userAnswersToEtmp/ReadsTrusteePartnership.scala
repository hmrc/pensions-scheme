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

import models.{CorrespondenceAddressDetails, CorrespondenceContactDetails, PartnershipTrustee}
import ReadsCommon.{partnershipReads, readsFiltered, previousAddressDetails}
import play.api.libs.functional.syntax._
import play.api.libs.json._

object ReadsTrusteePartnership {

  def readsTrusteePartnerships: Reads[Seq[PartnershipTrustee]] =
    readsFiltered(_ \ "partnershipDetails", readsTrusteePartnership, "partnershipDetails")

  private def readsTrusteePartnership: Reads[PartnershipTrustee] =
    JsPath.read(partnershipReads).map(partnership =>
      PartnershipTrustee(
        organizationName = partnership.name,
        utr = partnership.utr,
        noUtrReason = partnership.utrReason,
        vatRegistrationNumber = partnership.vat,
        payeReference = partnership.paye,
        correspondenceAddressDetails = CorrespondenceAddressDetails(partnership.address),
        correspondenceContactDetails = CorrespondenceContactDetails(partnership.contact),
        previousAddressDetails = previousAddressDetails(partnership.addressYears, partnership.previousAddress, partnership.tradingTime)
      )
    )

}
