/*
 * Copyright 2018 HM Revenue & Customs
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

package models.Reads.schemes

import org.joda.time.LocalDate
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import org.scalacheck.Arbitrary.arbitrary
import play.api.libs.json.{JsArray, JsObject, Json}

//scalastyle:off magic.number
trait PSASchemeDetailsGenerator {

  def nonEmptyString:Gen[String] = Gen.alphaStr.suchThat(!_.isEmpty)

  val dateGenerator: Gen[LocalDate] = for {
    day <- Gen.choose(1,28)
    month <-Gen.choose(1,12)
    year <-Gen.choose(2000,2018)
  } yield new LocalDate(year,month,day)

  val addressGenerator : Gen[JsObject] = for {
    nonUkAddress <- arbitrary[Boolean]
    line1 <- nonEmptyString
    line2 <- nonEmptyString
    line3 <- Gen.option(nonEmptyString)
    line4 <- Gen.option(nonEmptyString)
    postalCode <- Gen.option(nonEmptyString)
    countryCode <- Gen.listOfN(2, nonEmptyString).map(_.mkString)
  } yield {
    Json.obj(
      "nonUKAddress" -> nonUkAddress,
      "line1" -> line1,
      "line2" -> line2,
      "line3" -> line3,
      "line4" -> line4,
      "postalCode" -> postalCode,
      "countryCode" -> countryCode
    )
  }

  val previousAddressGenerator : Gen[JsObject] = for {
    isPreviousAddressLast12Month <- arbitrary[Boolean]
    previousAddress <- Gen.option(addressGenerator)
  } yield {
    Json.obj("isPreviousAddressLast12Month" -> isPreviousAddressLast12Month,
      "previousAddress" -> previousAddress)
  }

  val contactDetailsGenerator : Gen[JsObject] = for {
    telephone <- Gen.numStr
    email <- nonEmptyString
  } yield {
    Json.obj(
      "telephone" -> telephone,
      "email" -> email
    )
  }


  val personalDetailsGenerator : Gen[JsObject] = for {
    firstName <- nonEmptyString
    middleName <- Gen.option[String](nonEmptyString)
    lastName <- nonEmptyString
    dateOfBirth <- dateGenerator
  } yield {
    Json.obj(
      "firstName" -> firstName,
      "middleName" -> middleName,
      "lastName" -> lastName,
      "dateOfBirth" -> dateOfBirth
    )
  }

  val individualDetailsGenerator : Gen[JsObject] = for {
    personalDetails <- personalDetailsGenerator
    nino <- Gen.option[String](Gen.alphaUpperStr)
    utr <- Gen.option[String](Gen.alphaUpperStr)
    addressDetails <- addressGenerator
    contactDetails <- contactDetailsGenerator
    previousAddressDetails <- previousAddressGenerator
  } yield {
    Json.obj(
      "personDetails" -> personalDetails,
       "nino" -> nino,
      "utr" -> utr,
      "correspondenceAddressDetails" -> addressDetails,
      "correspondenceContactDetails" -> contactDetails,
      "previousAddressDetails" -> previousAddressDetails)
  }


//  val legalStatus: Gen[String] = Gen.oneOf("Individual","Partnership","Limited Company")
//
//  val idType: Gen[Option[String]] = Gen.option(Gen.oneOf("NINO","UTR"))
//
//  val customerIdentificationDetailsGenerator: Gen[JsObject] = for {
//    legalStatus <- legalStatus
//    idType <- idType
//    idNumber <- Gen.option(nonEmptyString)
//    noIdentifier <- arbitrary[Boolean]
//  } yield {
//    Json.obj(
//      "legalStatus" -> legalStatus,
//      "idType" -> idType,
//      "idNumber" -> idNumber,
//      "noIdentifier" -> noIdentifier
//    )
//  }
//  val orgOrPartnerDetailsGenerator: Gen[JsObject] = for {
//    name <- nonEmptyString
//    crnNumber <- Gen.option(nonEmptyString)
//    vatRegistrationNumber <- Gen.option(nonEmptyString)
//    payeReference <- Gen.option(nonEmptyString)
//  } yield {
//    Json.obj(
//      "name" -> name,
//      "crnNumber" -> crnNumber,
//      "vatRegistrationNumber" -> vatRegistrationNumber,
//      "payeReference" -> payeReference
//    )
//  }
//
//
//  val correspondenceDetailsGenerator: Gen[JsObject] = for {
//    addressDetails <- addressGenerator
//    contactDetails <- Gen.option(contactDetailsGenerator)
//  } yield {
//    Json.obj(
//      "addressDetails" -> addressDetails,
//      "contactDetails" -> contactDetails
//    )
//  }
//
//  val correspondenceCommonDetailsGenerator : Gen[JsObject] = for {
//    addressDetails <- addressGenerator
//    contactDetails <- Gen.option(contactDetailsGenerator)
//  } yield {
//    Json.obj(
//      "addressDetails" -> addressDetails,
//      "contactDetails" -> contactDetails
//    )
//  }
//  val psaDirectorOrPartnerDetailsGenerator : Gen[JsObject] = for {
//    entityType <- Gen.oneOf("Director","Partner")
//    firstName <- nonEmptyString
//    middleName <- Gen.option(nonEmptyString)
//    lastName <- nonEmptyString
//    dateOfBirth <- dateGenerator
//    nino <- Gen.alphaUpperStr
//    utr <- Gen.alphaUpperStr
//    previousAddressDetails <- previousAddressGenerator
//    correspondenceCommonDetails <- Gen.option(correspondenceCommonDetailsGenerator)
//  } yield {
//    Json.obj(
//      "entityType" -> entityType,
//      "firstName" -> firstName,
//      "middleName" -> middleName,
//      "lastName" -> lastName,
//      "dateOfBirth" -> dateOfBirth,
//      "nino" -> nino,
//      "utr" -> utr,
//      "previousAddressDetails" -> previousAddressDetails,
//      "correspondenceCommonDetails" -> correspondenceCommonDetails)
//  }
//  val pensionAdvisorGenerator: Gen[JsObject] = for {
//    name <- nonEmptyString
//    addressDetails <- addressGenerator
//    contactDetails <- Gen.option(contactDetailsGenerator)
//  } yield {
//    Json.obj(
//      "name" -> name,
//      "addressDetails" -> addressDetails,
//      "contactDetails" -> contactDetails
//    )
//  }
//  val directorsOrPartners = JsArray(Gen.listOf(psaDirectorOrPartnerDetailsGenerator).sample.get)
//
//  val psaSubscriptionDetailsGenerator: Gen[JsObject] = for {
//    isPSASuspension <- arbitrary[Boolean]
//    customerIdentificationDetails <- customerIdentificationDetailsGenerator
//    organisationOrPartnerDetails <- Gen.option(orgOrPartnerDetailsGenerator)
//    individualDetails <- Gen.option(personalDetailsGenerator)
//    correspondenceAddressDetails <- addressGenerator
//    correspondenceContactDetails <- contactDetailsGenerator
//    previousAddressDetails <- previousAddressGenerator
//    directorOrPartnerDetails <- Gen.option(directorsOrPartners)
//    pensionAdvisorDetails <- pensionAdvisorGenerator
//  } yield {
//    Json.obj(
//      "isPSASuspension" -> isPSASuspension,
//      "customerIdentificationDetails" -> customerIdentificationDetails,
//      "organisationOrPartnerDetails" -> organisationOrPartnerDetails,
//      "individualDetails" -> individualDetails,
//      "correspondenceAddressDetails" -> correspondenceAddressDetails,
//      "correspondenceContactDetails" -> correspondenceContactDetails,
//      "previousAddressDetails" -> previousAddressDetails,
//      "directorOrpartnerDetails" -> directorOrPartnerDetails,
//      "declarationDetails" -> Json.obj("pensionAdvisorDetails" -> pensionAdvisorDetails)
//    )
//  }
//  val psaDetailsGenerator: Gen[JsObject] = psaSubscriptionDetailsGenerator.map(psaSubscriptionDetails => Json.obj("psaSubscriptionDetails" -> psaSubscriptionDetails))
}
