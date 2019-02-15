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

package models

import models.enumeration.{Benefits, SchemeMembers, SchemeType}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import utils.Lens

case class AddressAndContactDetails(addressDetails: Address, contactDetails: ContactDetails)

object AddressAndContactDetails {
  implicit val formats: Format[AddressAndContactDetails] = Json.format[AddressAndContactDetails]
}

case class PersonalDetails(title: Option[String] = None, firstName: String, middleName: Option[String] = None,
                           lastName: String, dateOfBirth: String)

object PersonalDetails {
  implicit val formats: Format[PersonalDetails] = Json.format[PersonalDetails]
}

case class PreviousAddressDetails(isPreviousAddressLast12Month: Boolean,
                                  previousAddressDetails: Option[Address] = None)

object PreviousAddressDetails {
  implicit val formats: Format[PreviousAddressDetails] = Json.format[PreviousAddressDetails]

  val psaSubmissionWrites: Writes[PreviousAddressDetails] = (
    (JsPath \ "isPreviousAddressLast12Month").write[Boolean] and
      (JsPath \ "previousAddressDetail").writeNullable[Address]
    ) (previousAddress => (previousAddress.isPreviousAddressLast12Month, previousAddress.previousAddressDetails))

  val psaUpdateWrites: Writes[PreviousAddressDetails] = (
    (JsPath \ "isPreviousAddressLast12Month").write[Boolean] and
      (JsPath \ "previousAddressDetails").writeNullable[Address](Address.updateWrites)
    ) (previousAddress => (previousAddress.isPreviousAddressLast12Month, previousAddress.previousAddressDetails))

  def apiReads(typeOfAddressDetail: String): Reads[PreviousAddressDetails] = (
    (JsPath \ s"${typeOfAddressDetail}AddressYears").read[String] and
      (JsPath \ s"${typeOfAddressDetail}PreviousAddress").readNullable[Address]
    ) ((addressLast12Months, address) => {
    val isAddressLast12Months = if (addressLast12Months == "under_a_year") true else false
    PreviousAddressDetails(isAddressLast12Months, address)
  })
}

case class CorrespondenceAddressDetails(addressDetails: Address)

object CorrespondenceAddressDetails {
  implicit val formats: Format[CorrespondenceAddressDetails] = Json.format[CorrespondenceAddressDetails]

  val updateWrites : Writes[CorrespondenceAddressDetails] = (JsPath \ "addressDetails").write[Address](Address.updateWrites).contramap(c=>c.addressDetails)
}

case class CorrespondenceContactDetails(contactDetails: ContactDetails)

object CorrespondenceContactDetails {
  implicit val formats: Format[CorrespondenceContactDetails] = Json.format[CorrespondenceContactDetails]
}

case class CustomerAndSchemeDetails(schemeName: String, isSchemeMasterTrust: Boolean, schemeStructure: Option[String],
                                    otherSchemeStructure: Option[String] = None, haveMoreThanTenTrustee: Option[Boolean] = None,
                                    currentSchemeMembers: String, futureSchemeMembers: String, isReguledSchemeInvestment: Boolean,
                                    isOccupationalPensionScheme: Boolean, areBenefitsSecuredContractInsuranceCompany: Boolean,
                                    doesSchemeProvideBenefits: String, schemeEstablishedCountry: String, haveInvalidBank: Boolean,
                                    insuranceCompanyName: Option[String] = None, policyNumber: Option[String] = None,
                                    insuranceCompanyAddress: Option[Address] = None, isInsuranceDetailsChanged: Option[Boolean] = None)

object CustomerAndSchemeDetails {
  implicit val formats: Format[CustomerAndSchemeDetails] = Json.format[CustomerAndSchemeDetails]

  def insurerReads: Reads[(Option[String], Option[String])] = (
    ((JsPath \ "companyName").readNullable[String] and
      (JsPath \ "policyNumber").readNullable[String])
      ((companyName, policyNumber) => (companyName, policyNumber))
    )

