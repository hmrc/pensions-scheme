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

import models.enumeration.{Benefits, SchemeMembers, SchemeType}
import org.joda.time.LocalDate
import org.scalacheck.Gen
import org.scalacheck.Gen.const
import play.api.libs.json.Reads._
import play.api.libs.json.{JsObject, JsValue, Json, _}

trait PensionSchemeJsValueGenerators extends PensionSchemeGenerators {

  val schemeDetailsGen: Gen[(JsValue, JsValue)] = for {
    schemeName <- specialCharStringGen
    isSchemeMasterTrust <- Gen.option(booleanGen)
    schemeStructure <- schemeTypeGen
    currentSchemeMembers <- memberGen
    futureSchemeMembers <- memberGen
    isReguledSchemeInvestment <- boolenGen
    isOccupationalPensionScheme <- boolenGen
    areBenefitsSecuredContractInsuranceCompany <- boolenGen
    schemeProvideBenefits <- schemeProvideBenefitsGen
    schemeEstablishedCountry <- countryCode
    insuranceCompanyName <- Gen.option(specialCharStringGen)
    policyNumber <- Gen.option(policyNumberGen)
    insuranceAddress <- Gen.option(addressJsValueGen("insuranceCompanyAddressDetails", "insurerAddress", isDifferent = true))
    otherPensionSchemeStructure <- Gen.option(otherSchemeStructureGen)
    moreThanTenTrustees <- Gen.option(booleanGen)
    contactDetails <- contactDetailsJsValueGen
    optionalContact <- Gen.option(contactDetails._1)
    schemeStatus <- schemeStatusGen
    pstr <- Gen.option("12345678AB")
    relationshipDate <- Gen.option(dateGenerator)
  } yield {
    val schemeTypeName = if (isSchemeMasterTrust.contains(true)) Json.obj("name" -> "master") else
      schemeStructure.map(schemeType => Json.obj("name" -> SchemeType.nameWithValue(schemeType))).getOrElse(Json.obj())
    val otherDetails = optional("schemeTypeDetails", otherPensionSchemeStructure)
    val schemeType = schemeTypeName ++ otherDetails
    val schemeTypeJs = if (isSchemeMasterTrust.contains(true) | schemeStructure.nonEmpty | otherPensionSchemeStructure.nonEmpty)
      Json.obj("schemeType" -> schemeType) else Json.obj()
    val statusJs = Json.obj("schemeStatus" -> schemeStatus) ++ optional("pstr", pstr)
    val date = relationshipDate.map(dt => Json.obj("relationshipDate" -> dt.toString)).getOrElse(Json.obj())

    val schemeDetails =
      Json.obj(
        "srn" -> "",
        "schemeStatus" -> schemeStatus,
        "schemeName" -> schemeName,
        "currentSchemeMembers" -> currentSchemeMembers,
        "futureSchemeMembers" -> futureSchemeMembers,
        "isReguledSchemeInvestment" -> isReguledSchemeInvestment,
        "isOccupationalPensionScheme" -> isOccupationalPensionScheme,
        "schemeProvideBenefits" -> schemeProvideBenefits,
        "schemeEstablishedCountry" -> schemeEstablishedCountry,
        "isSchemeBenefitsInsuranceCompany" -> areBenefitsSecuredContractInsuranceCompany
      ) ++ isSchemeMasterTrust.map { value => Json.obj("isSchemeMasterTrust" -> value) }.getOrElse(Json.obj()) ++
        optional("pstr", pstr) ++
        optional("insuranceCompanyName", insuranceCompanyName) ++
        optional("policyNumber", policyNumber) ++
        optionalContact.map { value => Json.obj("insuranceCompanyContactDetails" -> value) }.getOrElse(Json.obj()) ++
        insuranceAddress.map { value => value._1.as[JsObject] }.getOrElse(Json.obj()) ++
        optional("otherPensionSchemeStructure", otherPensionSchemeStructure) ++
        optional("pensionSchemeStructure", schemeStructure) ++
        moreThanTenTrustees.map { value => Json.obj("hasMoreThanTenTrustees" -> value) }.getOrElse(Json.obj()) ++
        optional("insuranceCompanyName", insuranceCompanyName)

    (
      Json.obj(
        "psaSchemeDetails" -> Json.obj(
          "schemeDetails" -> schemeDetails,
          "psaDetails" -> JsArray(Seq(
            Json.obj("psaid" -> "A0000000",
              "firstName" -> "First",
              "middleName" -> "Middle",
              "lastName" -> "Last",
              "relationshipType" -> "Primary")
              ++ date,
            Json.obj("psaid" -> "A0000001",
              "organizationOrPartnershipName" -> "Acme Ltd",
              "relationshipType" -> "Primary"
            ) ++ date
          )
          )
        )
      ),
      Json.obj(
        "schemeName" -> schemeName,
        "isAboutBenefitsAndInsuranceComplete" -> true,
        "isAboutMembersComplete" -> true,
        "isBeforeYouStartComplete" -> true,
        "psaDetails" -> JsArray(
          Seq(
            Json.obj(
              "id" -> "A0000000",
              "individual" -> Json.obj(
                "firstName" -> "First",
                "middleName" -> "Middle",
                "lastName" -> "Last"
              )
            ) ++ date,
            Json.obj(
              "id" -> "A0000001",
              "organisationOrPartnershipName" -> "Acme Ltd"
            ) ++ date
          )
        ),
        "schemeEstablishedCountry" -> schemeEstablishedCountry,
        "membership" -> SchemeMembers.nameWithValue(currentSchemeMembers),
        "membershipFuture" -> SchemeMembers.nameWithValue(futureSchemeMembers),
        "investmentRegulated" -> isReguledSchemeInvestment,
        "occupationalPensionScheme" -> isOccupationalPensionScheme,
        "benefits" -> Benefits.nameWithValue(schemeProvideBenefits),
        "securedBenefits" -> areBenefitsSecuredContractInsuranceCompany
      ) ++ optional("insuranceCompanyName", insuranceCompanyName) ++
        optional("insurancePolicyNumber", policyNumber) ++
        insuranceAddress.map { value => value._2.as[JsObject] }.getOrElse(Json.obj()) ++
        schemeTypeJs ++
        statusJs ++
        moreThanTenTrustees.fold(Json.obj())(moreThanTenValue => Json.obj("moreThanTenTrustees" -> moreThanTenValue))
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

  def individualJsValueGen(isEstablisher: Boolean, isToggleOn: Boolean = false): Gen[(JsObject, JsObject)] = for {
    title <- Gen.option(titleGenerator)
    firstName <- nameGenerator
    middleName <- Gen.option(nameGenerator)
    lastName <- nameGenerator
    referenceOrNino <- Gen.option(ninoGenerator)
    utr <- Gen.option(utrGenerator)
    contactDetails <- contactDetailsJsValueGen
    address <- addressJsValueGen("correspondenceAddressDetails", if (isEstablisher) "address" else "trusteeAddressId", isDifferent = true)
    previousAddress <- addressJsValueGen("previousAddress", if (isEstablisher) "previousAddress" else "trusteePreviousAddress", isDifferent = true)
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
        "correspondenceContactDetails" -> desContactDetails,
        "previousAddressDetails" -> previousAddr
      ) ++ desAddress.as[JsObject] ++ optionalWithReason("nino", referenceOrNino, "noNinoReason")
        ++ optionalWithReason("utr", utr, "noUtrReason"),
      Json.obj(
        (if (isEstablisher) "establisherKind" else "trusteeKind") -> "individual",
        (if (isEstablisher) "addressYears" else "trusteeAddressYears") -> "under_a_year",
        (if (isEstablisher) "contactDetails" else "trusteeContactDetails") -> userAnswersContactDetails
      ) ++ userAnswersAddress.as[JsObject]
        ++ userAnswersPreviousAddress.as[JsObject]
        ++ (if(isEstablisher) ninoJsValue(referenceOrNino, isToggleOn, "establisherNino") else
            ninoJsValueHnS(referenceOrNino, isToggleOn, "trusteeNino"))
        ++ (if(isEstablisher) utrJsValue(utr, isToggleOn, "uniqueTaxReference") else
        utrJsValueHnS(utr, isToggleOn, "uniqueTaxReference")) ++
        (if(isToggleOn && !isEstablisher) getPersonName(firstName, lastName, date, "trusteeDetails")
        else if(isEstablisher)
          Json.obj(
          "establisherDetails" -> getPersonalDetails(firstName, lastName, middleName, date)
        ) else
          Json.obj(
            "trusteeDetails" -> getPersonalDetails(firstName, lastName, middleName, date))) ++
        (if (isEstablisher) Json.obj("isEstablisherComplete" -> true) else Json.obj())
    )
  }

