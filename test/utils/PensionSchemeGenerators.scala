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

import models._
import org.joda.time.LocalDate
import org.scalacheck.Gen
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}

trait PensionSchemeGenerators {
  private val addressLineGen: Gen[String] = Gen.listOfN[Char](35, Gen.alphaChar).map(_.mkString)
  private val addressLineOptional: Gen[Option[String]] = Gen.option(addressLineGen)
  private val postalCodeGem: Gen[String] = Gen.listOfN[Char](10, Gen.alphaChar).map(_.mkString)
  private val ninoGenerator = Gen.option("SL221122D")

  private val optionalPostalCodeGen: Gen[Option[String]] = Gen.option(Gen.listOfN[Char](10, Gen.alphaChar).map(_.mkString))
  private val countryCode: Gen[String] = Gen.oneOf(Seq("ES", "IT"))

  val ukAddressGen: Gen[UkAddress] = for {
    line1 <- addressLineGen
    line2 <- addressLineGen
    line3 <- addressLineOptional
    line4 <- addressLineOptional
    postalCode <- postalCodeGem
  } yield UkAddress(line1, Some(line2), line3, line4, "GB", postalCode)

  val internationalAddressGen: Gen[InternationalAddress] = for {
    line1 <- addressLineGen
    line2 <- addressLineGen
    line3 <- addressLineOptional
    line4 <- addressLineOptional
    countryCode <- countryCode
  } yield InternationalAddress(line1, Some(line2), line3, line4, countryCode)

  def randomNumberFromRange(min: Int, max: Int): Int = Gen.chooseNum(min, max).sample.fold(min)(c => c)

  val titleGenerator: Gen[String] = Gen.oneOf(Seq("Mr", "Mrs", "Miss", "Ms", "Dr", "Professor", "Lord"))
  val nameGenerator: Gen[String] = Gen.listOfN[Char](randomNumberFromRange(1, 35), Gen.alphaChar).map(_.mkString)
  val dateGenerator: Gen[LocalDate] = for {
    day <- Gen.choose(1, 28)
    month <- Gen.choose(1, 12)
    year <- Gen.choose(1990, 2000)
  } yield new LocalDate(year, month, day)
  val reasonGen: Gen[String] = Gen.listOfN[Char](randomNumberFromRange(1, 160), Gen.alphaChar).map(_.mkString)
  val contactDetailsGen: Gen[ContactDetails] = for {
    phone <- Gen.listOfN[Char](randomNumberFromRange(1, 24), Gen.numChar).map(_.mkString)
  } yield ContactDetails(telephone = phone, email = "test.email@email.com")

  val booleanGen: Gen[Boolean] = Gen.oneOf(true, false)

  val personalDetailsGen: Gen[PersonalDetails] = for {
    title <- Gen.option(titleGenerator)
    firstName <- nameGenerator
    middleName <- Gen.option(nameGenerator)
    lastName <- nameGenerator
  } yield PersonalDetails(title, firstName, middleName, lastName, dateGenerator.sample.get.toString)

  val previousAddressDetailsGen: Gen[PreviousAddressDetails] = for {
    isPrev: Boolean <- Gen.oneOf(true, false)
    address <- if (isPrev) Gen.option(ukAddressGen) else Gen.const(None)
  } yield PreviousAddressDetails(isPrev, address)


  val individualGen: Gen[Individual] = for {
    personalDetails <- personalDetailsGen
    referenceOrNino <- ninoGenerator
    noNinoReason <- Gen.option(reasonGen)
    utr <- Gen.option("1111111111")
    noUtrReason <- Gen.option(reasonGen)
    address <- ukAddressGen
    contact <- contactDetailsGen
    previousAddress <- Gen.option(previousAddressDetailsGen)
  } yield Individual(personalDetails, referenceOrNino, noNinoReason, utr,
    noUtrReason, CorrespondenceAddressDetails(address),
    CorrespondenceContactDetails(contact), previousAddress)

