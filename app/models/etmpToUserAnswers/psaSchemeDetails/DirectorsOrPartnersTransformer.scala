/*
 * Copyright 2021 HM Revenue & Customs
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

package models.etmpToUserAnswers.psaSchemeDetails

import com.google.inject.Inject
import models.etmpToUserAnswers.AddressTransformer
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

class DirectorsOrPartnersTransformer @Inject()(addressTransformer: AddressTransformer) extends JsonTransformer {

  def userAnswersDirectorReads: Reads[JsObject] = {
    userAnswersIndividualDetailsReads("directorDetails") and
      userAnswersNinoReads("directorNino") and
      userAnswersUtrReads and
      addressTransformer.getDifferentAddress(__ \ 'directorAddressId, __ \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears(__ \ 'companyDirectorAddressYears) and
      addressTransformer.getPreviousAddress( __ \ 'previousAddress) and
      userAnswersContactDetailsReads("directorContactDetails") and
      (__ \ 'isDirectorComplete).json.put(JsBoolean(true)) reduce
  }

  def userAnswersPartnerReads: Reads[JsObject] =
    userAnswersIndividualDetailsReads("partnerDetails") and
      userAnswersNinoReads("partnerNino") and
      userAnswersUtrReads and
      addressTransformer.getDifferentAddress(__ \ 'partnerAddressId, __ \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears( __ \ 'partnerAddressYears) and
      addressTransformer.getPreviousAddress( __ \ 'partnerPreviousAddress) and
      userAnswersContactDetailsReads("partnerContactDetails") and
      (__ \ 'isPartnerComplete).json.put(JsBoolean(true)) reduce
}
