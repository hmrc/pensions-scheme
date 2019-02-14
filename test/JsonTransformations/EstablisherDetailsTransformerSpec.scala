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
  private val desSchemeDetailsJsValue1: JsValue = readJsonFromFile("/data/validGetSchemeDetails1.json")
  private val desSchemeDetailsJsValue2: JsValue = readJsonFromFile("/data/validGetSchemeDetails2.json")

  private val addressTransformer = new AddressTransformer()
  private val transformer = new EstablisherDetailsTransformer(addressTransformer)

  private def desIndividualDetailsSeqJsValue(jsValue: JsValue) = jsValue
    .transform((__ \ 'psaSchemeDetails \ 'establisherDetails \ 'individualDetails).json.pick[JsArray]).asOpt.get.value

  private def desCompanyOrOrganisationDetailsSeqJsValue(jsValue: JsValue) = jsValue
    .transform((__ \ 'psaSchemeDetails \ 'establisherDetails \ 'companyOrOrganisationDetails).json.pick[JsArray]).asOpt.get.value

  private def desPartnershipDetailsSeqJsValue(jsValue: JsValue) = jsValue
    .transform((__ \ 'psaSchemeDetails \ 'establisherDetails \ 'partnershipTrusteeDetail).json.pick[JsArray]).asOpt.get.value

  "A DES payload containing establisher details" must {
    "have the individual details transformed correctly to valid user answers format for first json file" that {
      desIndividualDetailsSeqJsValue(desSchemeDetailsJsValue1).zipWithIndex.foreach {
        case (desIndividualDetailsJsValue, index) =>
          val desIndividualDetailsElementJsValue = desIndividualDetailsJsValue.transform(__.json.pick).asOpt.get
          s"has establisher details for element $index in establishers array" in {
            val actual = desIndividualDetailsElementJsValue
              .transform(transformer.transformPersonDetailsToUserAnswersReads).asOpt.get
            (actual \ "establisherDetails" \ "firstName").as[String] mustBe (desIndividualDetailsJsValue \ "personDetails" \ "firstName").as[String]
            (actual \ "establisherDetails" \ "middleName").asOpt[String] mustBe (desIndividualDetailsJsValue \ "personDetails" \ "middleName").asOpt[String]
            (actual \ "establisherDetails" \ "lastName").as[String] mustBe (desIndividualDetailsJsValue \ "personDetails" \ "lastName").as[String]
            (actual \ "establisherDetails" \ "date").as[String] mustBe (desIndividualDetailsJsValue \ "personDetails" \ "dateOfBirth").as[String]
          }

          s"has nino details for element $index in establishers array" in {
            val actual = desIndividualDetailsElementJsValue
              .transform(transformer.transformNinoDetailsToUserAnswersReads).asOpt.get

            (actual \ "establisherNino" \ "hasNino").as[Boolean] mustBe true
            (actual \ "establisherNino" \ "nino").asOpt[String] mustBe (desIndividualDetailsJsValue \ "nino").asOpt[String]
          }

          s"has utr details for element $index in establishers array" in {
            val actual = desIndividualDetailsElementJsValue
              .transform(transformer.transformUtrDetailsToUserAnswersReads("uniqueTaxReference")).asOpt.get

            (actual \ "uniqueTaxReference" \ "hasUtr").as[Boolean] mustBe true
            (actual \ "uniqueTaxReference" \ "utr").asOpt[String] mustBe (desIndividualDetailsJsValue \ "utr").asOpt[String]

          }

          s"has contact details for element $index in establishers array" in {
            val actual = desIndividualDetailsElementJsValue
              .transform(transformer.transformContactDetailsToUserAnswersReads("contactDetails")).asOpt.get

            (actual \ "contactDetails" \ "emailAddress").as[String] mustBe (desIndividualDetailsJsValue \ "correspondenceContactDetails" \ "email").as[String]
            (actual \ "contactDetails" \ "phoneNumber").as[String] mustBe
              (desIndividualDetailsJsValue \ "correspondenceContactDetails" \ "telephone").as[String]
          }
      }
    }

    "have the individual details  transformed correctly to valid user answers format for second json file" that {
      desIndividualDetailsSeqJsValue(desSchemeDetailsJsValue2).zipWithIndex.foreach {
        case (desIndividualDetailsJsValue, index) =>
          val desIndividualDetailsElementJsValue = desIndividualDetailsJsValue.transform(__.json.pick).asOpt.get

          s"has no nino details for element $index in establishers array" in {
            val actual = desIndividualDetailsElementJsValue
              .transform(transformer.transformNinoDetailsToUserAnswersReads).asOpt.get

            (actual \ "establisherNino" \ "hasNino").as[Boolean] mustBe false
            (actual \ "establisherNino" \ "reason").asOpt[String] mustBe (desIndividualDetailsJsValue \ "noNinoReason").asOpt[String]
          }

          s"has no utr details for element $index in establishers array" in {
            val actual = desIndividualDetailsElementJsValue
              .transform(transformer.transformUtrDetailsToUserAnswersReads("uniqueTaxReference")).asOpt.get

            (actual \ "uniqueTaxReference" \ "hasUtr").as[Boolean] mustBe false
            (actual \ "uniqueTaxReference" \ "reason").asOpt[String] mustBe (desIndividualDetailsJsValue \ "noUtrReason").asOpt[String]
          }
      }
    }


    "have the companyOrOrganisationDetails details for company transformed correctly to valid user answers format for first json file" that {
      desCompanyOrOrganisationDetailsSeqJsValue(desSchemeDetailsJsValue1).zipWithIndex.foreach {
        case (desCompanyOrOrganisationDetailsJsValue, index) =>
          val desCompanyOrOrganisationDetailsElementJsValue = desCompanyOrOrganisationDetailsJsValue.transform(__.json.pick).asOpt.get
          s"has establisher details for element $index in establishers array" in {
            val actual = desCompanyOrOrganisationDetailsElementJsValue
              .transform(transformer.transformCompanyDetailsToUserAnswersReads).asOpt.get
            (actual \ "companyDetails" \ "companyName").as[String] mustBe (desCompanyOrOrganisationDetailsJsValue \ "organisationName").as[String]
            (actual \ "companyDetails" \ "vatNumber").asOpt[String] mustBe (desCompanyOrOrganisationDetailsJsValue \ "vatRegistrationNumber").asOpt[String]
            (actual \ "companyDetails" \ "payeNumber").asOpt[String] mustBe (desCompanyOrOrganisationDetailsJsValue \ "payeReference").asOpt[String]
          }

          s"has crn details for element $index in establishers array" in {
            val actual = desCompanyOrOrganisationDetailsElementJsValue
              .transform(transformer.transformCRNDetailsToUserAnswersReads).asOpt.get

            (actual \ "companyRegistrationNumber" \ "hasCrn").as[Boolean] mustBe true
            (actual \ "companyRegistrationNumber" \ "crn").asOpt[String] mustBe (desCompanyOrOrganisationDetailsElementJsValue \ "crnNumber").asOpt[String]

          }

          s"has utr details for element $index in establishers array" in {
            val actual = desCompanyOrOrganisationDetailsElementJsValue
              .transform(transformer.transformUtrDetailsToUserAnswersReads("companyUniqueTaxReference")).asOpt.get

            (actual \ "companyUniqueTaxReference" \ "hasUtr").as[Boolean] mustBe true
            (actual \ "companyUniqueTaxReference" \ "utr").asOpt[String] mustBe (desCompanyOrOrganisationDetailsElementJsValue \ "utr").asOpt[String]

          }

          s"has contact details for element $index in establishers array" in {
            val actual = desCompanyOrOrganisationDetailsElementJsValue
              .transform(transformer.transformContactDetailsToUserAnswersReads("companyContactDetails")).asOpt.get

            (actual \ "companyContactDetails" \ "emailAddress").as[String] mustBe
              (desCompanyOrOrganisationDetailsElementJsValue \ "correspondenceContactDetails" \ "email").as[String]
            (actual \ "companyContactDetails" \ "phoneNumber").as[String] mustBe
              (desCompanyOrOrganisationDetailsElementJsValue \ "correspondenceContactDetails" \ "telephone").as[String]
          }
      }
    }

    "have the establisherPartnershipDetailsType details for partnership transformed correctly to valid user answers format for first json file" that {
      desPartnershipDetailsSeqJsValue(desSchemeDetailsJsValue1).zipWithIndex.foreach {
        case (desPartnershipDetailsJsValue, index) =>
          val desPartnershipDetailsElementJsValue = desPartnershipDetailsJsValue.transform(__.json.pick).asOpt.get
          s"has establisher details for element $index in establishers array" in {
            val actual = desPartnershipDetailsElementJsValue
              .transform(transformer.transformPartnershipDetailsToUserAnswersReads).asOpt.get
            (actual \ "partnershipDetails" \ "name").as[String] mustBe (desPartnershipDetailsJsValue \ "partnershipName").as[String]
          }

          s"has vat details for partnership $index in establishers array" in {
            val actual = desPartnershipDetailsElementJsValue
              .transform(transformer.transformVatToUserAnswersReads).asOpt.get

            (actual \ "partnershipVat" \ "hasVat").as[Boolean] mustBe false

          }

          s"has paye details for partnership $index in establishers array" in {
            val actual = desPartnershipDetailsElementJsValue
              .transform(transformer.transformPayeDetailsToUserAnswersReads).asOpt.get

            (actual \ "partnershipPaye" \ "hasPaye").as[Boolean] mustBe true
            (actual \ "partnershipPaye" \ "paye").asOpt[String] mustBe (desPartnershipDetailsElementJsValue \ "payeReference").asOpt[String]

          }

          s"has utr details for element $index in establishers array" in {
            val actual = desPartnershipDetailsElementJsValue
              .transform(transformer.transformUtrDetailsToUserAnswersReads("partnershipUniqueTaxReference")).asOpt.get

            (actual \ "partnershipUniqueTaxReference" \ "hasUtr").as[Boolean] mustBe true
            (actual \ "partnershipUniqueTaxReference" \ "utr").asOpt[String] mustBe (desPartnershipDetailsElementJsValue \ "utr").asOpt[String]

          }

          s"has contact details for element $index in establishers array" in {
            val actual = desPartnershipDetailsElementJsValue
              .transform(transformer.transformContactDetailsToUserAnswersReads("companyContactDetails")).asOpt.get

            (actual \ "companyContactDetails" \ "emailAddress").as[String] mustBe
              (desPartnershipDetailsElementJsValue \ "correspondenceContactDetails" \ "email").as[String]
            (actual \ "companyContactDetails" \ "phoneNumber").as[String] mustBe
              (desPartnershipDetailsElementJsValue \ "correspondenceContactDetails" \ "telephone").as[String]
          }
      }
    }

    "have an individual establisher transformed" that {
      desIndividualDetailsSeqJsValue(desSchemeDetailsJsValue1).zipWithIndex.foreach {
        case (desIndividualDetailsJsValue, index) =>
          val desIndividualDetailsElementJsValue = desIndividualDetailsJsValue.transform(__.json.pick).asOpt.get
          s"has establisher details for element $index in establishers array" in {
            val actual = desIndividualDetailsElementJsValue
              .transform(transformer.transformIndividualEstablisherToUserAnswersReads).asOpt.get

            (actual \ "establisherDetails").isDefined mustBe true
            (actual \ "establisherNino").isDefined mustBe true
            (actual \ "uniqueTaxReference").isDefined mustBe true
            (actual \ "address").isDefined mustBe true
            (actual \ "addressYears").isDefined mustBe true
            (actual \ "contactDetails").isDefined mustBe true
          }

      }
    }
  }
}
