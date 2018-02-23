/*
 * Copyright 2018 HM Revenue & Customs
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

package models

import play.api.libs.json.{Json, Reads, Writes}
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Individual(firstName: String, lastName: String, dateOfBirth: Option[String] = None)

object Individual {
  implicit val formats = Json.format[Individual]
}

case class Organisation(organisationName: String, organisationType: String)

object Organisation {
  implicit val reads: Reads[Organisation] = (
    (JsPath \ "organisationName").read[String] and
    (JsPath \ "organisationType").read[String]
  )(Organisation.apply _)

  implicit val writes: Writes[Organisation] = (
    (JsPath \ "organisationName").write[String] and
      (JsPath \ "organisationType").write[String]
    )(unlift(Organisation.unapply))
}

case class IndividualOrOrganisation(regime: String, requiresNameMatch: Boolean, isAnAgent: Boolean,
                                    individual: Option[Individual] = None, organisation: Option[Organisation] = None)

object IndividualOrOrganisation {
  implicit val formats = Json.format[IndividualOrOrganisation]
}
