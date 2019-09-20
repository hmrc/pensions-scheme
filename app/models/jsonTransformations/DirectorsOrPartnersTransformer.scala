/*
 * Copyright 2019 HM Revenue & Customs
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

package models.jsonTransformations

import com.google.inject.Inject
import config.FeatureSwitchManagementService
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import utils.Toggles

class DirectorsOrPartnersTransformer @Inject()(addressTransformer: AddressTransformer,
                                               override val fs: FeatureSwitchManagementService) extends JsonTransformer {

  def userAnswersDirectorReads(desPath: JsPath): Reads[JsObject] = {
      userAnswersIndividualDetailsReadsHnS("directorDetails", desPath) and
      userAnswersNinoReadsHnS("directorNino", desPath) and
      userAnswersUtrReadsHnS("directorUniqueTaxReference", desPath) and
      addressTransformer.getDifferentAddress(__ \ 'directorAddressId, desPath \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears(desPath, __ \ 'companyDirectorAddressYears) and
      addressTransformer.getPreviousAddress(desPath, __ \ 'previousAddress) and
      userAnswersContactDetailsReads("directorContactDetails", desPath) and
      (__ \ 'isDirectorComplete).json.put(JsBoolean(true)) reduce
  }

  def userAnswersPartnerReads(desPath: JsPath): Reads[JsObject] =
    (if(fs.get(Toggles.isHnSEnabled))
      userAnswersIndividualDetailsReadsHnS("partnerDetails", desPath)
    else
      userAnswersIndividualDetailsReads("partnerDetails", desPath)) and
    (if(fs.get(Toggles.isHnSEnabled))
      userAnswersNinoReadsHnS("partnerNino", desPath)
    else
      userAnswersNinoReads("partnerNino", desPath)) and
    (if(fs.get(Toggles.isHnSEnabled))
      userAnswersUtrReadsHnS("partnerUniqueTaxReference", desPath)
    else
      userAnswersUtrReads("partnerUniqueTaxReference", desPath)) and
      addressTransformer.getDifferentAddress(__ \ 'partnerAddressId, desPath \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears(desPath, __ \ 'partnerAddressYears) and
      addressTransformer.getPreviousAddress(desPath, __ \ 'partnerPreviousAddress) and
      userAnswersContactDetailsReads("partnerContactDetails", desPath) and
      (__ \ 'isPartnerComplete).json.put(JsBoolean(true)) reduce
}
