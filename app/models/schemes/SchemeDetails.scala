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

import models.CorrespondenceAddress
import play.api.libs.functional.syntax._
import play.api.libs.json._


case class SchemeMemberNumbers(current: String, future: String)

object SchemeMemberNumbers {
  implicit val formats : OFormat[SchemeMemberNumbers] = Json.format[SchemeMemberNumbers]
}

case class InsuranceCompany(name: Option[String],policyNumber: Option[String], address: Option[CorrespondenceAddress])

object InsuranceCompany {
  implicit val formats : OFormat[InsuranceCompany] = Json.format[InsuranceCompany]
}
//TODO: Do not use, this is part of ticket PODS-1589 still being worked on.
case class SchemeDetails(srn: Option[String],
                         pstr: Option[String],
                         status: String,
                         name: String,
                         isMasterTrust: Boolean,
                         typeOfScheme: Option[String],
                         otherTypeOfScheme: Option[String],
                         hasMoreThanTenTrustees: Boolean,
                         members: SchemeMemberNumbers,
                         isInvestmentRegulated: Boolean,
                         isOccupational: Boolean,
                         benefits: String,
                         country: String,
                         areBenefitsSecured: Boolean,
                         insuranceCompany: Option[InsuranceCompany]) {
}

object SchemeDetails {

  val apiReads : Reads[SchemeDetails] = (
    (JsPath \ "srn").readNullable[String] and
      (JsPath \ "pstr").readNullable[String] and
      (JsPath \ "schemeStatus").read[String] and
      (JsPath \ "schemeName").read[String] and
      (JsPath \ "isSchemeMasterTrust").readNullable[Boolean] and
      (JsPath \ "pensionSchemeStructure").readNullable[String] and
      (JsPath \ "otherPensionSchemeStructure").readNullable[String] and
      (JsPath \ "hasMoreThanTenTrustees").readNullable[Boolean] and
      (JsPath \ "currentSchemeMembers").read[String] and
      (JsPath \ "futureSchemeMembers").read[String] and
      (JsPath \ "isReguledSchemeInvestment").read[Boolean] and
      (JsPath \ "isOccupationalPensionScheme").read[Boolean] and
      (JsPath \ "schemeProvideBenefits").read[String] and
      (JsPath \ "schemeEstablishedCountry").read[String] and
      (JsPath \ "isSchemeBenefitsInsuranceCompany").read[Boolean] and
      (JsPath \ "insuranceCompanyName").readNullable[String] and
      (JsPath \ "policyNumber").readNullable[String] and
      (JsPath \ "insuranceCompanyAddressDetails").readNullable[CorrespondenceAddress]
    )((srn,
       pstr,
       status,
       name,
       isMasterTrust,
       typeOfScheme,
       otherTypeOfScheme,
       moreThan10Trustees,
       members,futureMembers,
       isRegulated,
       isOccupational,
       benefits,
       country,
       benefitsSecured,
       insuranceName,
       policy,
       insuranceAddress) =>
    SchemeDetails(srn,
      pstr,
      status,
      name,
      isMasterTrust.getOrElse(false),
      typeOfScheme,
      otherTypeOfScheme,
      moreThan10Trustees.getOrElse(false),
      SchemeMemberNumbers(members,futureMembers),
      isRegulated,
      isOccupational,
      benefits,
      country,
      benefitsSecured,
      getInsuranceCompany(insuranceName,policy,insuranceAddress)))

  implicit val formats: OFormat[SchemeDetails] = Json.format[SchemeDetails]

  private def getInsuranceCompany(name: Option[String],policyNumber: Option[String], address: Option[CorrespondenceAddress]) : Option[InsuranceCompany] = {
    (name,policyNumber,address) match {
      case (None,None,None) => None
      case _ => Some(InsuranceCompany(name,policyNumber,address))
    }
  }
}