  def schemeTypeReads: Reads[(String, Option[String])] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "schemeTypeDetails").readNullable[String]
    ) ((name, schemeDetails) => (name, schemeDetails))

  def apiReads: Reads[CustomerAndSchemeDetails] = (
    (JsPath \ "schemeName").read[String] and
      (JsPath \ "schemeType").read[(String, Option[String])](schemeTypeReads) and
      (JsPath \ "moreThanTenTrustees").readNullable[Boolean] and
      (JsPath \ "membership").read[String] and
      (JsPath \ "membershipFuture").read[String] and
      (JsPath \ "investmentRegulated").read[Boolean] and
      (JsPath \ "occupationalPensionScheme").read[Boolean] and
      (JsPath \ "securedBenefits").read[Boolean] and
      (JsPath \ "benefits").read[String] and
      (JsPath \ "schemeEstablishedCountry").read[String] and
      (JsPath \ "insuranceCompanyName").readNullable[String] and
      (JsPath \ "insurancePolicyNumber").readNullable[String] and
      (JsPath \ "insurerAddress").readNullable[Address] and
      (JsPath \ "isInsuranceDetailsChanged").readNullable[Boolean]
    ) (
    (name, schemeType, moreThanTenTrustees, membership, membershipFuture, investmentRegulated,
     occupationalPension, securedBenefits, benefits, country, insuranceCompanyName, insurancePolicyNumber, insurerAddress, isInsuranceDetailsChanged) => {

      val isMasterTrust = schemeType._1 == "master"

      val schemeTypeName = if (isMasterTrust) None else Some(SchemeType.valueWithName(schemeType._1))

      CustomerAndSchemeDetails(
        schemeName = name,
        isSchemeMasterTrust = isMasterTrust,
        schemeStructure = schemeTypeName,
        otherSchemeStructure = schemeType._2,
        haveMoreThanTenTrustee = moreThanTenTrustees,
        currentSchemeMembers = SchemeMembers.valueWithName(membership),
        futureSchemeMembers = SchemeMembers.valueWithName(membershipFuture),
        isReguledSchemeInvestment = investmentRegulated,
        isOccupationalPensionScheme = occupationalPension,
        areBenefitsSecuredContractInsuranceCompany = securedBenefits,
        doesSchemeProvideBenefits = Benefits.valueWithName(benefits),
        schemeEstablishedCountry = country,
        haveInvalidBank = false,
        insuranceCompanyName = insuranceCompanyName,
        policyNumber = insurancePolicyNumber,
        insuranceCompanyAddress = insurerAddress,
        isInsuranceDetailsChanged = isInsuranceDetailsChanged)
    }
  )
}

case class AdviserDetails(adviserName: String, emailAddress: String, phoneNumber: String)

object AdviserDetails {

  implicit val formats: Format[PensionSchemeDeclaration] = Json.format[PensionSchemeDeclaration]

  implicit val readsAdviserDetails: Reads[AdviserDetails] = (
    (JsPath \ "adviserName").read[String] and
      (JsPath \ "emailAddress").read[String] and
      (JsPath \ "phoneNumber").read[String]
    ) ((name, email, phone) => AdviserDetails(name, email, phone))
}


case class PensionSchemeDeclaration(box1: Boolean, box2: Boolean, box3: Option[Boolean] = None, box4: Option[Boolean] = None,
                                    box5: Option[Boolean] = None, box6: Boolean, box7: Boolean, box8: Boolean, box9: Boolean,
                                    box10: Option[Boolean] = None, box11: Option[Boolean] = None, pensionAdviserName: Option[String] = None,
                                    addressAndContactDetails: Option[AddressAndContactDetails] = None)

object PensionSchemeDeclaration {

  implicit val formats: Format[PensionSchemeDeclaration] = Json.format[PensionSchemeDeclaration]

