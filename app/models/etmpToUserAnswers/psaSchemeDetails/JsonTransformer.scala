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

import scala.language.postfixOps

trait JsonTransformer {

  val doNothing: Reads[JsObject] = {
    __.json.put(Json.obj())
  }

  def userAnswersIndividualDetailsReads(userAnswersPath: String): Reads[JsObject] =
    (__ \ userAnswersPath \ Symbol("firstName")).json.copyFrom((__ \ Symbol("personDetails") \ Symbol("firstName")).json.pick) and
      (__ \ userAnswersPath \ Symbol("lastName")).json.copyFrom((__ \ Symbol("personDetails") \ Symbol("lastName")).json.pick) and
      (__ \ Symbol("dateOfBirth")).json.copyFrom((__ \ Symbol("personDetails") \ Symbol("dateOfBirth")).json.pick) reduce

  def userAnswersNinoReads(userAnswersPath: String): Reads[JsObject] =
    (__ \ "nino").read[String].flatMap { _ =>
      (__ \ Symbol("hasNino")).json.put(JsBoolean(true)) and
        (__ \ userAnswersPath \ Symbol("value")).json.copyFrom((__ \ Symbol("nino")).json.pick) reduce
    } orElse {
      (__ \ Symbol("hasNino")).json.put(JsBoolean(false)) and
        (__ \ Symbol("noNinoReason")).json.copyFrom((__ \ Symbol("noNinoReason")).json.pick) reduce
    } orElse {
      doNothing
    }

  def userAnswersUtrReads: Reads[JsObject] =
    (__ \ "utr").read[String].flatMap { _ =>
      (__ \ Symbol("hasUtr")).json.put(JsBoolean(true)) and
        (__ \ Symbol("utr") \ Symbol("value")).json.copyFrom((__ \ Symbol("utr")).json.pick) reduce
    } orElse {
      (__ \ Symbol("hasUtr")).json.put(JsBoolean(false)) and
        (__ \ Symbol("noUtrReason")).json.copyFrom((__ \ Symbol("noUtrReason")).json.pick) reduce
    } orElse {
      doNothing
    }

  def userAnswersContactDetailsReads(userAnswersBase: String): Reads[JsObject] =
    (__ \ userAnswersBase \ Symbol("emailAddress")).json.copyFrom((__ \ Symbol("correspondenceContactDetails") \ Symbol("email")).json.pick) and
      (__ \ userAnswersBase \ Symbol("phoneNumber")).json.copyFrom((__ \ Symbol("correspondenceContactDetails") \ Symbol("telephone")).json.pick) reduce


  def userAnswersCompanyDetailsReads: Reads[JsObject] =
    (__ \ Symbol("companyDetails") \ Symbol("companyName")).json.copyFrom((__ \ Symbol("organisationName")).json.pick)

  def userAnswersCrnReads: Reads[JsObject] =
    (__ \ "crnNumber").read[String].flatMap { _ =>
      (__ \ Symbol("hasCrn")).json.put(JsBoolean(true)) and
        (__ \ Symbol("companyRegistrationNumber") \ Symbol("value")).json.copyFrom((__ \ Symbol("crnNumber")).json.pick) reduce
    } orElse {
      (__ \ Symbol("hasCrn")).json.put(JsBoolean(false)) and
        (__ \ Symbol("noCrnReason")).json.copyFrom((__ \ Symbol("noCrnReason")).json.pick) reduce
    } orElse {
      doNothing
    }


  def userAnswersPartnershipDetailsReads: Reads[JsObject] =
    (__ \ Symbol("partnershipDetails") \ Symbol("name")).json.copyFrom((__ \ Symbol("partnershipName")).json.pick)

  def userAnswersTrusteePartnershipDetailsReads: Reads[JsObject] =
    (__ \ Symbol("partnershipDetails") \ Symbol("name")).json.copyFrom((__ \ Symbol("organisationName")).json.pick)

  def transformVatToUserAnswersReads(userAnswersBase: String): Reads[JsObject] =
    (__ \ "vatRegistrationNumber").read[String].flatMap { _ =>
      (__ \ Symbol("hasVat")).json.put(JsBoolean(true)) and
        (__ \ userAnswersBase \ Symbol("value")).json.copyFrom((__ \ Symbol("vatRegistrationNumber")).json.pick) reduce
    } orElse {
      (__ \ Symbol("hasVat")).json.put(JsBoolean(false))
    }

  def userAnswersPayeReads(userAnswersBase: String): Reads[JsObject] =
    (__ \ "payeReference").read[String].flatMap { _ =>
      (__ \ Symbol("hasPaye")).json.put(JsBoolean(true)) and
        (__ \ userAnswersBase \ Symbol("value")).json.copyFrom((__ \ Symbol("payeReference")).json.pick) reduce
    } orElse {
      (__ \ Symbol("hasPaye")).json.put(JsBoolean(false))
    }
}
