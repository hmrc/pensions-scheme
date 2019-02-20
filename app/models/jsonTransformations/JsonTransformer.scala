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

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

trait JsonTransformer {
  val doNothing: Reads[JsObject] = __.json.put(Json.obj())

  def userAnswersIndividualDetailsReads(userAnswersPath: String): Reads[JsObject] =
    (__ \ userAnswersPath \ 'firstName).json.copyFrom((__ \ 'personDetails \ 'firstName).json.pick) and
      ((__ \ userAnswersPath \ 'middleName).json.copyFrom((__ \ 'personDetails \ 'middleName).json.pick) orElse doNothing) and
      (__ \ userAnswersPath \ 'lastName).json.copyFrom((__ \ 'personDetails \ 'lastName).json.pick) and
      (__ \ userAnswersPath \ 'date).json.copyFrom((__ \ 'personDetails \ 'dateOfBirth).json.pick) reduce

  def userAnswersNinoReads(userAnswersPath: String): Reads[JsObject] = {
    (__ \ "nino").read[String].flatMap { _ =>
      (__ \ userAnswersPath \ 'hasNino).json.put(JsBoolean(true)) and
        (__ \ userAnswersPath \ 'nino).json.copyFrom((__ \ 'nino).json.pick) reduce

    } orElse {
      (__ \ userAnswersPath \ 'hasNino).json.put(JsBoolean(false)) and
        (__ \ userAnswersPath \ 'reason).json.copyFrom((__ \ 'noNinoReason).json.pick) reduce

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

  def userAnswersContactDetailsReads(userAnswersBase: String): Reads[JsObject] =
    (__ \ userAnswersBase \ 'emailAddress).json.copyFrom((__ \ 'correspondenceContactDetails \ 'email).json.pick) and
      (__ \ userAnswersBase \ 'phoneNumber).json.copyFrom((__ \ 'correspondenceContactDetails \ 'telephone).json.pick) reduce



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
