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

package models.userAnswersToEtmp.reads.schemes

import org.scalacheck.Arbitrary.{arbitrary, _}
import org.scalacheck.Gen
import play.api.libs.json.{JsObject, Json}
import wolfendale.scalacheck.regexp.RegexpGen

import java.time.LocalDate

//scalastyle:off magic.number
trait PSASchemeDetailsGenerator {
  val ninoGen: Gen[String] = RegexpGen.from(""""^((?!(BG|GB|KN|NK|NT|TN|ZZ)|(D|F|I|Q|U|V)[A-Z]|[A-Z](D|F|I|O|Q|U|V))[A-Z]{2})[0-9]{6}[A-D]?$""")

  val utrGen: Gen[String] = RegexpGen.from(""""^[0-9]{10}""")

  val srnGen: Gen[String] = RegexpGen.from("""^S[0-9]{10}$""")

  val pstrGen: Gen[String] = RegexpGen.from("""^[0-9]{8}[A-Z]{2}$""")

  val crnGen: Gen[String] = RegexpGen.from("""^[A-Za-z0-9 -]{1,8}$""")

  val vatRegGen: Gen[String] = RegexpGen.from("""^[0-9]{9}$""")

  val payeRefGen: Gen[String] = RegexpGen.from("""^[0-9]{3}[0-9A-Za-z]{1,13}$""")

  val dateGenerator: Gen[LocalDate] = for {
    day <- Gen.choose(1, 28)
    month <- Gen.choose(1, 12)
    year <- Gen.choose(1990, 2000)
  } yield LocalDate.of(year, month, day)

