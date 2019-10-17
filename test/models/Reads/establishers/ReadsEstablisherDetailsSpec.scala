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

package models.Reads.establishers

import models.ReadsEstablisherDetails._
import models._
import org.scalatest.{Assertion, FlatSpec, Matchers}
import play.api.libs.json._

class ReadsEstablisherDetailsSpec extends FlatSpec with Matchers {

  import ReadsEstablisherDetailsSpec._

  "ReadsEstablisherDetails" should "read a company with minimal details" in {

    establisherCompanyTest(
      (CompanyEstablisherBuilder()
        .build(), true, false))

  }

  it should "read multiple companies" in {

    establisherTest(
      Nil,
      (1 to 3).map(
        _ =>
          (CompanyEstablisherBuilder()
            .build(), false, false)
      ),
      Nil,
      Nil,
      Nil
    )

  }

  it should "read multiple companies without deleted ones" in {

    establisherTest(
      Nil,
      (1 to 3).map(
        _ =>
          (CompanyEstablisherBuilder()
            .build(), false, false)
      ) :+ ((CompanyEstablisherBuilder()
        .build(), true, false)),
      Nil,
      Nil,
      Nil
    )

  }

  it should "read a company with a UTR" in {

    establisherCompanyTest(
      (CompanyEstablisherBuilder()
        .withUtr()
        .build(), false, false)
    )

  }

  it should "read a company with a CRN" in {

    establisherCompanyTest(
      (CompanyEstablisherBuilder()
        .withCrn()
        .build(), false, true)
    )

  }

  it should "read a company with a VAT number" in {

    establisherCompanyTest(
      (CompanyEstablisherBuilder()
        .withVat()
        .build(), false, true)
    )
  }

  it should "read a company with a PAYE reference" in {

    establisherCompanyTest(
      (CompanyEstablisherBuilder()
        .withPaye()
        .build(), false, true)
    )
  }

  it should "read a company with Other Directors true" in {

    establisherCompanyTest(
      (CompanyEstablisherBuilder()
        .withOtherDirectors(true)
        .build(), false, false)
    )

  }

  it should "read a company with a previous address" in {

    establisherCompanyTest(
      (CompanyEstablisherBuilder()
        .withPreviousAddress()
        .build(), false, false)
    )

  }

  it should "read a company wth a director with minimal details" in {

    companyDirectorTest(
      IndividualBuilder()
        .build()
    )

  }

  it should "read a company wth a director with a Nino" in {

    companyDirectorTest(
      IndividualBuilder()
        .withNino()
        .build()
    )
  }

  it should "read a company wth a director with a UTR" in {

    companyDirectorTest(
      IndividualBuilder()
        .withUtr()
        .build()
    )

  }

  it should "read a company wth a director with a previous address" in {

    companyDirectorTest(
      IndividualBuilder()
        .withPreviousAddress()
        .build()
    )

  }

  it should "read multiple companies with multiple directors" in {

    establisherTest(
      Nil,
      (1 to 3).map {
        i =>
          (CompanyEstablisherBuilder()
            .withDirectors(
              (1 to i).map {
                _ => IndividualBuilder().build()
              }
            )
            .build(), false, false)
      },
      Nil,
      Nil,
      Nil
    )

  }

  it should "read multiple directors without deleted ones" in {

    deletedDirectorsTest(
      (1 to 3).map { _ =>
        (IndividualBuilder().build(), true, false)
      } :+ ((IndividualBuilder().build(), false, false))
    )
  }

  it should "read a partnership with minimal details" in {

    establisherPartnershipTest(
      (PartnershipBuilder()
        .build(), true, false)
    )

  }

  it should "read multiple partnerships" in {

    establisherTest(
      Nil,
      Nil,
      (1 to 3).map(
        _ =>
          (PartnershipBuilder()
            .build(), false, false)
      ),
      Nil,
      Nil
    )

  }

  it should "read multiple partnerships without deleted ones" in {

    establisherTest(
      Nil,
      Nil,
      (1 to 3).map(
        _ =>
          (PartnershipBuilder()
            .build(), false, false)
      ) :+ ((PartnershipBuilder()
        .build(), true, false)),
      Nil,
      Nil
    )

  }

  it should "read a partnership with a UTR" in {

    establisherPartnershipTest(
      (PartnershipBuilder()
        .withUtr()
        .build(), false, false)
    )

  }

  it should "read a partnership with a VAT number" in {

    establisherPartnershipTest(
      (PartnershipBuilder()
        .withVat()
        .build(), false, true)
    )
  }

  it should "read a partnership with a PAYE reference" in {

    establisherPartnershipTest(
      (PartnershipBuilder()
        .withPaye()
        .build(), false, false)
    )

  }

