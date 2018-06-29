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

import models.ReadsEstablisherDetails._
import models._
import org.scalatest.{Assertion, FlatSpec, Matchers}
import play.api.libs.json._

class ReadsEstablisherDetailsSpec extends FlatSpec with Matchers {

  import ReadsEstablisherDetailsSpec._

  "ReadsEstablisherDetails" should "read a company with minimal details" in {

    establisherCompanyTest(
      (CompanyEstablisherBuilder()
        .build(), true))

  }

  it should "read multiple companies" in {

    establisherTest(
      Nil,
      (1 to 3).map(
        _ =>
          (CompanyEstablisherBuilder()
            .build(), false)
      ),
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
            .build(), false)
      ) :+ ((CompanyEstablisherBuilder()
        .build(), true)),
      Nil,
      Nil
    )

  }

  it should "read a company with a UTR" in {

    establisherCompanyTest(
      (CompanyEstablisherBuilder()
        .withUtr()
        .build(), false))

  }

  it should "read a company with a CRN" in {

    establisherCompanyTest(
      (CompanyEstablisherBuilder()
        .withCrn()
        .build(), false))

  }

  it should "read a company with a VAT number" in {

    establisherCompanyTest(
      (CompanyEstablisherBuilder()
        .withVat()
        .build(), false))

  }

  it should "read a company with a PAYE reference" in {

    establisherCompanyTest(
      (CompanyEstablisherBuilder()
        .withPaye()
        .build(), false))

  }

  it should "read a company with Other Directors true" in {

    establisherCompanyTest(
      (CompanyEstablisherBuilder()
        .withOtherDirectors(true)
        .build(), false))

  }

  it should "read a company with a previous address" in {

    establisherCompanyTest(
      (CompanyEstablisherBuilder()
        .withPreviousAddress()
        .build(), false))

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
            .build(), false)
      },
      Nil,
      Nil
    )

  }

  it should "read multiple directors without deleted ones" in {

    deletedDirectorsTest(
       (1 to 3).map { _ =>
          (IndividualBuilder().build(), true)
        } :+ ((IndividualBuilder().build(), false))
      )
  }

  it should "read an individual with minimal details" in {

    establisherIndividualTest(
      (IndividualBuilder()
        .build(), false)
    )

  }

  it should "read multiple individuals" in {

    establisherTest(
      (1 to 3).map { _ =>
        (IndividualBuilder().build(), false)
      },
      Nil,
      Nil,
      Nil
    )
  }

  it should "read multiple individuals without the deleted ones" in {

    establisherTest(
      (1 to 3).map { _ =>
        (IndividualBuilder().build(), false)
      } :+ ((IndividualBuilder()
        .build(), true)),
      Nil,
      Nil,
      Nil
    )

  }

  it should "read an individual with a Nino" in {

    establisherIndividualTest(
      (IndividualBuilder()
        .withNino()
        .build(), false)
    )

  }

  it should "read an individual with a UTR" in {

    establisherIndividualTest(
      (IndividualBuilder()
        .withUtr()
        .build(), false)
    )

  }

  it should "read an individual with a previous address" in {

    establisherIndividualTest(
      (IndividualBuilder()
        .withPreviousAddress()
        .build(), false)
    )

  }

  it should "read a mix of company and individual establishers without the deleted ones" in {

    establisherTest(
      (1 to 2).map(_ =>
        (IndividualBuilder().build(), false)
      ) :+ ((IndividualBuilder().build(), true)),
      (1 to 3).map(_ =>
        (CompanyEstablisherBuilder().build(), false)
      ) :+ ((CompanyEstablisherBuilder().build(), true)),
      Nil,
      Nil
    )

  }

  it should "read a trustee company" in {

    trusteeCompanyTest(
      (CompanyTrusteeBuilder()
        .build(), false)
    )

  }

  it should "read multiple trustee companies" in {

    establisherTest(
      Nil,
      Nil,
      Nil,
      (1 to 3).map {
        _ =>
          (CompanyTrusteeBuilder()
            .build(), false)
      }
    )

  }

  it should "read multiple trustee companies without the deleted ones" in {

    establisherTest(
      Nil,
      Nil,
      Nil,
      (1 to 3).map {
        _ =>
          (CompanyTrusteeBuilder()
            .build(), false)
      } :+ ((CompanyTrusteeBuilder()
        .build(), true))
    )

  }

  it should "read a trustee company with a UTR" in {

    trusteeCompanyTest(
      (CompanyTrusteeBuilder()
        .withUtr()
        .build(), false)
    )

  }

  it should "read a trustee company with a CRN" in {

    trusteeCompanyTest(
      (CompanyTrusteeBuilder()
        .withCrn()
        .build(), false)
    )

  }

  it should "read a trustee company with a VAT number" in {

    trusteeCompanyTest(
      (CompanyTrusteeBuilder()
        .withVat()
        .build(), false)
    )

  }

  it should "read a trustee company with a PAYE reference" in {

    trusteeCompanyTest(
      (CompanyTrusteeBuilder()
        .withPaye()
        .build(), false)
    )

  }

  it should "read a trustee company with a previous address" in {

    trusteeCompanyTest(
      (CompanyTrusteeBuilder()
        .withPreviousAddress()
        .build(), false)
    )

  }

  it should "read a trustee invidual with minimal details" in {

    trusteeIndividualTest(
      (IndividualBuilder()
        .build(), false)
    )

  }

  it should "read multiple trustee individuals" in {

    establisherTest(
      Nil,
      Nil,
      (1 to 3).map(
        _ =>
          (IndividualBuilder()
            .build(), false)
      ),
      Nil
    )

  }

  it should "read a trustee individual with a Nino" in {

    trusteeIndividualTest(
      (IndividualBuilder()
        .withNino()
        .build(), false)
    )

  }

  it should "read a trustee individual with a UTR" in {

    trusteeIndividualTest(
      (IndividualBuilder()
        .withUtr()
        .build(), false)
    )

  }

  it should "read a trustee individual with a previous address" in {

    trusteeIndividualTest(
      (IndividualBuilder()
        .withPreviousAddress()
        .build(), false)
    )

  }

  it should "read a mix of company and individual trustees" in {

    establisherTest(
      Nil,
      Nil,
      (1 to 2).map(_ => (IndividualBuilder().build(), false)),
      (1 to 3).map(_ => (CompanyTrusteeBuilder().build(), false))
    )

  }

  it should "read when there are no establishers or trustees" in {

    establisherTest(Nil, Nil, Nil, Nil)

  }

  it should "read a mix of company and individual establishers and company and individual trustees" in {

    establisherTest(
      (1 to 2).map(_ => (IndividualBuilder().build(), false)),
      (1 to 3).map(
        i =>
          (CompanyEstablisherBuilder()
            .withDirectors(
              (1 to i).map(_ => IndividualBuilder().build())
            )
            .build(), false)
      ),
      (1 to 3).map(_ => (IndividualBuilder().build(), false)),
      (1 to 4).map(_ => (CompanyTrusteeBuilder().build(), false))
    )

  }

  it should "read when neither the establishers nor trustees elements are present" in {

    establisherTest(Nil, Nil, Nil, Nil, Json.obj())

  }

}

