/*
 * Copyright 2024 HM Revenue & Customs
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

class TrusteeDetailsTransformer extends JsonTransformer {

  val userAnswersTrusteesReads: Reads[JsObject] = {
    (__ \ Symbol("trusteeDetails")).readNullable(__.read(
      (__ \ Symbol("individualDetails")).readNullable(
        __.read(Reads.seq(userAnswersTrusteeIndividualReads(__))).map(JsArray(_))).flatMap { individual =>
        (__ \ Symbol("companyOrOrgDetails")).readNullable(
          __.read(Reads.seq(userAnswersTrusteeCompanyReads(__))).map(JsArray(_))).flatMap { company =>
          (__ \ Symbol("partnershipDetails")).readNullable(
            __.read(Reads.seq(userAnswersTrusteePartnershipReads(__))).map(JsArray(_))).flatMap { partnership =>
            (__ \ Symbol("trustees")).json.put(individual.getOrElse(JsArray()) ++ company.getOrElse(JsArray()) ++ partnership.getOrElse(JsArray()))
          }
        }
      })).map {
      _.getOrElse(Json.obj())
    }
  }

  def userAnswersTrusteeIndividualReads(apiPath: JsPath): Reads[JsObject] =
    (__ \ Symbol("trusteeKind")).json.put(JsString("individual")) and
      (__ \ Symbol("trusteeDetails") \ Symbol("firstName")).json.copyFrom((apiPath \ Symbol("firstName")).json.pick) and
      (__ \ Symbol("trusteeDetails") \ Symbol("lastName")).json.copyFrom((apiPath \ Symbol("lastName")).json.pick) reduce

  def userAnswersTrusteeCompanyReads(apiPath: JsPath): Reads[JsObject] =
    (__ \ Symbol("trusteeKind")).json.put(JsString("company")) and
      (__ \ Symbol("companyDetails") \ Symbol("companyName")).json.copyFrom((apiPath \ Symbol("organisationName")).json.pick) reduce

  def userAnswersTrusteePartnershipReads(apiPath: JsPath): Reads[JsObject] =
    (__ \ Symbol("trusteeKind")).json.put(JsString("partnership")) and
      (__ \ Symbol("partnershipDetails") \ Symbol("name")).json.copyFrom((apiPath \ Symbol("partnershipName")).json.pick) reduce

}
