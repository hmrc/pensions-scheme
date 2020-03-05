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

import models.{CompanyTrustee, CorrespondenceAddressDetails, CorrespondenceContactDetails}
import ReadsCommon.{companyReads, readsFiltered, previousAddressDetails}
import play.api.libs.json.{JsPath, Reads}
import play.api.libs.functional.syntax._
import play.api.libs.json._

object ReadsTrusteeCompany {

  def readsTrusteeCompanies: Reads[Seq[CompanyTrustee]] =
    readsFiltered(_ \ "companyDetails", readsTrusteeCompany, "companyDetails")

  private def readsTrusteeCompany: Reads[CompanyTrustee] = JsPath.read(companyReads).map(test => CompanyTrustee(
    organizationName = test.name,
    utr = test.utr,
    noUtrReason = test.noUtrReason,
    crnNumber = test.crn,
    noCrnReason = test.noCrnReason,
    vatRegistrationNumber = test.vatNumber,
    payeReference = test.payeNumber,
    correspondenceAddressDetails = CorrespondenceAddressDetails(test.address),
    correspondenceContactDetails = CorrespondenceContactDetails(test.contactDetails),
    previousAddressDetails = previousAddressDetails(test.addressYears, test.previousAddress)))

}
