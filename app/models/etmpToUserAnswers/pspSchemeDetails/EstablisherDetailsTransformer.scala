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

package models.etmpToUserAnswers.pspSchemeDetails

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

import scala.language.postfixOps

class EstablisherDetailsTransformer extends JsonTransformer {


  val userAnswersEstablishersReads: Reads[JsObject] = {
    (__ \ Symbol("establisherDetails")).readNullable(__.read(
      (__ \ Symbol("individualDetails")).readNullable(
        __.read(Reads.seq(userAnswersEstablisherIndividualReads(__))).map(JsArray(_))).flatMap { individual =>
        (__ \ Symbol("companyOrOrgDetails")).readNullable(
          __.read(Reads.seq(userAnswersEstablisherCompanyReads(__))).map(JsArray(_))).flatMap { company =>
          (__ \ Symbol("partnershipDetails")).readNullable(
            __.read(Reads.seq(userAnswersEstablisherPartnershipReads(__))).map(JsArray(_))).flatMap { partnership =>
            (__ \ Symbol("establishers")).json.put(individual.getOrElse(JsArray()) ++ company.getOrElse(JsArray()) ++ partnership.getOrElse(JsArray())) orElse doNothing
          }
        }
      })).map {
      _.getOrElse(Json.obj())
    }
  }

  def userAnswersEstablisherIndividualReads(apiPath: JsPath): Reads[JsObject] = {
    (__ \ Symbol("establisherKind")).json.put(JsString("individual")) and
      (__ \ "establisherDetails" \ Symbol("firstName")).json.copyFrom((apiPath \ Symbol("firstName")).json.pick) and
      (__ \ "establisherDetails" \ Symbol("lastName")).json.copyFrom((apiPath \ Symbol("lastName")).json.pick) and
      (__ \ Symbol("isEstablisherComplete")).json.put(JsBoolean(true)) reduce
  }

  def userAnswersEstablisherCompanyReads(apiPath: JsPath): Reads[JsObject] =
    (__ \ Symbol("establisherKind")).json.put(JsString("company")) and
      (__ \ Symbol("companyDetails") \ Symbol("companyName")).json.copyFrom((apiPath \ Symbol("organisationName")).json.pick) and
      (__ \ Symbol("isEstablisherComplete")).json.put(JsBoolean(true)) reduce


  def userAnswersEstablisherPartnershipReads(apiPath: JsPath): Reads[JsObject] =
    (__ \ Symbol("establisherKind")).json.put(JsString("partnership")) and
      (__ \ Symbol("partnershipDetails") \ Symbol("name")).json.copyFrom((apiPath \ Symbol("partnershipName")).json.pick) and
      (__ \ Symbol("isEstablisherComplete")).json.put(JsBoolean(true)) reduce

}
