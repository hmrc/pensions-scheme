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

package utils.JsonTransformations

import com.google.inject.Inject
import models.jsonTransformations.{AddressTransformer, JsonTransformer}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

class EstablisherDetailsTransformer @Inject()(addressTransformer: AddressTransformer) extends JsonTransformer {

  def userAnswersEstablishersReads: Reads[JsObject] = {
    (__ \ 'individualDetails).readNullable(
      __.read(Reads.seq(userAnswersEstablisherIndividualReads)).map(JsArray(_))).flatMap { indv =>
      (__ \ 'companyOrOrganisationDetails).readNullable(
        __.read(Reads.seq(userAnswersEstablisherCompanyReads)).map(JsArray(_))).flatMap { comp =>
        (__ \ 'partnershipTrusteeDetail).readNullable(
          __.read(Reads.seq(userAnswersEstablisherPartnershipReads)).map(JsArray(_))).flatMap { part =>
          (__ \ 'establishers).json.put(indv.getOrElse(JsArray()) ++ comp.getOrElse(JsArray()) ++ part.getOrElse(JsArray()))
        }
      }
    }
  }

  def userAnswersEstablisherIndividualReads: Reads[JsObject] =
    (__ \ 'establisherKind).json.put(JsString("individual")) and
      userAnswersIndividualDetailsReads and
      userAnswersNinoReads and
      userAnswersUtrReads("uniqueTaxReference") and
      addressTransformer.getDifferentAddress(__ \ 'address, __ \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears(__, __ \ 'addressYears) and
      addressTransformer.getPreviousAddress(__, __ \ 'previousAddress) and
      userAnswersContactDetailsReads("contactDetails") and
      (__ \ 'isEstablisherComplete).json.put(JsBoolean(true)) reduce

  def userAnswersEstablisherCompanyReads: Reads[JsObject] =
    (__ \ 'establisherKind).json.put(JsString("company")) and
      userAnswersCompanyDetailsReads and
      userAnswersCrnReads and
      userAnswersUtrReads("companyUniqueTaxReference") and
      addressTransformer.getDifferentAddress(__ \ 'companyAddress, __ \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears(__, __ \ 'companyAddressYears) and
      addressTransformer.getPreviousAddress(__, __ \ 'companyPreviousAddress) and
      userAnswersContactDetailsReads("companyContactDetails") and
      (__ \ 'isCompanyComplete).json.put(JsBoolean(true)) reduce

  def userAnswersEstablisherPartnershipReads: Reads[JsObject] =
    (__ \ 'establisherKind).json.put(JsString("partnership")) and
      userAnswersPartnershipDetailsReads and
      transformVatToUserAnswersReads and
      userAnswersPayeReads and
      userAnswersUtrReads("partnershipUniqueTaxReference") and
      addressTransformer.getDifferentAddress(__ \ 'partnershipAddress, __ \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears(__, __ \ 'partnershipAddressYears) and
      addressTransformer.getPreviousAddress(__, __ \ 'partnershipPreviousAddress) and
      userAnswersContactDetailsReads("partnershipContactDetails") and
      (__ \ 'isPartnershipCompleteId).json.put(JsBoolean(true)) reduce

  def userAnswersIndividualDetailsReads: Reads[JsObject] =
    (__ \ 'establisherDetails \ 'firstName).json.copyFrom((__ \ 'personDetails \ 'firstName).json.pick) and
      ((__ \ 'establisherDetails \ 'middleName).json.copyFrom((__ \ 'personDetails \ 'middleName).json.pick) orElse doNothing) and
      (__ \ 'establisherDetails \ 'lastName).json.copyFrom((__ \ 'personDetails \ 'lastName).json.pick) and
      (__ \ 'establisherDetails \ 'date).json.copyFrom((__ \ 'personDetails \ 'dateOfBirth).json.pick) reduce

  def userAnswersNinoReads: Reads[JsObject] = {
    (__ \ "nino").read[String].flatMap { _ =>
      (__ \ 'establisherNino \ 'hasNino).json.put(JsBoolean(true)) and
        (__ \ 'establisherNino \ 'nino).json.copyFrom((__ \ 'nino).json.pick) reduce

    } orElse {
      (__ \ 'establisherNino \ 'hasNino).json.put(JsBoolean(false)) and
        (__ \ 'establisherNino \ 'reason).json.copyFrom((__ \ 'noNinoReason).json.pick) reduce

    }
  }

  def userAnswersUtrReads(userAnswersBase: String): Reads[JsObject] = {
    (__ \ "utr").read[String].flatMap { _ =>
      (__ \ userAnswersBase \ 'hasUtr).json.put(JsBoolean(true)) and
        (__ \ userAnswersBase \ 'utr).json.copyFrom((__ \ 'utr).json.pick) reduce

    } orElse {
      (__ \ userAnswersBase \ 'hasUtr).json.put(JsBoolean(false)) and
        (__ \ userAnswersBase \ 'reason).json.copyFrom((__ \ 'noUtrReason).json.pick) reduce

    }
  }

  def userAnswersCompanyDetailsReads: Reads[JsObject] =
    (__ \ 'companyDetails \ 'companyName).json.copyFrom((__ \ 'organisationName).json.pick) and
      (__ \ 'companyDetails \ 'vatNumber).json.copyFrom((__ \ 'vatRegistrationNumber).json.pick) and
      (__ \ 'companyDetails \ 'payeNumber).json.copyFrom((__ \ 'payeReference).json.pick) reduce


  def userAnswersContactDetailsReads(userAnswersBase: String): Reads[JsObject] =
    (__ \ userAnswersBase \ 'emailAddress).json.copyFrom((__ \ 'correspondenceContactDetails \ 'email).json.pick) and
      (__ \ userAnswersBase \ 'phoneNumber).json.copyFrom((__ \ 'correspondenceContactDetails \ 'telephone).json.pick) reduce

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
