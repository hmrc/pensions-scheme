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

package utils

import org.scalacheck.Gen
import play.api.libs.json.{JsObject, JsValue, Json}

trait PensionSchemeJsValueGenerators extends PensionSchemeGenerators {

  def addressJsValueGen(desKey: String = "desAddress", uaKey: String = "userAnswersAddress",
                        isDifferent: Boolean = false): Gen[(JsValue, JsValue)] = for {
    line1 <- addressLineGen
    line2 <- addressLineGen
    line3 <- addressLineOptional
    line4 <- addressLineOptional
    postalCode <- optionalPostalCodeGen
    countryCode <- countryCode
  } yield {
    (
      Json.obj(
        desKey -> (Json.obj("nonUKAddress" -> true) ++
          Json.obj("line1" -> line1) ++
          Json.obj("line2" -> line2) ++
          optional("line3", line3) ++
          optional("line4", line4) ++
          optional("postalCode", postalCode) ++
          Json.obj("countryCode" -> countryCode))
      ),
      Json.obj(
        uaKey -> (Json.obj("addressLine1" -> line1) ++
          Json.obj("addressLine2" -> line2) ++
          optional("addressLine3", line3) ++
          optional("addressLine4", line4) ++
          optional(if (isDifferent) "postcode" else "postalCode", postalCode) ++
          Json.obj((if (isDifferent) "country" else "countryCode") -> countryCode))
      )
    )
  }

  val contactDetailsJsValueGen = for {
    email <- Gen.const("aaa@gmail.com")
    phone <- Gen.listOfN[Char](randomNumberFromRange(1, 24), Gen.numChar).map(_.mkString)
  } yield {
    (
      Json.obj(
        "telephone" -> phone,
        "mobileNumber" -> phone,
        "fax" -> "0044-09876542312",
        "email" -> email
      ),
      Json.obj(
        "emailAddress" -> email,
        "phoneNumber" -> phone
      )
    )
  }

  val individualJsValueGen: Gen[(JsValue, JsValue)] = for {
    title <- Gen.option(titleGenerator)
    firstName <- nameGenerator
    middleName <- Gen.option(nameGenerator)
    lastName <- nameGenerator
    referenceOrNino <- Gen.const("SL221122D")
    contactDetails <- contactDetailsJsValueGen
    utr <- utrGenerator
    address <- addressJsValueGen("correspondenceAddressDetails", "address", true)
    previousAddress <- addressJsValueGen("previousAddress", "previousAddress", true)
    date <- dateGenerator
  } yield {
    val (desPreviousAddress, userAnswersPreviousAddress) = previousAddress
    val previousAddr = Json.obj("isPreviousAddressLast12Month" -> true) ++ desPreviousAddress.as[JsObject]
    val (desAddress, userAnswersAddress) = address
    val (desContactDetails, userAnswersContactDetails) = contactDetails
    (
      Json.obj(
        "personDetails" -> Json.obj(
          "title" -> title,
          "firstName" -> firstName,
          "middleName" -> middleName,
          "lastName" -> lastName,
          "dateOfBirth" -> date.toString
        ),
        "nino" -> referenceOrNino,
        "utr" -> utr,
        "correspondenceContactDetails" -> desContactDetails,
        "previousAddressDetails" -> previousAddr
      ) ++ desAddress.as[JsObject],
      Json.obj(
        "establisherKind" -> "individual",
        "establisherDetails" -> Json.obj(
          "firstName" -> firstName,
          "middleName" -> middleName,
          "lastName" -> lastName,
          "date" -> date.toString
        ),
        "establisherNino" -> Json.obj(
          "hasNino" -> true,
          "nino" -> referenceOrNino
        ),
        "uniqueTaxReference" -> Json.obj(
          "hasUtr" -> true,
          "utr" -> utr
        ),
        "addressYears" -> "under_a_year",
        "contactDetails" -> userAnswersContactDetails,
        "isEstablisherComplete" -> true
      ) ++ userAnswersAddress.as[JsObject]
        ++ userAnswersPreviousAddress.as[JsObject]
    )
  }

