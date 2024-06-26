/*
 * Copyright 2024 HM Revenue & Customs
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

import models.userAnswersToEtmp._
import models.userAnswersToEtmp.establisher.{CompanyEstablisher, EstablisherDetails, Partnership}
import models.userAnswersToEtmp.trustee.{CompanyTrustee, PartnershipTrustee, TrusteeDetails}
import org.scalacheck.Gen
import play.api.libs.json.{JsObject, Json}

import java.time.LocalDate

trait PensionSchemeGenerators {
  val specialCharStringGen: Gen[String] = Gen.listOfN[Char](160, Gen.alphaChar).map(_.mkString)
  val addressLineGen: Gen[String] = Gen.listOfN[Char](35, Gen.alphaChar).map(_.mkString)
  val addressLineOptional: Gen[Option[String]] = Gen.option(addressLineGen)
  val postalCodeGem: Gen[String] = Gen.listOfN[Char](10, Gen.alphaChar).map(_.mkString)
  val optionalNinoGenerator: Gen[Option[String]] = Gen.option("SL221122D")
  val ninoGenerator: Gen[String] = Gen.const("SL221122D")
  val utrGenerator: Gen[String] = Gen.listOfN[Char](10, Gen.numChar).map(_.mkString)

  val utrGeneratorFromUser: Gen[String] = {
    val utrRange: Gen[String] = Gen.listOfN[Char](randomNumberFromRange(10, 13), Gen.numChar).map(_.mkString)
    randomNumberFromRange(1, 3) match {
      case 1 => utrRange
      case 2 => "k" + utrRange
      case 3 => utrRange.toString + "k"
    }
  }

  val crnGenerator: Gen[String] = Gen.const("11111111")
  val vatGenerator: Gen[String] = Gen.const("123456789")
  val payeGenerator: Gen[String] = Gen.const("1111111111111")

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
    day <- Gen.choose(10, 28)
    month <- Gen.choose(10, 12)
    year <- Gen.choose(1990, 2000)
  } yield LocalDate.of(year, month, day)

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
    referenceOrNino <- optionalNinoGenerator
    noNinoReason <- Gen.option(reasonGen)
    utr <- Gen.option(utrGenerator)
    noUtrReason <- Gen.option(reasonGen)
    address <- ukAddressGen
    contact <- contactDetailsGen
    previousAddress <- Gen.option(previousAddressDetailsGen)
  } yield Individual(personalDetails, referenceOrNino, noNinoReason, utr,
    noUtrReason, CorrespondenceAddressDetails(address),
    CorrespondenceContactDetails(contact), previousAddress)

  val companyEstablisherGen: Gen[CompanyEstablisher] = for {
    orgName <- nameGenerator
    utr <- Gen.option(utrGenerator)
    noUtrReason <- Gen.option(reasonGen)
    crn <- Gen.option(crnGenerator)
    noCrnReason <- Gen.option(reasonGen)
    vat <- Gen.option(vatGenerator)
    paye <- Gen.option(payeGenerator)
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
    utr <- Gen.option(utrGenerator)
    noUtrReason <- Gen.option(reasonGen)
    crn <- Gen.option(crnGenerator)
    noCrnReason <- Gen.option(reasonGen)
    vat <- Gen.option(vatGenerator)
    paye <- Gen.option(payeGenerator)
    address <- ukAddressGen
    contact <- contactDetailsGen
    previous <- Gen.option(previousAddressDetailsGen)
  } yield CompanyTrustee(orgName, utr, noUtrReason, crn, noCrnReason, vat, paye, CorrespondenceAddressDetails(address),
    CorrespondenceContactDetails(contact), previous)

  val partnershipGen: Gen[Partnership] = for {
    name <- nameGenerator
    utr <- Gen.option(utrGenerator)
    noUtrReason <- Gen.option(reasonGen)
    vat <- Gen.option(vatGenerator)
    paye <- Gen.option(payeGenerator)
    haveMoreThan10Directors <- Gen.oneOf(Seq(true, false))
    address <- ukAddressGen
    contact <- contactDetailsGen
    previous <- Gen.option(previousAddressDetailsGen)
    partners <- Gen.listOfN(randomNumberFromRange(1, 10), individualGen)
  } yield Partnership(name, utr, noUtrReason, vat, paye, haveMoreThan10Directors, CorrespondenceAddressDetails(address),
    CorrespondenceContactDetails(contact), previous, partners)

  val partnershipTrusteeGen: Gen[PartnershipTrustee] = for {
    name <- nameGenerator
    utr <- Gen.option(utrGenerator)
    noUtrReason <- Gen.option(reasonGen)
    vat <- Gen.option(vatGenerator)
    paye <- Gen.option(payeGenerator)
    address <- ukAddressGen
    contact <- contactDetailsGen
    previous <- Gen.option(previousAddressDetailsGen)
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

  val establisherDetailsGenNonEmpty: Gen[EstablisherDetails] = for {
    individuals <- Gen.listOfN(randomNumberFromRange(0, 1), individualGen)
    companies <- Gen.listOfN(randomNumberFromRange(0, 1), companyEstablisherGen)
    partnerships <- Gen.listOfN(randomNumberFromRange(0, 1), partnershipGen)
  } yield EstablisherDetails(individuals, companies, partnerships)

  val trusteeDetailsGenNonEmpty: Gen[TrusteeDetails] = for {
    individuals <- Gen.listOfN(randomNumberFromRange(0, 1), individualGen)
    companies <- Gen.listOfN(randomNumberFromRange(0, 1), companyTrusteeGen)
    partnerships <- Gen.listOfN(randomNumberFromRange(0, 1), partnershipTrusteeGen)
  } yield TrusteeDetails(individuals, companies, partnerships)

  val establisherAndTrustDetailsGen : Gen[(Boolean, Option[Boolean], EstablisherDetails, Option[TrusteeDetails])]= for {
    changeOfEstablisherOrTrustDetails <- booleanGen
    haveMoreThanTenTrustees <- Gen.option(booleanGen)
    establisherDetails <- establisherDetailsGen
    trusteeDetailsType <- Gen.option(trusteeDetailsGen).suchThat(_.nonEmpty)
  } yield (changeOfEstablisherOrTrustDetails, haveMoreThanTenTrustees, establisherDetails, trusteeDetailsType)

  val establisherAndTrustDetailsGenNonEmpty : Gen[(Boolean, Option[Boolean], EstablisherDetails, Option[TrusteeDetails])]= for {
    changeOfEstablisherOrTrustDetails <- booleanGen
    haveMoreThanTenTrustees <- Gen.option(booleanGen).suchThat(_.nonEmpty)
    establisherDetails <- establisherDetailsGenNonEmpty
    trusteeDetailsType <- Gen.option(trusteeDetailsGenNonEmpty).suchThat(_.nonEmpty)
  } yield (changeOfEstablisherOrTrustDetails, haveMoreThanTenTrustees, establisherDetails, trusteeDetailsType)


  val establisherAndTrustDetailsNonEmpty: Gen[List[(Boolean, Option[Boolean], EstablisherDetails, Option[TrusteeDetails])]] =
    Gen.listOf(establisherAndTrustDetailsGenNonEmpty).suchThat(_.nonEmpty)

  val pensionSchemeUpdateDeclarationGen: Gen[PensionSchemeUpdateDeclaration] = for {
    declaration1 <- booleanGen
  } yield PensionSchemeUpdateDeclaration(declaration1)

  val schemeProvideBenefitsGen: Gen[String] = Gen.oneOf(Seq("Money Purchase benefits only (defined contribution)",
    "Defined Benefits only",
    "Mixture of money purchase benefits and defined benefits"))

  val moneyPurchaseBenefitsGen: Gen[Option[String]] = Gen.oneOf("01", "02", "03", "04", "05").map(Some(_))

  val policyNumberGen: Gen[String] = Gen.listOfN[Char](55, Gen.alphaChar).map(_.mkString)
  val otherSchemeStructureGen: Gen[String] = Gen.listOfN[Char](160, Gen.alphaChar).map(_.mkString)

  val boolenGen: Gen[Boolean] = Gen.oneOf(Seq(true, false))

  val schemeStatusGen: Gen[String] = Gen.oneOf(Seq("Open", "Pending", "Pending Info Required", "Pending Info Received", "Deregistered", "Wound-up", "Rejected Under Appeal"))

  val memberGen: Gen[String] = Gen.oneOf(Seq("0",
    "1",
    "2 to 11",
    "12 to 50",
    "51 to 10,000",
    "More than 10,000"))

  val schemeTypeGen: Gen[Option[String]] = Gen.option(Gen.oneOf(Seq("A single trust under which all of the assets are held for the benefit of all members of the scheme",
    "A group life/death in service scheme",
    "A body corporate",
    "Other")))

  val CustomerAndSchemeDetailsGen: Gen[CustomerAndSchemeDetails] = for {
    schemeName <- specialCharStringGen
    isSchemeMasterTrust <- boolenGen
    schemeStructure <- schemeTypeGen
    haveMoreThanTenTrustee <- Gen.option(booleanGen)
    currentSchemeMembers <- memberGen
    futureSchemeMembers <- memberGen
    isReguledSchemeInvestment <- boolenGen
    isOccupationalPensionScheme <- boolenGen
    areBenefitsSecuredContractInsuranceCompany <- boolenGen
    doesSchemeProvideBenefits <- schemeProvideBenefitsGen
    tcmpBenefitsType <- moneyPurchaseBenefitsGen
    schemeEstablishedCountry <- countryCode
    haveInvalidBank <- boolenGen
    insuranceCompanyName <- Gen.option(specialCharStringGen)
    policyNumber <- Gen.option(policyNumberGen)
    insuranceCompanyAddress <- Gen.option(internationalAddressGen)
    isInsuranceDetailsChanged <- Gen.option(boolenGen)
  } yield CustomerAndSchemeDetails(schemeName, isSchemeMasterTrust, schemeStructure,
    otherSchemeStructure = None, haveMoreThanTenTrustee,
    currentSchemeMembers, futureSchemeMembers, isReguledSchemeInvestment,
    isOccupationalPensionScheme, areBenefitsSecuredContractInsuranceCompany,
    doesSchemeProvideBenefits, tcmpBenefits(doesSchemeProvideBenefits, tcmpBenefitsType), schemeEstablishedCountry, haveInvalidBank,
    insuranceCompanyName, policyNumber,
    insuranceCompanyAddress, isInsuranceDetailsChanged)

  def tcmpBenefits(doesSchemeProvideBenefits: String, tcmpBenefitsGen: Option[String]): Option[String] =
    if(doesSchemeProvideBenefits.contains("Defined Benefits only")) None else tcmpBenefitsGen

  val schemeDetailsVariationGen: Gen[PensionsScheme]  = for {
    schemeDetails <- CustomerAndSchemeDetailsGen
    pensionSchemeDeclaration <- pensionSchemeUpdateDeclarationGen
    establisherAndTrustDetailsType <- establisherAndTrustDetailsGen
  } yield PensionsScheme(schemeDetails,
    pensionSchemeDeclaration,
    establisherAndTrustDetailsType._3,
    establisherAndTrustDetailsType._4.get
  )

  protected def optional(key: String, element: Option[String]): JsObject = {
    element.map { value =>
      Json.obj(key -> value)
    }.getOrElse(Json.obj())
  }

  protected def optionalWithDefaultValue(key: String, element: Option[String], defaultValue: String): JsObject = {
    element.map { value =>
      Json.obj(key -> value)
    }.getOrElse(Json.obj(key -> defaultValue))
  }

  protected def optionalWithReason(key: String, element: Option[String], reason: String): JsObject = {
    element.map { value =>
      Json.obj(key -> value)
    }.getOrElse(Json.obj(reason -> reason))
  }
}
