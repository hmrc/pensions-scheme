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
    (__ \ 'schemeDetails \ ifPath).read[String].flatMap {
      members =>
        (__ \ uaPath).json.put(JsString(SchemeMembers.nameWithValue(members)))
    }

  private val benefitsReads: Reads[JsObject] =
    (__ \ 'schemeDetails \ 'schemeProvideBenefits).read[String].flatMap {
      benefits =>
        (__ \ 'benefits).json.put(JsString(Benefits.nameWithValue(benefits)))
    }

  private val moneyPurchaseReads: Reads[JsObject] =
    (__ \ 'schemeDetails \ 'tcmpBenefitType).readNullable[String].flatMap {
      case Some(benefits) =>
        (__ \ 'moneyPurchaseBenefits).json.put(JsString(benefits))
      case _ =>
        doNothing
    }

  private val schemeTypeReads: Reads[JsObject] =
    (__ \ 'schemeDetails \ 'isSchemeMasterTrust).read[Boolean].flatMap {
      case true =>
        (__ \ 'schemeType \ 'name).json.put(JsString("master"))
      case _ =>
        (__ \ 'schemeDetails \ 'pensionSchemeStructure).readNullable[String].flatMap {
          _.map { schemeType =>
            (__ \ 'schemeType \ 'name).json.put(JsString(SchemeType.nameWithValue(schemeType)))
          } getOrElse doNothing
        }
    } and
      ((__ \ 'schemeType \ 'schemeTypeDetails).json.copyFrom(
        (__ \ 'schemeDetails \ 'otherPensionSchemeStructure).json.pick
      ) orElse doNothing) reduce

  private def pspRelationshipDetails: Reads[JsObject] =
    (
      (__ \ 'pspDetails \ 'id).json.copyFrom(
        (__ \ 'pspRelationshipDetails \ 'pspid).json.pick
      ) and
        ((__ \ 'pspDetails \ 'individual \ 'firstName).json.copyFrom(
          (__ \ 'pspRelationshipDetails \ 'firstName).json.pick
        ) orElse doNothing) and
        ((__ \ 'pspDetails \ 'individual \ 'middleName).json.copyFrom(
          (__ \ 'pspRelationshipDetails \ 'middleName).json.pick
        ) orElse doNothing) and
        ((__ \ 'pspDetails \ 'individual \ 'lastName).json.copyFrom(
          (__ \ 'pspRelationshipDetails \ 'lastName).json.pick
        ) orElse doNothing) and
        ((__ \ 'pspDetails \ 'organisationOrPartnershipName).json.copyFrom(
          (__ \ 'pspRelationshipDetails \ 'orgOrPartnershipName).json.pick
        ) orElse doNothing) and
        (__ \ 'pspDetails \ 'relationshipStartDate).json.copyFrom(
          (__ \ 'pspRelationshipDetails \ 'relationshipStartDate).json.pick
        ) and
        (__ \ 'pspDetails \ 'authorisingPSAID).json.copyFrom(
          (__ \ 'pspRelationshipDetails \ 'authorisedPSAID).json.pick
        ) and
        ((__ \ 'pspDetails \ 'authorisingPSA \ 'firstName).json.copyFrom(
          (__ \ 'pspRelationshipDetails \ 'authorisedPSAFirstName).json.pick
        ) orElse doNothing) and
        ((__ \ 'pspDetails \ 'authorisingPSA \ 'middleName).json.copyFrom(
          (__ \ 'pspRelationshipDetails \ 'authorisedPSAMiddleName).json.pick
        ) orElse doNothing) and
        ((__ \ 'pspDetails \ 'authorisingPSA \ 'lastName).json.copyFrom(
          (__ \ 'pspRelationshipDetails \ 'authorisedPSALastName).json.pick
        ) orElse doNothing) and
        ((__ \ 'pspDetails \ 'authorisingPSA \ 'organisationOrPartnershipName).json.copyFrom(
          (__ \ 'pspRelationshipDetails \ 'authorisedPSAOrgOrPartName).json.pick
        ) orElse doNothing) and
        ((__ \ 'pspDetails \ 'pspClientReference).json.copyFrom(
          (__ \ 'pspRelationshipDetails \ 'clientReference).json.pick
        ) orElse doNothing)
      ) reduce

  private val schemeDetailsReads: Reads[JsObject] =
    ((__ \ 'srn).json.copyFrom(
      (__ \ 'schemeDetails \ 'srn).json.pick
    ) orElse doNothing) and
      (__ \ 'pstr).json.copyFrom(
        (__ \ 'schemeDetails \ 'pstr).json.pick
      ) and
      (__ \ 'schemeStatus).json.copyFrom(
        (__ \ 'schemeDetails \ 'schemeStatus).json.pick
      ) and
      (__ \ 'schemeName).json.copyFrom(
        (__ \ 'schemeDetails \ 'schemeName).json.pick
      ) and
      schemeTypeReads and
      membersReads(ifPath = "currentSchemeMembers", uaPath = "membership") and
      membersReads(ifPath = "futureSchemeMembers", uaPath = "membershipFuture") and
      (__ \ 'investmentRegulated).json.copyFrom(
        (__ \ 'schemeDetails \ 'isRegulatedSchemeInvestment).json.pick
      ) and
      (__ \ 'occupationalPensionScheme).json.copyFrom(
        (__ \ 'schemeDetails \ 'isOccupationalPensionScheme).json.pick
      ) and
      benefitsReads and
      moneyPurchaseReads and
      (__ \ 'schemeEstablishedCountry).json.copyFrom(
        (__ \ 'schemeDetails \ 'schemeEstablishedCountry).json.pick
      ) and
      (__ \ 'securedBenefits).json.copyFrom(
        (__ \ 'schemeDetails \ 'isSchemeBenefitsInsuranceCompany).json.pick
      ) and
      ((__ \ 'insuranceCompanyName).json.copyFrom(
        (__ \ 'schemeDetails \ 'insuranceCompanyName).json.pick
      ) orElse doNothing) and
      ((__ \ 'insurancePolicyNumber).json.copyFrom(
        (__ \ 'schemeDetails \ 'policyNumber).json.pick
      ) orElse doNothing) and
      (addressTransformer.getDifferentAddress(
        __ \ 'insurerAddress, __ \ 'schemeDetails \ 'insuranceCompanyAddressDetails
      ) orElse doNothing) and
      (__ \ 'isAboutBenefitsAndInsuranceComplete).json.put(JsBoolean(true)) and
      (__ \ 'isAboutMembersComplete).json.put(JsBoolean(true)) and
      (__ \ 'isBeforeYouStartComplete).json.put(JsBoolean(true)) reduce

  private val racdacSchemeDetailsReads: Reads[JsObject] = {
    (__ \ 'racdacScheme).json.put(JsBoolean(true)) and
    ((__ \ 'srn).json.copyFrom(
      (__ \ 'racdacSchemeDetails \ 'srn).json.pick
    ) orElse doNothing) and
      (__ \ 'pstr).json.copyFrom(
        (__ \ 'racdacSchemeDetails \ 'pstr).json.pick
      ) and
      (__ \ 'schemeStatus).json.copyFrom(
        (__ \ 'racdacSchemeDetails \ 'schemeStatus).json.pick
      ) and
      (__ \ 'schemeName).json.copyFrom(
        (__ \ 'racdacSchemeDetails \ 'racdacName).json.pick
      ) and
      (__ \ 'racdac \ 'contractOrPolicyNumber).json.copyFrom(
        (__ \ 'racdacSchemeDetails \ 'contractOrPolicyNumber).json.pick
      ) and
      (__ \ 'registrationStartDate).json.copyFrom(
        (__ \ 'racdacSchemeDetails \ 'registrationStartDate).json.pick
      ) reduce
  }

  val userAnswersSchemeDetailsReads: Reads[JsObject] =
    pspRelationshipDetails and
      (__ \ 'racdacScheme).readNullable[String].flatMap {
        case Some("Yes") =>
          racdacSchemeDetailsReads
        case _ =>
          schemeDetailsReads
    } reduce
}
