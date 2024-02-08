/*
 * Copyright 2024 HM Revenue & Customs
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

package models.etmpToUserAnswers

import models.etmpToUserAnswers.psaSchemeDetails.{EstablisherDetailsTransformer, DirectorsOrPartnersTransformer}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.libs.json._

class EstablisherDetailsTransformerSpec extends TransformationSpec {

  private val addressTransformer = new AddressTransformer
  private val directorOrPartnerTransformer = new DirectorsOrPartnersTransformer(addressTransformer)
  private val transformer = new EstablisherDetailsTransformer(addressTransformer, directorOrPartnerTransformer)

  "An if payload containing establisher details" must {
    "have the individual details transformed correctly to valid user answers format" that {

      s"has person details in establishers array" in {
        forAll(individualJsValueGen(isEstablisher = true)) {
          individualDetails => {
            val details = individualDetails._1
            val result = details.transform(transformer.userAnswersIndividualDetailsReads("establisherDetails")).get

            (result \ "establisherDetails" \ "firstName").as[String] mustBe (details \ "personDetails" \ "firstName").as[String]
            (result \ "establisherDetails" \ "lastName").as[String] mustBe (details \ "personDetails" \ "lastName").as[String]
            (result \ "dateOfBirth").as[String] mustBe (details \ "personDetails" \ "dateOfBirth").as[String]
          }
        }
      }

      s"has nino details in establishers array" in {
        forAll(individualJsValueGen(isEstablisher = true)) {
          individualDetails => {
            val details = individualDetails._1
            val result = details.transform(transformer.userAnswersNinoReads("establisherNino")).get

            (result \ "establisherNino" \ "value").asOpt[String] mustBe (details \ "nino").asOpt[String]
            (result \ "noNinoReason").asOpt[String] mustBe (details \ "noNinoReason").asOpt[String]
          }
        }
      }

      s"has utr details in establishers array" in {
        forAll(individualJsValueGen(isEstablisher = true)) {
          individualDetails => {
            val details = individualDetails._1
            val result = details.transform(transformer.userAnswersUtrReads).get

            (result \ "utr" \ "value").asOpt[String] mustBe (details \ "utr").asOpt[String]
            (result \ "noUtrReason").asOpt[String] mustBe (details \ "noUtrReason").asOpt[String]
          }
        }
      }

      s"has contact details in establishers array" in {
        forAll(individualJsValueGen(isEstablisher = true)) {
          individualDetails => {
            val details = individualDetails._1
            val result = details.transform(transformer.userAnswersContactDetailsReads("contactDetails")).get

            (result \ "contactDetails" \ "emailAddress").as[String] mustBe (details \ "correspondenceContactDetails" \ "email").as[String]
            (result \ "contactDetails" \ "phoneNumber").as[String] mustBe
              (details \ "correspondenceContactDetails" \ "telephone").as[String]
          }
        }
      }

      "has complete individual details" in {
        forAll(individualJsValueGen(isEstablisher = true)) {
          individualDetails => {
            val (ifIndividualDetails, userAnswersIndividualDetails) = individualDetails
            val details = ifIndividualDetails

            val result = details.transform(transformer.userAnswersEstablisherIndividualReads).get
            result mustBe userAnswersIndividualDetails
          }
        }
      }
    }

    "have the companyOrOrganisationDetails details for company transformed correctly to valid user answers format for first json file" that {

      s"has establisher details in establishers array" in {
        forAll(companyJsValueGen(isEstablisher = true)) {
          companyDetails => {
            val details = companyDetails._1
            val result = details.transform(transformer.userAnswersCompanyDetailsReads).get

            (result \ "companyDetails" \ "companyName").as[String] mustBe (details \ "organisationName").as[String]
          }
        }
      }

      s"has vat details for company in establishers array" in {
        forAll(companyJsValueGen(isEstablisher = true)) {
          companyDetails => {
            val details = companyDetails._1
            val result = details.transform(transformer.transformVatToUserAnswersReads("companyVat")).get

            (result \ "companyVat" \ "value").asOpt[String] mustBe (details \ "vatRegistrationNumber").asOpt[String]
          }
        }
      }

      s"has paye details for company in establishers array" in {
        forAll(companyJsValueGen(isEstablisher = true)) {
          companyDetails => {
            val details = companyDetails._1
            val result = details.transform(transformer.userAnswersPayeReads("companyPaye")).get

            (result \ "companyPaye" \ "value").asOpt[String] mustBe (details \ "payeReference").asOpt[String]
          }
        }
      }

      s"has crn details in establishers array" in {
        forAll(companyJsValueGen(isEstablisher = true)) {
          companyDetails => {
            val details = companyDetails._1
            val result = details.transform(transformer.userAnswersCrnReads).get

            (result \ "noCrnReason").asOpt[String] mustBe (details \ "noCrnReason").asOpt[String]
            (result \ "companyRegistrationNumber" \ "value").asOpt[String] mustBe (details \ "crnNumber").asOpt[String]
          }
        }
      }

      s"has utr details in establishers array" in {
        forAll(companyJsValueGen(isEstablisher = true)) {
          companyDetails => {
            val details = companyDetails._1
            val result = details.transform(transformer.userAnswersUtrReads).get

            (result \ "utr" \ "value").asOpt[String] mustBe (details \ "utr").asOpt[String]
            (result \ "noUtrReason").asOpt[String] mustBe (details \ "noUtrReason").asOpt[String]
          }
        }
      }

      s"has contact details in establishers array" in {
        forAll(companyJsValueGen(isEstablisher = true)) {
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
        forAll(companyJsValueGen(isEstablisher = true)) {
          companyDetails => {
            val (ifCompanyDetails, userAnswersCompanyDetails) = companyDetails
            val details = ifCompanyDetails
            val result = details.transform(transformer.userAnswersEstablisherCompanyReads).get

            result mustBe userAnswersCompanyDetails
          }
        }
      }
    }

    "have the establisherPartnershipDetailsType details for partnership transformed correctly to valid user answers format for first json file" that {

      s"has establisher details in establishers array" in {
        forAll(partnershipJsValueGen(isEstablisher = true)) {
          partnershipDetails => {
            val details = partnershipDetails._1
            val result = details.transform(transformer.userAnswersPartnershipDetailsReads).get

            (result \ "partnershipDetails" \ "name").as[String] mustBe (details \ "partnershipName").as[String]
          }
        }
      }

      s"has vat details for partnership in establishers array" in {
        forAll(partnershipJsValueGen(isEstablisher = true)) {
          partnershipDetails => {
            val details = partnershipDetails._1
            val result = details.transform(transformer.transformVatToUserAnswersReads("partnershipVat")).get

            (result \ "partnershipVat" \ "value").asOpt[String] mustBe (details \ "vatRegistrationNumber").asOpt[String]
          }
        }
      }

      s"has paye details for partnership in establishers array" in {
        forAll(partnershipJsValueGen(isEstablisher = true)) {
          partnershipDetails => {
            val details = partnershipDetails._1
            val result = details.transform(transformer.userAnswersPayeReads("partnershipPaye")).get

            (result \ "partnershipPaye" \ "value").asOpt[String] mustBe (details \ "payeReference").asOpt[String]
          }
        }
      }

      s"has utr details in establishers array" in {
        forAll(partnershipJsValueGen(isEstablisher = true)) {
          partnershipDetails => {
            val details = partnershipDetails._1
            val result = details.transform(transformer.userAnswersUtrReads).get

            (result \ "utr" \ "value").asOpt[String] mustBe (details \ "utr").asOpt[String]
            (result \ "noUtrReason").asOpt[String] mustBe (details \ "noUtrReason").asOpt[String]
          }
        }
      }

      s"has contact details in establishers array" in {
        forAll(partnershipJsValueGen(isEstablisher = true)) {
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
        forAll(partnershipJsValueGen(isEstablisher = true)) {
          partnershipDetails => {
            val (ifPartnershipDetails, userAnswersPartnershipDetails) = partnershipDetails

            val details = ifPartnershipDetails
            val result = details.transform(transformer.userAnswersEstablisherPartnershipReads).get

            result mustBe userAnswersPartnershipDetails
          }
        }
      }
    }

    "have all establishers transformed" in {
      forAll(establisherOrTrusteeJsValueGen(isEstablisher = true)) {
        establishers =>
          val (ifEstablishers, uaEstablishers) = establishers
          val result = ifEstablishers.transform(transformer.userAnswersEstablishersReads).get
          result mustBe uaEstablishers
      }
    }

    "if no establishers are present" in {
      val result = Json.obj("psaPspSchemeDetails" -> "").transform(transformer.userAnswersEstablishersReads).get

      result mustBe Json.obj()
   }
  }
}

