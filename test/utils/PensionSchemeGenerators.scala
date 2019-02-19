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
  val addressLineGen: Gen[String] = Gen.listOfN[Char](35, Gen.alphaChar).map(_.mkString)
  val addressLineOptional: Gen[Option[String]] = Gen.option(addressLineGen)
  val postalCodeGem: Gen[String] = Gen.listOfN[Char](10, Gen.alphaChar).map(_.mkString)
  val ninoGenerator = Gen.option("SL221122D")
  val utrGenerator = Gen.listOfN[Char](10, Gen.numChar).map(_.mkString)

  val optionalPostalCodeGen: Gen[Option[String]] = Gen.option(Gen.listOfN[Char](10, Gen.alphaChar).map(_.mkString))
  val countryCode: Gen[String] = Gen.oneOf(Seq("ES", "IT"))

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

  protected def optional(key: String, element: Option[String]) = {
    element.map { value =>
      Json.obj(key -> value)
    }.getOrElse(Json.obj())
  }
}
