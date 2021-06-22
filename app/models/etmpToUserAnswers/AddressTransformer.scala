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

import models.etmpToUserAnswers.psaSchemeDetails.JsonTransformer
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.__
import play.api.libs.json._

class AddressTransformer extends JsonTransformer {

  private def getCommonAddressElements(userAnswersPath: JsPath, ifAddressPath: JsPath): Reads[JsObject] = {
    (userAnswersPath \ 'addressLine1).json.copyFrom((ifAddressPath \ 'line1).json.pick) and
      (userAnswersPath \ 'addressLine2).json.copyFrom((ifAddressPath \ 'line2).json.pick) and
      ((userAnswersPath \ 'addressLine3).json.copyFrom((ifAddressPath \ 'line3).json.pick)
        orElse doNothing) and
      ((userAnswersPath \ 'addressLine4).json.copyFrom((ifAddressPath \ 'line4).json.pick)
        orElse doNothing) reduce
  }

  def getAddress(userAnswersPath: JsPath, ifAddressPath: JsPath): Reads[JsObject] = {
    getCommonAddressElements(userAnswersPath, ifAddressPath) and
      ((userAnswersPath \ 'postalCode).json.copyFrom((ifAddressPath \ 'postalCode).json.pick)
        orElse doNothing) and
      (userAnswersPath \ 'countryCode).json.copyFrom((ifAddressPath \ 'countryCode).json.pick) reduce
  }

  def getDifferentAddress(userAnswersPath: JsPath, ifAddressPath: JsPath): Reads[JsObject] = {
    getCommonAddressElements(userAnswersPath, ifAddressPath) and
      ((userAnswersPath \ 'postcode).json.copyFrom((ifAddressPath \ 'postalCode).json.pick)
        orElse doNothing) and
      (userAnswersPath \ 'country).json.copyFrom((ifAddressPath \ 'countryCode).json.pick) reduce
  }

  def getAddressYears(uaAddressYearsPath: JsPath = __): Reads[JsObject] = {
    (__ \ "previousAddressDetails" \ "isPreviousAddressLast12Month").read[Boolean].flatMap { addressYearsValue =>
      val value = if (addressYearsValue) {
        JsString("under_a_year")
      } else {
        JsString("over_a_year")
      }
      uaAddressYearsPath.json.put(value)
    } orElse doNothing
  }

  def getPreviousAddress(userAnswersPath: JsPath): Reads[JsObject] = {
    (__ \ 'previousAddressDetails \ 'previousAddress).read[JsObject].flatMap { _ =>
      getDifferentAddress(userAnswersPath, __ \ 'previousAddressDetails \ 'previousAddress)
    } orElse doNothing
  }
}