  val apiReads: Reads[PensionSchemeDeclaration] = (
    (JsPath \ "declaration").read[Boolean] and
      (JsPath \ "schemeType" \ "name").read[String] and
      (JsPath \ "declarationDormant").readNullable[String] and
      (JsPath \ "declarationDuties").read[Boolean] and
      (JsPath \ "adviserName").readNullable[String] and
      (JsPath \ "adviserEmail").readNullable[String] and
      (JsPath \ "adviserPhone").readNullable[String] and
      (JsPath \ "adviserAddress").readNullable[Address]
    ) ((declaration, schemeTypeName, declarationDormant, declarationDuties, adviserName, adviserEmail, adviserPhone, adviserAddress) => {

    val basicDeclaration = PensionSchemeDeclaration(
      declaration,
      declaration,
      None, None, None,
      declaration,
      declaration,
      declaration,
      declaration,
      None, None,
      None)

    val dormant = (dec: PensionSchemeDeclaration) => {
      declarationDormant.fold(dec)(value => {
        if (value == "no") {
          dec.copy(box4 = Some(true))
        } else {
          dec.copy(box5 = Some(true))
        }
      }
      )
    }

    val isMasterTrust = (dec: PensionSchemeDeclaration) => {
      if (schemeTypeName == "master")
        dec.copy(box3 = Some(true))
      else
        dec
    }
    val decDuties = (dec: PensionSchemeDeclaration) => {

      if (declarationDuties) {
        dec.copy(box10 = Some(true))
      }
      else {
        dec.copy(
          box11 = Some(true),
          pensionAdviserName = adviserName,
          addressAndContactDetails = {
            (adviserEmail, adviserPhone, adviserAddress) match {
              case (Some(contactEmail), Some(contactPhone), Some(address)) =>
                Some(AddressAndContactDetails(
                  address,
                  ContactDetails(contactPhone, None, None, contactEmail)
                ))
              case _ => None
            }
          }
        )
      }

    }

    val completedDeclaration = dormant andThen isMasterTrust andThen decDuties
    completedDeclaration(basicDeclaration)
  })
}

case class Individual(
                       personalDetails: PersonalDetails,
                       referenceOrNino: Option[String] = None,
                       noNinoReason: Option[String] = None,
                       utr: Option[String] = None,
                       noUtrReason: Option[String] = None,
                       correspondenceAddressDetails: CorrespondenceAddressDetails,
                       correspondenceContactDetails: CorrespondenceContactDetails,
                       previousAddressDetails: Option[PreviousAddressDetails] = None
                     )

object Individual {
  implicit val formats: Format[Individual] = Json.format[Individual]

  private val commonIndividualWrites: Writes[(Option[String], Option[String], Option[String],
    Option[String], CorrespondenceAddressDetails, CorrespondenceContactDetails, PreviousAddressDetails)] = (
    (JsPath \ "nino").writeNullable[String] and
      (JsPath \ "noNinoReason").writeNullable[String] and
      (JsPath \ "utr").writeNullable[String] and
      (JsPath \ "noUtrReason").writeNullable[String] and
      (JsPath \ "correspondenceAddressDetails").write[CorrespondenceAddressDetails](CorrespondenceAddressDetails.updateWrites) and
      (JsPath \ "correspondenceContactDetails").write[CorrespondenceContactDetails] and
      (JsPath \ "previousAddressDetails").write[PreviousAddressDetails](PreviousAddressDetails.psaUpdateWrites)) (
    elements => elements)

  val individualUpdateWrites : Writes[Individual] = (
    (JsPath \ "personDetails").write[PersonalDetails] and
      JsPath.write(commonIndividualWrites)
  )(details => (details.personalDetails,
    (details.referenceOrNino,
    details.noNinoReason,
    details.utr,
    details.noUtrReason,
    details.correspondenceAddressDetails,
    details.correspondenceContactDetails,
    details.previousAddressDetails.fold(PreviousAddressDetails(isPreviousAddressLast12Month = false))(c=>c))))

