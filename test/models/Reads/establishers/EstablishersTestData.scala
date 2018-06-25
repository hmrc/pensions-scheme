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

package models.Reads.establishers

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import models._

import scala.util.Random

//scalastyle:off magic.number
object EstablishersTestData {

  private def alphanumeric(length: Int) =
    Random.alphanumeric.take(length).mkString

  private def numeric(length: Int) =
    (1 to length).foldLeft("")((s, _) => s + Random.nextInt(10).toString)

  private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  private def date: String =
    LocalDate.of(1950, 1, 1).plusDays(Random.nextInt(365 * 50)).format(dateFormatter)

  private def testAddress = {
    Random.nextInt(2) match {
      case 0 =>
        UkAddress(
          Random.alphanumeric.take(35).mkString,
          Some(alphanumeric(35)),
          Some(alphanumeric(35)),
          Some(alphanumeric(35)),
          "GB",
          alphanumeric(10)
        )
      case _ =>
        InternationalAddress(
          Random.alphanumeric.take(35).mkString,
          Some(alphanumeric(35)),
          Some(alphanumeric(35)),
          Some(alphanumeric(35)),
          "ES",
          Some(alphanumeric(10))
        )
    }
  }

  private def testCompanyName =
    alphanumeric(35)

  private def testEmail =
    alphanumeric(20) + "@test.net"

  def testIndividual(
      hasNino: Boolean,
      hasUtr: Boolean,
      hasPreviousAddress: Boolean
  ): Individual =
    Individual(
      personalDetails =
        PersonalDetails(
          firstName = alphanumeric(35),
          middleName = Some(alphanumeric(35)),
          lastName = alphanumeric(35),
          dateOfBirth = date
        ),
      referenceOrNino = if (hasNino) Some(alphanumeric(10)) else None,
      noNinoReason = if (hasNino) None else Some(alphanumeric(20)),
      utr = if (hasUtr) Some(alphanumeric(10)) else None,
      noUtrReason = if (hasUtr) None else Some(alphanumeric(20)),
      correspondenceAddressDetails = CorrespondenceAddressDetails(testAddress),
      correspondenceContactDetails = CorrespondenceContactDetails(
        ContactDetails(
          telephone = numeric(24),
          email = testEmail
        )
      ),
      previousAddressDetails =
        if (hasPreviousAddress) {
          Some(
            PreviousAddressDetails(
              isPreviousAddressLast12Month = true,
              previousAddressDetails = Some(testAddress)
            )
          )
        }
        else {
          None
        }
    )

  def testCompanyEstablisher(
      hasUtr: Boolean,
      hasCrn: Boolean,
      hasVat: Boolean,
      hasPaye: Boolean,
      hasPreviousAddress: Boolean,
      directors: Seq[Individual],
      otherDirectors: Option[Boolean]
  ): CompanyEstablisher =
    CompanyEstablisher(
      organizationName = testCompanyName,
      utr = if (hasUtr) Some(alphanumeric(10)) else None,
      noUtrReason = if (hasUtr) None else Some(alphanumeric(20)),
      crnNumber = if (hasCrn) Some(alphanumeric(10)) else None,
      noCrnReason = if (hasCrn) None else Some(alphanumeric(20)),
      vatRegistrationNumber = if (hasVat) Some(numeric(9)) else None,
      payeReference = if (hasPaye) Some(alphanumeric(13)) else None,
      haveMoreThanTenDirectorOrPartner = otherDirectors.getOrElse(false),
      correspondenceAddressDetails = CorrespondenceAddressDetails(testAddress),
      correspondenceContactDetails = CorrespondenceContactDetails(
        ContactDetails(
          telephone = numeric(24),
          email = testEmail
        )
      ),
      previousAddressDetails =
        if (hasPreviousAddress) {
          Some(
            PreviousAddressDetails(
              isPreviousAddressLast12Month = true,
              previousAddressDetails = Some(testAddress)
            )
          )
        }
        else {
          None
        },
      directorDetails = directors
    )

