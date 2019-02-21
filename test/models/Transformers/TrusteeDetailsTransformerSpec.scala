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
import models.jsonTransformations.{AddressTransformer, TrusteeDetailsTransformer}
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._
import utils.PensionSchemeJsValueGenerators


class TrusteeDetailsTransformerSpec extends WordSpec with MustMatchers with OptionValues with JsonFileReader with PensionSchemeJsValueGenerators {

  private val addressTransformer = new AddressTransformer()
  private val transformer = new TrusteeDetailsTransformer(addressTransformer)

  "A DES payload containing trustee details" must {
    "have the individual details transformed correctly to valid user answers format" that {
      val desTrusteeIndividualPath = __ \ 'psaSchemeDetails \ 'trusteeDetails \ 'individualTrusteeDetails
      /*s"has person details in trustee array" in {
        forAll(individualJsValueGen(isEstablisher = false)) {
          individualDetails => {
            val details = individualDetails._1
            val result = details.transform(transformer.userAnswersIndividualDetailsReads("trusteeDetails")).get
            (result \ "trusteeDetails" \ "firstName").as[String] mustBe (details \ "personDetails" \ "firstName").as[String]
            (result \ "trusteeDetails" \ "middleName").asOpt[String] mustBe (details \ "personDetails" \ "middleName").asOpt[String]
            (result \ "trusteeDetails" \ "lastName").as[String] mustBe (details \ "personDetails" \ "lastName").as[String]
            (result \ "trusteeDetails" \ "date").as[String] mustBe (details \ "personDetails" \ "dateOfBirth").as[String]
          }
        }
      }*/

     /* s"has nino details in trustees array" in {
        forAll(individualJsValueGen(isEstablisher = false)) {
          individualDetails => {
            val details = individualDetails._1
            val result = details.transform(transformer.userAnswersNinoReads("trusteeNino")).get

            (result \ "trusteeNino" \ "hasNino").as[Boolean] mustBe true
            (result \ "trusteeNino" \ "nino").asOpt[String] mustBe (details \ "nino").asOpt[String]

            val noNinoJs = details.as[JsObject] - "nino" + ("noNinoReason" -> JsString("test reason"))
            val noNinoResult = noNinoJs.transform(transformer.userAnswersNinoReads("trusteeNino")).get

            (noNinoResult \ "trusteeNino" \ "hasNino").as[Boolean] mustBe false
            (noNinoResult \ "trusteeNino" \ "reason").asOpt[String] mustBe (noNinoJs \ "noNinoReason").asOpt[String]
          }
        }
      }

      s"has utr details in trustees array" in {
        forAll(individualJsValueGen(isEstablisher = false)) {
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

      s"has contact details in trustees array" in {
        forAll(individualJsValueGen(isEstablisher = false)) {
          individualDetails => {
            val details = individualDetails._1
            val result = details.transform(transformer.userAnswersContactDetailsReads("trusteeContactDetails")).get

            (result \ "trusteeContactDetails" \ "emailAddress").as[String] mustBe (details \ "correspondenceContactDetails" \ "email").as[String]
            (result \ "trusteeContactDetails" \ "phoneNumber").as[String] mustBe (details \ "correspondenceContactDetails" \ "telephone").as[String]
          }
        }
      }*/

      "has complete individual details" in {
        forAll(individualJsValueGen(isEstablisher = false)) {
          individualDetails => {
            val (desIndividualDetails, userAnswersIndividualDetails) = individualDetails
            val desIndvTrusteeDetails = Json.obj(
              "psaSchemeDetails" -> Json.obj(
                "trusteeDetails" -> Json.obj(
                  "individualTrusteeDetails" -> desIndividualDetails
                )
              )
            )
            val result = desIndvTrusteeDetails.transform(transformer.userAnswersTrusteeIndividualReads(desTrusteeIndividualPath)).get

            result mustBe userAnswersIndividualDetails
          }
        }
      }
    }

    "have the companyOrOrganisationDetails details for company transformed correctly to valid user answers format for first json file" that {
      val desTrusteeCompanyPath = __ \ 'psaSchemeDetails \ 'trusteeDetails \ 'companyTrusteeDetails
      /*s"has trustee details in trustees array" in {
        forAll(companyJsValueGen(isEstablisher = false)) {
          companyDetails => {
            val details = companyDetails._1
            val result = details.transform(transformer.userAnswersCompanyDetailsReads).get
            (result \ "companyDetails" \ "companyName").as[String] mustBe (details \ "organisationName").as[String]
            (result \ "companyDetails" \ "vatNumber").asOpt[String] mustBe (details \ "vatRegistrationNumber").asOpt[String]
            (result \ "companyDetails" \ "payeNumber").asOpt[String] mustBe (details \ "payeReference").asOpt[String]
          }
        }
      }

      s"has crn details in trustees array" in {
        forAll(companyJsValueGen(isEstablisher = false)) {
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

      s"has utr details in trustees array" in {
        forAll(companyJsValueGen(isEstablisher = false)) {
          companyDetails => {
            val details = companyDetails._1
            val result = details.transform(transformer.userAnswersUtrReads("companyUniqueTaxReference")).get
            (result \ "companyUniqueTaxReference" \ "hasUtr").as[Boolean] mustBe true
            (result \ "companyUniqueTaxReference" \ "utr").asOpt[String] mustBe (details \ "utr").asOpt[String]
          }
        }
      }

      s"has contact details in trustees array" in {
        forAll(companyJsValueGen(isEstablisher = false)) {
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

      s"has complete company details in trustees array" in {
        forAll(companyJsValueGen(isEstablisher = false)) {
          companyDetails => {
            val (desCompanyDetails, userAnswersCompanyDetails) = companyDetails
            val desCompanyTrusteeDetails = Json.obj(
              "psaSchemeDetails" -> Json.obj(
                "trusteeDetails" -> Json.obj(
                  "companyTrusteeDetails" -> desCompanyDetails
                )
              )
            )
            val result = desCompanyTrusteeDetails.transform(transformer.userAnswersTrusteeCompanyReads(desTrusteeCompanyPath)).get

            result mustBe userAnswersCompanyDetails
          }
        }
      }
    }

    "have the trusteePartnershipDetailsType details for partnership transformed correctly to valid user answers format for first json file" that {
      val desTrusteePartnershipPath = __ \ 'psaSchemeDetails \ 'trusteeDetails \ 'partnershipTrusteeDetails
      /*s"has trustee details in trustees array" in {
        forAll(partnershipJsValueGen(isEstablisher = false)) {
          partnershipDetails => {
            val details = partnershipDetails._1
            val result = details.transform(transformer.userAnswersPartnershipDetailsReads).get
            (result \ "partnershipDetails" \ "name").as[String] mustBe (details \ "partnershipName").as[String]
          }
        }
      }

      s"has vat details for partnership in trustees array" in {
        forAll(partnershipJsValueGen(isEstablisher = false)) {
          partnershipDetails => {
            val details = partnershipDetails._1 - "vatRegistrationNumber"
            val result = details.transform(transformer.transformVatToUserAnswersReads).get
            (result \ "partnershipVat" \ "hasVat").as[Boolean] mustBe false
          }
        }
      }

      s"has paye details for partnership in trustees array" in {
        forAll(partnershipJsValueGen(isEstablisher = false)) {
          partnershipDetails => {
            val details = partnershipDetails._1
            val result = details.transform(transformer.userAnswersPayeReads).get
            (result \ "partnershipPaye" \ "hasPaye").as[Boolean] mustBe true
            (result \ "partnershipPaye" \ "paye").asOpt[String] mustBe (details \ "payeReference").asOpt[String]
          }
        }
      }

      s"has utr details in trustees array" in {
        forAll(partnershipJsValueGen(isEstablisher = false)) {
          partnershipDetails => {
            val details = partnershipDetails._1
            val result = details.transform(transformer.userAnswersUtrReads("partnershipUniqueTaxReference")).get
            (result \ "partnershipUniqueTaxReference" \ "hasUtr").as[Boolean] mustBe true
            (result \ "partnershipUniqueTaxReference" \ "utr").asOpt[String] mustBe (details \ "utr").asOpt[String]
          }
        }
      }

      s"has contact details in trustees array" in {
        forAll(partnershipJsValueGen(isEstablisher = false)) {
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

      s"has complete partnership details in trustees array" in {
        forAll(partnershipJsValueGen(isEstablisher = false)) {
          partnershipDetails => {
            val (desPartnershipDetails, userAnswersPartnershipDetails) = partnershipDetails
            val desPartnershipTrusteeDetails = Json.obj(
              "psaSchemeDetails" -> Json.obj(
                "trusteeDetails" -> Json.obj(
                  "partnershipTrusteeDetails" -> desPartnershipDetails
                )
              )
            )
            val result = desPartnershipTrusteeDetails.transform(transformer.userAnswersTrusteePartnershipReads(desTrusteePartnershipPath)).get

            result mustBe userAnswersPartnershipDetails
          }
        }
      }
    }

    "have all trustees transformed" in {
      forAll(establisherOrTrusteeJsValueGen(isEstablisher = false)) {
        trustees =>
          val result = trustees._1.transform(transformer.userAnswersTrusteesReads).get

          result mustBe trustees._2
      }
    }

    "if no trustees are present" in {

      val result = Json.obj("psaSchemeDetails" -> "").transform(transformer.userAnswersTrusteesReads).get

      result mustBe Json.obj()
    }
  }
}
