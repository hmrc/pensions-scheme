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
import models.enumeration.{Benefits, SchemeMembers, SchemeType}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

class SchemeDetailsTransformer @Inject()(addressTransformer: AddressTransformer) extends JsonTransformer {

  private def membersReads(desPath: String, uaPath: String): Reads[JsObject] =
    (__ \ desPath).read[String].flatMap { members =>
      (__ \ uaPath).json.put(JsString(SchemeMembers.nameWithValue(members)))
    }

  private val benefitsReads: Reads[JsObject] =
    (__ \ 'schemeProvideBenefits).read[String].flatMap { members =>
      (__ \ 'benefits).json.put(JsString(Benefits.nameWithValue(members)))
    }

  private val schemeTypeReads: Reads[JsObject] = {
    (__ \ 'isSchemeMasterTrust).readNullable[Boolean].flatMap {
      case Some(true) => (__ \ 'schemeType \ 'name).json.put(JsString("master"))
      case _ =>
        (__ \ 'pensionSchemeStructure).readNullable[String].flatMap { schemeStructure =>
          schemeStructure.map { schemeType =>
            (__ \ 'schemeType \ 'name).json.put(JsString(SchemeType.nameWithValue(schemeType)))
          } getOrElse doNothing
        }
    } and
    ((__ \ 'schemeType \ 'schemeTypeDetails).json.copyFrom((__ \ 'otherPensionSchemeStructure).json.pick)
      orElse doNothing) reduce
  }

  val userAnswersSchemeDetailsReads: Reads[JsObject] =
    (__ \ 'schemeName).json.copyFrom((__ \ 'schemeName).json.pick) and
      (__ \ 'investmentRegulated).json.copyFrom((__ \ 'isReguledSchemeInvestment).json.pick) and
      (__ \ 'occupationalPensionScheme).json.copyFrom((__ \ 'isOccupationalPensionScheme).json.pick) and
      (__ \ 'schemeEstablishedCountry).json.copyFrom((__ \ 'schemeEstablishedCountry).json.pick) and
      (__ \ 'securedBenefits).json.copyFrom((__ \ 'isSchemeBenefitsInsuranceCompany).json.pick) and
      ((__ \ 'insuranceCompanyName).json.copyFrom((__ \ 'insuranceCompanyName).json.pick)
        orElse doNothing) and
      ((__ \ 'insurancePolicyNumber).json.copyFrom((__ \ 'policyNumber).json.pick)
        orElse doNothing) and
      (addressTransformer.getAddress(__ \ 'insurerAddress, __ \ 'insuranceCompanyAddressDetails)
        orElse doNothing) and
      membersReads(desPath = "currentSchemeMembers", uaPath = "membership") and
      membersReads(desPath = "futureSchemeMembers", uaPath = "membershipFuture") and
      benefitsReads and
      schemeTypeReads reduce
}
