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

package models.schemes

import models.Name
import play.api.libs.functional.syntax._
import play.api.libs.json._


case class PsaDetails(id: String, organisationOrPartnershipName: Option[String], individual: Option[Name])

object PsaDetails {
  implicit val reads: Reads[PsaDetails] = (
    (JsPath \ "psaid").read[String] and
      (JsPath \ "organizationOrPartnershipName").readNullable[String] and
      (JsPath \ "firstName").readNullable[String] and
      (JsPath \ "middleName").readNullable[String] and
      (JsPath \ "lastName").readNullable[String]
    )((psaId,
       organizationOrPartnershipName,
       firstName,
       middleName,
       lastName) => PsaDetails(psaId, organizationOrPartnershipName, getPsaIndividual(firstName, middleName, lastName)))
  implicit val writes: Writes[PsaDetails] = Json.writes[PsaDetails]

  private def getPsaIndividual(firstName : Option[String], middleName : Option[String], lastName: Option[String]) : Option[Name] = {
    (firstName,middleName,lastName) match {
      case (None,None,None) => None
      case  _ => Some(Name(firstName,middleName,lastName))
    }
  }
}