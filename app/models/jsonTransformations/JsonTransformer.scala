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

  val doNothing: Reads[JsObject] = {
    __.json.put(Json.obj())
  }

  def userAnswersIndividualDetailsReads(userAnswersPath: String, desPath: JsPath): Reads[JsObject] =
    (__ \ userAnswersPath \ 'firstName).json.copyFrom((desPath \ 'personDetails \ 'firstName).json.pick) and
      (__ \ userAnswersPath \ 'lastName).json.copyFrom((desPath \ 'personDetails \ 'lastName).json.pick) and
      (__ \ 'dateOfBirth).json.copyFrom((desPath \ 'personDetails \ 'dateOfBirth).json.pick) reduce

  def userAnswersNinoReads(userAnswersPath: String, desPath: JsPath): Reads[JsObject] =
      (desPath \ "nino").read[String].flatMap { _ =>
        (__ \ 'hasNino).json.put(JsBoolean(true)) and
          (__ \ userAnswersPath \ 'value).json.copyFrom((desPath \ 'nino).json.pick) reduce
      } orElse {
        (__ \ 'hasNino).json.put(JsBoolean(false)) and
          (__ \ 'noNinoReason).json.copyFrom((desPath \ 'noNinoReason).json.pick) reduce
      } orElse {
        doNothing
      }

  def userAnswersUtrReads(desPath: JsPath): Reads[JsObject] =
      (desPath \ "utr").read[String].flatMap { _ =>
        (__ \ 'hasUtr).json.put(JsBoolean(true)) and
          (__ \ 'utr \ 'value).json.copyFrom((desPath \ 'utr).json.pick) reduce
      } orElse {
          (__ \ 'hasUtr).json.put(JsBoolean(false)) and
            (__ \ 'noUtrReason).json.copyFrom((desPath \ 'noUtrReason).json.pick) reduce
      } orElse {
      doNothing
    }

  def userAnswersContactDetailsReads(userAnswersBase: String, desPath: JsPath): Reads[JsObject] =
    (__ \ userAnswersBase \ 'emailAddress).json.copyFrom((desPath \ 'correspondenceContactDetails \ 'email).json.pick) and
      (__ \ userAnswersBase \ 'phoneNumber).json.copyFrom((desPath \ 'correspondenceContactDetails \ 'telephone).json.pick) reduce


  def userAnswersCompanyDetailsReads(desPath: JsPath): Reads[JsObject] =
    (__ \ 'companyDetails \ 'companyName).json.copyFrom((desPath \ 'organisationName).json.pick)

  def userAnswersCrnReads(desPath: JsPath): Reads[JsObject] =
      (desPath \ "crnNumber").read[String].flatMap { _ =>
        (__ \ 'hasCrn).json.put(JsBoolean(true)) and
          (__ \ 'companyRegistrationNumber \ 'value).json.copyFrom((desPath \ 'crnNumber).json.pick) reduce
      } orElse {
        (__ \ 'hasCrn).json.put(JsBoolean(false)) and
          (__ \ 'noCrnReason).json.copyFrom((desPath \ 'noCrnReason).json.pick) reduce
      } orElse {
      doNothing
      }


  def userAnswersPartnershipDetailsReads(desPath: JsPath): Reads[JsObject] =
    (__ \ 'partnershipDetails \ 'name).json.copyFrom((desPath \ 'partnershipName).json.pick)

  def transformVatToUserAnswersReads(desPath: JsPath, userAnswersBase: String): Reads[JsObject] =
      (desPath \ "vatRegistrationNumber").read[String].flatMap { _ =>
        (__ \ 'hasVat).json.put(JsBoolean(true)) and
          (__ \ userAnswersBase \ 'value).json.copyFrom((desPath \ 'vatRegistrationNumber).json.pick) reduce
      } orElse {
        (__ \ 'hasVat).json.put(JsBoolean(false))
      }

  def userAnswersPayeReads(desPath: JsPath, userAnswersBase: String): Reads[JsObject] =
      (desPath \ "payeReference").read[String].flatMap { _ =>
        (__ \ 'hasPaye).json.put(JsBoolean(true)) and
          (__ \ userAnswersBase \ 'value).json.copyFrom((desPath \ 'payeReference).json.pick) reduce
      } orElse {
        (__ \ 'hasPaye).json.put(JsBoolean(false))
      }
}