  val establisherIndividualDetailsUpdateWrites : Writes[Individual] = (
    (JsPath \ "personalDetails").write[PersonalDetails] and
      JsPath.write(commonIndividualWrites)
    )(details => (details.personalDetails,
    (details.referenceOrNino,
      details.noNinoReason,
      details.utr,
      details.noUtrReason,
      details.correspondenceAddressDetails,
      details.correspondenceContactDetails,
      details.previousAddressDetails.fold(PreviousAddressDetails(isPreviousAddressLast12Month = false))(c=>c))))
}

case class CompanyEstablisher(
                               organizationName: String,
                               utr: Option[String] = None,
                               noUtrReason: Option[String] = None,
                               crnNumber: Option[String] = None,
                               noCrnReason: Option[String] = None,
                               vatRegistrationNumber: Option[String] = None,
                               payeReference: Option[String] = None,
                               haveMoreThanTenDirectorOrPartner: Boolean,
                               correspondenceAddressDetails: CorrespondenceAddressDetails,
                               correspondenceContactDetails: CorrespondenceContactDetails,
                               previousAddressDetails: Option[PreviousAddressDetails] = None,
                               directorDetails: Seq[Individual]
                             )

object CompanyEstablisher {
  implicit val formats: Format[CompanyEstablisher] = Json.format[CompanyEstablisher]

  val updateWrites : Writes[CompanyEstablisher] = (
    (JsPath \ "organisationName").write[String] and
      (JsPath \ "utr").writeNullable[String] and
      (JsPath \ "noUtrReason").writeNullable[String] and
      (JsPath \ "crnNumber").writeNullable[String] and
      (JsPath \ "noCrnReason").writeNullable[String] and
      (JsPath \ "vatRegistrationNumber").writeNullable[String] and
      (JsPath \ "payeReference").writeNullable[String] and
      (JsPath \ "haveMoreThanTenDirectors").writeNullable[Boolean] and
      (JsPath \ "correspondenceAddressDetails").write[CorrespondenceAddressDetails](CorrespondenceAddressDetails.updateWrites) and
      (JsPath \ "correspondenceContactDetails").write[CorrespondenceContactDetails] and
      (JsPath \ "previousAddressDetails").write[PreviousAddressDetails](PreviousAddressDetails.psaUpdateWrites) and
      (JsPath \ "directorsDetails").write[JsArray]
    )(company => (company.organizationName,
    company.utr,
    company.noUtrReason,
    company.crnNumber,
    company.noCrnReason,
    company.vatRegistrationNumber,
    company.payeReference,
    Some(company.haveMoreThanTenDirectorOrPartner),
    company.correspondenceAddressDetails,
    company.correspondenceContactDetails,
    company.previousAddressDetails.fold(PreviousAddressDetails(isPreviousAddressLast12Month = false))(c=>c),
    JsArray(company.directorDetails.map(c=>Json.toJson(c)(Individual.individualUpdateWrites))))
  )
}

case class Partnership(
                        organizationName: String,
                        utr: Option[String] = None,
                        noUtrReason: Option[String] = None,
                        vatRegistrationNumber: Option[String] = None,
                        payeReference: Option[String] = None,
                        haveMoreThanTenDirectorOrPartner: Boolean,
                        correspondenceAddressDetails: CorrespondenceAddressDetails,
                        correspondenceContactDetails: CorrespondenceContactDetails,
                        previousAddressDetails: Option[PreviousAddressDetails] = None,
                        partnerDetails: Seq[Individual]
                      )

object Partnership {
  implicit val formats: Format[Partnership] = Json.format[Partnership]

