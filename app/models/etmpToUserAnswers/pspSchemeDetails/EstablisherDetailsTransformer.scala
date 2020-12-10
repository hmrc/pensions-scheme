/*
 * Copyright 2020 HM Revenue & Customs
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

package models.etmpToUserAnswers.pspSchemeDetails

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

class EstablisherDetailsTransformer extends JsonTransformer {


  val userAnswersEstablishersReads: Reads[JsObject] = {
    (__ \ 'establisherDetails).readNullable(__.read(
      (__ \ 'individualDetails).readNullable(
        __.read(Reads.seq(userAnswersEstablisherIndividualReads(__))).map(JsArray(_))).flatMap { individual =>
        (__ \ 'companyOrOrgDetails).readNullable(
          __.read(Reads.seq(userAnswersEstablisherCompanyReads(__))).map(JsArray(_))).flatMap { company =>
          (__ \ 'partnershipDetails).readNullable(
            __.read(Reads.seq(userAnswersEstablisherPartnershipReads(__))).map(JsArray(_))).flatMap { partnership =>
            (__ \ 'establishers).json.put(individual.getOrElse(JsArray()) ++ company.getOrElse(JsArray()) ++ partnership.getOrElse(JsArray())) orElse doNothing
          }
        }
      })).map {
      _.getOrElse(Json.obj())
    }
  }

  def userAnswersEstablisherIndividualReads(apiPath: JsPath): Reads[JsObject] = {
    (__ \ 'establisherKind).json.put(JsString("individual")) and
      (__ \ "establisherDetails" \ 'firstName).json.copyFrom((apiPath \ 'firstName).json.pick) and
      (__ \ "establisherDetails" \ 'lastName).json.copyFrom((apiPath \ 'lastName).json.pick) and
      (__ \ 'isEstablisherComplete).json.put(JsBoolean(true)) reduce
  }

  def userAnswersEstablisherCompanyReads(apiPath: JsPath): Reads[JsObject] =
    (__ \ 'establisherKind).json.put(JsString("company")) and
      (__ \ 'companyDetails \ 'companyName).json.copyFrom((apiPath \ 'companyOrOrgName).json.pick) and
      (__ \ 'isEstablisherComplete).json.put(JsBoolean(true)) reduce


  def userAnswersEstablisherPartnershipReads(apiPath: JsPath): Reads[JsObject] =
    (__ \ 'establisherKind).json.put(JsString("partnership")) and
      (__ \ 'partnershipDetails \ 'name).json.copyFrom((apiPath \ 'partnershipName).json.pick) and
      (__ \ 'isEstablisherComplete).json.put(JsBoolean(true)) reduce

}
