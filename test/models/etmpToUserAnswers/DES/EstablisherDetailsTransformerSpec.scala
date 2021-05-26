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

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.libs.json._

class EstablisherDetailsTransformerSpec extends TransformationSpec {

  import EstablisherDetailsTransformerSpec._

  private val addressTransformer = new AddressTransformer
  private val directorOrPartnerTransformer = new DirectorsOrPartnersTransformer(addressTransformer)
  private val transformer = new EstablisherDetailsTransformer(addressTransformer, directorOrPartnerTransformer)

  "A DES payload containing establisher details" must {
    "have the individual details transformed correctly to valid user answers format" that {

      val desEstablisherIndividualPath = __ \ 'psaSchemeDetails \ 'establisherDetails \ 'individualDetails

      def individualValuePath(details: JsObject): JsLookupResult = details \ "psaSchemeDetails" \ "establisherDetails" \ "individualDetails"

      s"has person details in establishers array" in {
        forAll(individualJsValueGen(isEstablisher = true)) {
          individualDetails => {
            val details = desIndividualJson(individualDetails._1)
            val result = details.transform(transformer.userAnswersIndividualDetailsReads("establisherDetails", desEstablisherIndividualPath)).get

            (result \ "establisherDetails" \ "firstName").as[String] mustBe (individualValuePath(details) \ "personDetails" \ "firstName").as[String]
            (result \ "establisherDetails" \ "lastName").as[String] mustBe (individualValuePath(details) \ "personDetails" \ "lastName").as[String]
            (result \ "dateOfBirth").as[String] mustBe (individualValuePath(details) \ "personDetails" \ "dateOfBirth").as[String]
          }
        }
      }

      s"has nino details in establishers array" in {
        forAll(individualJsValueGen(isEstablisher = true)) {
          individualDetails => {
            val details = desIndividualJson(individualDetails._1)
            val result = details.transform(transformer.userAnswersNinoReads("establisherNino", desEstablisherIndividualPath)).get

            (result \ "establisherNino" \ "value").asOpt[String] mustBe (individualValuePath(details) \ "nino").asOpt[String]
            (result \ "noNinoReason").asOpt[String] mustBe (individualValuePath(details) \ "noNinoReason").asOpt[String]
          }
        }
      }

      s"has utr details in establishers array" in {
        forAll(individualJsValueGen(isEstablisher = true)) {
          individualDetails => {
            val details = desIndividualJson(individualDetails._1)
            val result = details.transform(transformer.userAnswersUtrReads(desEstablisherIndividualPath)).get

            (result \ "utr" \ "value").asOpt[String] mustBe (individualValuePath(details) \ "utr").asOpt[String]
            (result \ "noUtrReason").asOpt[String] mustBe (individualValuePath(details) \ "noUtrReason").asOpt[String]
          }
        }
      }

      s"has contact details in establishers array" in {
        forAll(individualJsValueGen(isEstablisher = true)) {
          individualDetails => {
            val details = desIndividualJson(individualDetails._1)
            val result = details.transform(transformer.userAnswersContactDetailsReads("contactDetails", desEstablisherIndividualPath)).get

            (result \ "contactDetails" \ "emailAddress").as[String] mustBe (individualValuePath(details) \ "correspondenceContactDetails" \ "email").as[String]
            (result \ "contactDetails" \ "phoneNumber").as[String] mustBe
              (individualValuePath(details) \ "correspondenceContactDetails" \ "telephone").as[String]
          }
        }
      }

      "has complete individual details" in {
        forAll(individualJsValueGen(isEstablisher = true)) {
          individualDetails => {
            val (desIndividualDetails, userAnswersIndividualDetails) = individualDetails
            val details = desIndividualJson(desIndividualDetails)

            val result = details.transform(transformer.userAnswersEstablisherIndividualReads(desEstablisherIndividualPath)).get
            result mustBe userAnswersIndividualDetails
          }
        }
      }
    }

    "have the companyOrOrganisationDetails details for company transformed correctly to valid user answers format for first json file" that {
      val desCompanyPath = __ \ 'psaSchemeDetails \ 'establisherDetails \ 'companyOrOrganisationDetails

      def companyValuePath(details: JsObject): JsLookupResult = details \ "psaSchemeDetails" \ "establisherDetails" \ "companyOrOrganisationDetails"

      s"has establisher details in establishers array" in {
        forAll(companyJsValueGen(isEstablisher = true)) {
          companyDetails => {
            val details = desCompanyJson(companyDetails._1)
            val result = details.transform(transformer.userAnswersCompanyDetailsReads(desCompanyPath)).get

            (result \ "companyDetails" \ "companyName").as[String] mustBe (companyValuePath(details) \ "organisationName").as[String]
          }
        }
      }

      s"has vat details for company in establishers array" in {
        forAll(companyJsValueGen(isEstablisher = true)) {
          companyDetails => {
            val details = desCompanyJson(companyDetails._1)
            val result = details.transform(transformer.transformVatToUserAnswersReads(desCompanyPath, "companyVat")).get

            (result \ "companyVat" \ "value").asOpt[String] mustBe (companyValuePath(details) \ "vatRegistrationNumber").asOpt[String]
          }
        }
      }

      s"has paye details for company in establishers array" in {
        forAll(companyJsValueGen(isEstablisher = true)) {
          companyDetails => {
            val details = desCompanyJson(companyDetails._1)
            val result = details.transform(transformer.userAnswersPayeReads(desCompanyPath, "companyPaye")).get

            (result \ "companyPaye" \ "value").asOpt[String] mustBe (companyValuePath(details) \ "payeReference").asOpt[String]
          }
        }
      }

      s"has crn details in establishers array" in {
        forAll(companyJsValueGen(isEstablisher = true)) {
          companyDetails => {
            val details = desCompanyJson(companyDetails._1)
            val result = details.transform(transformer.userAnswersCrnReads(desCompanyPath)).get

            (result \ "noCrnReason").asOpt[String] mustBe (companyValuePath(details) \ "noCrnReason").asOpt[String]
            (result \ "companyRegistrationNumber" \ "value").asOpt[String] mustBe (companyValuePath(details) \ "crnNumber").asOpt[String]
          }
        }
      }

      s"has utr details in establishers array" in {
        forAll(companyJsValueGen(isEstablisher = true)) {
          companyDetails => {
            val details = desCompanyJson(companyDetails._1)
            val result = details.transform(transformer.userAnswersUtrReads(desCompanyPath)).get

            (result \ "utr" \ "value").asOpt[String] mustBe (companyValuePath(details) \ "utr").asOpt[String]
            (result \ "noUtrReason").asOpt[String] mustBe (companyValuePath(details) \ "noUtrReason").asOpt[String]
          }
        }
      }

      s"has contact details in establishers array" in {
        forAll(companyJsValueGen(isEstablisher = true)) {
          companyDetails => {
            val details = desCompanyJson(companyDetails._1)
            val result = details.transform(transformer.userAnswersContactDetailsReads("companyContactDetails", desCompanyPath)).get

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
            val (desCompanyDetails, userAnswersCompanyDetails) = companyDetails
            val details = desCompanyJson(desCompanyDetails)
            val result = details.transform(transformer.userAnswersEstablisherCompanyReads(desCompanyPath)).get

            result mustBe userAnswersCompanyDetails
          }
        }
      }
    }

    "have the establisherPartnershipDetailsType details for partnership transformed correctly to valid user answers format for first json file" that {

      val desPartnershipPath = __ \ 'psaSchemeDetails \ 'establisherDetails \ 'partnershipTrusteeDetail

      def partnershipValuePath(details: JsObject): JsLookupResult = details \ "psaSchemeDetails" \ "establisherDetails" \ "partnershipTrusteeDetail"

      s"has establisher details in establishers array" in {
        forAll(partnershipJsValueGen(isEstablisher = true)) {
          partnershipDetails => {
            val details = desPartnershipJson(partnershipDetails._1)
            val result = details.transform(transformer.userAnswersPartnershipDetailsReads(desPartnershipPath)).get

            (result \ "partnershipDetails" \ "name").as[String] mustBe (partnershipValuePath(details) \ "partnershipName").as[String]
          }
        }
      }

      s"has vat details for partnership in establishers array" in {
        forAll(partnershipJsValueGen(isEstablisher = true)) {
          partnershipDetails => {
            val details = desPartnershipJson(partnershipDetails._1)
            val result = details.transform(transformer.transformVatToUserAnswersReads(desPartnershipPath, "partnershipVat")).get

            (result \ "partnershipVat" \ "value").asOpt[String] mustBe (partnershipValuePath(details) \ "vatRegistrationNumber").asOpt[String]
          }
        }
      }

      s"has paye details for partnership in establishers array" in {
        forAll(partnershipJsValueGen(isEstablisher = true)) {
          partnershipDetails => {
            val details = desPartnershipJson(partnershipDetails._1)
            val result = details.transform(transformer.userAnswersPayeReads(desPartnershipPath, "partnershipPaye")).get

            (result \ "partnershipPaye" \ "value").asOpt[String] mustBe (partnershipValuePath(details) \ "payeReference").asOpt[String]
          }
        }
      }

      s"has utr details in establishers array" in {
        forAll(partnershipJsValueGen(isEstablisher = true)) {
          partnershipDetails => {
            val details = desPartnershipJson(partnershipDetails._1)
            val result = details.transform(transformer.userAnswersUtrReads(desPartnershipPath)).get

            (result \ "utr" \ "value").asOpt[String] mustBe (partnershipValuePath(details) \ "utr").asOpt[String]
            (result \ "noUtrReason").asOpt[String] mustBe (partnershipValuePath(details) \ "noUtrReason").asOpt[String]
          }
        }
      }

      s"has contact details in establishers array" in {
        forAll(partnershipJsValueGen(isEstablisher = true)) {
          partnershipDetails => {
            val details = desPartnershipJson(partnershipDetails._1)
            val result = details.transform(transformer.userAnswersContactDetailsReads("partnershipContactDetails", desPartnershipPath)).get

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
            val (desPartnershipDetails, userAnswersPartnershipDetails) = partnershipDetails

            val details = desPartnershipJson(desPartnershipDetails)
            val result = details.transform(transformer.userAnswersEstablisherPartnershipReads(desPartnershipPath)).get

            result mustBe userAnswersPartnershipDetails
          }
        }
      }
    }

    "have all establishers transformed" in {
      forAll(establisherOrTrusteeJsValueGen(isEstablisher = true)) {
        establishers =>
          val (desEstablishers, uaEstablishers) = establishers
          val result = desEstablishers.transform(transformer.userAnswersEstablishersReads).get
          result mustBe uaEstablishers
      }
    }

    "if no establishers are present" in {
      val result = Json.obj("psaSchemeDetails" -> "").transform(transformer.userAnswersEstablishersReads).get

      result mustBe Json.obj()
    }
  }
}

object EstablisherDetailsTransformerSpec {

  private def desIndividualJson(individualDetails: JsValue) = {
    Json.obj(
      "psaSchemeDetails" -> Json.obj(
        "establisherDetails" -> Json.obj(
          "individualDetails" -> individualDetails
        )
      )
    )
  }

  private def desCompanyJson(companyDetails: JsValue) = {
    Json.obj(
      "psaSchemeDetails" -> Json.obj(
        "establisherDetails" -> Json.obj(
          "companyOrOrganisationDetails" -> companyDetails
        )
      )
    )
  }

  private def desPartnershipJson(partnershipDetails: JsValue) = {
    Json.obj(
      "psaSchemeDetails" -> Json.obj(
        "establisherDetails" -> Json.obj(
          "partnershipTrusteeDetail" -> partnershipDetails
        )
      )
    )
  }
}