  val addressGenerator: Gen[JsObject] = for {
    nonUkAddress <- arbitrary[Boolean]
    line1 <- nonEmptyString
    line2 <- nonEmptyString
    line3 <- Gen.option(nonEmptyString)
    line4 <- Gen.option(nonEmptyString)
    postalCode <- Gen.option(nonEmptyString)
    countryCode <- Gen.listOfN(2, Gen.alphaChar).map(_.mkString)
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

  val previousAddressGenerator: Gen[JsObject] = for {
    isPreviousAddressLast12Month <- arbitrary[Boolean]
    previousAddress <- Gen.option(addressGenerator)
  } yield {
    Json.obj("isPreviousAddressLast12Month" -> isPreviousAddressLast12Month,
      "previousAddress" -> previousAddress)
  }

  val contactDetailsGenerator: Gen[JsObject] = for {
    telephone <- Gen.numStr
    email <- nonEmptyString
  } yield {
    Json.obj(
      "telephone" -> telephone,
      "email" -> email
    )
  }

  val personalDetailsGenerator: Gen[JsObject] = for {
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

  val individualDetailsGenerator: Gen[JsObject] = for {
    personalDetails <- personalDetailsGenerator
    nino <- Gen.option[String](ninoGen)
    utr <- Gen.option[String](utrGen)
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

  def companyOrOrganisationDetailsGenerator(noOfElements: Int = 1): Gen[JsObject] = for {
    organisationName <- nonEmptyString
    utr <- Gen.option[String](utrGen)
    crnNumber <- Gen.option[String](crnGen)
    vatRegistrationNumber <- Gen.option[String](vatRegGen)
    payeReference <- Gen.option[String](payeRefGen)
    addressDetails <- addressGenerator
    contactDetails <- contactDetailsGenerator
    previousAddressDetails <- Gen.option[JsObject](previousAddressGenerator)
    directorsDetails <- Gen.listOfN(noOfElements, individualDetailsGenerator)
  } yield {
    Json.obj("organisationName" -> organisationName,
      "utr" -> utr,
      "crnNumber" -> crnNumber,
      "vatRegistrationNumber" -> vatRegistrationNumber,
      "payeReference" -> payeReference,
      "correspondenceAddressDetails" -> addressDetails,
      "correspondenceContactDetails" -> contactDetails,
      "previousAddressDetails" -> previousAddressDetails,
      "directorsDetails" -> directorsDetails)
  }

  def establisherPartnershipDetailsDetailsGenerator(noOfElements: Int = 1): Gen[JsObject] = for {
    partnershipName <- nonEmptyString
    utr <- utrGen
    vatRegistrationNumber <- Gen.option[String](vatRegGen)
    payeReference <- Gen.option[String](payeRefGen)
    addressDetails <- addressGenerator
    contactDetails <- contactDetailsGenerator
    previousAddressDetails <- previousAddressGenerator
    partnerDetails <- Gen.listOfN(noOfElements, individualDetailsGenerator)
  } yield {
    Json.obj("partnershipName" -> partnershipName,
      "utr" -> utr,
      "vatRegistrationNumber" -> vatRegistrationNumber,
      "payeReference" -> payeReference,
      "correspondenceAddressDetails" -> addressDetails,
      "correspondenceContactDetails" -> contactDetails,
      "previousAddressDetails" -> previousAddressDetails,
      "partnerDetails" -> partnerDetails)
  }

  def establisherDetailsGenerator(noOfElements: Int = 1): Gen[JsObject] = for {
    individualDetails <- Gen.listOfN(noOfElements, individualDetailsGenerator)
    companyOrOrganisationDetails <- Gen.listOfN(noOfElements, companyOrOrganisationDetailsGenerator())
    partnershipTrusteeDetail <- Gen.listOfN(noOfElements, establisherPartnershipDetailsDetailsGenerator())
  } yield {
    Json.obj("individualDetails" -> individualDetails,
      "companyOrOrganisationDetails" -> companyOrOrganisationDetails,
      "partnershipTrusteeDetail" -> partnershipTrusteeDetail)
  }

  def trusteeDetailsGenerator(noOfElements: Int = 1): Gen[JsObject] = for {
    individualDetails <- Gen.listOfN(noOfElements, individualDetailsGenerator)
    companyOrOrganisationDetails <- Gen.listOfN(noOfElements, companyOrOrganisationDetailsGenerator())
    partnershipTrusteeDetail <- Gen.listOfN(noOfElements, establisherPartnershipDetailsDetailsGenerator())
  } yield {
    Json.obj("individualTrusteeDetails" -> individualDetails,
      "companyTrusteeDetails" -> companyOrOrganisationDetails,
      "partnershipTrusteeDetails" -> partnershipTrusteeDetail)
  }

  def psaDetailsGenerator(enforceProperty : Boolean = false): Gen[JsObject] = for {
    psaid <- nonEmptyString
    organizationOrPartnershipName <- Gen.option[String](nonEmptyString)
    firstName <- mustStringForOption(enforceProperty)
    middleName <- mustStringForOption(enforceProperty)
    lastName <- mustStringForOption(enforceProperty)
    relationshipDate <- dateGenerator
  } yield {
    Json.obj("psaid" -> psaid,
      "organizationOrPartnershipName" -> organizationOrPartnershipName,
      "firstName" -> firstName,
      "middleName" -> middleName,
      "lastName" -> lastName,
      "relationshipDate" -> relationshipDate
    )
  }


  def schemeDetailsGenerator(withCompanyAddress : Boolean =  false) : Gen[JsObject] = for {
    srn <- Gen.option[String](srnGen)
    pstr <- Gen.option[String](pstrGen)
    schemeStatus <- generateSchemeStatus
    schemeName <- nonEmptyString
    isSchemeMasterTrust <- arbitrary[Boolean]
    pensionSchemeStructure <-Gen.option[String](nonEmptyString)
    otherPensionSchemeStructure <- Gen.option[String](nonEmptyString)
    hasMoreThanTenTrustees <- arbitrary[Boolean]
    currentSchemeMembers <- Gen.numStr
    futureSchemeMembers <- Gen.numStr
    isReguledSchemeInvestment <- arbitrary[Boolean]
    isOccupationalPensionScheme <- arbitrary[Boolean]
    schemeProvideBenefits <- nonEmptyString
    schemeEstablishedCountry <- nonEmptyString
    isSchemeBenefitsInsuranceCompany <- arbitrary[Boolean]
    insuranceCompanyName <- mustStringForOption(withCompanyAddress)
    policyNumber <- mustStringForOption(withCompanyAddress)
    insuranceCompanyAddressDetails <- mustAddressForOption(withCompanyAddress)
  } yield {
    Json.obj("srn" -> srn,
      "pstr" -> pstr,
      "schemeStatus" -> schemeStatus,
      "schemeName" -> schemeName,
      "isSchemeMasterTrust" -> isSchemeMasterTrust,
      "pensionSchemeStructure" -> pensionSchemeStructure,
      "otherPensionSchemeStructure" -> otherPensionSchemeStructure,
      "hasMoreThanTenTrustees" -> hasMoreThanTenTrustees,
      "currentSchemeMembers" -> currentSchemeMembers,
      "futureSchemeMembers" -> futureSchemeMembers,
      "isReguledSchemeInvestment" -> isReguledSchemeInvestment,
      "isOccupationalPensionScheme" -> isOccupationalPensionScheme,
      "schemeProvideBenefits" -> schemeProvideBenefits,
      "schemeEstablishedCountry" -> schemeEstablishedCountry,
      "isSchemeBenefitsInsuranceCompany" -> isSchemeBenefitsInsuranceCompany,
      "insuranceCompanyName" -> insuranceCompanyName,
      "policyNumber" -> policyNumber,
      "insuranceCompanyAddressDetails" -> insuranceCompanyAddressDetails)
  }


  def psaSchemeDetailsGenerator(withAllEmpty: Boolean = false, noOfElements: Int = 1): Gen[JsObject] = for {
    schemeDetails <- schemeDetailsGenerator()
    establisherDetails <- if(withAllEmpty) Gen.const(None) else Gen.option(establisherDetailsGenerator())
    trusteeDetails <- if(withAllEmpty) Gen.const(None) else Gen.option(trusteeDetailsGenerator())
    psaDetails <- if(withAllEmpty) Gen.const(Nil) else Gen.listOfN(noOfElements, psaDetailsGenerator())
  } yield {
    val psaSchemeDetails =  (establisherDetails, trusteeDetails) match {
      case (None, None) => Json.obj("schemeDetails" -> schemeDetails,"psaDetails" -> psaDetails)
      case (_, None) => Json.obj("schemeDetails" -> schemeDetails, "establisherDetails" -> establisherDetails, "psaDetails" -> psaDetails)
      case (None, _) =>Json.obj("schemeDetails" -> schemeDetails, "trusteeDetails" -> trusteeDetails, "psaDetails" -> psaDetails)
      case (_,_) => Json.obj("schemeDetails" -> schemeDetails,
        "establisherDetails" -> establisherDetails,
        "trusteeDetails" -> trusteeDetails,
        "psaDetails" -> psaDetails)
    }

    Json.obj("psaSchemeDetails" -> psaSchemeDetails)
  }

  def mustStringForOption(mustName : Boolean): Gen[Option[String]] =  {
    if(mustName) Gen.option[String](nonEmptyString).suchThat(_.isDefined) else Gen.option[String](nonEmptyString)
  }

  def mustAddressForOption(mustName : Boolean): Gen[Option[JsObject]] =  {
    if(mustName) Gen.option(addressGenerator).suchThat(_.isDefined) else Gen.option(addressGenerator)
  }

  def generateSchemeStatus: Gen[String] = {
    Gen.oneOf[String]("Pending","Pending Info Required","Pending Info Received", "Rejected", "Open",
      "Deregistered", "Wound-up", "Rejected Under Appeal")
  }

  def nonEmptyString: Gen[String] = Gen.alphaStr.suchThat(_.nonEmpty)

}


