/*
 * Copyright 2024 HM Revenue & Customs
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

import models.userAnswersToEtmp.ReadsHelper.previousAddressDetails
import models.userAnswersToEtmp.{CorrespondenceAddressDetails, CorrespondenceContactDetails, PartnershipDetail, PreviousAddressDetails}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import utils.UtrHelper.stripUtr

case class PartnershipTrustee(
                               organizationName: String,
                               utr: Option[String] = None,
                               noUtrReason: Option[String] = None,
                               vatRegistrationNumber: Option[String] = None,
                               payeReference: Option[String] = None,
                               correspondenceAddressDetails: CorrespondenceAddressDetails,
                               correspondenceContactDetails: CorrespondenceContactDetails,
                               previousAddressDetails: Option[PreviousAddressDetails] = None
                             )

object PartnershipTrustee {
  implicit val formats: Format[PartnershipTrustee] = Json.format[PartnershipTrustee]

  val readsTrusteePartnership: Reads[PartnershipTrustee] =
    JsPath.read(PartnershipDetail.partnershipReads).map(partnership =>
      PartnershipTrustee(
        organizationName = partnership.name,
        utr = stripUtr(partnership.utr),
        noUtrReason = partnership.utrReason,
        vatRegistrationNumber = partnership.vat,
        payeReference = partnership.paye,
        correspondenceAddressDetails = CorrespondenceAddressDetails(partnership.address),
        correspondenceContactDetails = CorrespondenceContactDetails(partnership.contact),
        previousAddressDetails = previousAddressDetails(partnership.addressYears, partnership.previousAddress)
      )
    )

  val updateWrites: Writes[PartnershipTrustee] = (
    (JsPath \ "partnershipName").write[String] and
      (JsPath \ "utr").writeNullable[String] and
      (JsPath \ "noUtrReason").writeNullable[String] and
      (JsPath \ "vatRegistrationNumber").writeNullable[String] and
      (JsPath \ "payeReference").writeNullable[String] and
      (JsPath \ "correspondenceAddressDetails").write[CorrespondenceAddressDetails](CorrespondenceAddressDetails.updateWrites) and
      (JsPath \ "correspondenceContactDetails").write[CorrespondenceContactDetails] and
      (JsPath \ "previousAddressDetails").write[PreviousAddressDetails](PreviousAddressDetails.psaUpdateWrites)
    ) (company => (company.organizationName,
    company.utr,
    company.noUtrReason,
    company.vatRegistrationNumber,
    company.payeReference,
    company.correspondenceAddressDetails,
    company.correspondenceContactDetails,
    company.previousAddressDetails.fold(PreviousAddressDetails(isPreviousAddressLast12Month = false))(c => c))
  )
}