  val updateWrites : Writes[Partnership] = (
    (JsPath \ "partnershipName").write[String] and
      (JsPath \ "utr").writeNullable[String] and
      (JsPath \ "noUtrReason").writeNullable[String] and
      (JsPath \ "vatRegistrationNumber").writeNullable[String] and
      (JsPath \ "payeReference").writeNullable[String] and
      (JsPath \ "hasMoreThanTenPartners").writeNullable[Boolean] and
      (JsPath \ "correspondenceAddressDetails").write[CorrespondenceAddressDetails](CorrespondenceAddressDetails.updateWrites) and
      (JsPath \ "correspondenceContactDetails").write[CorrespondenceContactDetails] and
      (JsPath \ "previousAddressDetails").write[PreviousAddressDetails](PreviousAddressDetails.psaUpdateWrites) and
      (JsPath \ "partnerDetails").write[JsArray]
    )(partnership => (partnership.organizationName,
    partnership.utr,
    partnership.noUtrReason,
    partnership.vatRegistrationNumber,
    partnership.payeReference,
    Some(partnership.haveMoreThanTenDirectorOrPartner),
    partnership.correspondenceAddressDetails,
    partnership.correspondenceContactDetails,
    partnership.previousAddressDetails.fold(PreviousAddressDetails(isPreviousAddressLast12Month = false))(c=>c),
    JsArray(partnership.partnerDetails.map(c=>Json.toJson(c)(Individual.individualUpdateWrites))))
  )
}

case class CompanyTrustee(
                           organizationName: String,
                           utr: Option[String] = None,
                           noUtrReason: Option[String] = None,
                           crnNumber: Option[String] = None,
                           noCrnReason: Option[String] = None,
                           vatRegistrationNumber: Option[String] = None,
                           payeReference: Option[String] = None,
                           correspondenceAddressDetails: CorrespondenceAddressDetails,
                           correspondenceContactDetails: CorrespondenceContactDetails,
                           previousAddressDetails: Option[PreviousAddressDetails] = None
                         )

object CompanyTrustee {
  implicit val formats: Format[CompanyTrustee] = Json.format[CompanyTrustee]

  val updateWrites : Writes[CompanyTrustee] = (
    (JsPath \ "organisationName").write[String] and
      (JsPath \ "utr").writeNullable[String] and
      (JsPath \ "noUtrReason").writeNullable[String] and
      (JsPath \ "crnNumber").writeNullable[String] and
      (JsPath \ "noCrnReason").writeNullable[String] and
      (JsPath \ "vatRegistrationNumber").writeNullable[String] and
      (JsPath \ "payeReference").writeNullable[String] and
      (JsPath \ "correspondenceAddressDetails").write[CorrespondenceAddressDetails](CorrespondenceAddressDetails.updateWrites) and
      (JsPath \ "correspondenceContactDetails").write[CorrespondenceContactDetails] and
      (JsPath \ "previousAddressDetails").write[PreviousAddressDetails](PreviousAddressDetails.psaUpdateWrites)
    )(company => (company.organizationName,
    company.utr,
    company.noUtrReason,
    company.crnNumber,
    company.noCrnReason,
    company.vatRegistrationNumber,
    company.payeReference,
    company.correspondenceAddressDetails,
    company.correspondenceContactDetails,
    company.previousAddressDetails.fold(PreviousAddressDetails(isPreviousAddressLast12Month = false))(c=>c))
  )
}

case class PartnershipTrustee(
                               organizationName: String,
                               utr: Option[String] = None,
                               noUtrReason: Option[String] = None,
                               vatRegistrationNumber: Option[String] = None,
                               payeReference: Option[String] = None,
                               correspondenceAddressDetails: CorrespondenceAddressDetails,
                               correspondenceContactDetails: CorrespondenceContactDetails,
                               previousAddressDetails: Option[PreviousAddressDetails] = None
                             )

case class TrusteeDetails(
                           individualTrusteeDetail: Seq[Individual],
                           companyTrusteeDetail: Seq[CompanyTrustee],
                           partnershipTrusteeDetail: Seq[PartnershipTrustee]
                         )
object TrusteeDetails {
  implicit val formats : Format[TrusteeDetails] = Json.format[TrusteeDetails]

