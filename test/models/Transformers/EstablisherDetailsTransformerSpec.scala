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

package models.Transformers

import base.JsonFileReader
import models.jsonTransformations.{AddressTransformer, EstablisherDetailsTransformer}
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._
import utils.PensionSchemeJsValueGenerators


class EstablisherDetailsTransformerSpec extends WordSpec with MustMatchers with OptionValues with JsonFileReader with PensionSchemeJsValueGenerators {

  private val addressTransformer = new AddressTransformer()
  private val transformer = new EstablisherDetailsTransformer(addressTransformer)

  "A DES payload containing establisher details" must {
    "have the individual details transformed correctly to valid user answers format for first json file" that {

      s"has person details in establishers array" in {
        forAll(individualJsValueGen) {
          individualDetails => {
            val details = individualDetails._1
            val result = details.transform(transformer.userAnswersIndividualDetailsReads).get
            (result \ "establisherDetails" \ "firstName").as[String] mustBe (details \ "personDetails" \ "firstName").as[String]
            (result \ "establisherDetails" \ "middleName").asOpt[String] mustBe (details \ "personDetails" \ "middleName").asOpt[String]
            (result \ "establisherDetails" \ "lastName").as[String] mustBe (details \ "personDetails" \ "lastName").as[String]
            (result \ "establisherDetails" \ "date").as[String] mustBe (details \ "personDetails" \ "dateOfBirth").as[String]
          }
        }
      }

      s"has nino details in establishers array" in {
        forAll(individualJsValueGen) {
          individualDetails => {
            val details = individualDetails._1
            val result = details.transform(transformer.userAnswersNinoReads).get

            (result \ "establisherNino" \ "hasNino").as[Boolean] mustBe true
            (result \ "establisherNino" \ "nino").asOpt[String] mustBe (details \ "nino").asOpt[String]

            val noNinoJs = details.as[JsObject] - "nino" + ("noNinoReason" -> JsString("test reason"))
            val noNinoResult = noNinoJs.transform(transformer.userAnswersNinoReads).get

            (noNinoResult \ "establisherNino" \ "hasNino").as[Boolean] mustBe false
            (noNinoResult \ "establisherNino" \ "reason").asOpt[String] mustBe (noNinoJs \ "noNinoReason").asOpt[String]
          }
        }
      }

      s"has utr details in establishers array" in {
        forAll(individualJsValueGen) {
          individualDetails => {
            val details = individualDetails._1
            val result = details.transform(transformer.userAnswersUtrReads("uniqueTaxReference")).get

            (result \ "uniqueTaxReference" \ "hasUtr").as[Boolean] mustBe true
            (result \ "uniqueTaxReference" \ "utr").asOpt[String] mustBe (details \ "utr").asOpt[String]

            val noUtrJs = details.as[JsObject] - "utr" + ("noUtrReason" -> JsString("test reason"))
            val noUtrJsResult = noUtrJs.transform(transformer.userAnswersUtrReads("uniqueTaxReference")).get

            (noUtrJsResult \ "uniqueTaxReference" \ "hasUtr").as[Boolean] mustBe false
            (noUtrJsResult \ "uniqueTaxReference" \ "reason").asOpt[String] mustBe (noUtrJs \ "noUtrReason").asOpt[String]

          }
        }
      }

      s"has contact details in establishers array" in {
        forAll(individualJsValueGen) {
          individualDetails => {
            val details = individualDetails._1
            val result = details.transform(transformer.userAnswersContactDetailsReads("contactDetails")).get

            (result \ "contactDetails" \ "emailAddress").as[String] mustBe (details \ "correspondenceContactDetails" \ "email").as[String]
            (result \ "contactDetails" \ "phoneNumber").as[String] mustBe (details \ "correspondenceContactDetails" \ "telephone").as[String]
          }
        }
      }

      "has complete individual details" in {
        forAll(individualJsValueGen) {
          individualDetails => {
            val (desIndividualDetails, userAnswersIndividualDetails) = individualDetails
            val result = desIndividualDetails.transform(transformer.userAnswersEstablisherIndividualReads).get

            result mustBe userAnswersIndividualDetails
          }
        }
      }
    }

    "have the companyOrOrganisationDetails details for company transformed correctly to valid user answers format for first json file" that {
      s"has establisher details in establishers array" in {
        forAll(companyJsValueGen) {
          companyDetails => {
            val details = companyDetails._1
            val result = details.transform(transformer.userAnswersCompanyDetailsReads).get
            (result \ "companyDetails" \ "companyName").as[String] mustBe (details \ "organisationName").as[String]
            (result \ "companyDetails" \ "vatNumber").asOpt[String] mustBe (details \ "vatRegistrationNumber").asOpt[String]
            (result \ "companyDetails" \ "payeNumber").asOpt[String] mustBe (details \ "payeReference").asOpt[String]
          }
        }
      }

      s"has crn details in establishers array" in {
        forAll(companyJsValueGen) {
          companyDetails => {
            val details = companyDetails._1
            val result = details.transform(transformer.userAnswersCrnReads).get
            (result \ "companyRegistrationNumber" \ "hasCrn").as[Boolean] mustBe true
            (result \ "companyRegistrationNumber" \ "crn").asOpt[String] mustBe (details \ "crnNumber").asOpt[String]

            val noCrn = details.as[JsObject] - "crnNumber" + ("noCrnReason" -> JsString("no crn"))
            val noCrnResult = noCrn.transform(transformer.userAnswersCrnReads).get
            (noCrnResult \ "companyRegistrationNumber" \ "hasCrn").as[Boolean] mustBe false
            (noCrnResult \ "companyRegistrationNumber" \ "reason").as[String] mustBe "no crn"
          }
        }
      }

      s"has utr details in establishers array" in {
        forAll(companyJsValueGen) {
          companyDetails => {
            val details = companyDetails._1
            val result = details.transform(transformer.userAnswersUtrReads("companyUniqueTaxReference")).get
            (result \ "companyUniqueTaxReference" \ "hasUtr").as[Boolean] mustBe true
            (result \ "companyUniqueTaxReference" \ "utr").asOpt[String] mustBe (details \ "utr").asOpt[String]
          }
        }
      }

      s"has contact details in establishers array" in {
        forAll(companyJsValueGen) {
          companyDetails => {
            val details = companyDetails._1
            val result = details.transform(transformer.userAnswersContactDetailsReads("companyContactDetails")).get
            (result \ "companyContactDetails" \ "emailAddress").as[String] mustBe
              (details \ "correspondenceContactDetails" \ "email").as[String]
            (result \ "companyContactDetails" \ "phoneNumber").as[String] mustBe
              (details \ "correspondenceContactDetails" \ "telephone").as[String]
          }
        }
      }

      s"has complete company details in establishers array" in {
        forAll(companyJsValueGen) {
          companyDetails => {
            val (desCompanyDetails, userAnswersCompanyDetails) = companyDetails
            val result = desCompanyDetails.transform(transformer.userAnswersEstablisherCompanyReads).get

            result mustBe userAnswersCompanyDetails
          }
        }
      }
    }

    "have the establisherPartnershipDetailsType details for partnership transformed correctly to valid user answers format for first json file" that {

      s"has establisher details in establishers array" in {
        forAll(partnershipJsValueGen) {
          partnershipDetails => {
            val details = partnershipDetails._1
            val result = details.transform(transformer.userAnswersPartnershipDetailsReads).get
            (result \ "partnershipDetails" \ "name").as[String] mustBe (details \ "partnershipName").as[String]
          }
        }
      }

      s"has vat details for partnership in establishers array" in {
        forAll(partnershipJsValueGen) {
          partnershipDetails => {
            val details = partnershipDetails._1 - "vatRegistrationNumber"
            val result = details.transform(transformer.transformVatToUserAnswersReads).get
            (result \ "partnershipVat" \ "hasVat").as[Boolean] mustBe false
          }
        }
      }

      s"has paye details for partnership in establishers array" in {
        forAll(partnershipJsValueGen) {
          partnershipDetails => {
            val details = partnershipDetails._1
            val result = details.transform(transformer.userAnswersPayeReads).get
            (result \ "partnershipPaye" \ "hasPaye").as[Boolean] mustBe true
            (result \ "partnershipPaye" \ "paye").asOpt[String] mustBe (details \ "payeReference").asOpt[String]
          }
        }
      }

      s"has utr details in establishers array" in {
        forAll(partnershipJsValueGen) {
          partnershipDetails => {
            val details = partnershipDetails._1
            val result = details.transform(transformer.userAnswersUtrReads("partnershipUniqueTaxReference")).get
            (result \ "partnershipUniqueTaxReference" \ "hasUtr").as[Boolean] mustBe true
            (result \ "partnershipUniqueTaxReference" \ "utr").asOpt[String] mustBe (details \ "utr").asOpt[String]
          }
        }
      }

      s"has contact details in establishers array" in {
        forAll(partnershipJsValueGen) {
          partnershipDetails => {
            val details = partnershipDetails._1
            val result = details.transform(transformer.userAnswersContactDetailsReads("partnershipContactDetails")).get
            (result \ "partnershipContactDetails" \ "emailAddress").as[String] mustBe
              (details \ "correspondenceContactDetails" \ "email").as[String]
            (result \ "partnershipContactDetails" \ "phoneNumber").as[String] mustBe
              (details \ "correspondenceContactDetails" \ "telephone").as[String]
          }
        }
      }

      s"has complete partnership details in establishers array" in {
        forAll(partnershipJsValueGen) {
          partnershipDetails => {
            val (desPartnershipDetails, userAnswersPartnershipDetails) = partnershipDetails
            val result = desPartnershipDetails.transform(transformer.userAnswersEstablisherPartnershipReads).get

            result mustBe userAnswersPartnershipDetails
          }
        }
      }
    }

    "have all establishers transformed" in {
      forAll(establisherJsValueGen) {
        establishers =>
          val result = establishers._1.transform(transformer.userAnswersEstablishersReads).get

          result mustBe establishers._2
      }
    }

    "if no establishers are present" in {

      val result = Json.obj().transform(transformer.userAnswersEstablishersReads).get

      result mustBe Json.obj(
        "establishers" -> JsArray()
      )
    }
  }
}