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

package JsonTransformations

import base.JsonFileReader
import models.jsonTransformations.AddressTransformer
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._
import utils.JsonTransformations.EstablisherDetailsTransformer


class EstablisherDetailsTransformerSpec extends WordSpec with MustMatchers with OptionValues with JsonFileReader {
  private val desResponse1: JsValue = readJsonFromFile("/data/validGetSchemeDetails1.json")
  private val desResponse2: JsValue = readJsonFromFile("/data/validGetSchemeDetails2.json")

  private val addressTransformer = new AddressTransformer()
  private val transformer = new EstablisherDetailsTransformer(addressTransformer)

  private def desEstablisherDetailsSeqJsValue(jsValue: JsValue): JsValue = (jsValue \ "psaSchemeDetails" \ "establisherDetails").as[JsValue]

  private def getPartialSeqJsValue(jsValue: JsValue, path: JsPath) =
    jsValue.transform(path.json.pick[JsArray]).get

  private def individualPath = __ \ 'psaSchemeDetails \ 'establisherDetails \ 'individualDetails
  private def companyPath = __ \ 'psaSchemeDetails \ 'establisherDetails \ 'companyOrOrganisationDetails
  private def partnershipPath = __ \ 'psaSchemeDetails \ 'establisherDetails \ 'partnershipTrusteeDetail


  private def getTransformedResult(path: JsPath, reads: Reads[JsObject], response: JsValue = desResponse1) = {
    val desJsValue = getPartialSeqJsValue(response, path)(0)
    (desJsValue.transform(reads).get, desJsValue)
  }

