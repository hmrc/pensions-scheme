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

import com.google.inject.Inject
import models.enumeration.{Benefits, SchemeMembers, SchemeType}
import models.etmpToUserAnswers.AddressTransformer
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

import scala.language.postfixOps

class SchemeDetailsTransformer @Inject()(
                                          addressTransformer: AddressTransformer
                                        )
  extends JsonTransformer {

  private def membersReads(ifPath: String, uaPath: String): Reads[JsObject] =
    (__ \ Symbol("schemeDetails") \ ifPath).read[String].flatMap {
      members =>
        (__ \ uaPath).json.put(JsString(SchemeMembers.nameWithValue(members)))
    }

  private val benefitsReads: Reads[JsObject] =
    (__ \ Symbol("schemeDetails") \ Symbol("schemeProvideBenefits")).read[String].flatMap {
      benefits =>
        (__ \ Symbol("benefits")).json.put(JsString(Benefits.nameWithValue(benefits)))
    }

  private val moneyPurchaseReads: Reads[JsObject] =
    (__ \ Symbol("schemeDetails") \ Symbol("tcmpBenefitType")).readNullable[String].flatMap {
      case Some(benefits) =>
        (__ \ Symbol("moneyPurchaseBenefits")).json.put(JsString(benefits))
      case _ =>
        doNothing
    }

  private val schemeTypeReads: Reads[JsObject] =
    (__ \ Symbol("schemeDetails") \ Symbol("isSchemeMasterTrust")).read[Boolean].flatMap {
      case true =>
        (__ \ Symbol("schemeType") \ Symbol("name")).json.put(JsString("master"))
      case _ =>
        (__ \ Symbol("schemeDetails") \ Symbol("pensionSchemeStructure")).readNullable[String].flatMap {
          _.map { schemeType =>
            (__ \ Symbol("schemeType") \ Symbol("name")).json.put(JsString(SchemeType.nameWithValue(schemeType)))
          } getOrElse doNothing
        }
    } and
      ((__ \ Symbol("schemeType") \ Symbol("schemeTypeDetails")).json.copyFrom(
        (__ \ Symbol("schemeDetails") \ Symbol("otherPensionSchemeStructure")).json.pick
      ) orElse doNothing) reduce

  private def pspRelationshipDetails: Reads[JsObject] =
    (
      (__ \ Symbol("pspDetails") \ Symbol("id")).json.copyFrom(
        (__ \ Symbol("pspRelationshipDetails") \ Symbol("pspid")).json.pick
      ) and
        ((__ \ Symbol("pspDetails") \ Symbol("individual") \ Symbol("firstName")).json.copyFrom(
          (__ \ Symbol("pspRelationshipDetails") \ Symbol("firstName")).json.pick
        ) orElse doNothing) and
        ((__ \ Symbol("pspDetails") \ Symbol("individual") \ Symbol("middleName")).json.copyFrom(
          (__ \ Symbol("pspRelationshipDetails") \ Symbol("middleName")).json.pick
        ) orElse doNothing) and
        ((__ \ Symbol("pspDetails") \ Symbol("individual") \ Symbol("lastName")).json.copyFrom(
          (__ \ Symbol("pspRelationshipDetails") \ Symbol("lastName")).json.pick
        ) orElse doNothing) and
        ((__ \ Symbol("pspDetails") \ Symbol("organisationOrPartnershipName")).json.copyFrom(
          (__ \ Symbol("pspRelationshipDetails") \ Symbol("orgOrPartnershipName")).json.pick
        ) orElse doNothing) and
        (__ \ Symbol("pspDetails") \ Symbol("relationshipStartDate")).json.copyFrom(
          (__ \ Symbol("pspRelationshipDetails") \ Symbol("relationshipStartDate")).json.pick
        ) and
        (__ \ Symbol("pspDetails") \ Symbol("authorisingPSAID")).json.copyFrom(
          (__ \ Symbol("pspRelationshipDetails") \ Symbol("authorisedPSAID")).json.pick
        ) and
        ((__ \ Symbol("pspDetails") \ Symbol("authorisingPSA") \ Symbol("firstName")).json.copyFrom(
          (__ \ Symbol("pspRelationshipDetails") \ Symbol("authorisedPSAFirstName")).json.pick
        ) orElse doNothing) and
        ((__ \ Symbol("pspDetails") \ Symbol("authorisingPSA") \ Symbol("middleName")).json.copyFrom(
          (__ \ Symbol("pspRelationshipDetails") \ Symbol("authorisedPSAMiddleName")).json.pick
        ) orElse doNothing) and
        ((__ \ Symbol("pspDetails") \ Symbol("authorisingPSA") \ Symbol("lastName")).json.copyFrom(
          (__ \ Symbol("pspRelationshipDetails") \ Symbol("authorisedPSALastName")).json.pick
        ) orElse doNothing) and
        ((__ \ Symbol("pspDetails") \ Symbol("authorisingPSA") \ Symbol("organisationOrPartnershipName")).json.copyFrom(
          (__ \ Symbol("pspRelationshipDetails") \ Symbol("authorisedPSAOrgOrPartName")).json.pick
        ) orElse doNothing) and
        ((__ \ Symbol("pspDetails") \ Symbol("pspClientReference")).json.copyFrom(
          (__ \ Symbol("pspRelationshipDetails") \ Symbol("clientReference")).json.pick
        ) orElse doNothing)
      ) reduce

  private val schemeDetailsReads: Reads[JsObject] =
    ((__ \ Symbol("srn")).json.copyFrom(
      (__ \ Symbol("schemeDetails") \ Symbol("srn")).json.pick
    ) orElse doNothing) and
      (__ \ Symbol("pstr")).json.copyFrom(
        (__ \ Symbol("schemeDetails") \ Symbol("pstr")).json.pick
      ) and
      (__ \ Symbol("schemeStatus")).json.copyFrom(
        (__ \ Symbol("schemeDetails") \ Symbol("schemeStatus")).json.pick
      ) and
      (__ \ Symbol("schemeName")).json.copyFrom(
        (__ \ Symbol("schemeDetails") \ Symbol("schemeName")).json.pick
      ) and
      schemeTypeReads and
      membersReads(ifPath = "currentSchemeMembers", uaPath = "membership") and
      membersReads(ifPath = "futureSchemeMembers", uaPath = "membershipFuture") and
      (__ \ Symbol("investmentRegulated")).json.copyFrom(
        (__ \ Symbol("schemeDetails") \ Symbol("isRegulatedSchemeInvestment")).json.pick
      ) and
      (__ \ Symbol("occupationalPensionScheme")).json.copyFrom(
        (__ \ Symbol("schemeDetails") \ Symbol("isOccupationalPensionScheme")).json.pick
      ) and
      benefitsReads and
      moneyPurchaseReads and
      (__ \ Symbol("schemeEstablishedCountry")).json.copyFrom(
        (__ \ Symbol("schemeDetails") \ Symbol("schemeEstablishedCountry")).json.pick
      ) and
      (__ \ Symbol("securedBenefits")).json.copyFrom(
        (__ \ Symbol("schemeDetails") \ Symbol("isSchemeBenefitsInsuranceCompany")).json.pick
      ) and
      ((__ \ Symbol("insuranceCompanyName")).json.copyFrom(
        (__ \ Symbol("schemeDetails") \ Symbol("insuranceCompanyName")).json.pick
      ) orElse doNothing) and
      ((__ \ Symbol("insurancePolicyNumber")).json.copyFrom(
        (__ \ Symbol("schemeDetails") \ Symbol("policyNumber")).json.pick
      ) orElse doNothing) and
      (addressTransformer.getDifferentAddress(
        __ \ Symbol("insurerAddress"), __ \ Symbol("schemeDetails") \ Symbol("insuranceCompanyAddressDetails")
      ) orElse doNothing) and
      (__ \ Symbol("isAboutBenefitsAndInsuranceComplete")).json.put(JsBoolean(true)) and
      (__ \ Symbol("isAboutMembersComplete")).json.put(JsBoolean(true)) and
      (__ \ Symbol("isBeforeYouStartComplete")).json.put(JsBoolean(true)) reduce

  private val racdacSchemeDetailsReads: Reads[JsObject] = {
    (__ \ Symbol("racdacScheme")).json.put(JsBoolean(true)) and
      ((__ \ Symbol("srn")).json.copyFrom(
        (__ \ Symbol("racdacSchemeDetails") \ Symbol("srn")).json.pick
      ) orElse doNothing) and
      (__ \ Symbol("pstr")).json.copyFrom(
        (__ \ Symbol("racdacSchemeDetails") \ Symbol("pstr")).json.pick
      ) and
      (__ \ Symbol("schemeStatus")).json.copyFrom(
        (__ \ Symbol("racdacSchemeDetails") \ Symbol("schemeStatus")).json.pick
      ) and
      (__ \ Symbol("schemeName")).json.copyFrom(
        (__ \ Symbol("racdacSchemeDetails") \ Symbol("racdacName")).json.pick
      ) and
      (__ \ Symbol("racdac") \ Symbol("contractOrPolicyNumber")).json.copyFrom(
        (__ \ Symbol("racdacSchemeDetails") \ Symbol("contractOrPolicyNumber")).json.pick
      ) and
      (__ \ Symbol("registrationStartDate")).json.copyFrom(
        (__ \ Symbol("racdacSchemeDetails") \ Symbol("registrationStartDate")).json.pick
      ) reduce
  }

  val userAnswersSchemeDetailsReads: Reads[JsObject] =
    pspRelationshipDetails and
      (__ \ Symbol("racdacScheme")).readNullable[String].flatMap {
        case Some("Yes") =>
          racdacSchemeDetailsReads
        case _ =>
          schemeDetailsReads
      } reduce
}
