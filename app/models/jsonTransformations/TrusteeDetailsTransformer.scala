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
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

class TrusteeDetailsTransformer @Inject()(addressTransformer: AddressTransformer) extends JsonTransformer {

  def userAnswersTrusteesReads: Reads[JsObject] = {
    (__ \ 'individualDetails).readNullable(
      __.read(Reads.seq(userAnswersTrusteeIndividualReads)).map(JsArray(_))).flatMap { individual =>
      (__ \ 'companyOrOrganisationDetails).readNullable(
        __.read(Reads.seq(userAnswersTrusteeCompanyReads)).map(JsArray(_))).flatMap { company =>
        (__ \ 'partnershipTrusteeDetail).readNullable(
          __.read(Reads.seq(userAnswersTrusteePartnershipReads)).map(JsArray(_))).flatMap { partnership =>
          (__ \ 'trustees).json.put(individual.getOrElse(JsArray()) ++ company.getOrElse(JsArray()) ++ partnership.getOrElse(JsArray()))
        }
      }
    }
  }

  def userAnswersTrusteeIndividualReads: Reads[JsObject] =
    (__ \ 'trusteeKind).json.put(JsString("individual")) and
      userAnswersIndividualDetailsReads("trusteeDetails") and
      userAnswersNinoReads("trusteeNino") and
      userAnswersUtrReads("uniqueTaxReference") and
      addressTransformer.getDifferentAddress(__ \ 'trusteeAddressId, __ \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears(__, __ \ 'trusteeAddressYears) and
      addressTransformer.getPreviousAddress(__, __ \ 'trusteePreviousAddress) and
      userAnswersContactDetailsReads("trusteeContactDetails") and
      (__ \ 'isTrusteeComplete).json.put(JsBoolean(true)) reduce

  def userAnswersTrusteeCompanyReads: Reads[JsObject] =
    (__ \ 'trusteeKind).json.put(JsString("company")) and
      userAnswersCompanyDetailsReads and
      userAnswersCrnReads and
      userAnswersUtrReads("companyUniqueTaxReference") and
      addressTransformer.getDifferentAddress(__ \ 'companyAddress, __ \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears(__, __ \ 'companyAddressYears) and
      addressTransformer.getPreviousAddress(__, __ \ 'companyPreviousAddress) and
      userAnswersContactDetailsReads("companyContactDetails") and
      (__ \ 'isCompanyComplete).json.put(JsBoolean(true)) reduce

  def userAnswersTrusteePartnershipReads: Reads[JsObject] =
    (__ \ 'trusteeKind).json.put(JsString("partnership")) and
      userAnswersPartnershipDetailsReads and
      transformVatToUserAnswersReads and
      userAnswersPayeReads and
      userAnswersUtrReads("partnershipUniqueTaxReference") and
      addressTransformer.getDifferentAddress(__ \ 'partnershipAddress, __ \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears(__, __ \ 'partnershipAddressYears) and
      addressTransformer.getPreviousAddress(__, __ \ 'partnershipPreviousAddress) and
      userAnswersContactDetailsReads("partnershipContactDetails") and
      (__ \ 'isPartnershipCompleteId).json.put(JsBoolean(true)) reduce

  def userAnswersCompanyDetailsReads: Reads[JsObject] =
    (__ \ 'companyDetails \ 'companyName).json.copyFrom((__ \ 'organisationName).json.pick) and
      (__ \ 'companyDetails \ 'vatNumber).json.copyFrom((__ \ 'vatRegistrationNumber).json.pick) and
      (__ \ 'companyDetails \ 'payeNumber).json.copyFrom((__ \ 'payeReference).json.pick) reduce


  def userAnswersCrnReads: Reads[JsObject] = {
    (__ \ "crnNumber").read[String].flatMap { _ =>
      (__ \ 'companyRegistrationNumber \ 'hasCrn).json.put(JsBoolean(true)) and
        (__ \ 'companyRegistrationNumber \ 'crn).json.copyFrom((__ \ 'crnNumber).json.pick) reduce

    } orElse {
      (__ \ 'companyRegistrationNumber \ 'hasCrn).json.put(JsBoolean(false)) and
        (__ \ 'companyRegistrationNumber \ 'reason).json.copyFrom((__ \ 'noCrnReason).json.pick) reduce

    }
  }

  def userAnswersPartnershipDetailsReads: Reads[JsObject] =
    (__ \ 'partnershipDetails \ 'name).json.copyFrom((__ \ 'partnershipName).json.pick)

  def transformVatToUserAnswersReads: Reads[JsObject] = (__ \ "vatRegistrationNumber").read[String].flatMap { _ =>
    (__ \ 'partnershipVat \ 'hasVat).json.put(JsBoolean(true)) and
      (__ \ 'partnershipVat \ 'vat).json.copyFrom((__ \ 'vatRegistrationNumber).json.pick) reduce

  } orElse {
    (__ \ 'partnershipVat \ 'hasVat).json.put(JsBoolean(false))

  }

  def userAnswersPayeReads: Reads[JsObject] = (__ \ "payeReference").read[String].flatMap { _ =>
    (__ \ 'partnershipPaye \ 'hasPaye).json.put(JsBoolean(true)) and
      (__ \ 'partnershipPaye \ 'paye).json.copyFrom((__ \ 'payeReference).json.pick) reduce

  } orElse {
    (__ \ 'partnershipPaye \ 'hasPaye).json.put(JsBoolean(false))

  }
}
