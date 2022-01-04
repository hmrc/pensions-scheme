/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

trait JsonTransformer {

  val doNothing: Reads[JsObject] = {
    __.json.put(Json.obj())
  }

  def userAnswersIndividualDetailsReads(userAnswersPath: String): Reads[JsObject] =
    (__ \ userAnswersPath \ 'firstName).json.copyFrom((__ \ 'personDetails \ 'firstName).json.pick) and
      (__ \ userAnswersPath \ 'lastName).json.copyFrom((__ \ 'personDetails \ 'lastName).json.pick) and
      (__ \ 'dateOfBirth).json.copyFrom((__ \ 'personDetails \ 'dateOfBirth).json.pick) reduce

  def userAnswersNinoReads(userAnswersPath: String): Reads[JsObject] =
    (__ \ "nino").read[String].flatMap { _ =>
      (__ \ 'hasNino).json.put(JsBoolean(true)) and
        (__ \ userAnswersPath \ 'value).json.copyFrom((__ \ 'nino).json.pick) reduce
    } orElse {
      (__ \ 'hasNino).json.put(JsBoolean(false)) and
        (__ \ 'noNinoReason).json.copyFrom((__ \ 'noNinoReason).json.pick) reduce
    } orElse {
      doNothing
    }

  def userAnswersUtrReads: Reads[JsObject] =
    (__ \ "utr").read[String].flatMap { _ =>
      (__ \ 'hasUtr).json.put(JsBoolean(true)) and
        (__ \ 'utr \ 'value).json.copyFrom((__ \ 'utr).json.pick) reduce
    } orElse {
      (__ \ 'hasUtr).json.put(JsBoolean(false)) and
        (__ \ 'noUtrReason).json.copyFrom((__ \ 'noUtrReason).json.pick) reduce
    } orElse {
      doNothing
    }

  def userAnswersContactDetailsReads(userAnswersBase: String): Reads[JsObject] =
    (__ \ userAnswersBase \ 'emailAddress).json.copyFrom((__ \ 'correspondenceContactDetails \ 'email).json.pick) and
      (__ \ userAnswersBase \ 'phoneNumber).json.copyFrom((__ \ 'correspondenceContactDetails \ 'telephone).json.pick) reduce


  def userAnswersCompanyDetailsReads: Reads[JsObject] =
    (__ \ 'companyDetails \ 'companyName).json.copyFrom((__ \ 'organisationName).json.pick)

  def userAnswersCrnReads: Reads[JsObject] =
    (__ \ "crnNumber").read[String].flatMap { _ =>
      (__ \ 'hasCrn).json.put(JsBoolean(true)) and
        (__ \ 'companyRegistrationNumber \ 'value).json.copyFrom((__ \ 'crnNumber).json.pick) reduce
    } orElse {
      (__ \ 'hasCrn).json.put(JsBoolean(false)) and
        (__ \ 'noCrnReason).json.copyFrom((__ \ 'noCrnReason).json.pick) reduce
    } orElse {
      doNothing
    }


  def userAnswersPartnershipDetailsReads: Reads[JsObject] =
    (__ \ 'partnershipDetails \ 'name).json.copyFrom((__ \ 'partnershipName).json.pick)

  def userAnswersTrusteePartnershipDetailsReads: Reads[JsObject] =
    (__ \ 'partnershipDetails \ 'name).json.copyFrom((__ \ 'organisationName).json.pick)

  def transformVatToUserAnswersReads(userAnswersBase: String): Reads[JsObject] =
    (__ \ "vatRegistrationNumber").read[String].flatMap { _ =>
      (__ \ 'hasVat).json.put(JsBoolean(true)) and
        (__ \ userAnswersBase \ 'value).json.copyFrom((__ \ 'vatRegistrationNumber).json.pick) reduce
    } orElse {
      (__ \ 'hasVat).json.put(JsBoolean(false))
    }

  def userAnswersPayeReads(userAnswersBase: String): Reads[JsObject] =
    (__ \ "payeReference").read[String].flatMap { _ =>
      (__ \ 'hasPaye).json.put(JsBoolean(true)) and
        (__ \ userAnswersBase \ 'value).json.copyFrom((__ \ 'payeReference).json.pick) reduce
    } orElse {
      (__ \ 'hasPaye).json.put(JsBoolean(false))
    }
}
