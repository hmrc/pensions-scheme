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
        __.read(Reads.seq(userAnswersTrusteeIndividualReads)).map(JsArray(_))).flatMap { individual =>
        (__ \ 'companyTrusteeDetails).readNullable(
          __.read(Reads.seq(userAnswersTrusteeCompanyReads)).map(JsArray(_))).flatMap { company =>
          (__ \ 'partnershipTrusteeDetails).readNullable(
            __.read(Reads.seq(userAnswersTrusteePartnershipReads)).map(JsArray(_))).flatMap { partnership =>
            (__ \ 'trustees).json.put(individual.getOrElse(JsArray()) ++ company.getOrElse(JsArray()) ++ partnership.getOrElse(JsArray()))
          }
        }
      })).map {
      _.getOrElse(Json.obj())
    }
  }

  def userAnswersTrusteeIndividualReads: Reads[JsObject] =
    (__ \ 'trusteeKind).json.put(JsString("individual")) and
      userAnswersIndividualDetailsReads("trusteeDetails")and
      userAnswersNinoReads("trusteeNino") and
      userAnswersUtrReads and
      addressTransformer.getDifferentAddress(__ \ 'trusteeAddressId, __ \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears( __ \ 'trusteeAddressYears) and
      addressTransformer.getPreviousAddress( __ \ 'trusteePreviousAddress) and
      userAnswersContactDetailsReads("trusteeContactDetails") reduce

  def userAnswersTrusteeCompanyReads: Reads[JsObject] =
    (__ \ 'trusteeKind).json.put(JsString("company")) and
      userAnswersCompanyDetailsReads and
      transformVatToUserAnswersReads("companyVat") and
      userAnswersPayeReads("companyPaye") and
      userAnswersCrnReads and
      userAnswersUtrReads and
      addressTransformer.getDifferentAddress(__ \ 'companyAddress, __ \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears( __ \ 'trusteesCompanyAddressYears) and
      addressTransformer.getPreviousAddress( __ \ 'companyPreviousAddress) and
      userAnswersContactDetailsReads("companyContactDetails") reduce

  def userAnswersTrusteePartnershipReads: Reads[JsObject] =
    (__ \ 'trusteeKind).json.put(JsString("partnership")) and
      userAnswersTrusteePartnershipDetailsReads and
      transformVatToUserAnswersReads("partnershipVat") and
      userAnswersPayeReads("partnershipPaye") and
      userAnswersUtrReads and
      addressTransformer.getDifferentAddress(__ \ 'partnershipAddress, __ \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears( __ \ 'partnershipAddressYears) and
      addressTransformer.getPreviousAddress( __ \ 'partnershipPreviousAddress) and
      userAnswersContactDetailsReads("partnershipContactDetails") reduce

}