  private def getPersonalDetails(firstName: String, lastName: String, middleName: Option[String], date: LocalDate) = Json.obj(
    "firstName" -> firstName,
    "middleName" -> middleName,
    "lastName" -> lastName,
    "date" -> date.toString
  )

  private def getPersonName(firstName: String, lastName: String, date: LocalDate, element: String) = {
    Json.obj(
      element -> Json.obj(
        "firstName" -> firstName,
        "lastName" -> lastName
      ),
      "dateOfBirth" -> date.toString)
  }

  // scalastyle:off method.length
  //scalastyle:off cyclomatic.complexity
  def companyJsValueGen(isEstablisher: Boolean, isToggleOn: Boolean = false): Gen[(JsObject, JsObject)] = for {
    orgName <- nameGenerator
    utr <- Gen.option(utrGenerator)
    crn <- Gen.option(crnGenerator)
    vat <- Gen.option(vatGenerator)
    paye <- Gen.option(payeGenerator)
    address <- addressJsValueGen("correspondenceAddressDetails", "companyAddress", isDifferent = true)
    previousAddress <- addressJsValueGen("previousAddress", "companyPreviousAddress", isDifferent = true)
    contactDetails <- contactDetailsJsValueGen
    directorDetails <- Gen.option(Gen.listOfN(randomNumberFromRange(0, 10),
      directorOrPartnerJsValueGen("director", isToggleOn)))
    haveMoreThanTenDirectors <- Gen.option(booleanGen)
  } yield {
    val (desPreviousAddress, userAnswersPreviousAddress) = previousAddress
    val (desAddress, userAnswersAddress) = address
    val (desContactDetails, userAnswersContactDetails) = contactDetails
    val pa = Json.obj("isPreviousAddressLast12Month" -> true) ++ desPreviousAddress.as[JsObject]
    val desDirectors = if (isEstablisher) directorDetails.map(dd => if (dd.nonEmpty)
      Json.obj("directorsDetails" -> dd.map(_._1)) else Json.obj()).getOrElse(Json.obj()) else Json.obj()
    val uaDirectors = if (isEstablisher) directorDetails.map(dd => if (dd.nonEmpty)
      Json.obj("director" -> dd.map(_._2)) else Json.obj()).getOrElse(Json.obj()) else Json.obj()
    val uaMoreThanTenDirectors = if (isEstablisher) haveMoreThanTenDirectors.map(value =>
      Json.obj("otherDirectors" -> value)).getOrElse(Json.obj()) else Json.obj()
    val desMoreThanTenDirectors = if (isEstablisher) haveMoreThanTenDirectors.map(value =>
      Json.obj("haveMoreThanTenDirectors" -> value)).getOrElse(Json.obj()) else Json.obj()
    (
      Json.obj(
        "organisationName" -> orgName,
        "correspondenceContactDetails" -> desContactDetails,
        "previousAddressDetails" -> pa
      ) ++ desAddress.as[JsObject] ++ optionalWithReason("utr", utr, "noUtrReason")
        ++ optionalWithReason("crnNumber", crn, "noCrnReason")
        ++ desMoreThanTenDirectors
        ++ optional("vatRegistrationNumber", vat)
        ++ optional("payeReference", paye)
        ++ desDirectors,
      Json.obj(
        (if (isEstablisher) "establisherKind" else "trusteeKind") -> "company",
        "companyDetails" -> Json.obj(
          "companyName" -> orgName
        ),
        (if (isEstablisher) "companyAddressYears" else "trusteesCompanyAddressYears") -> "under_a_year",
        "companyContactDetails" -> userAnswersContactDetails

      ) ++ userAnswersAddress.as[JsObject]
        ++ userAnswersPreviousAddress.as[JsObject]
        ++ vatJsValueHnS(vat, isToggleOn, "companyVat")
        ++ payeJsValueHnS(paye, isToggleOn, "companyPaye")
        ++ crnJsValueHnS(crn, isToggleOn, "companyRegistrationNumber")
        ++ uaMoreThanTenDirectors
        ++ uaDirectors
        ++ utrJsValueHnS(utr, isToggleOn, "companyUniqueTaxReference")
    )
  }

