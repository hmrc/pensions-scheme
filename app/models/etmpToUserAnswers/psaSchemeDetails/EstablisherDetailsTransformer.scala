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
        __.read(Reads.seq(userAnswersEstablisherIndividualReads)).map(JsArray(_))).flatMap { individual =>
        (__ \ 'companyOrOrganisationDetails).readNullable(
          __.read(Reads.seq(userAnswersEstablisherCompanyReads)).map(JsArray(_))).flatMap { company =>
          (__ \ 'partnershipEstablisherDetails).readNullable(
            __.read(Reads.seq(userAnswersEstablisherPartnershipReads)).map(JsArray(_))).flatMap { partnership =>
            (__ \ 'establishers).json.put(individual.getOrElse(JsArray()) ++ company.getOrElse(JsArray()) ++ partnership.getOrElse(JsArray())) orElse doNothing
          }
        }
      })).map {
      _.getOrElse(Json.obj())
    }
  }

  def userAnswersEstablisherIndividualReads: Reads[JsObject] = {
    (__ \ 'establisherKind).json.put(JsString("individual")) and
      userAnswersIndividualDetailsReads("establisherDetails") and
      userAnswersNinoReads("establisherNino") and
      userAnswersUtrReads and
      addressTransformer.getDifferentAddress(__ \ 'address, __ \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears(__ \ 'addressYears) and
      addressTransformer.getPreviousAddress( __ \ 'previousAddress) and
      userAnswersContactDetailsReads("contactDetails") and
      (__ \ 'isEstablisherComplete).json.put(JsBoolean(true)) reduce
  }

  def userAnswersEstablisherCompanyReads: Reads[JsObject] =
    (__ \ 'establisherKind).json.put(JsString("company")) and
      userAnswersCompanyDetailsReads and
      transformVatToUserAnswersReads("companyVat") and
      userAnswersPayeReads("companyPaye") and
      userAnswersCrnReads and
      userAnswersUtrReads and
      addressTransformer.getDifferentAddress(__ \ 'companyAddress, __ \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears(__ \ 'companyAddressYears) and
      addressTransformer.getPreviousAddress(__ \ 'companyPreviousAddress) and
      userAnswersContactDetailsReads("companyContactDetails") and
      ((__ \ 'otherDirectors).json.copyFrom((__ \ 'haveMoreThanTenDirectors).json.pick) orElse doNothing) and
      getDirector reduce

  def getDirector: Reads[JsObject] = (__ \ 'directorsDetails).readNullable(
    __.read(Reads.seq(directorsOrPartnersTransformer.userAnswersDirectorReads)).map(JsArray(_))).flatMap { directors =>
    directors.map(allDirectors => (__ \ 'director).json.put(allDirectors)).getOrElse(doNothing)
  }

  def userAnswersEstablisherPartnershipReads: Reads[JsObject] =
    (__ \ 'establisherKind).json.put(JsString("partnership")) and
      userAnswersPartnershipDetailsReads and
      transformVatToUserAnswersReads("partnershipVat") and
      userAnswersPayeReads("partnershipPaye") and
      userAnswersUtrReads and
      addressTransformer.getDifferentAddress(__ \ 'partnershipAddress, __ \ 'correspondenceAddressDetails) and
      addressTransformer.getAddressYears(__ \ 'partnershipAddressYears) and
      addressTransformer.getPreviousAddress(__ \ 'partnershipPreviousAddress) and
      userAnswersContactDetailsReads("partnershipContactDetails") and
      (__ \ 'otherPartners).json.copyFrom((__ \ 'areMorethanTenPartners).json.pick) and
      (__ \ 'isEstablisherComplete).json.put(JsBoolean(true)) and
      getPartner reduce

  def getPartner: Reads[JsObject] = (__ \ 'partnerDetails).read(
    __.read(Reads.seq(directorsOrPartnersTransformer.userAnswersPartnerReads)).map(JsArray(_))).flatMap(
    partners => (__ \ 'partner).json.put(partners)
  )
}