object ReadsEstablisherDetailsSpec extends Matchers {

  import EstablishersTestJson._

  def establisherIndividualTest(establisher: (Individual, Boolean)): Assertion =
    establisherTest(Seq(establisher), Nil, Nil, Nil)

  def establisherCompanyTest(establisher: (CompanyEstablisher, Boolean)): Assertion =
    establisherTest(Nil, Seq(establisher), Nil, Nil)

  def companyDirectorTest(director: Individual): Assertion = {

    establisherCompanyTest(
      (CompanyEstablisherBuilder()
        .withDirectors(Seq(director))
        .build(), false)
    )

  }

  def deletedDirectorsTest(directors: Seq[(Individual, Boolean)]): Assertion = {
    val allDirectorsJson = Json.obj(
      "director" -> toJsonArray(directors, companyDirectorJson)
    )
    val companyEstablisher = Seq((CompanyEstablisherBuilder().withDirectors(directors.filterNot(_._2).map(_._1)).build(), false))
    val establishers = toJsonArray(companyEstablisher, establisherCompany).value.map(_.as[JsObject] ++ allDirectorsJson)

    val json: JsValue = Json.obj(
      "establishers" -> establishers
    )

    val expectedEstablishers = EstablisherDetails(
      individual = Nil,
      companyOrOrganization = companyEstablisher.map(_._1)
    )
    json.validate(readsEstablisherDetails).fold(
      errors => fail(s"JSON errors: $errors"),
      actual => actual shouldBe expectedEstablishers
    )
  }

  def trusteeIndividualTest(trustee: (Individual, Boolean)): Assertion =
    establisherTest(Nil, Nil, Seq(trustee), Nil)

  def trusteeCompanyTest(trustee: (CompanyTrustee, Boolean)): Assertion =
    establisherTest(Nil, Nil, Nil, Seq(trustee))

  def establisherTest(establisherIndividuals: Seq[(Individual, Boolean)],
                         establisherCompanies: Seq[(CompanyEstablisher, Boolean)],
                         trusteeIndividuals: Seq[(Individual, Boolean)],
                         trusteeCompanies: Seq[(CompanyTrustee, Boolean)]
                        ): Assertion = {

    val establishers =
      toJsonArray(establisherIndividuals, establisherIndividualJson) ++
        toJsonArray(establisherCompanies, establisherCompany)

    val trustees =
      toJsonArray(trusteeIndividuals, trusteeIndividualJson) ++
        toJsonArray(trusteeCompanies, trusteeCompanyJson)

    val json: JsValue = Json.obj(
      "establishers" -> establishers,
      "trustees" -> trustees
    )

    establisherTest(establisherIndividuals.filterNot(_._2).map(_._1),
      establisherCompanies.filterNot(_._2).map(_._1),
      trusteeIndividuals.filterNot(_._2).map(_._1),
      trusteeCompanies.filterNot(_._2).map(_._1), json)

  }

  def establisherTest(establisherIndividuals: Seq[Individual],
                      establisherCompanies: Seq[CompanyEstablisher],
                      trusteeIndividuals: Seq[Individual],
                      trusteeCompanies: Seq[CompanyTrustee],
                      json: JsValue
                     ): Assertion = {

    val expectedEstablishers = EstablisherDetails(
      individual = establisherIndividuals,
      companyOrOrganization = establisherCompanies
    )

    json.validate(readsEstablisherDetails).fold(
      errors => fail(s"JSON errors: $errors"),
      actual => actual shouldBe expectedEstablishers
    )

    val expectedTrustees = TrusteeDetails(
      individualTrusteeDetail = trusteeIndividuals,
      companyTrusteeDetail = trusteeCompanies
    )

    json.validate(readsTrusteeDetails).fold(
      errors => fail(s"JSON errors: $errors"),
      actual => actual shouldBe expectedTrustees
    )

  }

  private def toJsonArray[T](ts: Seq[(T, Boolean)], f: (T, Boolean) => JsValue): JsArray = {
    ts.foldLeft(Json.arr())((json, t) => json :+ f(t._1, t._2))
  }

}