  it should "read a partnership with Other Directors true" in {

    establisherPartnershipTest(
      (PartnershipBuilder()
        .withOtherPartners(true)
        .build(), false, false)
    )

  }

  it should "read a partnership with a previous address" in {

    establisherPartnershipTest(
      (PartnershipBuilder()
        .withPreviousAddress()
        .build(), false, false)
    )

  }

  it should "read a partnership wth a partner with minimal details" in {

    partnerTest(
      IndividualBuilder()
        .build()
    )
  }

  it should "read a partnership wth a partner with a Nino" in {

    partnerTest(
      IndividualBuilder()
        .withNino()
        .build()
    )
  }

  it should "read a partnership wth a partner with a UTR" in {

    partnerTest(
      IndividualBuilder()
        .withUtr()
        .build()
    )

  }

  it should "read a partnership wth a partner with a previous address" in {

    partnerTest(
      IndividualBuilder()
        .withPreviousAddress()
        .build()
    )

  }

  it should "read multiple partnerships with multiple partners" in {

    establisherTest(
      Nil,
      Nil,
      (1 to 3).map {
        i =>
          (PartnershipBuilder()
            .withPartners(
              (1 to i).map {
                _ => IndividualBuilder().build()
              }
            )
            .build(), false, false)
      },
      Nil,
      Nil
    )

  }

  it should "read multiple partners without deleted ones" in {

    deletedPartnersTest(
      (1 to 3).map { _ =>
        (IndividualBuilder().build(), true, false)
      } :+ ((IndividualBuilder().build(), false, false))
    )
  }

  it should "read an individual with minimal details" in {

    establisherIndividualTest(
      (IndividualBuilder()
        .build(), false, false)
    )

  }

  it should "read multiple individuals" in {

    establisherTest(
      (1 to 3).map { _ =>
        (IndividualBuilder().build(), false, false)
      },
      Nil,
      Nil,
      Nil,
      Nil
    )
  }

  it should "read multiple individuals without the deleted ones" in {

    establisherTest(
      (1 to 3).map { _ =>
        (IndividualBuilder().build(), false, false)
      } :+ ((IndividualBuilder()
        .build(), true, false)),
      Nil,
      Nil,
      Nil,
      Nil
    )

  }

  it should "read an individual with a Nino" in {

    establisherIndividualTest(
      (IndividualBuilder()
        .withNino()
        .build(), false, true)
    )

  }

  it should "read an individual with a UTR" in {

    establisherIndividualTest(
      (IndividualBuilder()
        .withUtr()
        .build(), false, false)
    )

  }

  it should "read an individual with a previous address" in {

    establisherIndividualTest(
      (IndividualBuilder()
        .withPreviousAddress()
        .build(), false, false)
    )

  }

  it should "read a mix of company, partnership and individual establishers without the deleted ones" in {

    establisherTest(
      (1 to 2).map(_ =>
        (IndividualBuilder().build(), false, false)
      ) :+ ((IndividualBuilder().build(), true, false)),
      (1 to 3).map(_ =>
        (CompanyEstablisherBuilder().build(), false, false)
      ) :+ ((CompanyEstablisherBuilder().build(), true, false)),
      (1 to 3).map(_ =>
        (PartnershipBuilder().build(), false, false)
      ) :+ ((PartnershipBuilder().build(), true, false)),
      Nil,
      Nil
    )

  }

  it should "read a trustee company" in {

    trusteeCompanyTest(
      (CompanyTrusteeBuilder()
        .build(), false, false)
    )

  }

  it should "read multiple trustee companies" in {

    establisherTest(
      Nil,
      Nil,
      Nil,
      Nil,
      (1 to 3).map {
        _ =>
          (CompanyTrusteeBuilder()
            .build(), false, false)
      }
    )

  }

  it should "read multiple trustee companies without the deleted ones" in {

    establisherTest(
      Nil,
      Nil,
      Nil,
      Nil,
      (1 to 3).map {
        _ =>
          (CompanyTrusteeBuilder()
            .build(), false, false)
      } :+ ((CompanyTrusteeBuilder()
        .build(), true, false))
    )

  }

  it should "read a trustee company with a UTR" in {

    trusteeCompanyTest(
      (CompanyTrusteeBuilder()
        .withUtr()
        .build(), false, false)
    )

  }

  it should "read a trustee company with a CRN" in {

    trusteeCompanyTest(
      (CompanyTrusteeBuilder()
        .withCrn()
        .build(), false, true)
    )
  }

  it should "read a trustee company with a VAT number" in {

    trusteeCompanyTest(
      (CompanyTrusteeBuilder()
        .withVat()
        .build(), false, true)
    )
  }

