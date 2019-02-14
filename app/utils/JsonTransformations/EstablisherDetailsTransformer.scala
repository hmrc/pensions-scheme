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
import models.jsonTransformations.JsonTransformer
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{JsBoolean, JsObject, Reads, __}

class EstablisherDetailsTransformer @Inject()() extends JsonTransformer {

  def transformPersonDetailsToUserAnswersReads: Reads[JsObject] =
    (__ \ 'establisherDetails \ 'firstName).json.copyFrom((__ \ 'firstName).json.pick) and
      ((__ \ 'establisherDetails \ 'middleName).json.copyFrom((__ \ 'middleName).json.pick) orElse doNothing) and
      (__ \ 'establisherDetails \ 'lastName).json.copyFrom((__ \ 'lastName).json.pick) and
      (__ \ 'establisherDetails \ 'date).json.copyFrom((__ \ 'dateOfBirth).json.pick) reduce

  def transformNinoDetailsToUserAnswersReads: Reads[JsObject] = {
    (__ \ "nino").read[String].flatMap { _ =>
      (__ \ 'establisherNino \ 'hasNino).json.put(JsBoolean(true)) and
        (__ \ 'establisherNino \ 'nino).json.copyFrom((__ \ 'nino).json.pick) reduce

    } orElse {
      (__ \ 'establisherNino \ 'hasNino).json.put(JsBoolean(false)) and
        (__ \ 'establisherNino \ 'reason ).json.copyFrom((__ \ 'noNinoReason).json.pick) reduce

    }
  }

}