  val companyJsValueGen = for {
    orgName <- nameGenerator
    utr <- utrGenerator
    crn <- Gen.const("11111111")
    vat <- Gen.const("123456789")
    paye <- Gen.const("1111111111111")
    address <- addressJsValueGen("correspondenceAddressDetails", "companyAddress", true)
    previousAddress <- addressJsValueGen("previousAddress", "companyPreviousAddress", true)
    contactDetails <- contactDetailsJsValueGen
  } yield {
    val (desPreviousAddress, userAnswersPreviousAddress) = previousAddress
    val (desAddress, userAnswersAddress) = address
    val (desContactDetails, userAnswersContactDetails) = contactDetails
    val pa = Json.obj("isPreviousAddressLast12Month" -> true) ++ desPreviousAddress.as[JsObject]
    (
      Json.obj(
        "organisationName" -> orgName,
        "utr" -> utr,
        "crnNumber" -> crn,
        "vatRegistrationNumber" -> vat,
        "payeReference" -> paye,
        "correspondenceContactDetails" -> desContactDetails,
        "previousAddressDetails" -> pa
      ) ++ desAddress.as[JsObject],
      Json.obj(
        "establisherKind" -> "company",
        "companyDetails" -> Json.obj(
          "companyName" -> orgName,
          "vatNumber" -> vat,
          "payeNumber" -> paye
        ),
        "companyRegistrationNumber" -> Json.obj(
          "hasCrn" -> true,
          "crn" -> crn
        ),
        "companyUniqueTaxReference" -> Json.obj(
          "hasUtr" -> true,
          "utr" -> utr
        ),
        "companyAddressYears" -> "under_a_year",
        "companyContactDetails" -> userAnswersContactDetails,
        "isCompanyComplete" -> true
      ) ++ userAnswersAddress.as[JsObject]
        ++ userAnswersPreviousAddress.as[JsObject]
    )
  }

  val partnershipJsValueGen = for {
    orgName <- nameGenerator
    vat <- Gen.const("123456789")
    utr <- utrGenerator
    paye <- Gen.const("1111111111111")
    address <- addressJsValueGen("correspondenceAddressDetails", "partnershipAddress", true)
    previousAddress <- addressJsValueGen("previousAddress", "partnershipPreviousAddress", true)
    contactDetails <- contactDetailsJsValueGen
  } yield {
    val (desPreviousAddress, userAnswersPreviousAddress) = previousAddress
    val (desAddress, userAnswersAddress) = address
    val (desContactDetails, userAnswersContactDetails) = contactDetails
    val pa = Json.obj("isPreviousAddressLast12Month" -> true) ++ desPreviousAddress.as[JsObject]
    (
      Json.obj(
        "partnershipName" -> orgName,
        "utr" -> utr,
        "vatRegistrationNumber" -> vat,
        "payeReference" -> paye,
        "correspondenceContactDetails" -> desContactDetails,
        "previousAddressDetails" -> pa
      ) ++ desAddress.as[JsObject],
      Json.obj(
        "establisherKind" -> "partnership",
        "partnershipDetails" -> Json.obj(
          "name" -> orgName
        ),
        "partnershipVat" -> Json.obj(
          "hasVat" -> true,
          "vat" -> vat
        ),
        "partnershipPaye" -> Json.obj(
          "hasPaye" -> true,
          "paye" -> paye
        ),
        "partnershipUniqueTaxReference" -> Json.obj(
          "hasUtr" -> true,
          "utr" -> utr
        ),
        "partnershipAddressYears" -> "under_a_year",
        "partnershipContactDetails" -> userAnswersContactDetails,
        "isPartnershipCompleteId" -> true
      ) ++ userAnswersAddress.as[JsObject]
        ++ userAnswersPreviousAddress.as[JsObject]
    )
  }

  val establisherJsValueGen = for {
    individual <- Gen.listOfN(randomNumberFromRange(0, 3), individualJsValueGen)
    company <- Gen.listOfN(randomNumberFromRange(0, 3), companyJsValueGen)
    partnership <- Gen.listOfN(randomNumberFromRange(0, 4), partnershipJsValueGen)
  } yield {
    val userAnswersListOfEstablishers = individual.map(_._2) ++ company.map(_._2) ++ partnership.map(_._2)
    (
      Json.obj(
        "individualDetails" -> individual.map(_._1),
        "companyOrOrganisationDetails" -> company.map(_._1),
        "partnershipTrusteeDetail" -> partnership.map(_._1)
      ),
      Json.obj(
        "establishers" -> userAnswersListOfEstablishers
      )
    )
  }
}
