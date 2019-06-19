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
import config.FeatureSwitchManagementService
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{__, _}

class AddressTransformer @Inject()(override val fs: FeatureSwitchManagementService) extends JsonTransformer {

  private def getCommonAddressElements(userAnswersPath: JsPath, desAddressPath: JsPath): Reads[JsObject] = {
    (userAnswersPath \ 'addressLine1).json.copyFrom((desAddressPath \ 'line1).json.pick) and
      (userAnswersPath \ 'addressLine2).json.copyFrom((desAddressPath \ 'line2).json.pick) and
      ((userAnswersPath \ 'addressLine3).json.copyFrom((desAddressPath \ 'line3).json.pick)
        orElse doNothing) and
      ((userAnswersPath \ 'addressLine4).json.copyFrom((desAddressPath \ 'line4).json.pick)
        orElse doNothing) reduce
  }

  def getAddress(userAnswersPath: JsPath, desAddressPath: JsPath): Reads[JsObject] = {
    getCommonAddressElements(userAnswersPath, desAddressPath) and
      ((userAnswersPath \ 'postalCode).json.copyFrom((desAddressPath \ 'postalCode).json.pick)
        orElse doNothing) and
      (userAnswersPath \ 'countryCode).json.copyFrom((desAddressPath \ 'countryCode).json.pick) reduce
  }

  def getDifferentAddress(userAnswersPath: JsPath, desAddressPath: JsPath): Reads[JsObject] = {
    getCommonAddressElements(userAnswersPath, desAddressPath) and
      ((userAnswersPath \ 'postcode).json.copyFrom((desAddressPath \ 'postalCode).json.pick)
        orElse doNothing) and
      (userAnswersPath \ 'country).json.copyFrom((desAddressPath \ 'countryCode).json.pick) reduce
  }

  def getAddressYears(desPath: JsPath = __, uaAddressYearsPath: JsPath = __): Reads[JsObject] = {
    (desPath \ "previousAddressDetails" \ "isPreviousAddressLast12Month").read[Boolean].flatMap { addressYearsValue =>
      val value = if (addressYearsValue) {
        JsString("under_a_year")
      } else {
        JsString("over_a_year")
      }
      uaAddressYearsPath.json.put(value)
    } orElse doNothing
  }

  def getPreviousAddress(desPath: JsPath, userAnswersPath: JsPath): Reads[JsObject] = {
    (desPath \ 'previousAddressDetails \ 'previousAddress).read[JsObject].flatMap { _ =>
      getDifferentAddress(userAnswersPath, desPath \ 'previousAddressDetails \ 'previousAddress)
    } orElse doNothing
  }
}