  def partnershipJsValueGen(isEstablisher: Boolean, isToggleOn: Boolean = false): Gen[(JsObject, JsObject)] = for {
    orgName <- nameGenerator
    vat <- Gen.option(vatGenerator)
    utr <- Gen.option(utrGenerator)
    paye <- Gen.option(payeGenerator)
    address <- addressJsValueGen("correspondenceAddressDetails", "partnershipAddress", isDifferent = true)
    previousAddress <- addressJsValueGen("previousAddress", "partnershipPreviousAddress", isDifferent = true)
    contactDetails <- contactDetailsJsValueGen
    partnerDetails <- Gen.listOfN(randomNumberFromRange(0, 10),
      directorOrPartnerJsValueGen("partner", isToggleOn))
    areMorethanTenPartners <- booleanGen
  } yield {
    val (desPreviousAddress, userAnswersPreviousAddress) = previousAddress
    val (desAddress, userAnswersAddress) = address
    val (desContactDetails, userAnswersContactDetails) = contactDetails
    val pa = Json.obj("isPreviousAddressLast12Month" -> true) ++ desPreviousAddress.as[JsObject]
    val desPartners = if (isEstablisher) Json.obj("partnerDetails" -> partnerDetails.map(_._1)) else Json.obj()
    val uaPartners = if (isEstablisher) Json.obj("partner" -> partnerDetails.map(_._2)) else Json.obj()
    val desMoreThanTenPartner = if (isEstablisher) Json.obj("areMorethanTenPartners" -> areMorethanTenPartners) else Json.obj()
    val uaMoreThanTenPartner = if (isEstablisher) Json.obj("otherPartners" -> areMorethanTenPartners) else Json.obj()
    val isPartnershipComplete = if (isEstablisher) Json.obj("isPartnershipCompleteId" -> true) else Json.obj()
    (
      Json.obj(
        "partnershipName" -> orgName,
        "correspondenceContactDetails" -> desContactDetails,
        "previousAddressDetails" -> pa
      ) ++ desAddress.as[JsObject] ++ optional("vatRegistrationNumber", vat)
        ++ optionalWithReason("utr", utr, "noUtrReason")
        ++ optional("payeReference", paye)
        ++ desMoreThanTenPartner
        ++ desPartners,
      Json.obj(
        (if (isEstablisher) "establisherKind" else "trusteeKind") -> "partnership",
        "partnershipDetails" -> Json.obj(
          "name" -> orgName
        ),
        "partnershipAddressYears" -> "under_a_year",
        "partnershipContactDetails" -> userAnswersContactDetails
      ) ++ userAnswersAddress.as[JsObject]
        ++ userAnswersPreviousAddress.as[JsObject]
        ++ vatJsValue(vat, isToggleOn, "partnershipVat")
        ++ payeJsValue(paye, isToggleOn, "partnershipPaye")
        ++ isPartnershipComplete
        ++ uaMoreThanTenPartner
        ++ uaPartners
        ++ (if (isEstablisher) utrJsValue(utr, isToggleOn, "partnershipUniqueTaxReference")
      else utrJsValueHnS(utr, isToggleOn, "partnershipUniqueTaxReference"))
      ++ (if (isEstablisher) Json.obj("isEstablisherComplete" -> true) else Json.obj())
    )
  }

