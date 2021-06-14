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

class TrusteeDetailsTransformer @Inject()(addressTransformer: AddressTransformer) extends JsonTransformer {

  val userAnswersTrusteesReads: Reads[JsObject] = {
    (__ \ 'psaPspSchemeDetails \ 'trusteeDetails).readNullable(__.read(
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

  def userAnswersTrusteeIndividualReads(ifPath: JsPath): Reads[JsObject] =
    (__ \ 'trusteeKind).json.put(JsString("individual")) and
      userAnswersIndividualDetailsReads("trusteeDetails", ifPath)and
      userAnswersNinoReads("trusteeNino", ifPath) and
      userAnswersUtrReads(ifPath) and
      addressTransformer.getDifferentAddress(__ \ 'trusteeAddressId, ifPath \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears(ifPath, __ \ 'trusteeAddressYears) and
      addressTransformer.getPreviousAddress(ifPath, __ \ 'trusteePreviousAddress) and
      userAnswersContactDetailsReads("trusteeContactDetails", ifPath) reduce

  def userAnswersTrusteeCompanyReads(ifPath: JsPath): Reads[JsObject] =
    (__ \ 'trusteeKind).json.put(JsString("company")) and
      userAnswersCompanyDetailsReads(ifPath) and
      transformVatToUserAnswersReads(ifPath, "companyVat") and
      userAnswersPayeReads(ifPath, "companyPaye") and
      userAnswersCrnReads(ifPath) and
      userAnswersUtrReads(ifPath) and
      addressTransformer.getDifferentAddress(__ \ 'companyAddress, ifPath \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears(ifPath, __ \ 'trusteesCompanyAddressYears) and
      addressTransformer.getPreviousAddress(ifPath, __ \ 'companyPreviousAddress) and
      userAnswersContactDetailsReads("companyContactDetails", ifPath) reduce

  def userAnswersTrusteePartnershipReads(ifPath: JsPath): Reads[JsObject] =
    (__ \ 'trusteeKind).json.put(JsString("partnership")) and
      userAnswersTrusteePartnershipDetailsReads(ifPath) and
      transformVatToUserAnswersReads(ifPath, "partnershipVat") and
      userAnswersPayeReads(ifPath, "partnershipPaye") and
      userAnswersUtrReads(ifPath) and
      addressTransformer.getDifferentAddress(__ \ 'partnershipAddress, ifPath \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears(ifPath, __ \ 'partnershipAddressYears) and
      addressTransformer.getPreviousAddress(ifPath, __ \ 'partnershipPreviousAddress) and
      userAnswersContactDetailsReads("partnershipContactDetails", ifPath) reduce

}
