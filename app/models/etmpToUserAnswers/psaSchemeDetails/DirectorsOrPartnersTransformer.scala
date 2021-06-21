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

  def userAnswersDirectorReads(ifPath: JsPath): Reads[JsObject] = {
    userAnswersIndividualDetailsReads("directorDetails", ifPath) and
      userAnswersNinoReads("directorNino", ifPath) and
      userAnswersUtrReads(ifPath) and
      addressTransformer.getDifferentAddress(__ \ 'directorAddressId, ifPath \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears(ifPath, __ \ 'companyDirectorAddressYears) and
      addressTransformer.getPreviousAddress(ifPath, __ \ 'previousAddress) and
      userAnswersContactDetailsReads("directorContactDetails", ifPath) and
      (__ \ 'isDirectorComplete).json.put(JsBoolean(true)) reduce
  }

  def userAnswersPartnerReads(ifPath: JsPath): Reads[JsObject] =
    userAnswersIndividualDetailsReads("partnerDetails", ifPath) and
      userAnswersNinoReads("partnerNino", ifPath) and
      userAnswersUtrReads(ifPath) and
      addressTransformer.getDifferentAddress(__ \ 'partnerAddressId, ifPath \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears(ifPath, __ \ 'partnerAddressYears) and
      addressTransformer.getPreviousAddress(ifPath, __ \ 'partnerPreviousAddress) and
      userAnswersContactDetailsReads("partnerContactDetails", ifPath) and
      (__ \ 'isPartnerComplete).json.put(JsBoolean(true)) reduce
}
