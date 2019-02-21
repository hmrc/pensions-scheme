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
import models.jsonTransformations.{AddressTransformer, DirectorsOrPartnersTransformer, EstablisherDetailsTransformer}
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._
import utils.PensionSchemeJsValueGenerators


class EstablisherDetailsTransformerSpec extends WordSpec with MustMatchers with OptionValues with JsonFileReader with PensionSchemeJsValueGenerators {

  private val addressTransformer = new AddressTransformer()
  private val directorOrPartnerTransformer = new DirectorsOrPartnersTransformer(addressTransformer)
  private val transformer = new EstablisherDetailsTransformer(addressTransformer, directorOrPartnerTransformer)

  "A DES payload containing establisher details" must {
    "have the individual details transformed correctly to valid user answers format" that {

      val desEstablisherIndividualPath = __ \ 'psaSchemeDetails \ 'establisherDetails \ 'individualDetails

      /*s"has person details in establishers array" in {
        forAll(individualJsValueGen(isEstablisher = true)) {
          individualDetails => {
            val details = individualDetails._1
            val result = details.transform(transformer.userAnswersIndividualDetailsReads("establisherDetails")).get

            (result \ "establisherDetails" \ "firstName").as[String] mustBe (details \ "personDetails" \ "firstName").as[String]
            (result \ "establisherDetails" \ "middleName").asOpt[String] mustBe (details \ "personDetails" \ "middleName").asOpt[String]
            (result \ "establisherDetails" \ "lastName").as[String] mustBe (details \ "personDetails" \ "lastName").as[String]
            (result \ "establisherDetails" \ "date").as[String] mustBe (details \ "personDetails" \ "dateOfBirth").as[String]
          }
        }
      }

      s"has nino details in establishers array" in {
        forAll(individualJsValueGen(isEstablisher = true)) {
          individualDetails => {
            val details = individualDetails._1
            val result = details.transform(transformer.userAnswersNinoReads("establisherNino", desEstablisherIndividualPath)).get

            (result \ "establisherNino" \ "hasNino").as[Boolean] mustBe (details \ "nino").isDefined
            (result \ "establisherNino" \ "nino").asOpt[String] mustBe (details \ "nino").asOpt[String]
            (result \ "establisherNino" \ "reason").asOpt[String] mustBe (details \ "noNinoReason").asOpt[String]
          }
        }
      }

      s"has utr details in establishers array" in {
        forAll(individualJsValueGen(isEstablisher = true)) {
          individualDetails => {
            val details = individualDetails._1
            val result = details.transform(transformer.userAnswersUtrReads("uniqueTaxReference", desEstablisherIndividualPath)).get

            (result \ "uniqueTaxReference" \ "hasUtr").as[Boolean] mustBe (details \ "utr").isDefined
            (result \ "uniqueTaxReference" \ "utr").asOpt[String] mustBe (details \ "utr").asOpt[String]
            (result \ "uniqueTaxReference" \ "reason").asOpt[String] mustBe (details \ "noUtrReason").asOpt[String]

          }
        }
      }

      s"has contact details in establishers array" in {
        forAll(individualJsValueGen(isEstablisher = true)) {
          individualDetails => {
            val details = individualDetails._1
            val result = details.transform(transformer.userAnswersContactDetailsReads("contactDetails", desEstablisherIndividualPath)).get

            (result \ "contactDetails" \ "emailAddress").as[String] mustBe (details \ "correspondenceContactDetails" \ "email").as[String]
            (result \ "contactDetails" \ "phoneNumber").as[String] mustBe (details \ "correspondenceContactDetails" \ "telephone").as[String]
          }
        }
      }*/

      "has complete individual details" in {
        forAll(individualJsValueGen(isEstablisher = true)) {
          individualDetails => {
            val (desIndividualDetails, userAnswersIndividualDetails) = individualDetails
            val desIndvEstDetails = Json.obj(
              "psaSchemeDetails" -> Json.obj(
                "establisherDetails" -> Json.obj(
                  "individualDetails" -> desIndividualDetails
                )
              )
            )

            val result = desIndvEstDetails.transform(transformer.userAnswersEstablisherIndividualReads(desEstablisherIndividualPath)).get
            result mustBe userAnswersIndividualDetails
          }
        }
      }
    }

    "have the companyOrOrganisationDetails details for company transformed correctly to valid user answers format for first json file" that {
      val desCompanyPath = __ \ 'psaSchemeDetails \ 'establisherDetails \ 'companyOrOrganisationDetails
      /*s"has establisher details in establishers array" in {
        forAll(companyJsValueGen(isEstablisher = true)) {
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
        forAll(companyJsValueGen(isEstablisher = true)) {
          companyDetails => {
            val details = companyDetails._1
            val result = details.transform(transformer.userAnswersCrnReads).get

            (result \ "companyRegistrationNumber" \ "hasCrn").as[Boolean] mustBe (details \ "crnNumber").isDefined
            (result \ "companyRegistrationNumber" \ "crn").asOpt[String] mustBe (details \ "crnNumber").asOpt[String]
            (result \ "companyRegistrationNumber" \ "reason").asOpt[String] mustBe (details \ "noCrnReason").asOpt[String]
          }
        }
      }

      s"has utr details in establishers array" in {
        forAll(companyJsValueGen(isEstablisher = true)) {
          companyDetails => {
            val details = companyDetails._1
            val result = details.transform(transformer.userAnswersUtrReads("companyUniqueTaxReference")).get

            (result \ "companyUniqueTaxReference" \ "hasUtr").as[Boolean] mustBe (details \ "utr").isDefined
            (result \ "companyUniqueTaxReference" \ "utr").asOpt[String] mustBe (details \ "utr").asOpt[String]
            (result \ "companyUniqueTaxReference" \ "reason").asOpt[String] mustBe (details \ "noUtrReason").asOpt[String]
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
      }*/

      s"has complete company details in establishers array" in {
        forAll(companyJsValueGen(isEstablisher = true)) {
          companyDetails => {
            val (desCompanyDetails, userAnswersCompanyDetails) = companyDetails
            val desCompanyEstDetails = Json.obj(
              "psaSchemeDetails" -> Json.obj(
                "establisherDetails" -> Json.obj(
                  "companyOrOrganisationDetails" -> desCompanyDetails
                )
              )
            )
            val result = desCompanyEstDetails.transform(transformer.userAnswersEstablisherCompanyReads(desCompanyPath)).get

            result mustBe userAnswersCompanyDetails
          }
        }
      }
    }

    "have the establisherPartnershipDetailsType details for partnership transformed correctly to valid user answers format for first json file" that {

      val desPartnershipPath = __ \ 'psaSchemeDetails \ 'establisherDetails \ 'partnershipTrusteeDetail
      /*s"has establisher details in establishers array" in {
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
            val result = details.transform(transformer.transformVatToUserAnswersReads).get

            (result \ "partnershipVat" \ "hasVat").as[Boolean] mustBe (details \ "vatRegistrationNumber").isDefined
            (result \ "partnershipVat" \ "vat").asOpt[String] mustBe (details \ "vatRegistrationNumber").asOpt[String]
          }
        }
      }

      s"has paye details for partnership in establishers array" in {
        forAll(partnershipJsValueGen(isEstablisher = true)) {
          partnershipDetails => {
            val details = partnershipDetails._1
            val result = details.transform(transformer.userAnswersPayeReads).get

            (result \ "partnershipPaye" \ "hasPaye").as[Boolean] mustBe (details \ "payeReference").isDefined
            (result \ "partnershipPaye" \ "paye").asOpt[String] mustBe (details \ "payeReference").asOpt[String]
          }
        }
      }

      s"has utr details in establishers array" in {
        forAll(partnershipJsValueGen(isEstablisher = true)) {
          partnershipDetails => {
            val details = partnershipDetails._1
            val result = details.transform(transformer.userAnswersUtrReads("partnershipUniqueTaxReference")).get

            (result \ "partnershipUniqueTaxReference" \ "hasUtr").as[Boolean] mustBe (details \ "utr").isDefined
            (result \ "partnershipUniqueTaxReference" \ "utr").asOpt[String] mustBe (details \ "utr").asOpt[String]
            (result \ "partnershipUniqueTaxReference" \ "reason").asOpt[String] mustBe (details \ "noUtrReason").asOpt[String]
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
      }*/

      s"has complete partnership details in establishers array" in {
        forAll(partnershipJsValueGen(isEstablisher = true)) {
          partnershipDetails => {
            val (desPartnershipDetails, userAnswersPartnershipDetails) = partnershipDetails

            val desPartnershipEstDetails = Json.obj(
              "psaSchemeDetails" -> Json.obj(
                "establisherDetails" -> Json.obj(
                  "partnershipTrusteeDetail" -> desPartnershipDetails
                )
              )
            )
            val result = desPartnershipEstDetails.transform(transformer.userAnswersEstablisherPartnershipReads(desPartnershipPath)).get

            result mustBe userAnswersPartnershipDetails
          }
        }
      }
    }

    "have all establishers transformed" in {
      forAll(establisherOrTrusteeJsValueGen(isEstablisher = true)) {
        establishers =>
          val result = establishers._1.transform(transformer.userAnswersEstablishersReads).get
          result mustBe establishers._2
      }
    }

    "if no establishers are present" in {

      val result = Json.obj("psaSchemeDetails" -> "").transform(transformer.userAnswersEstablishersReads).get

      result mustBe Json.obj()
    }
  }
}
