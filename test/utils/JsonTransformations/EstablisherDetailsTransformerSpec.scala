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

package utils.JsonTransformations

import base.JsonFileReader
import models.jsonTransformations.AddressTransformer
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._
import utils.PensionSchemeGenerators


class EstablisherDetailsTransformerSpec extends WordSpec with MustMatchers with OptionValues with JsonFileReader with PensionSchemeGenerators {
  private val desResponse1: JsValue = readJsonFromFile("/data/validGetSchemeDetails1.json")
  private val desResponse2: JsValue = readJsonFromFile("/data/validGetSchemeDetails2.json")

  private val addressTransformer = new AddressTransformer()
  private val transformer = new EstablisherDetailsTransformer(addressTransformer)

  private val individual = (desResponse1 \ "psaSchemeDetails" \ "establisherDetails" \ "individualDetails" \ 0).as[JsValue]
  private val individual2 = (desResponse2 \ "psaSchemeDetails" \ "establisherDetails" \ "individualDetails" \ 0).as[JsValue]
  private val company = (desResponse1 \ "psaSchemeDetails" \ "establisherDetails" \ "companyOrOrganisationDetails" \ 0).as[JsValue]
  private val partnership = (desResponse1 \ "psaSchemeDetails" \ "establisherDetails" \ "partnershipTrusteeDetail" \ 0).as[JsValue]
  private val establishers: JsValue = (desResponse1 \ "psaSchemeDetails" \ "establisherDetails").as[JsValue]
  private val establishers2: JsValue = (desResponse2 \ "psaSchemeDetails" \ "establisherDetails").as[JsValue]


