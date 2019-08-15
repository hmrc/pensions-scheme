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

class TrusteeDetailsTransformer @Inject()(addressTransformer: AddressTransformer,
                                          override val fs: FeatureSwitchManagementService) extends JsonTransformer {

  val userAnswersTrusteesReads: Reads[JsObject] = {
    (__ \ 'psaSchemeDetails \ 'trusteeDetails).readNullable(__.read(
      (__ \ 'individualTrusteeDetails).readNullable(
        __.read(Reads.seq(userAnswersTrusteeIndividualReads(__))).map(JsArray(_))).flatMap { individual =>
        (__ \ 'companyTrusteeDetails).readNullable(
          __.read(Reads.seq(userAnswersTrusteeCompanyReads(__))).map(JsArray(_))).flatMap { company =>
          (__ \ 'partnershipTrusteeDetails).readNullable(
            __.read(Reads.seq(userAnswersTrusteePartnershipReads(__))).map(JsArray(_))).flatMap { partnership =>
            (__ \ 'trustees).json.put(individual.getOrElse(JsArray()) ++ company.getOrElse(JsArray()) ++ partnership.getOrElse(JsArray()))
          }
        }
      })).map {
      _.getOrElse(Json.obj())
    }
  }

  def userAnswersTrusteeIndividualReads(desPath: JsPath): Reads[JsObject] =
    (__ \ 'trusteeKind).json.put(JsString("individual")) and
      (
        if(fs.get(Toggles.isEstablisherCompanyHnSEnabled))
          userAnswersIndividualDetailsReadsHnS("trusteeDetails", desPath)
        else
          userAnswersIndividualDetailsReads("trusteeDetails", desPath)) and
      userAnswersNinoReadsHnS("trusteeNino", desPath) and
      userAnswersUtrReadsHnS("uniqueTaxReference", desPath) and
      addressTransformer.getDifferentAddress(__ \ 'trusteeAddressId, desPath \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears(desPath, __ \ 'trusteeAddressYears) and
      addressTransformer.getPreviousAddress(desPath, __ \ 'trusteePreviousAddress) and
      userAnswersContactDetailsReads("trusteeContactDetails", desPath) and
      (__ \ 'isTrusteeComplete).json.put(JsBoolean(true)) reduce

  def userAnswersTrusteeCompanyReads(desPath: JsPath): Reads[JsObject] =
    (__ \ 'trusteeKind).json.put(JsString("company")) and
      userAnswersCompanyDetailsReads(desPath) and
      transformVatToUserAnswersReadsHnS(desPath, "companyVat") and
      userAnswersPayeReadsHnS(desPath, "companyPaye") and
      userAnswersCrnReadsHnS(desPath) and
      userAnswersUtrReadsHnS("companyUniqueTaxReference", desPath) and
      addressTransformer.getDifferentAddress(__ \ 'companyAddress, desPath \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears(desPath, __ \ 'trusteesCompanyAddressYears) and
      addressTransformer.getPreviousAddress(desPath, __ \ 'companyPreviousAddress) and
      userAnswersContactDetailsReads("companyContactDetails", desPath) and
      (__ \ 'isTrusteeComplete).json.put(JsBoolean(true)) reduce

  def userAnswersTrusteePartnershipReads(desPath: JsPath): Reads[JsObject] =
    (__ \ 'trusteeKind).json.put(JsString("partnership")) and
      userAnswersPartnershipDetailsReads(desPath) and
      transformVatToUserAnswersReads(desPath, "partnershipVat") and
      userAnswersPayeReads(desPath, "partnershipPaye") and
      userAnswersUtrReads("partnershipUniqueTaxReference", desPath) and
      addressTransformer.getDifferentAddress(__ \ 'partnershipAddress, desPath \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears(desPath, __ \ 'partnershipAddressYears) and
      addressTransformer.getPreviousAddress(desPath, __ \ 'partnershipPreviousAddress) and
      userAnswersContactDetailsReads("partnershipContactDetails", desPath) and
      (__ \ 'isPartnershipCompleteId).json.put(JsBoolean(true)) reduce
}
