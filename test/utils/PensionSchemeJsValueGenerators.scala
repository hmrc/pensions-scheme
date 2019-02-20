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

  private def utrJsValue(utr: String) = Json.obj(
    "hasUtr" -> true,
    "utr" -> utr
  )

  private def ninoJsValue(nino: String) = Json.obj(
    "hasNino" -> true,
    "nino" -> nino
  )
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

  private val contactDetailsJsValueGen = for {
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

  def individualJsValueGen(isEstablisher:Boolean): Gen[(JsValue, JsValue)] = for {
    title <- Gen.option(titleGenerator)
    firstName <- nameGenerator
    middleName <- Gen.option(nameGenerator)
    lastName <- nameGenerator
    referenceOrNino <- ninoGenerator
    contactDetails <- contactDetailsJsValueGen
    utr <- utrGenerator
    address <- addressJsValueGen("correspondenceAddressDetails", if(isEstablisher)"address" else "trusteeAddressId", isDifferent = true)
    previousAddress <- addressJsValueGen("previousAddress", if(isEstablisher) "previousAddress" else "trusteePreviousAddress", isDifferent = true)
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
        (if(isEstablisher)"establisherKind" else "trusteeKind") -> "individual",
        (if(isEstablisher)"establisherDetails" else "trusteeDetails") -> Json.obj(
          "firstName" -> firstName,
          "middleName" -> middleName,
          "lastName" -> lastName,
          "date" -> date.toString
        ),
        (if(isEstablisher)"establisherNino" else "trusteeNino") -> ninoJsValue(referenceOrNino),
        "uniqueTaxReference" -> utrJsValue(utr),
        (if(isEstablisher)"addressYears" else "trusteeAddressYears") -> "under_a_year",
        (if(isEstablisher) "contactDetails" else "trusteeContactDetails") -> userAnswersContactDetails,
        (if(isEstablisher) "isEstablisherComplete" else "isTrusteeComplete") -> true
      ) ++ userAnswersAddress.as[JsObject]
        ++ userAnswersPreviousAddress.as[JsObject]
    )
  }

  def companyJsValueGen(isEstablisher:Boolean): Gen[(JsObject, JsObject)]= for {
    orgName <- nameGenerator
    utr <- utrGenerator
    crn <- crnGenerator
    vat <- vatGenerator
    paye <- payeGenerator
    address <- addressJsValueGen("correspondenceAddressDetails", "companyAddress", isDifferent = true)
    previousAddress <- addressJsValueGen("previousAddress", "companyPreviousAddress", isDifferent = true)
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
        (if(isEstablisher)"establisherKind" else "trusteeKind") -> "company",
        "companyDetails" -> Json.obj(
          "companyName" -> orgName,
          "vatNumber" -> vat,
          "payeNumber" -> paye
        ),
        "companyRegistrationNumber" -> Json.obj(
          "hasCrn" -> true,
          "crn" -> crn
        ),
        "companyUniqueTaxReference" -> utrJsValue(utr),
        (if(isEstablisher)"companyAddressYears" else "trusteesCompanyAddressYears") -> "under_a_year",
        "companyContactDetails" -> userAnswersContactDetails,
        (if(isEstablisher)"isCompanyComplete" else "isTrusteeComplete") -> true
      ) ++ userAnswersAddress.as[JsObject]
        ++ userAnswersPreviousAddress.as[JsObject]
    )
  }

  val partnershipJsValueGen: Gen[(JsObject, JsObject)] = for {
    orgName <- nameGenerator
    vat <- vatGenerator
    utr <- utrGenerator
    paye <- payeGenerator
    address <- addressJsValueGen("correspondenceAddressDetails", "partnershipAddress", isDifferent = true)
    previousAddress <- addressJsValueGen("previousAddress", "partnershipPreviousAddress", isDifferent = true)
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
        "partnershipUniqueTaxReference" -> utrJsValue(utr),
        "partnershipAddressYears" -> "under_a_year",
        "partnershipContactDetails" -> userAnswersContactDetails,
        "isPartnershipCompleteId" -> true
      ) ++ userAnswersAddress.as[JsObject]
        ++ userAnswersPreviousAddress.as[JsObject]
    )
  }

  val establisherJsValueGen: Gen[(JsObject, JsObject)] = for {
    individual <- Gen.listOfN(randomNumberFromRange(0, 3), individualJsValueGen(isEstablisher = true))
    company <- Gen.listOfN(randomNumberFromRange(0, 3), companyJsValueGen(isEstablisher = true))
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

  def directorOrPartnerJsValueGen(directorOrPartner: String): Gen[(JsValue, JsValue)] = for {
    title <- Gen.option(titleGenerator)
    firstName <- nameGenerator
    middleName <- Gen.option(nameGenerator)
    lastName <- nameGenerator
    referenceOrNino <- ninoGenerator
    contactDetails <- contactDetailsJsValueGen
    utr <- utrGenerator
    address <- addressJsValueGen("correspondenceAddressDetails", s"${directorOrPartner}AddressId", isDifferent = true)
    partnerPreviousAddress <- addressJsValueGen("previousAddress", "partnerPreviousAddress", isDifferent = true)
    directorPreviousAddress <- addressJsValueGen("previousAddress", "previousAddress", isDifferent = true)
    date <- dateGenerator
  } yield {
    val (desPreviousAddress, userAnswersPreviousAddress) = if (directorOrPartner.contains("partner")) partnerPreviousAddress else directorPreviousAddress
    val previousAddress = Json.obj("isPreviousAddressLast12Month" -> true) ++ desPreviousAddress.as[JsObject]
    val addressYearsKey = if (directorOrPartner.contains("partner")) directorOrPartner else "companyDirector"
    val userAnswersIsComplete = if (directorOrPartner.contains("partner")) Json.obj("isPartnerComplete" -> true) else Json.obj("isDirectorComplete" -> true)
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
        "previousAddressDetails" -> previousAddress
      ) ++ desAddress.as[JsObject],
      Json.obj(
        s"${directorOrPartner}Details" -> Json.obj(
          "firstName" -> firstName,
          "middleName" -> middleName,
          "lastName" -> lastName,
          "date" -> date.toString
        ),
        s"${directorOrPartner}Nino" -> ninoJsValue(referenceOrNino),
        s"${directorOrPartner}UniqueTaxReference" -> utrJsValue(utr),
        s"${addressYearsKey}AddressYears" -> "under_a_year",
        s"${directorOrPartner}ContactDetails" -> userAnswersContactDetails
      ) ++ userAnswersAddress.as[JsObject]
        ++ userAnswersPreviousAddress.as[JsObject]
        ++ userAnswersIsComplete
    )
  }


}