  it should "read a trustee company with a PAYE reference" in {

    trusteeCompanyTest(
      (CompanyTrusteeBuilder()
        .withPaye()
        .build(), false, true)
    )

  }

  it should "read a trustee company with a previous address" in {

    trusteeCompanyTest(
      (CompanyTrusteeBuilder()
        .withPreviousAddress()
        .build(), false, false)
    )

  }

  it should "read a trustee invidual with minimal details" in {

    trusteeIndividualTest(
      (IndividualBuilder()
        .build(), false, false)
    )

  }

  it should "read multiple trustee individuals" in {

    establisherTest(
      Nil,
      Nil,
      Nil,
      (1 to 3).map(
        _ =>
          (IndividualBuilder()
            .build(), false, false)
      ),
      Nil
    )

  }

  it should "read a trustee individual with a Nino" in {

    trusteeIndividualTest(
      (IndividualBuilder()
        .withNino()
        .build(), false, true)
    )

  }

  it should "read a trustee individual with a UTR" in {

    trusteeIndividualTest(
      (IndividualBuilder()
        .withUtr()
        .build(), false, false)
    )

  }

  it should "read a trustee individual with a previous address" in {

    trusteeIndividualTest(
      (IndividualBuilder()
        .withPreviousAddress()
        .build(), false, false)
    )

  }

  it should "read a mix of company and individual trustees" in {

    establisherTest(
      Nil,
      Nil,
      Nil,
      (1 to 2).map(_ => (IndividualBuilder().build(), false, false)),
      (1 to 3).map(_ => (CompanyTrusteeBuilder().build(), false, false))
    )

  }

  it should "read when there are no establishers or trustees" in {

    establisherTest(Nil, Nil, Nil, Nil, Nil)

  }

  it should "read a mix of company, individual and partneship establishers and company and individual trustees" in {

    establisherTest(
      (1 to 2).map(_ => (IndividualBuilder().build(), false, false)),
      (1 to 3).map(
        i =>
          (CompanyEstablisherBuilder()
            .withDirectors(
              (1 to i).map(_ => IndividualBuilder().build())
            )
            .build(), false, false)
      ),
      (1 to 3).map(
        i =>
          (PartnershipBuilder()
            .withPartners(
              (1 to i).map(_ => IndividualBuilder().build())
            )
            .build(), false, false)
      ),
      (1 to 3).map(_ => (IndividualBuilder().build(), false, false)),
      (1 to 4).map(_ => (CompanyTrusteeBuilder().build(), false, false))
    )

  }

  it should "read when neither the establishers nor trustees elements are present" in {

    establisherTest(Nil, Nil, Nil, Nil, Nil, Nil, Json.obj())

  }

  "PreviousAddressDetails" should "return an appropriate value for different combinations of address years, trading time and previous address" in {

    previousAddressDetails("over_a_year", None, None) shouldBe None
    previousAddressDetails("under_a_year", None, Some(false)) shouldBe None
    previousAddressDetails("under_a_year", Some(previousAddress), None) shouldBe Some(PreviousAddressDetails(true, Some(previousAddress)))
    previousAddressDetails("under_a_year", Some(previousAddress), Some(true)) shouldBe Some(PreviousAddressDetails(true, Some(previousAddress)))
  }

}

object ReadsEstablisherDetailsSpec extends Matchers {

  val previousAddress: Address = UkAddress("addressLine 1", Some("addressLine2"), None, None, "GB", "ZZ11ZZ")

  import EstablishersTestJson._

  def establisherIndividualTest(establisher: (Individual, Boolean, Boolean)): Assertion =
    establisherTest(Seq(establisher), Nil, Nil, Nil, Nil)

  def establisherCompanyTest(establisher: (CompanyEstablisher, Boolean, Boolean)): Assertion =
    establisherTest(Nil, Seq(establisher), Nil, Nil, Nil)

  def establisherPartnershipTest(establisher: (Partnership, Boolean, Boolean)): Assertion =
    establisherTest(Nil, Nil, Seq(establisher), Nil, Nil)

  def companyDirectorTest(director: Individual): Assertion = {

    establisherCompanyTest(
      (CompanyEstablisherBuilder()
        .withDirectors(Seq(director))
        .build(), false, true)
    )

  }

  def partnerTest(partner: Individual): Assertion = {

    establisherPartnershipTest(
      (PartnershipBuilder()
        .withPartners(Seq(partner))
        .build(), false, true)
    )

  }

