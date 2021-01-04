/*
 * Copyright 2021 HM Revenue & Customs
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

package models.etmpToUserAnswers.DES

import com.google.inject.Inject
import models.enumeration.Benefits
import models.enumeration.SchemeMembers
import models.enumeration.SchemeType
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

class SchemeDetailsTransformer @Inject()(addressTransformer: AddressTransformer) extends JsonTransformer {

  private def membersReads(desPath: String, uaPath: String): Reads[JsObject] =
    (__ \ basePath \ 'schemeDetails \ desPath).read[String].flatMap { members =>
      (__ \ uaPath).json.put(JsString(SchemeMembers.nameWithValue(members)))
    }

  private val benefitsReads: Reads[JsObject] =
    (__ \ basePath \ 'schemeDetails \ 'schemeProvideBenefits).read[String].flatMap { members =>
      (__ \ 'benefits).json.put(JsString(Benefits.nameWithValue(members)))
    }

  private val schemeTypeReads: Reads[JsObject] = {
    (__ \ basePath \ 'schemeDetails \ 'isSchemeMasterTrust).readNullable[Boolean].flatMap {
      case Some(true) => (__ \ 'schemeType \ 'name).json.put(JsString("master"))
      case _ =>
        (__ \ basePath \ 'schemeDetails \ 'pensionSchemeStructure).readNullable[String].flatMap { schemeStructure =>
          schemeStructure.map { schemeType =>
            (__ \ 'schemeType \ 'name).json.put(JsString(SchemeType.nameWithValue(schemeType)))
          } getOrElse doNothing
        }
    } and
      ((__ \ 'schemeType \ 'schemeTypeDetails).json.copyFrom((__ \ basePath \ 'schemeDetails \ 'otherPensionSchemeStructure).json.pick)
        orElse doNothing) reduce
  }

  private def getPsaIds: Reads[JsObject] = {
    val psaReads = (
      (__ \ 'id).json.copyFrom((__ \ 'psaid).json.pick) and
        ((__ \ 'individual \ 'firstName).json.copyFrom((__ \ 'firstName).json.pick)
          orElse doNothing) and
        ((__ \ 'individual \ 'middleName).json.copyFrom((__ \ 'middleName).json.pick)
          orElse doNothing) and
        ((__ \ 'individual \ 'lastName).json.copyFrom((__ \ 'lastName).json.pick)
          orElse doNothing) and
        ((__ \ 'organisationOrPartnershipName).json.copyFrom((__ \ 'organizationOrPartnershipName).json.pick)
          orElse doNothing) and
        ((__ \ 'relationshipDate).json.copyFrom((__ \ 'relationshipDate).json.pick)
          orElse doNothing)
      ) reduce

    (__ \ basePath \ 'psaDetails).readNullable(
      __.read(Reads.seq(psaReads).map(JsArray(_))))
      .flatMap { partnership =>
        (__ \ 'psaDetails).json.put(partnership.getOrElse(JsArray())) orElse doNothing
      }
  }

  private def getPspDetails: Reads[JsObject] = {
    val pspReads = (
      (__ \ 'id).json.copyFrom((__ \ 'pspid).json.pick) and
        ((__ \ 'individual \ 'firstName).json.copyFrom((__ \ 'firstName).json.pick) orElse doNothing) and
        ((__ \ 'individual \ 'middleName).json.copyFrom((__ \ 'middleName).json.pick) orElse doNothing) and
        ((__ \ 'individual \ 'lastName).json.copyFrom((__ \ 'lastName).json.pick) orElse doNothing) and
        ((__ \ 'organisationOrPartnershipName).json.copyFrom((__ \ 'organizationOrPartnershipName).json.pick) orElse doNothing) and
        (__ \ 'relationshipStartDate).json.copyFrom((__ \ 'relationshipStartDate).json.pick) and
        (__ \ 'authorisingPSAID).json.copyFrom((__ \ 'authorisedPSAID).json.pick) and
        ((__ \ 'authorisingPSA \ 'firstName).json.copyFrom((__ \ 'authorisedPSAFirstName).json.pick) orElse doNothing) and
        ((__ \ 'authorisingPSA \ 'middleName).json.copyFrom((__ \ 'authorisedPSAMiddleName).json.pick) orElse doNothing) and
        ((__ \ 'authorisingPSA \ 'lastName).json.copyFrom((__ \ 'authorisedPSALastName).json.pick) orElse doNothing) and
        ((__ \ 'authorisingPSA \ 'organisationOrPartnershipName).json.copyFrom((__ \ 'authorisedPSAOrganizationOrPartnershipName).json.pick) orElse doNothing)
      ).reduce

    (__ \ basePath \ 'pspDetails).readNullable(
      __.read(Reads.seq(pspReads).map(JsArray(_))))
      .flatMap { psp =>
        (__ \ 'pspDetails).json.put(psp.getOrElse(JsArray())) orElse doNothing
      }
  }

  val userAnswersSchemeDetailsReads: Reads[JsObject] =
    getPsaIds and
    getPspDetails and
      (__ \ 'schemeName).json.copyFrom((__ \ basePath \ 'schemeDetails \ 'schemeName).json.pick) and
      (__ \ 'investmentRegulated).json.copyFrom((__ \ basePath \ 'schemeDetails \ 'isReguledSchemeInvestment).json.pick) and
      (__ \ 'occupationalPensionScheme).json.copyFrom((__ \ basePath \ 'schemeDetails \ 'isOccupationalPensionScheme).json.pick) and
      (__ \ 'schemeEstablishedCountry).json.copyFrom((__ \ basePath \ 'schemeDetails \ 'schemeEstablishedCountry).json.pick) and
      (__ \ 'securedBenefits).json.copyFrom((__ \ basePath \ 'schemeDetails \ 'isSchemeBenefitsInsuranceCompany).json.pick) and
      ((__ \ 'insuranceCompanyName).json.copyFrom((__ \ basePath \ 'schemeDetails \ 'insuranceCompanyName).json.pick)
        orElse doNothing) and
      ((__ \ 'insurancePolicyNumber).json.copyFrom((__ \ basePath \ 'schemeDetails \ 'policyNumber).json.pick)
        orElse doNothing) and
      (addressTransformer.getDifferentAddress(__ \ 'insurerAddress, __ \ basePath \ 'schemeDetails \ 'insuranceCompanyAddressDetails)
        orElse doNothing) and
      membersReads(desPath = "currentSchemeMembers", uaPath = "membership") and
      membersReads(desPath = "futureSchemeMembers", uaPath = "membershipFuture") and
      benefitsReads and
      schemeTypeReads and
      (__ \ 'schemeStatus).json.copyFrom((__ \ basePath \ 'schemeDetails \ 'schemeStatus).json.pick) and
      ((__ \ 'pstr).json.copyFrom((__ \ basePath \ 'schemeDetails \ 'pstr).json.pick)
        orElse doNothing) and
      (__ \ 'isAboutBenefitsAndInsuranceComplete).json.put(JsBoolean(true)) and
      (__ \ 'isAboutMembersComplete).json.put(JsBoolean(true)) and
      (__ \ 'isBeforeYouStartComplete).json.put(JsBoolean(true)) and
      ((__ \ "moreThanTenTrustees").json.copyFrom((__ \ basePath \ 'schemeDetails \ 'hasMoreThanTenTrustees).json.pick)
        orElse doNothing) reduce
}
