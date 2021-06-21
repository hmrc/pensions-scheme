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

package models.etmpToUserAnswers.psaSchemeDetails

import com.google.inject.Inject
import models.etmpToUserAnswers.AddressTransformer
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

class EstablisherDetailsTransformer @Inject()(addressTransformer: AddressTransformer,
                                              directorsOrPartnersTransformer: DirectorsOrPartnersTransformer) extends JsonTransformer {


  val userAnswersEstablishersReads: Reads[JsObject] = {
    (__ \ 'psaPspSchemeDetails \ 'establisherDetails).readNullable(__.read(
      (__ \ 'individualDetails).readNullable(
        __.read(Reads.seq(userAnswersEstablisherIndividualReads(__))).map(JsArray(_))).flatMap { individual =>
        (__ \ 'companyOrOrganisationDetails).readNullable(
          __.read(Reads.seq(userAnswersEstablisherCompanyReads(__))).map(JsArray(_))).flatMap { company =>
          (__ \ 'partnershipEstablisherDetails).readNullable(
            __.read(Reads.seq(userAnswersEstablisherPartnershipReads(__))).map(JsArray(_))).flatMap { partnership =>
            (__ \ 'establishers).json.put(individual.getOrElse(JsArray()) ++ company.getOrElse(JsArray()) ++ partnership.getOrElse(JsArray())) orElse doNothing
          }
        }
      })).map {
      _.getOrElse(Json.obj())
    }
  }

  def userAnswersEstablisherIndividualReads(ifPath: JsPath): Reads[JsObject] = {
    (__ \ 'establisherKind).json.put(JsString("individual")) and
      userAnswersIndividualDetailsReads("establisherDetails", ifPath) and
      userAnswersNinoReads("establisherNino", ifPath) and
      userAnswersUtrReads(ifPath) and
      addressTransformer.getDifferentAddress(__ \ 'address, ifPath \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears(ifPath, __ \ 'addressYears) and
      addressTransformer.getPreviousAddress(ifPath, __ \ 'previousAddress) and
      userAnswersContactDetailsReads("contactDetails", ifPath) and
      (__ \ 'isEstablisherComplete).json.put(JsBoolean(true)) reduce
  }

  def userAnswersEstablisherCompanyReads(ifPath: JsPath): Reads[JsObject] =
    (__ \ 'establisherKind).json.put(JsString("company")) and
      userAnswersCompanyDetailsReads(ifPath) and
      transformVatToUserAnswersReads(ifPath, "companyVat") and
      userAnswersPayeReads(ifPath, "companyPaye") and
      userAnswersCrnReads(ifPath) and
      userAnswersUtrReads(ifPath) and
      addressTransformer.getDifferentAddress(__ \ 'companyAddress, ifPath \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears(ifPath, __ \ 'companyAddressYears) and
      addressTransformer.getPreviousAddress(ifPath, __ \ 'companyPreviousAddress) and
      userAnswersContactDetailsReads("companyContactDetails", ifPath) and
      ((__ \ 'otherDirectors).json.copyFrom((ifPath \ 'haveMoreThanTenDirectors).json.pick) orElse doNothing) and
      getDirector(ifPath) reduce

  def getDirector(ifPath: JsPath): Reads[JsObject] = (ifPath \ 'directorsDetails).readNullable(
    __.read(Reads.seq(directorsOrPartnersTransformer.userAnswersDirectorReads(__))).map(JsArray(_))).flatMap { directors =>
    directors.map(allDirectors => (__ \ 'director).json.put(allDirectors)).getOrElse(doNothing)
  }

  def userAnswersEstablisherPartnershipReads(ifPath: JsPath): Reads[JsObject] =
    (__ \ 'establisherKind).json.put(JsString("partnership")) and
      userAnswersPartnershipDetailsReads(ifPath) and
      transformVatToUserAnswersReads(ifPath, "partnershipVat") and
      userAnswersPayeReads(ifPath, "partnershipPaye") and
      userAnswersUtrReads(ifPath) and
      addressTransformer.getDifferentAddress(__ \ 'partnershipAddress, ifPath \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears(ifPath, __ \ 'partnershipAddressYears) and
      addressTransformer.getPreviousAddress(ifPath, __ \ 'partnershipPreviousAddress) and
      userAnswersContactDetailsReads("partnershipContactDetails", ifPath) and
      (__ \ 'otherPartners).json.copyFrom((ifPath \ 'areMorethanTenPartners).json.pick) and
      (__ \ 'isEstablisherComplete).json.put(JsBoolean(true)) and
      getPartner(ifPath) reduce

  def getPartner(ifPath: JsPath): Reads[JsObject] = (ifPath \ 'partnerDetails).read(
    __.read(Reads.seq(directorsOrPartnersTransformer.userAnswersPartnerReads(__))).map(JsArray(_))).flatMap(
    partners => (__ \ 'partner).json.put(partners)
  )
}