  "A DES payload containing establisher details" must {
    "have the individual details transformed correctly to valid user answers format for first json file" that {

      s"has person details in establishers array" in {
        forAll(individualJsValueGen) {
          individualDetails => {
            val result = individualDetails.transform(transformer.userAnswersIndividualDetailsReads).get
            (result \ "establisherDetails" \ "firstName").as[String] mustBe (individualDetails \ "personDetails" \ "firstName").as[String]
            (result \ "establisherDetails" \ "middleName").asOpt[String] mustBe (individualDetails \ "personDetails" \ "middleName").asOpt[String]
            (result \ "establisherDetails" \ "lastName").as[String] mustBe (individualDetails \ "personDetails" \ "lastName").as[String]
            (result \ "establisherDetails" \ "date").as[String] mustBe (individualDetails \ "personDetails" \ "dateOfBirth").as[String]
          }
        }
      }

      s"has nino details in establishers array" in {
        forAll(individualJsValueGen) {
          individualDetails => {
            val result = individualDetails.transform(transformer.userAnswersNinoReads).get

            (result \ "establisherNino" \ "hasNino").as[Boolean] mustBe true
            (result \ "establisherNino" \ "nino").asOpt[String] mustBe (individualDetails \ "nino").asOpt[String]
          }
        }
      }

      s"has utr details in establishers array" in {
        forAll(individualJsValueGen) {
          individualDetails => {
            val result = individualDetails.transform(transformer.userAnswersUtrReads("uniqueTaxReference")).get

            (result \ "uniqueTaxReference" \ "hasUtr").as[Boolean] mustBe true
            (result \ "uniqueTaxReference" \ "utr").asOpt[String] mustBe (individualDetails \ "utr").asOpt[String]

          }
        }
      }

      s"has contact details in establishers array" in {
        forAll(individualJsValueGen) {
          individualDetails => {
            val result = individualDetails.transform(transformer.userAnswersContactDetailsReads("contactDetails")).get

            (result \ "contactDetails" \ "emailAddress").as[String] mustBe (individualDetails \ "correspondenceContactDetails" \ "email").as[String]
            (result \ "contactDetails" \ "phoneNumber").as[String] mustBe
              (individualDetails \ "correspondenceContactDetails" \ "telephone").as[String]
          }
        }
      }

    }

    "have the individual details  transformed correctly to valid user answers format for second json file" that {

      s"has no nino details in establishers array" in {
        forAll(noNinoReasonJsValue) {
          ninoJsValue => {
            val result = ninoJsValue.transform(transformer.userAnswersNinoReads).get

            (result \ "establisherNino" \ "hasNino").as[Boolean] mustBe false
            (result \ "establisherNino" \ "reason").asOpt[String] mustBe (ninoJsValue \ "noNinoReason").asOpt[String]
          }
        }
      }

      s"has no utr details in establishers array" in {
        forAll(noUtrReasonJsValue) {
          utrJsValue => {
            val result = utrJsValue.transform(transformer.userAnswersUtrReads("uniqueTaxReference")).get

            (result \ "uniqueTaxReference" \ "hasUtr").as[Boolean] mustBe false
            (result \ "uniqueTaxReference" \ "reason").asOpt[String] mustBe (utrJsValue \ "noUtrReason").asOpt[String]
          }
        }
      }
    }


    "have the companyOrOrganisationDetails details for company transformed correctly to valid user answers format for first json file" that {
      s"has establisher details in establishers array" in {
        val result = company.transform(transformer.userAnswersCompanyDetailsReads).get
        (result \ "companyDetails" \ "companyName").as[String] mustBe (company \ "organisationName").as[String]
        (result \ "companyDetails" \ "vatNumber").asOpt[String] mustBe (company \ "vatRegistrationNumber").asOpt[String]
        (result \ "companyDetails" \ "payeNumber").asOpt[String] mustBe (company \ "payeReference").asOpt[String]
      }

      s"has crn details in establishers array" in {
        val result = company.transform(transformer.userAnswersCrnReads).get
        (result \ "companyRegistrationNumber" \ "hasCrn").as[Boolean] mustBe true
        (result \ "companyRegistrationNumber" \ "crn").asOpt[String] mustBe (company \ "crnNumber").asOpt[String]

      }

      s"has utr details in establishers array" in {
        val result = company.transform(transformer.userAnswersUtrReads("companyUniqueTaxReference")).get
        (result \ "companyUniqueTaxReference" \ "hasUtr").as[Boolean] mustBe true
        (result \ "companyUniqueTaxReference" \ "utr").asOpt[String] mustBe (company \ "utr").asOpt[String]

      }

      s"has contact details in establishers array" in {
        val result = company.transform(transformer.userAnswersContactDetailsReads("companyContactDetails")).get
        (result \ "companyContactDetails" \ "emailAddress").as[String] mustBe
          (company \ "correspondenceContactDetails" \ "email").as[String]
        (result \ "companyContactDetails" \ "phoneNumber").as[String] mustBe
          (company \ "correspondenceContactDetails" \ "telephone").as[String]
      }
    }

    "have the establisherPartnershipDetailsType details for partnership transformed correctly to valid user answers format for first json file" that {

      s"has establisher details in establishers array" in {
        val result = partnership.transform(transformer.userAnswersPartnershipDetailsReads).get
        (result \ "partnershipDetails" \ "name").as[String] mustBe (partnership \ "partnershipName").as[String]
      }

      s"has vat details for partnership in establishers array" in {
        val result = partnership.transform(transformer.transformVatToUserAnswersReads).get
        (result \ "partnershipVat" \ "hasVat").as[Boolean] mustBe false

      }

      s"has paye details for partnership in establishers array" in {
        val result = partnership.transform(transformer.userAnswersPayeReads).get
        (result \ "partnershipPaye" \ "hasPaye").as[Boolean] mustBe true
        (result \ "partnershipPaye" \ "paye").asOpt[String] mustBe (partnership \ "payeReference").asOpt[String]

      }

      s"has utr details in establishers array" in {
        val result = partnership.transform(transformer.userAnswersUtrReads("partnershipUniqueTaxReference")).get
        (result \ "partnershipUniqueTaxReference" \ "hasUtr").as[Boolean] mustBe true
        (result \ "partnershipUniqueTaxReference" \ "utr").asOpt[String] mustBe (partnership \ "utr").asOpt[String]

      }

      s"has contact details in establishers array" in {
        val result = partnership.transform(transformer.userAnswersContactDetailsReads("partnershipContactDetails")).get
        (result \ "partnershipContactDetails" \ "emailAddress").as[String] mustBe
          (partnership \ "correspondenceContactDetails" \ "email").as[String]
        (result \ "partnershipContactDetails" \ "phoneNumber").as[String] mustBe
          (partnership \ "correspondenceContactDetails" \ "telephone").as[String]
      }
    }

    "have an individual establisher transformed" in {

      val result = individual.transform(transformer.userAnswersEstablisherIndividualReads).get

      (result \ "establisherKind").as[String] mustBe "individual"
      (result \ "establisherDetails").isDefined mustBe true
      (result \ "establisherNino").isDefined mustBe true
      (result \ "uniqueTaxReference").isDefined mustBe true
      (result \ "address").isDefined mustBe true
      (result \ "addressYears").isDefined mustBe true
      (result \ "previousAddress").isDefined mustBe true
      (result \ "contactDetails").isDefined mustBe true
      (result \ "isEstablisherComplete").as[Boolean] mustBe true

    }

    "have an company establisher transformed" in {
      val result = company.transform(transformer.userAnswersEstablisherCompanyReads).get

      (result \ "establisherKind").as[String] mustBe "company"
      (result \ "companyDetails").isDefined mustBe true
      (result \ "companyRegistrationNumber").isDefined mustBe true
      (result \ "companyUniqueTaxReference").isDefined mustBe true
      (result \ "companyAddress").isDefined mustBe true
      (result \ "companyAddressYears").isDefined mustBe true
      (result \ "previousAddress").isDefined mustBe false
      (result \ "companyContactDetails").isDefined mustBe true
      (result \ "isCompanyComplete").as[Boolean] mustBe true
    }

    "have an partnership establisher transformed" in {
      val result = partnership.transform(transformer.userAnswersEstablisherPartnershipReads).get

      (result \ "establisherKind").as[String] mustBe "partnership"
      (result \ "partnershipDetails").isDefined mustBe true
      (result \ "partnershipVat").isDefined mustBe true
      (result \ "partnershipPaye").isDefined mustBe true
      (result \ "partnershipUniqueTaxReference").isDefined mustBe true
      (result \ "partnershipAddress").isDefined mustBe true
      (result \ "partnershipAddressYears").isDefined mustBe true
      (result \ "partnershipPreviousAddress").isDefined mustBe false
      (result \ "partnershipContactDetails").isDefined mustBe true
      (result \ "isPartnershipCompleteId").as[Boolean] mustBe true
    }

    "have all establisher transformed" in {
      val result = (transformer.userAnswersEstablishersReads(establishers) \ "establishers").as[JsArray]

      result.value.size mustBe 3
      (result(0) \ "establisherDetails" \ "firstName").as[String] mustBe
        (establishers \ "individualDetails" \ 0 \ "personDetails" \ "firstName").as[String]
      (result(1) \ "companyDetails" \ "companyName").as[String] mustBe
        (establishers \ "companyOrOrganisationDetails" \ 0 \ "organisationName").as[String]
      (result(2) \ "partnershipDetails" \ "name").as[String] mustBe
        (establishers \ "partnershipTrusteeDetail" \ 0 \ "partnershipName").as[String]

    }

    "if only inidividual and company establishers are present" in {
      val result = (transformer.userAnswersEstablishersReads(establishers2) \ "establishers").as[JsArray]

      result.value.size mustBe 2
    }

  }
}