  def testCompanyTrustee(
      hasUtr: Boolean,
      hasCrn: Boolean,
      hasVat: Boolean,
      hasPaye: Boolean,
      hasPreviousAddress: Boolean
  ): CompanyTrustee =
    CompanyTrustee(
      organizationName = testCompanyName,
      utr = if (hasUtr) Some(alphanumeric(10)) else None,
      noUtrReason = if (hasUtr) None else Some(alphanumeric(20)),
      crnNumber = if (hasCrn) Some(alphanumeric(10)) else None,
      noCrnReason = if (hasCrn) None else Some(alphanumeric(20)),
      vatRegistrationNumber = if (hasVat) Some(numeric(9)) else None,
      payeReference = if (hasPaye) Some(alphanumeric(13)) else None,
      correspondenceAddressDetails = CorrespondenceAddressDetails(testAddress),
      correspondenceContactDetails = CorrespondenceContactDetails(
        ContactDetails(
          telephone = numeric(24),
          email = testEmail
        )
      ),
      previousAddressDetails =
        if (hasPreviousAddress) {
          Some(
            PreviousAddressDetails(
              isPreviousAddressLast12Month = true,
              previousAddressDetails = Some(testAddress)
            )
          )
        }
        else {
          None
        }
    )

}

case class IndividualBuilder(
    hasNino: Boolean,
    hasUtr: Boolean,
    hasPreviousAddress: Boolean
) {

  import EstablishersTestData._

  def withNino(): IndividualBuilder =
    copy(hasNino = true)

  def withUtr(): IndividualBuilder =
    copy(hasUtr = true)

  def withPreviousAddress(): IndividualBuilder =
    copy(hasPreviousAddress = true)

  def build(): Individual =
    testIndividual(
      hasNino,
      hasUtr,
      hasPreviousAddress
    )

}

object IndividualBuilder {

  def apply(): IndividualBuilder =
    IndividualBuilder(false, false, false)

}

case class CompanyEstablisherBuilder(
    hasUtr: Boolean,
    hasCrn: Boolean,
    hasVat: Boolean,
    hasPaye: Boolean,
    hasPreviousAddress: Boolean,
    directors: Seq[Individual],
    otherDirectors: Option[Boolean]
) {

  import EstablishersTestData._

  def withUtr(): CompanyEstablisherBuilder =
    copy(hasUtr = true)

  def withCrn(): CompanyEstablisherBuilder =
    copy(hasCrn = true)

  def withVat(): CompanyEstablisherBuilder =
    copy(hasVat = true)

  def withPaye(): CompanyEstablisherBuilder =
    copy(hasPaye = true)

  def withPreviousAddress(): CompanyEstablisherBuilder =
    copy(hasPreviousAddress = true)

  def withDirectors(directors: Seq[Individual]): CompanyEstablisherBuilder =
    copy(directors = directors)

  def withOtherDirectors(otherDirectors: Boolean): CompanyEstablisherBuilder =
    copy(otherDirectors = Some(otherDirectors))

  def build(): CompanyEstablisher =
    testCompanyEstablisher(
      hasUtr,
      hasCrn,
      hasVat,
      hasPaye,
      hasPreviousAddress,
      directors,
      otherDirectors
    )

}

object CompanyEstablisherBuilder {

  def apply(): CompanyEstablisherBuilder =
    CompanyEstablisherBuilder(false, false, false, false, false, Nil, None)

}

case class CompanyTrusteeBuilder(
  hasUtr: Boolean,
  hasCrn: Boolean,
  hasVat: Boolean,
  hasPaye: Boolean,
  hasPreviousAddress: Boolean
) {

  import EstablishersTestData._

  def withUtr(): CompanyTrusteeBuilder =
    copy(hasUtr = true)

  def withCrn(): CompanyTrusteeBuilder =
    copy(hasCrn = true)

  def withVat(): CompanyTrusteeBuilder =
    copy(hasVat = true)

  def withPaye(): CompanyTrusteeBuilder =
    copy(hasPaye = true)

  def withPreviousAddress(): CompanyTrusteeBuilder =
    copy(hasPreviousAddress = true)

  def build(): CompanyTrustee =
    testCompanyTrustee(
      hasUtr,
      hasCrn,
      hasVat,
      hasPaye,
      hasPreviousAddress
    )

}

object CompanyTrusteeBuilder {

  def apply(): CompanyTrusteeBuilder =
    CompanyTrusteeBuilder(false, false, false, false, false)

}