  "A DES payload containing establisher details" must {
    "have the individual details transformed correctly to valid user answers format for first json file" that {

      s"has establisher details in establishers array" in {
        val (actual, expected) = getTransformedResult(individualPath, transformer.userAnswersIndividualDetailsReads)
        (actual \ "establisherDetails" \ "firstName").as[String] mustBe (expected \ "personDetails" \ "firstName").as[String]
        (actual \ "establisherDetails" \ "middleName").asOpt[String] mustBe (expected \ "personDetails" \ "middleName").asOpt[String]
        (actual \ "establisherDetails" \ "lastName").as[String] mustBe (expected \ "personDetails" \ "lastName").as[String]
        (actual \ "establisherDetails" \ "date").as[String] mustBe (expected \ "personDetails" \ "dateOfBirth").as[String]
      }

      s"has nino details in establishers array" in {
        val (actual, expected) = getTransformedResult(individualPath, transformer.userAnswersNinoReads)

        (actual \ "establisherNino" \ "hasNino").as[Boolean] mustBe true
        (actual \ "establisherNino" \ "nino").asOpt[String] mustBe (expected \ "nino").asOpt[String]
      }

      s"has utr details in establishers array" in {
        val (actual, expected) = getTransformedResult(individualPath, transformer.userAnswersUtrReads("uniqueTaxReference"))

        (actual \ "uniqueTaxReference" \ "hasUtr").as[Boolean] mustBe true
        (actual \ "uniqueTaxReference" \ "utr").asOpt[String] mustBe (expected \ "utr").asOpt[String]

      }

      s"has contact details in establishers array" in {
        val (actual, expected) = getTransformedResult(individualPath, transformer.userAnswersContactDetailsReads("contactDetails"))

        (actual \ "contactDetails" \ "emailAddress").as[String] mustBe (expected \ "correspondenceContactDetails" \ "email").as[String]
        (actual \ "contactDetails" \ "phoneNumber").as[String] mustBe
          (expected \ "correspondenceContactDetails" \ "telephone").as[String]
      }

    }

    "have the individual details  transformed correctly to valid user answers format for second json file" that {

      s"has no nino details in establishers array" in {
        val (actual, expected) = getTransformedResult(individualPath, transformer.userAnswersNinoReads, desResponse2)

        (actual \ "establisherNino" \ "hasNino").as[Boolean] mustBe false
        (actual \ "establisherNino" \ "reason").asOpt[String] mustBe (expected \ "noNinoReason").asOpt[String]
      }

      s"has no utr details in establishers array" in {
        val (actual, expected) = getTransformedResult(individualPath, transformer.userAnswersUtrReads("uniqueTaxReference"), desResponse2)

        (actual \ "uniqueTaxReference" \ "hasUtr").as[Boolean] mustBe false
        (actual \ "uniqueTaxReference" \ "reason").asOpt[String] mustBe (expected \ "noUtrReason").asOpt[String]
      }
    }


    "have the companyOrOrganisationDetails details for company transformed correctly to valid user answers format for first json file" that {
      s"has establisher details in establishers array" in {
        val (actual, expected) = getTransformedResult(companyPath, transformer.userAnswersCompanyDetailsReads)

        (actual \ "companyDetails" \ "companyName").as[String] mustBe (expected \ "organisationName").as[String]
        (actual \ "companyDetails" \ "vatNumber").asOpt[String] mustBe (expected \ "vatRegistrationNumber").asOpt[String]
        (actual \ "companyDetails" \ "payeNumber").asOpt[String] mustBe (expected \ "payeReference").asOpt[String]
      }

      s"has crn details in establishers array" in {
        val (actual, expected) = getTransformedResult(companyPath, transformer.userAnswersCrnReads)
        (actual \ "companyRegistrationNumber" \ "hasCrn").as[Boolean] mustBe true
        (actual \ "companyRegistrationNumber" \ "crn").asOpt[String] mustBe (expected \ "crnNumber").asOpt[String]

      }

      s"has utr details in establishers array" in {
        val (actual, expected) = getTransformedResult(companyPath, transformer.userAnswersUtrReads("companyUniqueTaxReference"))
        (actual \ "companyUniqueTaxReference" \ "hasUtr").as[Boolean] mustBe true
        (actual \ "companyUniqueTaxReference" \ "utr").asOpt[String] mustBe (expected \ "utr").asOpt[String]

      }

      s"has contact details in establishers array" in {
        val (actual, expected) = getTransformedResult(companyPath, transformer.userAnswersContactDetailsReads("companyContactDetails"))
        (actual \ "companyContactDetails" \ "emailAddress").as[String] mustBe
          (expected \ "correspondenceContactDetails" \ "email").as[String]
        (actual \ "companyContactDetails" \ "phoneNumber").as[String] mustBe
          (expected \ "correspondenceContactDetails" \ "telephone").as[String]
      }
    }

    "have the establisherPartnershipDetailsType details for partnership transformed correctly to valid user answers format for first json file" that {

      s"has establisher details in establishers array" in {
        val (actual, expected) = getTransformedResult(partnershipPath, transformer.userAnswersPartnershipDetailsReads)
        (actual \ "partnershipDetails" \ "name").as[String] mustBe (expected \ "partnershipName").as[String]
      }

      s"has vat details for partnership in establishers array" in {
        val (actual, expected) = getTransformedResult(partnershipPath, transformer.transformVatToUserAnswersReads)
        (actual \ "partnershipVat" \ "hasVat").as[Boolean] mustBe false

      }

      s"has paye details for partnership in establishers array" in {
        val (actual, expected) = getTransformedResult(partnershipPath, transformer.userAnswersPayeReads)
        (actual \ "partnershipPaye" \ "hasPaye").as[Boolean] mustBe true
        (actual \ "partnershipPaye" \ "paye").asOpt[String] mustBe (expected \ "payeReference").asOpt[String]

      }

      s"has utr details in establishers array" in {
        val (actual, expected) = getTransformedResult(partnershipPath, transformer.userAnswersUtrReads("partnershipUniqueTaxReference"))
        (actual \ "partnershipUniqueTaxReference" \ "hasUtr").as[Boolean] mustBe true
        (actual \ "partnershipUniqueTaxReference" \ "utr").asOpt[String] mustBe (expected \ "utr").asOpt[String]

      }

      s"has contact details in establishers array" in {
        val (actual, expected) = getTransformedResult(partnershipPath, transformer.userAnswersContactDetailsReads("partnershipContactDetails"))
        (actual \ "partnershipContactDetails" \ "emailAddress").as[String] mustBe
          (expected \ "correspondenceContactDetails" \ "email").as[String]
        (actual \ "partnershipContactDetails" \ "phoneNumber").as[String] mustBe
          (expected \ "correspondenceContactDetails" \ "telephone").as[String]
      }
    }

    "have an individual establisher transformed" in {

      val (actual, _) = getTransformedResult(individualPath, transformer.userAnswersEstablisherIndividualReads)

      (actual \ "establisherKind").as[String] mustBe "individual"
      (actual \ "establisherDetails").isDefined mustBe true
      (actual \ "establisherNino").isDefined mustBe true
      (actual \ "uniqueTaxReference").isDefined mustBe true
      (actual \ "address").isDefined mustBe true
      (actual \ "addressYears").isDefined mustBe true
      (actual \ "previousAddress").isDefined mustBe true
      (actual \ "contactDetails").isDefined mustBe true
      (actual \ "isEstablisherComplete").as[Boolean] mustBe true

    }

    "have an organisation establisher transformed" in {
      val (actual, _) = getTransformedResult(companyPath, transformer.userAnswersEstablisherCompanyReads)

      (actual \ "establisherKind").as[String] mustBe "company"
      (actual \ "companyDetails").isDefined mustBe true
      (actual \ "companyRegistrationNumber").isDefined mustBe true
      (actual \ "companyUniqueTaxReference").isDefined mustBe true
      (actual \ "companyAddress").isDefined mustBe true
      (actual \ "companyAddressYears").isDefined mustBe true
      (actual \ "previousAddress").isDefined mustBe false
      (actual \ "companyContactDetails").isDefined mustBe true
      (actual \ "isCompanyComplete").as[Boolean] mustBe true
    }

    "have an partnership establisher transformed" in {
      val (actual, _) = getTransformedResult(partnershipPath, transformer.userAnswersEstablisherPartnershipReads)

      (actual \ "establisherKind").as[String] mustBe "partnership"
      (actual \ "partnershipDetails").isDefined mustBe true
      (actual \ "partnershipVat").isDefined mustBe true
      (actual \ "partnershipPaye").isDefined mustBe true
      (actual \ "partnershipUniqueTaxReference").isDefined mustBe true
      (actual \ "partnershipAddress").isDefined mustBe true
      (actual \ "partnershipAddressYears").isDefined mustBe true
      (actual \ "partnershipPreviousAddress").isDefined mustBe false
      (actual \ "partnershipContactDetails").isDefined mustBe true
      (actual \ "isPartnershipCompleteId").as[Boolean] mustBe true
    }

    "have all establisher transformed" in {
      val desEstablisherDetailsSection = desEstablisherDetailsSeqJsValue(desResponse1)
      val userAnswersEstablishers: JsValue = transformer.userAnswersEstablishersReads(desEstablisherDetailsSection)
      val userAnswersEstablishersJsArray = (userAnswersEstablishers \ "establishers").as[JsArray]

      userAnswersEstablishersJsArray.value.size mustBe 3
      (userAnswersEstablishersJsArray(0) \ "establisherDetails" \ "firstName").as[String] mustBe
        (desEstablisherDetailsSection \ "individualDetails" \ 0 \ "personDetails" \ "firstName").as[String]
      (userAnswersEstablishersJsArray(1) \ "companyDetails" \ "companyName").as[String] mustBe
        (desEstablisherDetailsSection \ "companyOrOrganisationDetails" \ 0 \ "organisationName").as[String]
      (userAnswersEstablishersJsArray(2) \ "partnershipDetails" \ "name").as[String] mustBe
        (desEstablisherDetailsSection \ "partnershipTrusteeDetail" \ 0 \ "partnershipName").as[String]

    }

    "if only inidividual and company establishers are present" in {
      val desEstablisherDetailsSection = desEstablisherDetailsSeqJsValue(desResponse2)
      val userAnswersEstablishers: JsValue = transformer.userAnswersEstablishersReads(desEstablisherDetailsSection)
      val userAnswersEstablishersJsArray = (userAnswersEstablishers \ "establishers").as[JsArray]

      userAnswersEstablishersJsArray.value.size mustBe 2
    }

  }
}
