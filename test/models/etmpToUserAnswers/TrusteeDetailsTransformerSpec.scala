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

import models.etmpToUserAnswers.psaSchemeDetails.TrusteeDetailsTransformer
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.libs.json._

class TrusteeDetailsTransformerSpec extends TransformationSpec {

  import TrusteeDetailsTransformerSpec._

  private val addressTransformer = new AddressTransformer
  private def transformer = new TrusteeDetailsTransformer(addressTransformer)

  "A DES payload containing trustee details" must {
    "have the individual details transformed correctly to valid user answers format" that {
      val ifTrusteeIndividualPath = __ \ 'psaPspSchemeDetails \ 'trusteeDetails \ 'individualTrusteeDetails

      def individualValuePath(details: JsObject): JsLookupResult = details \ "psaPspSchemeDetails" \ "trusteeDetails" \ "individualTrusteeDetails"

      s"has person details in trustee array" in {
        forAll(individualJsValueGen(isEstablisher = false)) {
          individualDetails => {
            val details = ifIndividualJson(individualDetails._1)
            val result = details.transform(transformer.userAnswersIndividualDetailsReads("trusteeDetails", ifTrusteeIndividualPath)).get
            (result \ "trusteeDetails" \ "firstName").as[String] mustBe (individualValuePath(details) \ "personDetails" \ "firstName").as[String]
            (result \ "trusteeDetails" \ "lastName").as[String] mustBe (individualValuePath(details) \ "personDetails" \ "lastName").as[String]
            (result \ "dateOfBirth").as[String] mustBe (individualValuePath(details) \ "personDetails" \ "dateOfBirth").as[String]
          }
        }
      }

      s"has nino details in trustees array" in {
          forAll(individualJsValueGen(isEstablisher = false)) {
            individualDetails => {
              val details = ifIndividualJson(individualDetails._1)
              val result = details.transform(transformer.userAnswersNinoReads("trusteeNino", ifTrusteeIndividualPath)).get

              (result \ "trusteeNino" \ "value").asOpt[String] mustBe (individualValuePath(details) \ "nino").asOpt[String]
              (result \ "noNinoReason").asOpt[String] mustBe (individualValuePath(details) \ "noNinoReason").asOpt[String]
            }
          }
      }

      s"has utr details in trustees array" in {
          forAll(individualJsValueGen(isEstablisher = false)) {
            individualDetails => {
              val details = ifIndividualJson(individualDetails._1)
              val result = details.transform(transformer.
                userAnswersUtrReads(ifTrusteeIndividualPath)).get

              (result \ "utr" \ "value").asOpt[String] mustBe (individualValuePath(details) \ "utr").asOpt[String]
              (result \ "noUtrReason").asOpt[String] mustBe (individualValuePath(details) \ "noUtrReason").asOpt[String]

            }
          }
      }

      s"has contact details in trustees array" in {
        forAll(individualJsValueGen(isEstablisher = false)) {
          individualDetails => {
            val details = ifIndividualJson(individualDetails._1)
            val result = details.transform(transformer.userAnswersContactDetailsReads("trusteeContactDetails", ifTrusteeIndividualPath)).get

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
              val (ifIndividualDetails, userAnswersIndividualDetails) = individualDetails
              val ifIndvTrusteeDetails = Json.obj(
                "psaPspSchemeDetails" -> Json.obj(
                  "trusteeDetails" -> Json.obj(
                    "individualTrusteeDetails" -> ifIndividualDetails
                  )
                )
              )
              val result = ifIndvTrusteeDetails.transform(transformer.userAnswersTrusteeIndividualReads(ifTrusteeIndividualPath)).get

              result mustBe userAnswersIndividualDetails
            }
          }
      }
    }

    "have the companyOrOrganisationDetails details for company transformed correctly to valid user answers format for first json file" that {
      val ifTrusteeCompanyPath = __ \ 'psaPspSchemeDetails \ 'trusteeDetails \ 'companyTrusteeDetails

      def companyValuePath(details: JsObject): JsLookupResult = details \ "psaPspSchemeDetails" \ "trusteeDetails" \ "companyTrusteeDetails"

      s"has trustee details in trustees array" in {
        forAll(companyJsValueGen(isEstablisher = false)) {
          companyDetails => {
            val details = ifCompanyPath(companyDetails._1)
            val result = details.transform(transformer.userAnswersCompanyDetailsReads(ifTrusteeCompanyPath)).get
            (result \ "companyDetails" \ "companyName").as[String] mustBe (companyValuePath(details) \ "organisationName").as[String]
          }
        }
      }

      s"has vat details for company in trustees array" in {
          forAll(companyJsValueGen(isEstablisher = true)) {
            companyDetails => {
              val details = ifCompanyPath(companyDetails._1)
              val result = details.transform(transformer.transformVatToUserAnswersReads(ifTrusteeCompanyPath, "companyVat")).get

              (result \ "companyVat" \ "value").asOpt[String] mustBe (companyValuePath(details) \ "vatRegistrationNumber").asOpt[String]
            }
          }
      }

      s"has paye details for company in trustees array" in {
          forAll(companyJsValueGen(isEstablisher = true)) {
            companyDetails => {
              val details = ifCompanyPath(companyDetails._1)
              val result = details.transform(transformer.userAnswersPayeReads(ifTrusteeCompanyPath, "companyPaye")).get

              (result \ "companyPaye" \ "value").asOpt[String] mustBe (companyValuePath(details) \ "payeReference").asOpt[String]
            }
          }

      }

      s"has crn details in trustees array" in {
          forAll(companyJsValueGen(isEstablisher = false)) {
            companyDetails => {
              val details = ifCompanyPath(companyDetails._1)
              val result = details.transform(transformer.userAnswersCrnReads(ifTrusteeCompanyPath)).get

              (result \ "noCrnReason").asOpt[String] mustBe (companyValuePath(details) \ "noCrnReason").asOpt[String]
              (result \ "companyRegistrationNumber" \ "value").asOpt[String] mustBe (companyValuePath(details) \ "crnNumber").asOpt[String]
            }
          }
      }

      s"has utr details in trustees array" in {
        forAll(companyJsValueGen(isEstablisher = false)) {
          companyDetails => {
            val details = ifCompanyPath(companyDetails._1)
            val result = details.transform(transformer.userAnswersUtrReads(ifTrusteeCompanyPath)).get

            (result \ "utr" \ "value").asOpt[String] mustBe (companyValuePath(details) \ "utr").asOpt[String]
            (result \ "noUtrReason").asOpt[String] mustBe (companyValuePath(details) \ "noUtrReason").asOpt[String]
          }
        }
      }

      s"has contact details in trustees array" in {
        forAll(companyJsValueGen(isEstablisher = false)) {
          companyDetails => {
            val details = ifCompanyPath(companyDetails._1)
            val result = details.transform(transformer.userAnswersContactDetailsReads("companyContactDetails", ifTrusteeCompanyPath)).get
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
            val (ifCompanyDetails, userAnswersCompanyDetails) = companyDetails
            val ifCompanyTrusteeDetails = ifCompanyPath(ifCompanyDetails)
            val result = ifCompanyTrusteeDetails.transform(transformer.userAnswersTrusteeCompanyReads(ifTrusteeCompanyPath)).get

            result mustBe userAnswersCompanyDetails
          }
        }
      }
    }

    "have the trusteePartnershipDetailsType details for partnership transformed correctly to valid user answers format for first json file" that {

      val ifTrusteePartnershipPath = __ \ 'psaPspSchemeDetails \ 'trusteeDetails \ 'partnershipTrusteeDetails

      def partnershipValuePath(details: JsObject): JsLookupResult = details \ "psaPspSchemeDetails" \ "trusteeDetails" \ "partnershipTrusteeDetails"

      s"has trustee details in trustees array" in {
        forAll(partnershipJsValueGen(isEstablisher = false)) {
          partnershipDetails => {
            val details = ifPartnershipPath(partnershipDetails._1)
            val result = details.transform(transformer.userAnswersTrusteePartnershipReads(ifTrusteePartnershipPath)).get

            (result \ "partnershipDetails" \ "name").as[String] mustBe (partnershipValuePath(details) \ "organisationName").as[String]
          }
        }
      }

      s"has vat details for partnership in trustees array" in {
          forAll(partnershipJsValueGen(isEstablisher = false)) {
            partnershipDetails => {
              val details = ifPartnershipPath(partnershipDetails._1)

              val result = details.transform(transformer.transformVatToUserAnswersReads(ifTrusteePartnershipPath, "partnershipVat")).get

              (result \ "partnershipVat" \ "value").asOpt[String] mustBe (partnershipValuePath(details) \ "vatRegistrationNumber").asOpt[String]
            }
          }
      }

      s"has paye details for partnership in trustees array" in {
          forAll(partnershipJsValueGen(isEstablisher = false)) {
            partnershipDetails => {
              val details = ifPartnershipPath(partnershipDetails._1)
              val result = details.transform(transformer.userAnswersPayeReads(ifTrusteePartnershipPath, "partnershipPaye")).get

              (result \ "partnershipPaye" \ "value").asOpt[String] mustBe (partnershipValuePath(details) \ "payeReference").asOpt[String]
            }
          }
      }

      s"has utr details in trustees array" in {
        forAll(partnershipJsValueGen(isEstablisher = false)) {
          partnershipDetails => {
            val details = ifPartnershipPath(partnershipDetails._1)
            val result = details.transform(transformer.userAnswersUtrReads(ifTrusteePartnershipPath)).get

            (result \ "utr" \ "value").asOpt[String] mustBe (partnershipValuePath(details) \ "utr").asOpt[String]
            (result \ "noUtrReason").asOpt[String] mustBe (partnershipValuePath(details) \ "noUtrReason").asOpt[String]
          }
        }
      }

      s"has contact details in trustees array" in {
        forAll(partnershipJsValueGen(isEstablisher = false)) {
          partnershipDetails => {
            val details = ifPartnershipPath(partnershipDetails._1)
            val result = details.transform(transformer.userAnswersContactDetailsReads("partnershipContactDetails", ifTrusteePartnershipPath)).get
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
            val (ifPartnershipDetails, userAnswersPartnershipDetails) = partnershipDetails
            val ifPartnershipTrusteeDetails = ifPartnershipPath(ifPartnershipDetails)
            val result = ifPartnershipTrusteeDetails.transform(transformer.userAnswersTrusteePartnershipReads(ifTrusteePartnershipPath)).get

            result mustBe userAnswersPartnershipDetails
          }
        }
      }
    }

    "have all trustees transformed" in {
      forAll(establisherOrTrusteeJsValueGen(isEstablisher = false)) {
        trustees =>
          val (ifTrustees, uaTrustees) = trustees
          val result = ifTrustees.transform(transformer.userAnswersTrusteesReads).get
          result mustBe uaTrustees
      }
    }

    "if no trustees are present" in {
      val result = Json.obj("psaPspSchemeDetails" -> "").transform(transformer.userAnswersTrusteesReads).get
      result mustBe Json.obj()
    }
  }
}

object TrusteeDetailsTransformerSpec {
  private def ifIndividualJson(individualDetails: JsValue) = {
    Json.obj(
      "psaPspSchemeDetails" -> Json.obj(
        "trusteeDetails" -> Json.obj(
          "individualTrusteeDetails" -> individualDetails
        )
      )
    )
  }

  private def ifCompanyPath(companyDetails: JsValue) = {
    Json.obj(
      "psaPspSchemeDetails" -> Json.obj(
        "trusteeDetails" -> Json.obj(
          "companyTrusteeDetails" -> companyDetails
        )
      )
    )
  }

  private def ifPartnershipPath(partnershipDetails: JsValue) = {
    Json.obj(
      "psaPspSchemeDetails" -> Json.obj(
        "trusteeDetails" -> Json.obj(
          "partnershipTrusteeDetails" -> partnershipDetails
        )
      )
    )
  }
}