  def deletedDirectorsTest(directors: Seq[(Individual, Boolean, Boolean)]): Assertion = {
    val allDirectorsJson = Json.obj(
      "director" -> toJsonArray(directors, companyDirectorJson)
    )
    val companyEstablisher = Seq((CompanyEstablisherBuilder().withDirectors(directors.filterNot(_._2).map(_._1)).build(), false, false))
    val establishers = toJsonArray(companyEstablisher, establisherCompany).value.map(_.as[JsObject] ++ allDirectorsJson)

    val json: JsValue = Json.obj(
      "establishers" -> establishers
    )

    val expectedEstablishers = EstablisherDetails(
      individual = Nil,
      companyOrOrganization = companyEstablisher.map(_._1),
      partnership = Nil
    )
    json.validate(readsEstablisherDetails).fold(
      errors => fail(s"JSON errors: $errors"),
      actual => actual shouldBe expectedEstablishers
    )
  }

  def deletedPartnersTest(partners: Seq[(Individual, Boolean, Boolean)]): Assertion = {
    val allPartnersJson = Json.obj(
      "partner" -> toJsonArray(partners, partnerJson)
    )
    val partnershipEstablisher = Seq((PartnershipBuilder().withPartners(partners.filterNot(_._2).map(_._1)).build(), false, false))
    val establishers = toJsonArray(partnershipEstablisher, partnership).value.map(_.as[JsObject] ++ allPartnersJson)

    val json: JsValue = Json.obj(
      "establishers" -> establishers
    )

    val expectedEstablishers = EstablisherDetails(
      individual = Nil,
      companyOrOrganization = Nil,
      partnership = partnershipEstablisher.map(_._1)
    )
    json.validate(readsEstablisherDetails).fold(
      errors => fail(s"JSON errors: $errors"),
      actual => actual shouldBe expectedEstablishers
    )
  }

  def trusteeIndividualTest(trustee: (Individual, Boolean, Boolean)): Assertion =
    establisherTest(Nil, Nil, Nil, Seq(trustee), Nil)

  def trusteeCompanyTest(trustee: (CompanyTrustee, Boolean, Boolean)): Assertion =
    establisherTest(Nil, Nil, Nil, Nil, Seq(trustee))

  def establisherTest(establisherIndividuals: Seq[(Individual, Boolean, Boolean)],
                      establisherCompanies: Seq[(CompanyEstablisher, Boolean, Boolean)],
                      establisherPartnerships: Seq[(Partnership, Boolean, Boolean)],
                      trusteeIndividuals: Seq[(Individual, Boolean, Boolean)],
                      trusteeCompanies: Seq[(CompanyTrustee, Boolean, Boolean)]
                     ): Assertion = {

    val establishers: JsArray =
      toJsonArray(establisherIndividuals, establisherIndividualJson) ++
        toJsonArray(establisherCompanies, establisherCompany) ++
        toJsonArray(establisherPartnerships, partnership)

    val trustees =
      toJsonArray(trusteeIndividuals, trusteeIndividualJson) ++
        toJsonArray(trusteeCompanies, trusteeCompanyJson)

    val json: JsValue = Json.obj(
      "establishers" -> establishers,
      "trustees" -> trustees
    )

    establisherTest(establisherIndividuals.filterNot(_._2).map(_._1),
      establisherCompanies.filterNot(_._2).map(_._1),
      establisherPartnerships.filterNot(_._2).map(_._1),
      trusteeIndividuals.filterNot(_._2).map(_._1),
      trusteeCompanies.filterNot(_._2).map(_._1), Nil, json
    )

  }

  def establisherTest(establisherIndividuals: Seq[Individual],
                      establisherCompanies: Seq[CompanyEstablisher],
                      establisherPartnerships: Seq[Partnership],
                      trusteeIndividuals: Seq[Individual],
                      trusteeCompanies: Seq[CompanyTrustee],
                      trusteePartnerships: Seq[PartnershipTrustee] = Nil,
                      json: JsValue
                     ): Assertion = {

    val expectedEstablishers = EstablisherDetails(
      individual = establisherIndividuals,
      companyOrOrganization = establisherCompanies,
      partnership = establisherPartnerships
    )

    json.validate(readsEstablisherDetails).fold(
      errors => fail(s"JSON errors: $errors"),
      actual => actual shouldBe expectedEstablishers
    )

    val expectedTrustees = TrusteeDetails(
      individualTrusteeDetail = trusteeIndividuals,
      companyTrusteeDetail = trusteeCompanies,
      partnershipTrusteeDetail = trusteePartnerships
    )

    json.validate(readsTrusteeDetails).fold(
      errors => fail(s"JSON errors: $errors"),
      actual => actual shouldBe expectedTrustees
    )

  }

  private def toJsonArray[T](ts: Seq[(T, Boolean, Boolean)], f: (T, Boolean) => JsValue): JsArray = {
    ts.foldLeft(Json.arr())((json, t) => json :+ f(t._1, t._2))
  }

}
