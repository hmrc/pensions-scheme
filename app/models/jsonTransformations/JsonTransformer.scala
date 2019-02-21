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
      ((__ \ userAnswersPath \ 'middleName).json.copyFrom((desPath \ 'personDetails \ 'middleName).json.pick) orElse doNothing) and
      (__ \ userAnswersPath \ 'lastName).json.copyFrom((desPath \ 'personDetails \ 'lastName).json.pick) and
      (__ \ userAnswersPath \ 'date).json.copyFrom((desPath \ 'personDetails \ 'dateOfBirth).json.pick) reduce

  def userAnswersNinoReads(userAnswersPath: String, desPath: JsPath): Reads[JsObject] = {
    (desPath \ "nino").read[String].flatMap { _ =>
      (__ \ userAnswersPath \ 'hasNino).json.put(JsBoolean(true)) and
        (__ \ userAnswersPath \ 'nino).json.copyFrom((desPath \ 'nino).json.pick) reduce

    } orElse {
      (__ \ userAnswersPath \ 'hasNino).json.put(JsBoolean(false)) and
        (__ \ userAnswersPath \ 'reason).json.copyFrom((__ \ 'noNinoReason).json.pick) reduce
    } orElse {
      doNothing
    }
  }

  def userAnswersUtrReads(userAnswersBase: String, desPath: JsPath): Reads[JsObject] = {
    (desPath \ "utr").read[String].flatMap { _ =>
      (__ \ userAnswersBase \ 'hasUtr).json.put(JsBoolean(true)) and
        (__ \ userAnswersBase \ 'utr).json.copyFrom((desPath \ 'utr).json.pick) reduce

    } orElse {
      (__ \ userAnswersBase \ 'hasUtr).json.put(JsBoolean(false)) and
        (__ \ userAnswersBase \ 'reason).json.copyFrom((desPath \ 'noUtrReason).json.pick) reduce
    } orElse {
      doNothing
    }
  }

  def userAnswersContactDetailsReads(userAnswersBase: String, desPath: JsPath): Reads[JsObject] =
    (__ \ userAnswersBase \ 'emailAddress).json.copyFrom((desPath \ 'correspondenceContactDetails \ 'email).json.pick) and
      (__ \ userAnswersBase \ 'phoneNumber).json.copyFrom((desPath \ 'correspondenceContactDetails \ 'telephone).json.pick) reduce


  def userAnswersCompanyDetailsReads(desPath: JsPath): Reads[JsObject] =
    (__ \ 'companyDetails \ 'companyName).json.copyFrom((desPath \ 'organisationName).json.pick) and
      ((__ \ 'companyDetails \ 'vatNumber).json.copyFrom((desPath \ 'vatRegistrationNumber).json.pick)
        orElse doNothing) and
      ((__ \ 'companyDetails \ 'payeNumber).json.copyFrom((desPath \ 'payeReference).json.pick)
        orElse doNothing) reduce


  def userAnswersCrnReads(desPath: JsPath): Reads[JsObject] = {
    (desPath \ "crnNumber").read[String].flatMap { _ =>
      (__ \ 'companyRegistrationNumber \ 'hasCrn).json.put(JsBoolean(true)) and
        (__ \ 'companyRegistrationNumber \ 'crn).json.copyFrom((desPath \ 'crnNumber).json.pick) reduce

    } orElse {
      (__ \ 'companyRegistrationNumber \ 'hasCrn).json.put(JsBoolean(false)) and
        (__ \ 'companyRegistrationNumber \ 'reason).json.copyFrom((desPath \ 'noCrnReason).json.pick) reduce

    } orElse {
      doNothing
    }
  }

  def userAnswersPartnershipDetailsReads(desPath: JsPath): Reads[JsObject] =
    (__ \ 'partnershipDetails \ 'name).json.copyFrom((desPath \ 'partnershipName).json.pick)

  def transformVatToUserAnswersReads(desPath: JsPath): Reads[JsObject] = (desPath \ "vatRegistrationNumber").read[String].flatMap { _ =>
    (__ \ 'partnershipVat \ 'hasVat).json.put(JsBoolean(true)) and
      (__ \ 'partnershipVat \ 'vat).json.copyFrom((desPath \ 'vatRegistrationNumber).json.pick) reduce

  } orElse {
    (__ \ 'partnershipVat \ 'hasVat).json.put(JsBoolean(false))
  }

  def userAnswersPayeReads(desPath: JsPath): Reads[JsObject] = (desPath \ "payeReference").read[String].flatMap { _ =>
    (__ \ 'partnershipPaye \ 'hasPaye).json.put(JsBoolean(true)) and
      (__ \ 'partnershipPaye \ 'paye).json.copyFrom((desPath \ 'payeReference).json.pick) reduce

  } orElse {
    (__ \ 'partnershipPaye \ 'hasPaye).json.put(JsBoolean(false))

  }
}