  val updateWrites : Writes[TrusteeDetails] = (
    (JsPath \ "individualDetails").writeNullable[JsArray] and
      (JsPath \ "companyOrOrganisationDetails").writeNullable[JsArray] and
      (JsPath \ "partnershipDetails").writeNullable[JsArray]
    )(trustee => (
    if(trustee.individualTrusteeDetail.nonEmpty) Some(JsArray(trustee.individualTrusteeDetail.map(i=>Json.toJson(i)(Individual.establisherIndividualDetailsUpdateWrites)))) else None,
    if(trustee.companyTrusteeDetail.nonEmpty) Some(JsArray(trustee.companyTrusteeDetail.map(c=>Json.toJson(c)(CompanyTrustee.updateWrites)))) else None,
    if(trustee.partnershipTrusteeDetail.nonEmpty) Some(JsArray(trustee.partnershipTrusteeDetail.map(p=>Json.toJson(p)(Partnership.updateWrites)))) else None
  ))
}

case class EstablisherDetails(
                               individual: Seq[Individual],
                               companyOrOrganization: Seq[CompanyEstablisher],
                               partnership: Seq[Partnership]
                             )

object EstablisherDetails {
  implicit val formats : Format[EstablisherDetails] = Json.format[EstablisherDetails]

  val updateWrites : Writes[EstablisherDetails] = (
    (JsPath \ "individualDetails").writeNullable[JsArray] and
      (JsPath \ "companyOrOrganisationDetails").writeNullable[JsArray] and
      (JsPath \ "partnershipDetails").writeNullable[JsArray]
  )(establishers => (
    if(establishers.individual.nonEmpty) Some(JsArray(establishers.individual.map(i=>Json.toJson(i)(Individual.establisherIndividualDetailsUpdateWrites)))) else None,
    if(establishers.companyOrOrganization.nonEmpty) Some(JsArray(establishers.companyOrOrganization.map(c=>Json.toJson(c)(CompanyEstablisher.updateWrites)))) else None,
    if(establishers.partnership.nonEmpty) Some(JsArray(establishers.partnership.map(p=>Json.toJson(p)(Partnership.updateWrites)))) else None
  ))
}

case class PensionsScheme(customerAndSchemeDetails: CustomerAndSchemeDetails, pensionSchemeDeclaration: PensionSchemeDeclaration,
                          establisherDetails: EstablisherDetails, trusteeDetails: TrusteeDetails, isEstablisherOrTrusteeDetailsChanged: Option[Boolean] = None)

object PensionsScheme {

  implicit val formatsIndividual: Format[Individual] = Json.format[Individual]
  implicit val formatsCompanyEstablisher: Format[CompanyEstablisher] = Json.format[CompanyEstablisher]
  implicit val formatsPartnershipEstablisher: Format[Partnership] = Json.format[Partnership]
  implicit val formatsCompanyTrustee: Format[CompanyTrustee] = Json.format[CompanyTrustee]
  implicit val formatsPartnershipTrustee: Format[PartnershipTrustee] = Json.format[PartnershipTrustee]

  implicit val formatsTrusteeDetails: Format[TrusteeDetails] = Json.format[TrusteeDetails]
  implicit val formatsEstablisherDetails: Format[EstablisherDetails] = Json.format[EstablisherDetails]
  implicit val formatsPensionsScheme: Format[PensionsScheme] = Json.format[PensionsScheme]

  val pensionSchemeHaveInvalidBank: Lens[PensionsScheme, Boolean] = new Lens[PensionsScheme, Boolean] {
    override def get: PensionsScheme => Boolean = pensionsScheme => pensionsScheme.customerAndSchemeDetails.haveInvalidBank

    override def set: (PensionsScheme, Boolean) => PensionsScheme =
      (pensionsScheme, haveInvalidBank) =>
        pensionsScheme.copy(
          customerAndSchemeDetails =
            pensionsScheme.customerAndSchemeDetails.copy(haveInvalidBank = haveInvalidBank)
        )
  }

}