  def establisherOrTrusteeJsValueGen(isEstablisher: Boolean, isToggleOn: Boolean = false): Gen[(JsObject, JsObject)] = for {
    individual <- Gen.option(Gen.listOfN(randomNumberFromRange(1, 1), individualJsValueGen(isEstablisher, isToggleOn)))
    company <- Gen.option(Gen.listOfN(randomNumberFromRange(1, 1), companyJsValueGen(isEstablisher, isToggleOn)))
    partnership <- Gen.option(Gen.listOfN(randomNumberFromRange(1, 1), partnershipJsValueGen(isEstablisher, isToggleOn)))
  } yield {

    val uaIndividualDetails = individual.map { indv => indv.map(_._2) }.getOrElse(Nil)
    val uaCompanyDetails = company.map { comp => comp.map(_._2) }.getOrElse(Nil)
    val uaPartnershipDetails = partnership.map { part => part.map(_._2) }.getOrElse(Nil)

    val desEstablishers = individual.map { indv => Json.obj("individualDetails" -> indv.map(_._1)) }.getOrElse(Json.obj()) ++
      company.map { comp => Json.obj("companyOrOrganisationDetails" -> comp.map(_._1)) }.getOrElse(Json.obj()) ++
      partnership.map { part => Json.obj("partnershipTrusteeDetail" -> part.map(_._1)) }.getOrElse(Json.obj())

    val desTrustees = individual.map { indv => Json.obj("individualTrusteeDetails" -> indv.map(_._1)) }.getOrElse(Json.obj()) ++
      company.map { comp => Json.obj("companyTrusteeDetails" -> comp.map(_._1)) }.getOrElse(Json.obj()) ++
      partnership.map { part => Json.obj("partnershipTrusteeDetails" -> part.map(_._1)) }.getOrElse(Json.obj())

    val desEstablishersJson = Json.obj(
      "psaSchemeDetails" -> Json.obj(
        "establisherDetails" -> desEstablishers
      )
    )

    val desTrusteesJson = Json.obj(
      "psaSchemeDetails" -> Json.obj(
        "trusteeDetails" -> desTrustees
      )
    )
    val lisOfAllUserAnswersEstablishers = uaIndividualDetails ++ uaCompanyDetails ++ uaPartnershipDetails
    (
      if (isEstablisher) desEstablishersJson else desTrusteesJson,
      Json.obj(
        (if (isEstablisher) "establishers" else "trustees") -> lisOfAllUserAnswersEstablishers
      )
    )
  }

