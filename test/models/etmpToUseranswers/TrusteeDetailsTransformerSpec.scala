/*
 * Copyright 2020 HM Revenue & Customs
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

package models.etmpToUseranswers

import base.JsonFileReader
import models.etmpToUserAnswers.{AddressTransformer, TrusteeDetailsTransformer}
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._
import utils.PensionSchemeJsValueGenerators

class TrusteeDetailsTransformerSpec extends WordSpec with MustMatchers with OptionValues with JsonFileReader with PensionSchemeJsValueGenerators {

  import TrusteeDetailsTransformerSpec._

  private val addressTransformer = new AddressTransformer
  private def transformer = new TrusteeDetailsTransformer(addressTransformer)

  "A DES payload containing trustee details" must {
    "have the individual details transformed correctly to valid user answers format" that {
      val desTrusteeIndividualPath = __ \ 'psaSchemeDetails \ 'trusteeDetails \ 'individualTrusteeDetails

      def individualValuePath(details: JsObject): JsLookupResult = details \ "psaSchemeDetails" \ "trusteeDetails" \ "individualTrusteeDetails"

      s"has person details in trustee array" in {
        forAll(individualJsValueGen(isEstablisher = false)) {
          individualDetails => {
            val details = desIndividualJson(individualDetails._1)
            val result = details.transform(transformer.userAnswersIndividualDetailsReads("trusteeDetails", desTrusteeIndividualPath)).get
            (result \ "trusteeDetails" \ "firstName").as[String] mustBe (individualValuePath(details) \ "personDetails" \ "firstName").as[String]
            (result \ "trusteeDetails" \ "lastName").as[String] mustBe (individualValuePath(details) \ "personDetails" \ "lastName").as[String]
            (result \ "dateOfBirth").as[String] mustBe (individualValuePath(details) \ "personDetails" \ "dateOfBirth").as[String]
          }
        }
      }

      s"has nino details in trustees array" in {
          forAll(individualJsValueGen(isEstablisher = false)) {
            individualDetails => {
              val details = desIndividualJson(individualDetails._1)
              val result = details.transform(transformer.userAnswersNinoReads("trusteeNino", desTrusteeIndividualPath)).get

              (result \ "trusteeNino" \ "value").asOpt[String] mustBe (individualValuePath(details) \ "nino").asOpt[String]
              (result \ "noNinoReason").asOpt[String] mustBe (individualValuePath(details) \ "noNinoReason").asOpt[String]
            }
          }
      }

      s"has utr details in trustees array" in {
          forAll(individualJsValueGen(isEstablisher = false)) {
            individualDetails => {
              val details = desIndividualJson(individualDetails._1)
              val result = details.transform(transformer.
                userAnswersUtrReads(desTrusteeIndividualPath)).get

              (result \ "utr" \ "value").asOpt[String] mustBe (individualValuePath(details) \ "utr").asOpt[String]
              (result \ "noUtrReason").asOpt[String] mustBe (individualValuePath(details) \ "noUtrReason").asOpt[String]

            }
          }
      }

      s"has contact details in trustees array" in {
        forAll(individualJsValueGen(isEstablisher = false)) {
          individualDetails => {
            val details = desIndividualJson(individualDetails._1)
            val result = details.transform(transformer.userAnswersContactDetailsReads("trusteeContactDetails", desTrusteeIndividualPath)).get

            (result \ "trusteeContactDetails" \ "emailAddress").as[String] mustBe
              (individualValuePath(details) \ "correspondenceContactDetails" \ "email").as[String]
            (result \ "trusteeContactDetails" \ "phoneNumber").as[String] mustBe
              (individualValuePath(details) \ "correspondenceContactDetails" \ "telephone").as[String]
          }
        }
      }

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

      def companyValuePath(details: JsObject): JsLookupResult = details \ "psaSchemeDetails" \ "trusteeDetails" \ "companyTrusteeDetails"

      s"has trustee details in trustees array" in {
        forAll(companyJsValueGen(isEstablisher = false)) {
          companyDetails => {
            val details = desCompanyPath(companyDetails._1)
            val result = details.transform(transformer.userAnswersCompanyDetailsReads(desTrusteeCompanyPath)).get
            (result \ "companyDetails" \ "companyName").as[String] mustBe (companyValuePath(details) \ "organisationName").as[String]
          }
        }
      }

      s"has vat details for company in trustees array" in {
          forAll(companyJsValueGen(isEstablisher = true)) {
            companyDetails => {
              val details = desCompanyPath(companyDetails._1)
              val result = details.transform(transformer.transformVatToUserAnswersReads(desTrusteeCompanyPath, "companyVat")).get

              (result \ "companyVat" \ "value").asOpt[String] mustBe (companyValuePath(details) \ "vatRegistrationNumber").asOpt[String]
            }
          }
      }

      s"has paye details for company in trustees array" in {
          forAll(companyJsValueGen(isEstablisher = true)) {
            companyDetails => {
              val details = desCompanyPath(companyDetails._1)
              val result = details.transform(transformer.userAnswersPayeReads(desTrusteeCompanyPath, "companyPaye")).get

              (result \ "companyPaye" \ "value").asOpt[String] mustBe (companyValuePath(details) \ "payeReference").asOpt[String]
            }
          }

      }

      s"has crn details in trustees array" in {
          forAll(companyJsValueGen(isEstablisher = false)) {
            companyDetails => {
              val details = desCompanyPath(companyDetails._1)
              val result = details.transform(transformer.userAnswersCrnReads(desTrusteeCompanyPath)).get

              (result \ "noCrnReason").asOpt[String] mustBe (companyValuePath(details) \ "noCrnReason").asOpt[String]
              (result \ "companyRegistrationNumber" \ "value").asOpt[String] mustBe (companyValuePath(details) \ "crnNumber").asOpt[String]
            }
          }
      }

      s"has utr details in trustees array" in {
        forAll(companyJsValueGen(isEstablisher = false)) {
          companyDetails => {
            val details = desCompanyPath(companyDetails._1)
            val result = details.transform(transformer.userAnswersUtrReads(desTrusteeCompanyPath)).get

            (result \ "utr" \ "value").asOpt[String] mustBe (companyValuePath(details) \ "utr").asOpt[String]
            (result \ "noUtrReason").asOpt[String] mustBe (companyValuePath(details) \ "noUtrReason").asOpt[String]
          }
        }
      }

      s"has contact details in trustees array" in {
        forAll(companyJsValueGen(isEstablisher = false)) {
          companyDetails => {
            val details = desCompanyPath(companyDetails._1)
            val result = details.transform(transformer.userAnswersContactDetailsReads("companyContactDetails", desTrusteeCompanyPath)).get
            (result \ "companyContactDetails" \ "emailAddress").as[String] mustBe
              (companyValuePath(details) \ "correspondenceContactDetails" \ "email").as[String]
            (result \ "companyContactDetails" \ "phoneNumber").as[String] mustBe
              (companyValuePath(details) \ "correspondenceContactDetails" \ "telephone").as[String]
          }
        }
      }

      s"has complete company details in trustees array" in {
        forAll(companyJsValueGen(isEstablisher = false)) {
          companyDetails => {
            val (desCompanyDetails, userAnswersCompanyDetails) = companyDetails
            val desCompanyTrusteeDetails = desCompanyPath(desCompanyDetails)
            val result = desCompanyTrusteeDetails.transform(transformer.userAnswersTrusteeCompanyReads(desTrusteeCompanyPath)).get

            result mustBe userAnswersCompanyDetails
          }
        }
      }
    }

    "have the trusteePartnershipDetailsType details for partnership transformed correctly to valid user answers format for first json file" that {

      val desTrusteePartnershipPath = __ \ 'psaSchemeDetails \ 'trusteeDetails \ 'partnershipTrusteeDetails

      def partnershipValuePath(details: JsObject): JsLookupResult = details \ "psaSchemeDetails" \ "trusteeDetails" \ "partnershipTrusteeDetails"

      s"has trustee details in trustees array" in {
        forAll(partnershipJsValueGen(isEstablisher = false)) {
          partnershipDetails => {
            val details = desPartnershipPath(partnershipDetails._1)
            val result = details.transform(transformer.userAnswersPartnershipDetailsReads(desTrusteePartnershipPath)).get

            (result \ "partnershipDetails" \ "name").as[String] mustBe (partnershipValuePath(details) \ "partnershipName").as[String]
          }
        }
      }

      s"has vat details for partnership in trustees array" in {
          forAll(partnershipJsValueGen(isEstablisher = false)) {
            partnershipDetails => {
              val details = desPartnershipPath(partnershipDetails._1)

              val result = details.transform(transformer.transformVatToUserAnswersReads(desTrusteePartnershipPath, "partnershipVat")).get

              (result \ "partnershipVat" \ "value").asOpt[String] mustBe (partnershipValuePath(details) \ "vatRegistrationNumber").asOpt[String]
            }
          }
      }

      s"has paye details for partnership in trustees array" in {
          forAll(partnershipJsValueGen(isEstablisher = false)) {
            partnershipDetails => {
              val details = desPartnershipPath(partnershipDetails._1)
              val result = details.transform(transformer.userAnswersPayeReads(desTrusteePartnershipPath, "partnershipPaye")).get

              (result \ "partnershipPaye" \ "value").asOpt[String] mustBe (partnershipValuePath(details) \ "payeReference").asOpt[String]
            }
          }
      }

      s"has utr details in trustees array" in {
        forAll(partnershipJsValueGen(isEstablisher = false)) {
          partnershipDetails => {
            val details = desPartnershipPath(partnershipDetails._1)
            val result = details.transform(transformer.userAnswersUtrReads(desTrusteePartnershipPath)).get

            (result \ "utr" \ "value").asOpt[String] mustBe (partnershipValuePath(details) \ "utr").asOpt[String]
            (result \ "noUtrReason").asOpt[String] mustBe (partnershipValuePath(details) \ "noUtrReason").asOpt[String]
          }
        }
      }

      s"has contact details in trustees array" in {
        forAll(partnershipJsValueGen(isEstablisher = false)) {
          partnershipDetails => {
            val details = desPartnershipPath(partnershipDetails._1)
            val result = details.transform(transformer.userAnswersContactDetailsReads("partnershipContactDetails", desTrusteePartnershipPath)).get
            (result \ "partnershipContactDetails" \ "emailAddress").as[String] mustBe
              (partnershipValuePath(details) \ "correspondenceContactDetails" \ "email").as[String]
            (result \ "partnershipContactDetails" \ "phoneNumber").as[String] mustBe
              (partnershipValuePath(details) \ "correspondenceContactDetails" \ "telephone").as[String]
          }
        }
      }

      s"has complete partnership details in trustees array" in {
        forAll(partnershipJsValueGen(isEstablisher = false)) {
          partnershipDetails => {
            val (desPartnershipDetails, userAnswersPartnershipDetails) = partnershipDetails
            val desPartnershipTrusteeDetails = desPartnershipPath(desPartnershipDetails)
            val result = desPartnershipTrusteeDetails.transform(transformer.userAnswersTrusteePartnershipReads(desTrusteePartnershipPath)).get

            result mustBe userAnswersPartnershipDetails
          }
        }
      }
    }

    "have all trustees transformed" in {
      forAll(establisherOrTrusteeJsValueGen(isEstablisher = false)) {
        trustees =>
          val (desTrustees, uaTrustees) = trustees
          val result = desTrustees.transform(transformer.userAnswersTrusteesReads).get
          result mustBe uaTrustees
      }
    }

    "if no trustees are present" in {
      val result = Json.obj("psaSchemeDetails" -> "").transform(transformer.userAnswersTrusteesReads).get
      result mustBe Json.obj()
    }
  }
}

object TrusteeDetailsTransformerSpec {
  private def desIndividualJson(individualDetails: JsValue) = {
    Json.obj(
      "psaSchemeDetails" -> Json.obj(
        "trusteeDetails" -> Json.obj(
          "individualTrusteeDetails" -> individualDetails
        )
      )
    )
  }

  private def desCompanyPath(companyDetails: JsValue) = {
    Json.obj(
      "psaSchemeDetails" -> Json.obj(
        "trusteeDetails" -> Json.obj(
          "companyTrusteeDetails" -> companyDetails
        )
      )
    )
  }

  private def desPartnershipPath(partnershipDetails: JsValue) = {
    Json.obj(
      "psaSchemeDetails" -> Json.obj(
        "trusteeDetails" -> Json.obj(
          "partnershipTrusteeDetails" -> partnershipDetails
        )
      )
    )
  }
}