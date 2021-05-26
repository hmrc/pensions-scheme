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

  def userAnswersDirectorReads(desPath: JsPath): Reads[JsObject] = {
    userAnswersIndividualDetailsReads("directorDetails", desPath) and
      userAnswersNinoReads("directorNino", desPath) and
      userAnswersUtrReads(desPath) and
      addressTransformer.getDifferentAddress(__ \ 'directorAddressId, desPath \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears(desPath, __ \ 'companyDirectorAddressYears) and
      addressTransformer.getPreviousAddress(desPath, __ \ 'previousAddress) and
      userAnswersContactDetailsReads("directorContactDetails", desPath) and
      (__ \ 'isDirectorComplete).json.put(JsBoolean(true)) reduce
  }

  def userAnswersPartnerReads(desPath: JsPath): Reads[JsObject] =
    userAnswersIndividualDetailsReads("partnerDetails", desPath) and
      userAnswersNinoReads("partnerNino", desPath) and
      userAnswersUtrReads(desPath) and
      addressTransformer.getDifferentAddress(__ \ 'partnerAddressId, desPath \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears(desPath, __ \ 'partnerAddressYears) and
      addressTransformer.getPreviousAddress(desPath, __ \ 'partnerPreviousAddress) and
      userAnswersContactDetailsReads("partnerContactDetails", desPath) and
      (__ \ 'isPartnerComplete).json.put(JsBoolean(true)) reduce
}