  def directorOrPartnerJsValueGen(directorOrPartner: String, isToggleOn: Boolean = false): Gen[(JsValue, JsValue)] = for {
    title <- Gen.option(titleGenerator)
    firstName <- nameGenerator
    middleName <- Gen.option(nameGenerator)
    lastName <- nameGenerator
    referenceOrNino <- Gen.option(ninoGenerator)
    utr <- Gen.option(utrGenerator)
    contactDetails <- contactDetailsJsValueGen
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
        "correspondenceContactDetails" -> desContactDetails,
        "previousAddressDetails" -> previousAddress
      ) ++ desAddress.as[JsObject] ++ optionalWithReason("nino", referenceOrNino, "noNinoReason")
        ++ optionalWithReason("utr", utr, "noUtrReason"),
      Json.obj(
        s"${addressYearsKey}AddressYears" -> "under_a_year",
        s"${directorOrPartner}ContactDetails" -> userAnswersContactDetails
      ) ++ personDetailsExpectedJsonHnS(directorOrPartner, firstName, middleName, lastName, date, isToggleOn)
        ++ userAnswersAddress.as[JsObject]
        ++ userAnswersPreviousAddress.as[JsObject]
        ++ userAnswersIsComplete
        ++ (if (directorOrPartner.contains("partner"))
              ninoJsValue(referenceOrNino, isToggleOn, s"${directorOrPartner}Nino")
                ++ utrJsValue(utr, isToggleOn, s"${directorOrPartner}UniqueTaxReference")
            else
              ninoJsValueHnS(referenceOrNino, isToggleOn, s"${directorOrPartner}Nino")
                ++ utrJsValueHnS(utr, isToggleOn, s"${directorOrPartner}UniqueTaxReference"))

    )
  }

  def personDetailsExpectedJsonHnS(directorOrPartner: String,
                                   firstName: String,
                                   middleName: Option[String],
                                   lastName: String,
                                   date: LocalDate,
                                   isToggleOn: Boolean): JsObject = {
    if(directorOrPartner.contains("director") && isToggleOn)
      getPersonName(firstName, lastName, date, s"${directorOrPartner}Details")
      else
      Json.obj(
        s"${directorOrPartner}Details" ->
          getPersonalDetails(firstName, lastName, middleName, date)
    )
  }


  def getSchemeDetailsGen(isToggleOn: Boolean = false): Gen[(JsObject, JsObject)] = for {
    schemeDetails <- schemeDetailsGen
    establishers <- Gen.option(establisherOrTrusteeJsValueGen(true, isToggleOn))
    trustees <- Gen.option(establisherOrTrusteeJsValueGen(false, isToggleOn))
  } yield {
    val (desSchemeDetails, uaSchemeDetails) = schemeDetails
    val establisherDetails = establishers.map(_._1).getOrElse(Json.obj("psaSchemeDetails" -> Json.obj()))
    val trusteeDetails = trustees.map(_._1).getOrElse(Json.obj("psaSchemeDetails" -> Json.obj()))

    val branch = (__ \ 'psaSchemeDetails).json.pick
    val desEstablishers = establisherDetails.transform(branch).get
    val desTrustee = trusteeDetails.transform(branch).get

    val uaEstablisherDetails = establishers.map(_._2).getOrElse(Json.obj())
    val uaErusteeDetails = trustees.map(_._2).getOrElse(Json.obj())

    val jsonTransformer = (__ \ 'psaSchemeDetails).json.update(
      __.read[JsObject].map { o => o ++ desEstablishers.as[JsObject] ++ desTrustee.as[JsObject] }
    ).orElse(__.json.put(Json.obj()))

    (desSchemeDetails.as[JsObject].transform(jsonTransformer).get,
      uaSchemeDetails.as[JsObject] ++ uaEstablisherDetails ++ uaErusteeDetails
    )
  }

  private def utrJsValue(utr: Option[String], isToggleOn: Boolean = false, wrapper: String) =
      utr.fold(
        Json.obj(wrapper -> Json.obj("reason" -> "noUtrReason", "hasUtr" -> false)))(
        utr => Json.obj(wrapper -> Json.obj("hasUtr" -> true, "utr" -> utr))
      )

  private def utrJsValueHnS(utr: Option[String], isToggleOn: Boolean = false, wrapper: String) =
    if (isToggleOn) {
      utr.fold(
        Json.obj("hasUtr" -> false, "noUtrReason" -> "noUtrReason"))(
        utr => Json.obj("hasUtr" -> true, "utr" -> utr)
      )
    } else {
      utr.fold(
        Json.obj(wrapper -> Json.obj("reason" -> "noUtrReason", "hasUtr" -> false)))(
        utr => Json.obj(wrapper -> Json.obj("hasUtr" -> true, "utr" -> utr))
      )
    }

  private def crnJsValue(crn: Option[String], isToggleOn: Boolean = false, wrapper: String) = {
    crn.fold(
        Json.obj(wrapper -> Json.obj("reason" -> "noCrnReason", "hasCrn" -> false)))(crn =>
      Json.obj(wrapper -> Json.obj("value" -> crn)))
  }

  private def crnJsValueHnS(crn: Option[String], isToggleOn: Boolean = false, wrapper: String) =
      if (isToggleOn)
        crn.fold(Json.obj("hasCrn" -> false, "noCrnReason" -> "noCrnReason"))(crn =>
          Json.obj("hasCrn" -> true, wrapper -> Json.obj("value" -> crn)))
      else
        crn.fold(Json.obj(wrapper -> Json.obj("reason" -> "noCrnReason", "hasCrn" -> false)))(crn =>
      Json.obj(wrapper -> Json.obj("hasCrn" -> true, "value" -> crn)))

  private def ninoJsValue(nino: Option[String], isToggleOn: Boolean = false, wrapper: String) = {
    nino.fold(
     Json.obj(wrapper -> Json.obj("reason" -> "noNinoReason", "hasNino" -> false)))(nino =>
      Json.obj(wrapper -> Json.obj("value" -> nino)))
  }

  private def ninoJsValueHnS(nino: Option[String], isToggleOn: Boolean = false, wrapper: String) =
    if (isToggleOn)
      nino.fold(Json.obj("hasNino" -> false, "noNinoReason" -> "noNinoReason"))(nino =>
        Json.obj("hasNino" -> true, wrapper -> Json.obj("value" -> nino)))
    else
      nino.fold(Json.obj(wrapper -> Json.obj("reason" -> "noNinoReason", "hasNino" -> false)))(nino =>
        Json.obj(wrapper -> Json.obj("hasNino" -> true,"value" -> nino)))

  private def vatJsValue(vat: Option[String], isToggleOn: Boolean = false, wrapper: String) = {
    vat.fold(Json.obj(wrapper -> Json.obj("hasVat" -> false)))(vat => Json.obj(wrapper -> Json.obj("hasVat" -> true, "value" -> vat)))
  }

  private def payeJsValue(paye: Option[String], isToggleOn: Boolean = false, wrapper: String) = {
    paye.fold(Json.obj(wrapper -> Json.obj("hasPaye" -> false)))(paye => Json.obj(wrapper -> Json.obj("hasPaye" -> true, "value" -> paye)))
  }

  private def vatJsValueHnS(vat: Option[String], isToggleOn: Boolean = false, wrapper: String) =
    if(isToggleOn)
    vat.fold(Json.obj("hasVat" -> false))(vat =>
      Json.obj("hasVat" -> true, wrapper -> Json.obj("value" -> vat)))
  else
    vat.fold(Json.obj(wrapper -> Json.obj("hasVat" -> false)))(vat =>
      Json.obj(wrapper -> Json.obj("hasVat" -> true,"value" -> vat)))

  private def payeJsValueHnS(paye: Option[String], isToggleOn: Boolean = false, wrapper: String) =
    if(isToggleOn)
    paye.fold(Json.obj("hasPaye" -> false))(paye =>
      Json.obj("hasPaye" -> true, wrapper -> Json.obj("value" -> paye)))
  else
    paye.fold(Json.obj(wrapper -> Json.obj("hasPaye" -> false)))(paye =>
      Json.obj(wrapper -> Json.obj("hasPaye" -> true, "value" -> paye)))

}