  val companyEstablisherGen: Gen[CompanyEstablisher] = for {
    orgName <- nameGenerator
    utr <- Gen.option("1111111111")
    noUtrReason <- Gen.option(reasonGen)
    crn <- Gen.option("11111111")
    noCrnReason <- Gen.option(reasonGen)
    vat <- Gen.option("123456789")
    paye <- Gen.option("1111111111111")
    haveMoreThan10Directors <- Gen.oneOf(Seq(true, false))
    address <- ukAddressGen
    contact <- contactDetailsGen
    previous <- Gen.option(previousAddressDetailsGen)
    directors <- Gen.listOfN(randomNumberFromRange(1, 10), individualGen)
  } yield CompanyEstablisher(orgName, utr, noUtrReason, crn, noCrnReason, vat,
    paye, haveMoreThan10Directors, CorrespondenceAddressDetails(address), CorrespondenceContactDetails(contact),
    previous, directors)


  val companyTrusteeGen: Gen[CompanyTrustee] = for {
    orgName <- nameGenerator
    utr <- Gen.option("1111111111")
    noUtrReason <- Gen.option(reasonGen)
    crn <- Gen.option("11111111")
    noCrnReason <- Gen.option(reasonGen)
    vat <- Gen.option("123456789")
    paye <- Gen.option("1111111111111")
    address <- ukAddressGen
    contact <- contactDetailsGen
    previous <- Gen.option(previousAddressDetailsGen)
  } yield CompanyTrustee(orgName, utr, noUtrReason, crn, noCrnReason, vat, paye, CorrespondenceAddressDetails(address),
    CorrespondenceContactDetails(contact), previous)

  val partnershipGen: Gen[Partnership] = for {
    name <- nameGenerator
    utr <- Gen.option("1111111111")
    noUtrReason <- Gen.option(reasonGen)
    vat <- Gen.option("123456789")
    paye <- Gen.option("1111111111111")
    haveMoreThan10Directors <- Gen.oneOf(Seq(true, false))
    address <- ukAddressGen
    contact <- contactDetailsGen
    previous <- Gen.option(previousAddressDetailsGen)
    partners <- Gen.listOfN(randomNumberFromRange(1, 10), individualGen)
  } yield Partnership(name, utr, noUtrReason, vat, paye, haveMoreThan10Directors, CorrespondenceAddressDetails(address),
    CorrespondenceContactDetails(contact), previous, partners)

  val partnershipTrusteeGen: Gen[PartnershipTrustee] = for {
    name <- nameGenerator
    utr <- Gen.option("1111111111")
    noUtrReason <- Gen.option(reasonGen)
    vat <- Gen.option("123456789")
    paye <- Gen.option("1111111111111")
    address <- ukAddressGen
    contact <- contactDetailsGen
    previous <- Gen.option(previousAddressDetailsGen)
    partners <- Gen.listOfN(randomNumberFromRange(1, 10), individualGen)
  } yield PartnershipTrustee(name, utr, noUtrReason, vat, paye, CorrespondenceAddressDetails(address), CorrespondenceContactDetails(contact), previous)

  val establisherDetailsGen: Gen[EstablisherDetails] = for {
    individuals <- Gen.listOfN(randomNumberFromRange(0, 10), individualGen)
    companies <- Gen.listOfN(randomNumberFromRange(0, 10), companyEstablisherGen)
    partnerships <- Gen.listOfN(randomNumberFromRange(0, 10), partnershipGen)
  } yield EstablisherDetails(individuals, companies, partnerships)

  val trusteeDetailsGen: Gen[TrusteeDetails] = for {
    individuals <- Gen.listOfN(randomNumberFromRange(0, 10), individualGen)
    companies <- Gen.listOfN(randomNumberFromRange(0, 10), companyTrusteeGen)
    partnerships <- Gen.listOfN(randomNumberFromRange(0, 10), partnershipTrusteeGen)
  } yield TrusteeDetails(individuals, companies, partnerships)

  def addressJsValueGen(desKey: String = "desAddress", uaKey: String = "userAnswersAddress", isDifferent: Boolean = false): Gen[(JsValue, JsValue)] = for {
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
    (Json.obj(
      "telephone" -> phone,
      "mobileNumber" -> phone,
      "fax" -> "0044-09876542312",
      "email" -> email
    ), Json.obj(
      "emailAddress" -> email,
      "phoneNumber" -> phone
    ))
  }

