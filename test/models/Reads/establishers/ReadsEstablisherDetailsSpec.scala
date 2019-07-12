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
        .build(), true, false),
      false)

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
      Nil,
      false
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
      Nil,
      false
    )

  }

  it should "read a company with a UTR" in {

    establisherCompanyTest(
      (CompanyEstablisherBuilder()
        .withUtr()
        .build(), false, false),
      false
    )

  }

  it should "read a company with a CRN when toggle(separate-ref-collection) off" in {

    establisherCompanyTest(
      (CompanyEstablisherBuilder()
        .withCrn()
        .build(), false, false),
      false
    )

  }

  it should "read a company with a CRN when toggle(separate-ref-collection) on" in {

    establisherCompanyTest(
      (CompanyEstablisherBuilder()
        .withCrn()
        .build(), false, true),
      true
    )

  }

  it should "read a company with a VAT number when toggle(separate-ref-collection) off" in {

    establisherCompanyTest(
      (CompanyEstablisherBuilder()
        .withVat()
        .build(), false, false),
      false
    )
  }

  it should "read a company with a VAT number when toggle(separate-ref-collection) on" in {

    establisherCompanyTest(
      (CompanyEstablisherBuilder()
        .withVat()
        .build(), false, true),
      true
    )
  }

  it should "read a company with a PAYE reference when toggle(separate-ref-collection) off" in {

    establisherCompanyTest(
      (CompanyEstablisherBuilder()
        .withPaye()
        .build(), false, false),
      false
    )
  }

  it should "read a company with a PAYE reference when toggle(separate-ref-collection) on" in {

    establisherCompanyTest(
      (CompanyEstablisherBuilder()
        .withPaye()
        .build(), false, true),
      true
    )
  }

  it should "read a company with Other Directors true" in {

    establisherCompanyTest(
      (CompanyEstablisherBuilder()
        .withOtherDirectors(true)
        .build(), false, false),
      false
    )

  }

  it should "read a company with a previous address" in {

    establisherCompanyTest(
      (CompanyEstablisherBuilder()
        .withPreviousAddress()
        .build(), false, false),
      false
    )

  }

  it should "read a company wth a director with minimal details" in {

    companyDirectorTest(
      IndividualBuilder()
        .build(),
      false
    )

  }

  it should "read a company wth a director with a Nino when toggle(separate-ref-collection) off" in {

    companyDirectorTest(
      IndividualBuilder()
        .withNino()
        .build(),
      false
    )
  }

  it should "read a company wth a director with a Nino when toggle(separate-ref-collection) on" in {

    companyDirectorTest(
      IndividualBuilder()
        .withNino()
        .build(),
      true
    )
  }

  it should "read a company wth a director with a UTR" in {

    companyDirectorTest(
      IndividualBuilder()
        .withUtr()
        .build(),
      false
    )

  }

  it should "read a company wth a director with a previous address" in {

    companyDirectorTest(
      IndividualBuilder()
        .withPreviousAddress()
        .build(),
      false
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
      Nil,
      false
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
        .build(), true, false),
      false
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
      Nil,
      false
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
      Nil,
      false
    )

  }

  it should "read a partnership with a UTR" in {

    establisherPartnershipTest(
      (PartnershipBuilder()
        .withUtr()
        .build(), false, false),
      false
    )

  }

  it should "read a partnership with a VAT number when toggle(separate-ref-collection) off" in {

    establisherPartnershipTest(
      (PartnershipBuilder()
        .withVat()
        .build(), false, false),
      false
    )
  }

  it should "read a partnership with a VAT number when toggle(separate-ref-collection) on" in {

    establisherPartnershipTest(
      (PartnershipBuilder()
        .withVat()
        .build(), false, true),
      true
    )
  }

  it should "read a partnership with a PAYE reference" in {

    establisherPartnershipTest(
      (PartnershipBuilder()
        .withPaye()
        .build(), false, false),
      false
    )

  }

  it should "read a partnership with Other Directors true" in {

    establisherPartnershipTest(
      (PartnershipBuilder()
        .withOtherPartners(true)
        .build(), false, false),
      false
    )

  }

  it should "read a partnership with a previous address" in {

    establisherPartnershipTest(
      (PartnershipBuilder()
        .withPreviousAddress()
        .build(), false, false),
      false
    )

  }

  it should "read a partnership wth a partner with minimal details" in {

    partnerTest(
      IndividualBuilder()
        .build()
    )

  }

  it should "read a partnership wth a partner with a Nino when toggle(separate-ref-collection) off" in {

    partnerTest(
      IndividualBuilder()
        .withNino()
        .build()
    )
  }

  it should "read a partnership wth a partner with a Nino when toggle(separate-ref-collection) on" in {

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
      Nil,
      false
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
        .build(), false, false),
      false
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
      Nil,
      false
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
      Nil,
      false
    )

  }

  it should "read an individual with a Nino when toggle(separate-ref-collection) off" in {

    establisherIndividualTest(
      (IndividualBuilder()
        .withNino()
        .build(), false, false),
      false
    )

  }

  it should "read an individual with a Nino when toggle(separate-ref-collection) on" in {

    establisherIndividualTest(
      (IndividualBuilder()
        .withNino()
        .build(), false, true),
      true
    )

  }

  it should "read an individual with a UTR" in {

    establisherIndividualTest(
      (IndividualBuilder()
        .withUtr()
        .build(), false, false),
      false
    )

  }

  it should "read an individual with a previous address" in {

    establisherIndividualTest(
      (IndividualBuilder()
        .withPreviousAddress()
        .build(), false, false),
      false
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
      Nil,
      false
    )

  }

  it should "read a trustee company" in {

    trusteeCompanyTest(
      (CompanyTrusteeBuilder()
        .build(), false, false),
      false
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
      },
      false
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
        .build(), true, false)),
      false
    )

  }

  it should "read a trustee company with a UTR" in {

    trusteeCompanyTest(
      (CompanyTrusteeBuilder()
        .withUtr()
        .build(), false, false),
      false
    )

  }

  it should "read a trustee company with a CRN when toggle(separate-ref-collection) off" in {

    trusteeCompanyTest(
      (CompanyTrusteeBuilder()
        .withCrn()
        .build(), false, false),
      false
    )
  }

  it should "read a trustee company with a CRN when toggle(separate-ref-collection) on" in {

    trusteeCompanyTest(
      (CompanyTrusteeBuilder()
        .withCrn()
        .build(), false, true),
      true
    )
  }

  it should "read a trustee company with a VAT number when toggle(separate-ref-collection) off" in {

    trusteeCompanyTest(
      (CompanyTrusteeBuilder()
        .withVat()
        .build(), false, false),
      false
    )
  }

  it should "read a trustee company with a VAT number when toggle(separate-ref-collection) on" in {

    trusteeCompanyTest(
      (CompanyTrusteeBuilder()
        .withVat()
        .build(), false, true),
      true
    )
  }

  it should "read a trustee company with a PAYE reference when toggle(separate-ref-collection) off" in {

    trusteeCompanyTest(
      (CompanyTrusteeBuilder()
        .withPaye()
        .build(), false, false),
      false
    )
  }

  it should "read a trustee company with a PAYE reference when toggle(separate-ref-collection) on" in {

    trusteeCompanyTest(
      (CompanyTrusteeBuilder()
        .withPaye()
        .build(), false, true),
      true
    )

  }

  it should "read a trustee company with a previous address" in {

    trusteeCompanyTest(
      (CompanyTrusteeBuilder()
        .withPreviousAddress()
        .build(), false, false),
      false
    )

  }

  it should "read a trustee invidual with minimal details" in {

    trusteeIndividualTest(
      (IndividualBuilder()
        .build(), false, false),
      false
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
      Nil,
      false
    )

  }

  it should "read a trustee individual with a Nino when toggle(separate-ref-collection) off" in {

    trusteeIndividualTest(
      (IndividualBuilder()
        .withNino()
        .build(), false, false),
      false
    )

  }

  it should "read a trustee individual with a Nino when toggle(separate-ref-collection) on" in {

    trusteeIndividualTest(
      (IndividualBuilder()
        .withNino()
        .build(), false, true),
      true
    )

  }

  it should "read a trustee individual with a UTR" in {

    trusteeIndividualTest(
      (IndividualBuilder()
        .withUtr()
        .build(), false, false),
      false
    )

  }

  it should "read a trustee individual with a previous address" in {

    trusteeIndividualTest(
      (IndividualBuilder()
        .withPreviousAddress()
        .build(), false, false),
      false
    )

  }

  it should "read a mix of company and individual trustees" in {

    establisherTest(
      Nil,
      Nil,
      Nil,
      (1 to 2).map(_ => (IndividualBuilder().build(), false, false)),
      (1 to 3).map(_ => (CompanyTrusteeBuilder().build(), false, false)),
      false
    )

  }

  it should "read when there are no establishers or trustees" in {

    establisherTest(Nil, Nil, Nil, Nil, Nil, false)

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
      (1 to 4).map(_ => (CompanyTrusteeBuilder().build(), false, false)),
      false
    )

  }

  it should "read when neither the establishers nor trustees elements are present" in {

    establisherTest(Nil, Nil, Nil, Nil, Nil, Nil, Json.obj(), false)

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

  def establisherIndividualTest(establisher: (Individual, Boolean, Boolean), isToggleOn: Boolean): Assertion =
    establisherTest(Seq(establisher), Nil, Nil, Nil, Nil, isToggleOn)

  def establisherCompanyTest(establisher: (CompanyEstablisher, Boolean, Boolean), isToggleOn: Boolean): Assertion =
    establisherTest(Nil, Seq(establisher), Nil, Nil, Nil, isToggleOn)

  def establisherPartnershipTest(establisher: (Partnership, Boolean, Boolean), isToggleOn: Boolean): Assertion =
    establisherTest(Nil, Nil, Seq(establisher), Nil, Nil, isToggleOn)

  def companyDirectorTest(director: Individual, isToggleOn: Boolean): Assertion = {

    establisherCompanyTest(
      (CompanyEstablisherBuilder()
        .withDirectors(Seq(director))
        .build(), false, false)
      , isToggleOn
    )

  }

  def partnerTest(partner: Individual): Assertion = {

    establisherPartnershipTest(
      (PartnershipBuilder()
        .withPartners(Seq(partner))
        .build(), false, false),
      false
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
    json.validate(readsEstablisherDetails(false)).fold(
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
    json.validate(readsEstablisherDetails(false)).fold(
      errors => fail(s"JSON errors: $errors"),
      actual => actual shouldBe expectedEstablishers
    )
  }

  def trusteeIndividualTest(trustee: (Individual, Boolean, Boolean), isToggleOn: Boolean): Assertion =
    establisherTest(Nil, Nil, Nil, Seq(trustee), Nil, isToggleOn)

  def trusteeCompanyTest(trustee: (CompanyTrustee, Boolean, Boolean), isToggleOn: Boolean): Assertion =
    establisherTest(Nil, Nil, Nil, Nil, Seq(trustee), isToggleOn)

  def establisherTest(establisherIndividuals: Seq[(Individual, Boolean, Boolean)],
                      establisherCompanies: Seq[(CompanyEstablisher, Boolean, Boolean)],
                      establisherPartnerships: Seq[(Partnership, Boolean, Boolean)],
                      trusteeIndividuals: Seq[(Individual, Boolean, Boolean)],
                      trusteeCompanies: Seq[(CompanyTrustee, Boolean, Boolean)],
                      isToggleOn: Boolean
                     ): Assertion = {

    val establishers =
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
      trusteeCompanies.filterNot(_._2).map(_._1), Nil, json,
      isToggleOn
    )

  }

  def establisherTest(establisherIndividuals: Seq[Individual],
                      establisherCompanies: Seq[CompanyEstablisher],
                      establisherPartnerships: Seq[Partnership],
                      trusteeIndividuals: Seq[Individual],
                      trusteeCompanies: Seq[CompanyTrustee],
                      trusteePartnerships: Seq[PartnershipTrustee] = Nil,
                      json: JsValue,
                      isToggleOn: Boolean
                     ): Assertion = {

    val expectedEstablishers = EstablisherDetails(
      individual = establisherIndividuals,
      companyOrOrganization = establisherCompanies,
      partnership = establisherPartnerships
    )

    json.validate(readsEstablisherDetails(isToggleOn)).fold(
      errors => fail(s"JSON errors: $errors"),
      actual => actual shouldBe expectedEstablishers
    )

    val expectedTrustees = TrusteeDetails(
      individualTrusteeDetail = trusteeIndividuals,
      companyTrusteeDetail = trusteeCompanies,
      partnershipTrusteeDetail = trusteePartnerships
    )

    json.validate(readsTrusteeDetails(isToggleOn)).fold(
      errors => fail(s"JSON errors: $errors"),
      actual => actual shouldBe expectedTrustees
    )

  }

  private def toJsonArray[T](ts: Seq[(T, Boolean, Boolean)], f: (T, Boolean, Boolean) => JsValue): JsArray = {
    ts.foldLeft(Json.arr())((json, t) => json :+ f(t._1, t._2, t._3))
  }

}
