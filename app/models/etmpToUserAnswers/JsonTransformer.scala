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

package models.etmpToUserAnswers

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

trait JsonTransformer {

  val doNothing: Reads[JsObject] = {
    __.json.put(Json.obj())
  }

  def userAnswersIndividualDetailsReads(userAnswersPath: String, apiPath: JsPath): Reads[JsObject] =
    (__ \ userAnswersPath \ 'firstName).json.copyFrom((apiPath \ 'personDetails \ 'firstName).json.pick) and
      (__ \ userAnswersPath \ 'lastName).json.copyFrom((apiPath \ 'personDetails \ 'lastName).json.pick) and
      (__ \ 'dateOfBirth).json.copyFrom((apiPath \ 'personDetails \ 'dateOfBirth).json.pick) reduce

  def userAnswersNinoReads(userAnswersPath: String, apiPath: JsPath): Reads[JsObject] =
      (apiPath \ "nino").read[String].flatMap { _ =>
        (__ \ 'hasNino).json.put(JsBoolean(true)) and
          (__ \ userAnswersPath \ 'value).json.copyFrom((apiPath \ 'nino).json.pick) reduce
      } orElse {
        (__ \ 'hasNino).json.put(JsBoolean(false)) and
          (__ \ 'noNinoReason).json.copyFrom((apiPath \ 'noNinoReason).json.pick) reduce
      } orElse {
        doNothing
      }

  def userAnswersUtrReads(apiPath: JsPath): Reads[JsObject] =
      (apiPath \ "utr").read[String].flatMap { _ =>
        (__ \ 'hasUtr).json.put(JsBoolean(true)) and
          (__ \ 'utr \ 'value).json.copyFrom((apiPath \ 'utr).json.pick) reduce
      } orElse {
          (__ \ 'hasUtr).json.put(JsBoolean(false)) and
            (__ \ 'noUtrReason).json.copyFrom((apiPath \ 'noUtrReason).json.pick) reduce
      } orElse {
      doNothing
    }

  def userAnswersContactDetailsReads(userAnswersBase: String, apiPath: JsPath): Reads[JsObject] =
    (__ \ userAnswersBase \ 'emailAddress).json.copyFrom((apiPath \ 'correspondenceContactDetails \ 'email).json.pick) and
      (__ \ userAnswersBase \ 'phoneNumber).json.copyFrom((apiPath \ 'correspondenceContactDetails \ 'telephone).json.pick) reduce


  def userAnswersCompanyDetailsReads(apiPath: JsPath): Reads[JsObject] =
    (__ \ 'companyDetails \ 'companyName).json.copyFrom((apiPath \ 'organisationName).json.pick)

  def userAnswersCrnReads(apiPath: JsPath): Reads[JsObject] =
      (apiPath \ "crnNumber").read[String].flatMap { _ =>
        (__ \ 'hasCrn).json.put(JsBoolean(true)) and
          (__ \ 'companyRegistrationNumber \ 'value).json.copyFrom((apiPath \ 'crnNumber).json.pick) reduce
      } orElse {
        (__ \ 'hasCrn).json.put(JsBoolean(false)) and
          (__ \ 'noCrnReason).json.copyFrom((apiPath \ 'noCrnReason).json.pick) reduce
      } orElse {
      doNothing
      }


  def userAnswersPartnershipDetailsReads(apiPath: JsPath): Reads[JsObject] =
    (__ \ 'partnershipDetails \ 'name).json.copyFrom((apiPath \ 'partnershipName).json.pick)

  def userAnswersTrusteePartnershipDetailsReads(apiPath: JsPath): Reads[JsObject] =
    (__ \ 'partnershipDetails \ 'name).json.copyFrom((apiPath \ 'organisationName).json.pick)

  def transformVatToUserAnswersReads(apiPath: JsPath, userAnswersBase: String): Reads[JsObject] =
      (apiPath \ "vatRegistrationNumber").read[String].flatMap { _ =>
        (__ \ 'hasVat).json.put(JsBoolean(true)) and
          (__ \ userAnswersBase \ 'value).json.copyFrom((apiPath \ 'vatRegistrationNumber).json.pick) reduce
      } orElse {
        (__ \ 'hasVat).json.put(JsBoolean(false))
      }

  def userAnswersPayeReads(apiPath: JsPath, userAnswersBase: String): Reads[JsObject] =
      (apiPath \ "payeReference").read[String].flatMap { _ =>
        (__ \ 'hasPaye).json.put(JsBoolean(true)) and
          (__ \ userAnswersBase \ 'value).json.copyFrom((apiPath \ 'payeReference).json.pick) reduce
      } orElse {
        (__ \ 'hasPaye).json.put(JsBoolean(false))
      }
}
