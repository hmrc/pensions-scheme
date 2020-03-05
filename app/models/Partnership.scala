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

import play.api.libs.json.{Format, JsPath, Json, Writes}
import play.api.libs.json.Writes.seq
import play.api.libs.functional.syntax._

case class Partnership(
                        organizationName: String,
                        utr: Option[String] = None,
                        noUtrReason: Option[String] = None,
                        vatRegistrationNumber: Option[String] = None,
                        payeReference: Option[String] = None,
                        haveMoreThanTenDirectorOrPartner: Boolean,
                        correspondenceAddressDetails: CorrespondenceAddressDetails,
                        correspondenceContactDetails: CorrespondenceContactDetails,
                        previousAddressDetails: Option[PreviousAddressDetails] = None,
                        partnerDetails: Seq[Individual]
                      )

object Partnership {
  implicit val formats: Format[Partnership] = Json.format[Partnership]

  val updateWrites: Writes[Partnership] = (
    (JsPath \ "partnershipName").write[String] and
      (JsPath \ "utr").writeNullable[String] and
      (JsPath \ "noUtrReason").writeNullable[String] and
      (JsPath \ "vatRegistrationNumber").writeNullable[String] and
      (JsPath \ "payeReference").writeNullable[String] and
      (JsPath \ "hasMoreThanTenPartners").writeNullable[Boolean] and
      (JsPath \ "correspondenceAddressDetails").write[CorrespondenceAddressDetails](CorrespondenceAddressDetails.updateWrites) and
      (JsPath \ "correspondenceContactDetails").write[CorrespondenceContactDetails] and
      (JsPath \ "previousAddressDetails").write[PreviousAddressDetails](PreviousAddressDetails.psaUpdateWrites) and
      (JsPath \ "partnerDetails").write(seq(Individual.individualUpdateWrites))
    ) (partnership => (partnership.organizationName,
    partnership.utr,
    partnership.noUtrReason,
    partnership.vatRegistrationNumber,
    partnership.payeReference,
    Some(partnership.haveMoreThanTenDirectorOrPartner),
    partnership.correspondenceAddressDetails,
    partnership.correspondenceContactDetails,
    partnership.previousAddressDetails.fold(PreviousAddressDetails(isPreviousAddressLast12Month = false))(c => c),
    partnership.partnerDetails
  ))
}