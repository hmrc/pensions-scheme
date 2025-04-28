/*
 * Copyright 2025 HM Revenue & Customs
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

package models.etmpToUserAnswers.psaSchemeDetails

import com.google.inject.Inject
import models.enumeration.{Benefits, SchemeMembers, SchemeType}
import models.etmpToUserAnswers.AddressTransformer
import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import play.api.libs.json.Reads.JsObjectReducer

import scala.language.postfixOps

class SchemeDetailsTransformer @Inject()(
                                          addressTransformer: AddressTransformer
                                        )
  extends JsonTransformer {

  private def membersReads(ifPath: String, uaPath: String): Reads[JsObject] =
    (__ \ Symbol("psaPspSchemeDetails") \ Symbol("schemeDetails") \ ifPath).read[String].flatMap {
      members =>
        (__ \ uaPath).json.put(JsString(SchemeMembers.nameWithValue(members)))
    }

  private val benefitsReads: Reads[JsObject] =
    (__ \ Symbol("psaPspSchemeDetails") \ Symbol("schemeDetails") \ Symbol("schemeProvideBenefits")).read[String].flatMap {
      benefits =>
        (__ \ Symbol("benefits")).json.put(JsString(Benefits.nameWithValue(benefits)))
    }

  private val moneyPurchaseReads: Reads[JsObject] =
    (__ \ Symbol("psaPspSchemeDetails") \ Symbol("schemeDetails") \ Symbol("tcmpBenefitType")).readNullable[String].flatMap {
      case Some(benefits) =>
        (__ \ Symbol("moneyPurchaseBenefits")).json.put(JsString(benefits))
      case _ =>
        doNothing
    }

  private val schemeTypeReads: Reads[JsObject] = {
    (__ \ Symbol("psaPspSchemeDetails") \ Symbol("schemeDetails") \ Symbol("isSchemeMasterTrust")).readNullable[Boolean].flatMap {
      case Some(true) =>
        (__ \ Symbol("schemeType") \ Symbol("name")).json.put(JsString("master"))
      case _ =>
        (__ \ Symbol("psaPspSchemeDetails") \ Symbol("schemeDetails") \ Symbol("pensionSchemeStructure")).readNullable[String].flatMap {
          _.map {
            schemeType =>
              (__ \ Symbol("schemeType") \ Symbol("name")).json.put(
                JsString(SchemeType.nameWithValue(schemeType))
              )
          } getOrElse doNothing
        }
    } and (__ \ Symbol("schemeType") \ Symbol("schemeTypeDetails")).json.copyFrom(schemeTypeDetails).orElse(doNothing) reduce
  }

  private def schemeTypeDetails: Reads[JsString] = {
    (__ \ Symbol("psaPspSchemeDetails") \ Symbol("schemeDetails") \ Symbol("pensionSchemeStructure")).readNullable[String].flatMap {
      case Some("Other") =>
        (__ \ Symbol("psaPspSchemeDetails") \ Symbol("schemeDetails") \ Symbol("otherPensionSchemeStructure")).readNullable[String].map {
          case Some(v) => JsString(v)
          case _ => JsString("Unknown")
        }
      case _ => Reads.failed[JsString]("Not applicable")
    }
  }

  private def getPsaIds: Reads[JsObject] = {
    val psaReads: Reads[JsObject] = (
      (__ \ Symbol("id")).json.copyFrom((__ \ Symbol("psaid")).json.pick) and
        ((__ \ Symbol("individual") \ Symbol("firstName")).json.copyFrom(
          (__ \ Symbol("firstName")).json.pick
        ) orElse doNothing) and
        ((__ \ Symbol("individual") \ Symbol("middleName")).json.copyFrom(
          (__ \ Symbol("middleName")).json.pick
        ) orElse doNothing) and
        ((__ \ Symbol("individual") \ Symbol("lastName")).json.copyFrom(
          (__ \ Symbol("lastName")).json.pick
        ) orElse doNothing) and
        ((__ \ Symbol("organisationOrPartnershipName")).json.copyFrom(
          (__ \ Symbol("orgOrPartnershipName")).json.pick
        ) orElse doNothing) and
        ((__ \ Symbol("relationshipDate")).json.copyFrom(
          (__ \ Symbol("relationshipDate")).json.pick
        ) orElse doNothing)
      ) reduce

    (__ \ Symbol("psaPspSchemeDetails") \ Symbol("psaDetails")).readNullable(
      __.read(Reads.seq(psaReads).map(JsArray(_))))
      .flatMap { psa =>
        (__ \ Symbol("psaDetails")).json.put(
          psa.getOrElse(JsArray())
        ).orElse(doNothing)
      }
  }

  private def getPspDetails: Reads[JsObject] = {
    val pspReads = (
      (__ \ Symbol("id")).json.copyFrom(
        (__ \ Symbol("pspid")).json.pick
      ) `and`
        (__ \ Symbol("individual") \ Symbol("firstName")).json.copyFrom(
          (__ \ Symbol("firstName")).json.pick
        ).orElse(doNothing) `and`
        (__ \ Symbol("individual") \ Symbol("middleName")).json.copyFrom(
          (__ \ Symbol("middleName")).json.pick
        ).orElse(doNothing) `and`
        (__ \ Symbol("individual") \ Symbol("lastName")).json.copyFrom(
          (__ \ Symbol("lastName")).json.pick
        ).orElse(doNothing) `and`
        (__ \ Symbol("organisationOrPartnershipName")).json.copyFrom(
          (__ \ Symbol("orgOrPartnershipName")).json.pick
        ).orElse(doNothing) `and`
        (__ \ Symbol("relationshipStartDate")).json.copyFrom(
          (__ \ Symbol("relationshipStartDate")).json.pick
        ) `and`
        (__ \ Symbol("authorisingPSAID")).json.copyFrom(
          (__ \ Symbol("authorisedPSAID")).json.pick
        ) `and`
        (__ \ Symbol("authorisingPSA") \ Symbol("firstName")).json.copyFrom(
          (__ \ Symbol("authorisedPSAFirstName")).json.pick
        ).orElse(doNothing) `and`
        (__ \ Symbol("authorisingPSA") \ Symbol("middleName")).json.copyFrom(
          (__ \ Symbol("authorisedPSAMiddleName")).json.pick
        ).orElse(doNothing) `and`
        (__ \ Symbol("authorisingPSA") \ Symbol("lastName")).json.copyFrom(
          (__ \ Symbol("authorisedPSALastName")).json.pick
        ).orElse(doNothing) `and`
        (__ \ Symbol("authorisingPSA") \ Symbol("organisationOrPartnershipName")).json.copyFrom(
          (__ \ Symbol("authorisedPSAOrgOrPartName")).json.pick
        ).orElse(doNothing) `and`
        (__ \ Symbol("clientReference")).json.copyFrom(
          (__ \ Symbol("clientReference")).json.pick
        ).orElse(doNothing)
      ) reduce

    (__ \ Symbol("psaPspSchemeDetails") \ Symbol("pspDetails")).readNullable(
      __.read(Reads.seq(pspReads).map(JsArray(_))))
      .flatMap { psp =>
        (__ \ Symbol("pspDetails")).json.put(psp.getOrElse(JsArray())).orElse(doNothing)
      }
  }

  private def schemeDetailsReads(pstr:Option[String]): Reads[JsObject] =
    getPsaIds `and`
      getPspDetails `and`
        (__ \ Symbol("srn")).json.copyFrom(
          (__ \ Symbol("psaPspSchemeDetails") \ Symbol("schemeDetails") \ Symbol("srn")).json.pick
        ).orElse(doNothing) `and`
        (__ \ Symbol("pstr")).json.copyFrom(
          (__ \ Symbol("psaPspSchemeDetails") \ Symbol("schemeDetails") \ Symbol("pstr")).json.pick
        ).orElse(Reads.pure(
          pstr.map(value => Json.obj("pstr" -> JsString(value))).getOrElse(Json.obj())
        )) `and`
        (__ \ Symbol("schemeStatus")).json.copyFrom(
          (__ \ Symbol("psaPspSchemeDetails") \ Symbol("schemeDetails") \ Symbol("schemeStatus")).json.pick
        ) `and`
        (__ \ Symbol("schemeName")).json.copyFrom(
          (__ \ Symbol("psaPspSchemeDetails") \ Symbol("schemeDetails") \ Symbol("schemeName")).json.pick
        ) `and`
        schemeTypeReads `and`
        (__ \ "moreThanTenTrustees").json.copyFrom(
          (__ \ Symbol("psaPspSchemeDetails") \ Symbol("schemeDetails") \ Symbol("hasMoreThanTenTrustees")).json.pick
        ).orElse(doNothing) `and`
        membersReads(ifPath = "currentSchemeMembers", uaPath = "membership") `and`
        membersReads(ifPath = "futureSchemeMembers", uaPath = "membershipFuture") `and`
        (__ \ Symbol("investmentRegulated")).json.copyFrom(
          (__ \ Symbol("psaPspSchemeDetails") \ Symbol("schemeDetails") \ Symbol("isRegulatedSchemeInvestment")).json.pick
        ) `and`
        (__ \ Symbol("occupationalPensionScheme")).json.copyFrom(
          (__ \ Symbol("psaPspSchemeDetails") \ Symbol("schemeDetails") \ Symbol("isOccupationalPensionScheme")).json.pick
        ) `and`
        benefitsReads `and`
        moneyPurchaseReads `and`
        (__ \ Symbol("schemeEstablishedCountry")).json.copyFrom(
          (__ \ Symbol("psaPspSchemeDetails") \ Symbol("schemeDetails") \ Symbol("schemeEstablishedCountry")).json.pick
        ) `and`
        (__ \ Symbol("securedBenefits")).json.copyFrom(
          (__ \ Symbol("psaPspSchemeDetails") \ Symbol("schemeDetails") \ Symbol("isSchemeBenefitsInsuranceCompany")).json.pick
        ) `and`
        (__ \ Symbol("insuranceCompanyName")).json.copyFrom(
          (__ \ Symbol("psaPspSchemeDetails") \ Symbol("schemeDetails") \ Symbol("insuranceCompanyName")).json.pick
        ).orElse(doNothing) `and`
        (__ \ Symbol("insurancePolicyNumber")).json.copyFrom(
          (__ \ Symbol("psaPspSchemeDetails") \ Symbol("schemeDetails") \ Symbol("policyNumber")).json.pick
        ).orElse(doNothing) `and`
        addressTransformer.getDifferentAddress(
          __ \ Symbol("insurerAddress"), __ \ Symbol("psaPspSchemeDetails") \ Symbol("schemeDetails") \ Symbol("insuranceCompanyAddressDetails")
        ).orElse(doNothing) `and`
        (__ \ Symbol("isAboutBenefitsAndInsuranceComplete")).json.put(JsBoolean(true)) and
        (__ \ Symbol("isAboutMembersComplete")).json.put(JsBoolean(true)) and
        (__ \ Symbol("isBeforeYouStartComplete")).json.put(JsBoolean(true)) reduce

  private val racdacSchemeDetailsReads: Reads[JsObject] =
    (__ \ Symbol("racdacScheme")).json.put(JsBoolean(true)) `and`
      (__ \ Symbol("srn")).json.copyFrom(
        (__ \ Symbol("psaPspSchemeDetails") \ Symbol("racdacSchemeDetails") \ Symbol("srn")).json.pick
      ).orElse(doNothing) `and`
      (__ \ Symbol("pstr")).json.copyFrom(
        (__ \ Symbol("psaPspSchemeDetails") \ Symbol("racdacSchemeDetails") \ Symbol("pstr")).json.pick
      ).orElse(doNothing) `and`
      (__ \ Symbol("schemeStatus")).json.copyFrom(
        (__ \ Symbol("psaPspSchemeDetails") \ Symbol("racdacSchemeDetails") \ Symbol("schemeStatus")).json.pick
      ) `and`
      (__ \ Symbol("schemeName")).json.copyFrom(
        (__ \ Symbol("psaPspSchemeDetails") \ Symbol("racdacSchemeDetails") \ Symbol("racdacName")).json.pick
      ) `and`
      (__ \ Symbol("racdac") \ Symbol("contractOrPolicyNumber")).json.copyFrom(
        (__ \ Symbol("psaPspSchemeDetails") \ Symbol("racdacSchemeDetails") \ Symbol("contractOrPolicyNumber")).json.pick
      ) `and`
      (__ \ Symbol("racdac") \ Symbol("name")).json.copyFrom(
        (__ \ Symbol("psaPspSchemeDetails") \ Symbol("racdacSchemeDetails") \ Symbol("racdacName")).json.pick
      ) `and`
      (__ \ Symbol("registrationStartDate")).json.copyFrom(
        (__ \ Symbol("psaPspSchemeDetails") \ Symbol("racdacSchemeDetails") \ Symbol("registrationStartDate")).json.pick
      ).orElse(doNothing) reduce

  def userAnswersSchemeDetailsReads(pstr:Option[String]): Reads[JsObject] =
    getPsaIds `and`
      getPspDetails `and`
        (__ \ Symbol("psaPspSchemeDetails") \ Symbol("racdacScheme")).readNullable[String].flatMap {
          case Some(_) =>
            racdacSchemeDetailsReads
          case _ =>
            schemeDetailsReads(pstr)
        } reduce
}
