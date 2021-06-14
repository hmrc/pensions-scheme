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

package models.etmpToUserAnswers

import models.etmpToUserAnswers.psaSchemeDetails.{DirectorsOrPartnersTransformer, EstablisherDetailsTransformer}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.libs.json._

class EstablisherDetailsTransformerSpec extends TransformationSpec {

  import EstablisherDetailsTransformerSpec._

  private val addressTransformer = new AddressTransformer
  private val directorOrPartnerTransformer = new DirectorsOrPartnersTransformer(addressTransformer)
  private val transformer = new EstablisherDetailsTransformer(addressTransformer, directorOrPartnerTransformer)

  "An if payload containing establisher details" must {
    "have the individual details transformed correctly to valid user answers format" that {

      val ifEstablisherIndividualPath = __ \ 'psaPspSchemeDetails \ 'establisherDetails \ 'individualDetails

      def individualValuePath(details: JsObject): JsLookupResult = details \ "psaPspSchemeDetails" \ "establisherDetails" \ "individualDetails"

      s"has person details in establishers array" in {
        forAll(individualJsValueGen(isEstablisher = true)) {
          individualDetails => {
            val details = ifIndividualJson(individualDetails._1)
            val result = details.transform(transformer.userAnswersIndividualDetailsReads("establisherDetails", ifEstablisherIndividualPath)).get

            (result \ "establisherDetails" \ "firstName").as[String] mustBe (individualValuePath(details) \ "personDetails" \ "firstName").as[String]
            (result \ "establisherDetails" \ "lastName").as[String] mustBe (individualValuePath(details) \ "personDetails" \ "lastName").as[String]
            (result \ "dateOfBirth").as[String] mustBe (individualValuePath(details) \ "personDetails" \ "dateOfBirth").as[String]
          }
        }
      }

      s"has nino details in establishers array" in {
        forAll(individualJsValueGen(isEstablisher = true)) {
          individualDetails => {
            val details = ifIndividualJson(individualDetails._1)
            val result = details.transform(transformer.userAnswersNinoReads("establisherNino", ifEstablisherIndividualPath)).get

            (result \ "establisherNino" \ "value").asOpt[String] mustBe (individualValuePath(details) \ "nino").asOpt[String]
            (result \ "noNinoReason").asOpt[String] mustBe (individualValuePath(details) \ "noNinoReason").asOpt[String]
          }
        }
      }

      s"has utr details in establishers array" in {
        forAll(individualJsValueGen(isEstablisher = true)) {
          individualDetails => {
            val details = ifIndividualJson(individualDetails._1)
            val result = details.transform(transformer.userAnswersUtrReads(ifEstablisherIndividualPath)).get

            (result \ "utr" \ "value").asOpt[String] mustBe (individualValuePath(details) \ "utr").asOpt[String]
            (result \ "noUtrReason").asOpt[String] mustBe (individualValuePath(details) \ "noUtrReason").asOpt[String]
          }
        }
      }

      s"has contact details in establishers array" in {
        forAll(individualJsValueGen(isEstablisher = true)) {
          individualDetails => {
            val details = ifIndividualJson(individualDetails._1)
            val result = details.transform(transformer.userAnswersContactDetailsReads("contactDetails", ifEstablisherIndividualPath)).get

            (result \ "contactDetails" \ "emailAddress").as[String] mustBe (individualValuePath(details) \ "correspondenceContactDetails" \ "email").as[String]
            (result \ "contactDetails" \ "phoneNumber").as[String] mustBe
              (individualValuePath(details) \ "correspondenceContactDetails" \ "telephone").as[String]
          }
        }
      }

      "has complete individual details" in {
        forAll(individualJsValueGen(isEstablisher = true)) {
          individualDetails => {
            val (ifIndividualDetails, userAnswersIndividualDetails) = individualDetails
            val details = ifIndividualJson(ifIndividualDetails)

            val result = details.transform(transformer.userAnswersEstablisherIndividualReads(ifEstablisherIndividualPath)).get
            result mustBe userAnswersIndividualDetails
          }
        }
      }
    }

    "have the companyOrOrganisationDetails details for company transformed correctly to valid user answers format for first json file" that {
      val ifCompanyPath = __ \ 'psaPspSchemeDetails \ 'establisherDetails \ 'companyOrOrganisationDetails

      def companyValuePath(details: JsObject): JsLookupResult = details \ "psaPspSchemeDetails" \ "establisherDetails" \ "companyOrOrganisationDetails"

      s"has establisher details in establishers array" in {
        forAll(companyJsValueGen(isEstablisher = true)) {
          companyDetails => {
            val details = ifCompanyJson(companyDetails._1)
            val result = details.transform(transformer.userAnswersCompanyDetailsReads(ifCompanyPath)).get

            (result \ "companyDetails" \ "companyName").as[String] mustBe (companyValuePath(details) \ "organisationName").as[String]
          }
        }
      }

      s"has vat details for company in establishers array" in {
        forAll(companyJsValueGen(isEstablisher = true)) {
          companyDetails => {
            val details = ifCompanyJson(companyDetails._1)
            val result = details.transform(transformer.transformVatToUserAnswersReads(ifCompanyPath, "companyVat")).get

            (result \ "companyVat" \ "value").asOpt[String] mustBe (companyValuePath(details) \ "vatRegistrationNumber").asOpt[String]
          }
        }
      }

      s"has paye details for company in establishers array" in {
        forAll(companyJsValueGen(isEstablisher = true)) {
          companyDetails => {
            val details = ifCompanyJson(companyDetails._1)
            val result = details.transform(transformer.userAnswersPayeReads(ifCompanyPath, "companyPaye")).get

            (result \ "companyPaye" \ "value").asOpt[String] mustBe (companyValuePath(details) \ "payeReference").asOpt[String]
          }
        }
      }

      s"has crn details in establishers array" in {
        forAll(companyJsValueGen(isEstablisher = true)) {
          companyDetails => {
            val details = ifCompanyJson(companyDetails._1)
            val result = details.transform(transformer.userAnswersCrnReads(ifCompanyPath)).get

            (result \ "noCrnReason").asOpt[String] mustBe (companyValuePath(details) \ "noCrnReason").asOpt[String]
            (result \ "companyRegistrationNumber" \ "value").asOpt[String] mustBe (companyValuePath(details) \ "crnNumber").asOpt[String]
          }
        }
      }

      s"has utr details in establishers array" in {
        forAll(companyJsValueGen(isEstablisher = true)) {
          companyDetails => {
            val details = ifCompanyJson(companyDetails._1)
            val result = details.transform(transformer.userAnswersUtrReads(ifCompanyPath)).get

            (result \ "utr" \ "value").asOpt[String] mustBe (companyValuePath(details) \ "utr").asOpt[String]
            (result \ "noUtrReason").asOpt[String] mustBe (companyValuePath(details) \ "noUtrReason").asOpt[String]
          }
        }
      }

      s"has contact details in establishers array" in {
        forAll(companyJsValueGen(isEstablisher = true)) {
          companyDetails => {
            val details = ifCompanyJson(companyDetails._1)
            val result = details.transform(transformer.userAnswersContactDetailsReads("companyContactDetails", ifCompanyPath)).get

            (result \ "companyContactDetails" \ "emailAddress").as[String] mustBe
              (companyValuePath(details) \ "correspondenceContactDetails" \ "email").as[String]
            (result \ "companyContactDetails" \ "phoneNumber").as[String] mustBe
              (companyValuePath(details) \ "correspondenceContactDetails" \ "telephone").as[String]
          }
        }
      }

      s"has complete company details in establishers array" in {
        forAll(companyJsValueGen(isEstablisher = true)) {
          companyDetails => {
            val (ifCompanyDetails, userAnswersCompanyDetails) = companyDetails
            val details = ifCompanyJson(ifCompanyDetails)
            val result = details.transform(transformer.userAnswersEstablisherCompanyReads(ifCompanyPath)).get

            result mustBe userAnswersCompanyDetails
          }
        }
      }
    }

    "have the establisherPartnershipDetailsType details for partnership transformed correctly to valid user answers format for first json file" that {

      val ifPartnershipPath = __ \ 'psaPspSchemeDetails \ 'establisherDetails \ 'partnershipEstablisherDetails

      def partnershipValuePath(details: JsObject): JsLookupResult = details \ "psaPspSchemeDetails" \ "establisherDetails" \ "partnershipEstablisherDetails"

      s"has establisher details in establishers array" in {
        forAll(partnershipJsValueGen(isEstablisher = true)) {
          partnershipDetails => {
            val details = ifPartnershipJson(partnershipDetails._1)
            val result = details.transform(transformer.userAnswersPartnershipDetailsReads(ifPartnershipPath)).get

            (result \ "partnershipDetails" \ "name").as[String] mustBe (partnershipValuePath(details) \ "partnershipName").as[String]
          }
        }
      }

      s"has vat details for partnership in establishers array" in {
        forAll(partnershipJsValueGen(isEstablisher = true)) {
          partnershipDetails => {
            val details = ifPartnershipJson(partnershipDetails._1)
            val result = details.transform(transformer.transformVatToUserAnswersReads(ifPartnershipPath, "partnershipVat")).get

            (result \ "partnershipVat" \ "value").asOpt[String] mustBe (partnershipValuePath(details) \ "vatRegistrationNumber").asOpt[String]
          }
        }
      }

      s"has paye details for partnership in establishers array" in {
        forAll(partnershipJsValueGen(isEstablisher = true)) {
          partnershipDetails => {
            val details = ifPartnershipJson(partnershipDetails._1)
            val result = details.transform(transformer.userAnswersPayeReads(ifPartnershipPath, "partnershipPaye")).get

            (result \ "partnershipPaye" \ "value").asOpt[String] mustBe (partnershipValuePath(details) \ "payeReference").asOpt[String]
          }
        }
      }

      s"has utr details in establishers array" in {
        forAll(partnershipJsValueGen(isEstablisher = true)) {
          partnershipDetails => {
            val details = ifPartnershipJson(partnershipDetails._1)
            val result = details.transform(transformer.userAnswersUtrReads(ifPartnershipPath)).get

            (result \ "utr" \ "value").asOpt[String] mustBe (partnershipValuePath(details) \ "utr").asOpt[String]
            (result \ "noUtrReason").asOpt[String] mustBe (partnershipValuePath(details) \ "noUtrReason").asOpt[String]
          }
        }
      }

      s"has contact details in establishers array" in {
        forAll(partnershipJsValueGen(isEstablisher = true)) {
          partnershipDetails => {
            val details = ifPartnershipJson(partnershipDetails._1)
            val result = details.transform(transformer.userAnswersContactDetailsReads("partnershipContactDetails", ifPartnershipPath)).get

            (result \ "partnershipContactDetails" \ "emailAddress").as[String] mustBe
              (partnershipValuePath(details) \ "correspondenceContactDetails" \ "email").as[String]
            (result \ "partnershipContactDetails" \ "phoneNumber").as[String] mustBe
              (partnershipValuePath(details) \ "correspondenceContactDetails" \ "telephone").as[String]
          }
        }
      }

      s"has complete partnership details in establishers array" in {
        forAll(partnershipJsValueGen(isEstablisher = true)) {
          partnershipDetails => {
            val (ifPartnershipDetails, userAnswersPartnershipDetails) = partnershipDetails

            val details = ifPartnershipJson(ifPartnershipDetails)
            val result = details.transform(transformer.userAnswersEstablisherPartnershipReads(ifPartnershipPath)).get

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

object EstablisherDetailsTransformerSpec {

  private def ifIndividualJson(individualDetails: JsValue) = {
    Json.obj(
      "psaPspSchemeDetails" -> Json.obj(
        "establisherDetails" -> Json.obj(
          "individualDetails" -> individualDetails
        )
      )
    )
  }

  private def ifCompanyJson(companyDetails: JsValue) = {
    Json.obj(
      "psaPspSchemeDetails" -> Json.obj(
        "establisherDetails" -> Json.obj(
          "companyOrOrganisationDetails" -> companyDetails
        )
      )
    )
  }

  private def ifPartnershipJson(partnershipDetails: JsValue) = {
    Json.obj(
      "psaPspSchemeDetails" -> Json.obj(
        "establisherDetails" -> Json.obj(
          "partnershipEstablisherDetails" -> partnershipDetails
        )
      )
    )
  }
}