  val individualJsValueGen: Gen[(JsValue, JsValue)] = for {
    title <- Gen.option(titleGenerator)
    firstName <- nameGenerator
    middleName <- Gen.option(nameGenerator)
    lastName <- nameGenerator
    referenceOrNino <- Gen.const("SL221122D")
    contactDetails <- contactDetailsJsValueGen
    utr <- Gen.const("1111111111")
    address <- addressJsValueGen("correspondenceAddressDetails", "address", true)
    previousAddress <- addressJsValueGen("previousAddress", "previousAddress", true)
    date <- dateGenerator
  } yield {
    val pa = Json.obj("isPreviousAddressLast12Month" -> true) ++ previousAddress._1.as[JsObject]
    (Json.obj(
      "personDetails" -> Json.obj(
        "title" -> title,
        "firstName" -> firstName,
        "middleName" -> middleName,
        "lastName" -> lastName,
        "dateOfBirth" -> date.toString
      ),
      "nino" -> referenceOrNino,
      "utr" -> utr,
      "correspondenceContactDetails" -> contactDetails._1,
      "previousAddressDetails" -> pa
    ) ++ address._1.as[JsObject],
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
        "contactDetails" -> contactDetails._2,
        "isEstablisherComplete" -> true
      ) ++ address._2.as[JsObject]
        ++ previousAddress._2.as[JsObject]
    )
  }

  val companyJsValueGen = for {
    orgName <- nameGenerator
    utr <- Gen.const("1111111111")
    crn <- Gen.const("11111111")
    vat <- Gen.const("123456789")
    paye <- Gen.const("1111111111111")
    address <- addressJsValueGen("correspondenceAddressDetails", "companyAddress", true)
    previousAddress <- addressJsValueGen("previousAddress", "companyPreviousAddress", true)
    contactDetails <- contactDetailsJsValueGen
  } yield {
    val pa = Json.obj("isPreviousAddressLast12Month" -> true) ++ previousAddress._1.as[JsObject]
    (
      Json.obj(
        "organisationName" -> orgName,
        "utr" -> utr,
        "crnNumber" -> crn,
        "vatRegistrationNumber" -> vat,
        "payeReference" -> paye,
        "correspondenceContactDetails" -> contactDetails._1,
        "previousAddressDetails" -> pa
      ) ++ (address._1).as[JsObject],
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
        "companyContactDetails" -> contactDetails._2,
        "isCompanyComplete" -> true
      ) ++ address._2.as[JsObject]
        ++ previousAddress._2.as[JsObject]
    )
  }

  val partnershipJsValueGen = for {
    orgName <- nameGenerator
    vat <- Gen.const("123456789")
    utr <- Gen.const("1111111111")
    paye <- Gen.const("1111111111111")
    address <- addressJsValueGen("correspondenceAddressDetails", "partnershipAddress", true)
    previousAddress <- addressJsValueGen("previousAddress", "partnershipPreviousAddress", true)
    contactDetails <- contactDetailsJsValueGen
  } yield {
    val pa = Json.obj("isPreviousAddressLast12Month" -> true) ++ previousAddress._1.as[JsObject]
    (Json.obj(
      "partnershipName" -> orgName,
      "utr" -> utr,
      "vatRegistrationNumber" -> vat,
      "payeReference" -> paye,
      "correspondenceContactDetails" -> contactDetails._1,
      "previousAddressDetails" -> pa
    ) ++ (address._1).as[JsObject],
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
        "partnershipContactDetails" -> contactDetails._2,
        "isPartnershipCompleteId" -> true
      ) ++ address._2.as[JsObject]
        ++ previousAddress._2.as[JsObject]
    )
  }

  val establisherJsValueGen = for {
    individual <- Gen.listOfN(randomNumberFromRange(0, 2), individualJsValueGen)
    company <- Gen.listOfN(randomNumberFromRange(0, 4), companyJsValueGen)
    partnership <- Gen.listOfN(randomNumberFromRange(0, 4), partnershipJsValueGen)
  } yield {
    val listOfEst = individual.map(_._2) ++ company.map(_._2) ++ partnership.map(_._2)
    (Json.obj(
      "individualDetails" -> individual.map(_._1),
      "companyOrOrganisationDetails" -> company.map(_._1),
      "partnershipTrusteeDetail" -> partnership.map(_._1)

    ),
      Json.obj(
        "establishers" -> listOfEst
      )
    )
  }

  private def optional(key: String, element: Option[String]) = {
    element.map { value =>
      Json.obj(key -> value)
    }.getOrElse(Json.obj())
  }
}
