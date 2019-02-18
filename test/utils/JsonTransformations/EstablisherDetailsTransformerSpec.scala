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
import org.scalacheck.Gen
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
            (result \ "contactDetails" \ "phoneNumber").as[String] mustBe
              (details \ "correspondenceContactDetails" \ "telephone").as[String]
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
            val details = partnershipDetails._1
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
    }

    "have an individual establisher transformed" in {
      forAll(individualJsValueGen) {
        individualDetails => {
          val details = individualDetails._1
          val result = details.transform(transformer.userAnswersEstablisherIndividualReads).get

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
      }
    }

    "have an company establisher transformed" in {
      forAll(companyJsValueGen) {
        companyDetails => {
          val details = companyDetails._1
          val result = details.transform(transformer.userAnswersEstablisherCompanyReads).get

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
      }
    }

    "have an partnership establisher transformed" in {
      forAll(partnershipJsValueGen) {
        partnershipDetails => {
          val details = partnershipDetails._1
          val updatedPartnership = details + ("vatRegistrationNumber" -> JsString("123456789"))
          val result = updatedPartnership.transform(transformer.userAnswersEstablisherPartnershipReads).get

          (result \ "establisherKind").as[String] mustBe "partnership"
          (result \ "partnershipDetails").isDefined mustBe true
          (result \ "partnershipVat").isDefined mustBe true
          (result \ "partnershipPaye").isDefined mustBe true
          (result \ "partnershipUniqueTaxReference").isDefined mustBe true
          (result \ "partnershipAddress").isDefined mustBe true
          (result \ "partnershipAddressYears").isDefined mustBe true
          (result \ "partnershipPreviousAddress").isDefined mustBe true
          (result \ "partnershipContactDetails").isDefined mustBe true
          (result \ "isPartnershipCompleteId").as[Boolean] mustBe true
        }
      }
    }

    "have all establishers transformed" in {
      forAll(establisherJsValueGen) {
        establishers =>
        val result = transformer.userAnswersEstablishersReads(establishers._1)

        result mustBe establishers._2

        /*result.value.size mustBe 3
        (result(0) \ "establisherDetails" \ "firstName").as[String] mustBe
          (establishers \ "individualDetails" \ 0 \ "personDetails" \ "firstName").as[String]
        (result(1) \ "companyDetails" \ "companyName").as[String] mustBe
          (establishers \ "companyOrOrganisationDetails" \ 0 \ "organisationName").as[String]
        (result(2) \ "partnershipDetails" \ "name").as[String] mustBe
          (establishers \ "partnershipTrusteeDetail" \ 0 \ "partnershipName").as[String]*/
      }

    }

    "if only inidividual and company establishers are present" in {
      val result = (transformer.userAnswersEstablishersReads(establishers2) \ "establishers").as[JsArray]

      result.value.size mustBe 2
    }

  }
}
