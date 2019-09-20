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
import config.FeatureSwitchManagementService
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import utils.Toggles

class EstablisherDetailsTransformer @Inject()(addressTransformer: AddressTransformer,
                                              directorsOrPartnersTransformer: DirectorsOrPartnersTransformer,
                                              override val fs: FeatureSwitchManagementService) extends JsonTransformer {


  val userAnswersEstablishersReads: Reads[JsObject] = {
    (__ \ 'psaSchemeDetails \ 'establisherDetails).readNullable(__.read(
      (__ \ 'individualDetails).readNullable(
        __.read(Reads.seq(userAnswersEstablisherIndividualReads(__))).map(JsArray(_))).flatMap { individual =>
        (__ \ 'companyOrOrganisationDetails).readNullable(
          __.read(Reads.seq(userAnswersEstablisherCompanyReads(__))).map(JsArray(_))).flatMap { company =>
          (__ \ 'partnershipTrusteeDetail).readNullable(
            __.read(Reads.seq(userAnswersEstablisherPartnershipReads(__))).map(JsArray(_))).flatMap { partnership =>
            (__ \ 'establishers).json.put(individual.getOrElse(JsArray()) ++ company.getOrElse(JsArray()) ++ partnership.getOrElse(JsArray())) orElse doNothing
          }
        }
      })).map {
      _.getOrElse(Json.obj())
    }
  }

  def userAnswersEstablisherIndividualReads(desPath: JsPath): Reads[JsObject] = {
    (__ \ 'establisherKind).json.put(JsString("individual")) and
      (if(fs.get(Toggles.isHnSEnabled))
      userAnswersIndividualDetailsReadsHnS("establisherDetails", desPath)
        else
      userAnswersIndividualDetailsReads("establisherDetails", desPath)) and
      (if(fs.get(Toggles.isHnSEnabled))
        userAnswersNinoReadsHnS("establisherNino", desPath)
      else
        userAnswersNinoReads("establisherNino", desPath)) and
      (if(fs.get(Toggles.isHnSEnabled))
        userAnswersUtrReadsHnS("uniqueTaxReference", desPath)
      else
        userAnswersUtrReads("uniqueTaxReference", desPath)) and
      addressTransformer.getDifferentAddress(__ \ 'address, desPath \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears(desPath, __ \ 'addressYears) and
      addressTransformer.getPreviousAddress(desPath, __ \ 'previousAddress) and
      userAnswersContactDetailsReads("contactDetails", desPath) and
      (__ \ 'isEstablisherComplete).json.put(JsBoolean(true)) reduce
  }

  def userAnswersEstablisherCompanyReads(desPath: JsPath): Reads[JsObject] =
    (__ \ 'establisherKind).json.put(JsString("company")) and
      userAnswersCompanyDetailsReads(desPath) and
      transformVatToUserAnswersReadsHnS(desPath, "companyVat") and
      userAnswersPayeReadsHnS(desPath, "companyPaye") and
      userAnswersCrnReadsHnS(desPath) and
      userAnswersUtrReadsHnS("companyUniqueTaxReference", desPath) and
      addressTransformer.getDifferentAddress(__ \ 'companyAddress, desPath \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears(desPath, __ \ 'companyAddressYears) and
      addressTransformer.getPreviousAddress(desPath, __ \ 'companyPreviousAddress) and
      userAnswersContactDetailsReads("companyContactDetails", desPath) and
      ((__ \ 'otherDirectors).json.copyFrom((desPath \ 'haveMoreThanTenDirectors).json.pick) orElse doNothing) and
      getDirector(desPath) reduce

  def getDirector(desPath: JsPath): Reads[JsObject] = (desPath \ 'directorsDetails).readNullable(
    __.read(Reads.seq(directorsOrPartnersTransformer.userAnswersDirectorReads(__))).map(JsArray(_))).flatMap { directors =>
    directors.map(allDirectors => (__ \ 'director).json.put(allDirectors)).getOrElse(doNothing)
  }

  def userAnswersEstablisherPartnershipReads(desPath: JsPath): Reads[JsObject] =
    (__ \ 'establisherKind).json.put(JsString("partnership")) and
      userAnswersPartnershipDetailsReads(desPath) and
      (if(fs.get(Toggles.isHnSEnabled))
      transformVatToUserAnswersReadsHnS(desPath, "partnershipVat")
        else
      transformVatToUserAnswersReads(desPath, "partnershipVat")) and
        (if(fs.get(Toggles.isHnSEnabled))
      userAnswersPayeReadsHnS(desPath, "partnershipPaye")
        else
      userAnswersPayeReads(desPath, "partnershipPaye")) and
        (if(fs.get(Toggles.isHnSEnabled))
      userAnswersUtrReadsHnS("partnershipUniqueTaxReference", desPath)
        else
      userAnswersUtrReads("partnershipUniqueTaxReference", desPath)) and
      addressTransformer.getDifferentAddress(__ \ 'partnershipAddress, desPath \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears(desPath, __ \ 'partnershipAddressYears) and
      addressTransformer.getPreviousAddress(desPath, __ \ 'partnershipPreviousAddress) and
      userAnswersContactDetailsReads("partnershipContactDetails", desPath) and
      (__ \ 'otherPartners).json.copyFrom((desPath \ 'areMorethanTenPartners).json.pick) and
      (__ \ 'isEstablisherComplete).json.put(JsBoolean(true)) and
      getPartner(desPath) reduce

  def getPartner(desPath: JsPath): Reads[JsObject] = (desPath \ 'partnerDetails).read(
    __.read(Reads.seq(directorsOrPartnersTransformer.userAnswersPartnerReads(__))).map(JsArray(_))).flatMap(
    partners => (__ \ 'partner).json.put(partners)
  )
}
