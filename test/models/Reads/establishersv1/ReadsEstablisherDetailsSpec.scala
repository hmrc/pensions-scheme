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

///*
// * Copyright 2018 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package models.Reads.establishers
//
//import models.ReadsEstablisherDetails._
//import models._
//import org.scalatest.{Assertion, FlatSpec, Matchers}
//import play.api.libs.json._
//
//class ReadsEstablisherDetailsSpec extends FlatSpec with Matchers {
//
//  import ReadsEstablisherDetailsSpec._
//
//  "ReadsEstablisherDetails" should "read a company with minimial details" in {
//
//    companyEstablisherTest(
//      CompanyBuilder(EstablisherCompanyType)
//        .build())
//
//  }
//
//  it should "read multiple companies" in {
//
//    establisherTest(
//      (1 to 3).map(
//        _ => EstablisherCompany(
//          CompanyBuilder(EstablisherCompanyType)
//          .build(),
//          Seq.empty[EstablisherDetails]
//        )
//      ),
//      Nil
//    )
//
//  }
//
//  it should "read a company with a UTR" in {
//
//    companyEstablisherTest(
//      CompanyBuilder(EstablisherCompanyType)
//        .withUtr()
//        .build())
//
//  }
//
//  it should "read a company with a CRN" in {
//
//    companyEstablisherTest(
//      CompanyBuilder(EstablisherCompanyType)
//        .withCrn()
//        .build())
//
//  }
//
//  it should "read a company with a VAT number" in {
//
//    companyEstablisherTest(
//      CompanyBuilder(EstablisherCompanyType)
//        .withVat()
//        .build())
//
//  }
//
//  it should "read a company with a PAYE reference" in {
//
//    companyEstablisherTest(
//      CompanyBuilder(EstablisherCompanyType)
//        .withPaye()
//        .build())
//
//  }
//
//  it should "read a company with Other Directors true" in {
//
//    companyEstablisherTest(
//      CompanyBuilder(EstablisherCompanyType)
//        .withOtherDirectors(true)
//        .build())
//
//  }
//
//  it should "read a company with Other Directors false" in {
//
//    companyEstablisherTest(
//      CompanyBuilder(EstablisherCompanyType)
//        .withOtherDirectors(false)
//        .build())
//
//  }
//
//  it should "read a company with a previous address" in {
//
//    companyEstablisherTest(
//      CompanyBuilder(EstablisherCompanyType)
//        .withPreviousAddress()
//        .build())
//
//  }
//
//  it should "read a company wth a director with minimal details" in {
//
//    val company =
//      CompanyBuilder(EstablisherCompanyType)
//        .build()
//
//    val director =
//      IndividualBuilder(CompanyDirectorType)
//        .build()
//
//    companyEstablisherTest(company, Seq(director))
//
//  }
//
//  it should "read a company wth a director with a Nino" in {
//
//    val company =
//      CompanyBuilder(EstablisherCompanyType)
//        .build()
//
//    val director =
//      IndividualBuilder(CompanyDirectorType)
//        .withNino()
//        .build()
//
//    companyEstablisherTest(company, Seq(director))
//
//  }
//
//  it should "read a company wth a director with a UTR" in {
//
//    val company =
//      CompanyBuilder(EstablisherCompanyType)
//        .build()
//
//    val director =
//      IndividualBuilder(CompanyDirectorType)
//        .withUtr()
//        .build()
//
//    companyEstablisherTest(company, Seq(director))
//
//  }
//
//  it should "read a company wth a director with a previous address" in {
//
//    val company =
//      CompanyBuilder(EstablisherCompanyType)
//        .build()
//
//    val director =
//      IndividualBuilder(CompanyDirectorType)
//        .withPreviousAddress()
//        .build()
//
//    companyEstablisherTest(company, Seq(director))
//
//  }
//
//  it should "read multiple companies with multiple directors" in {
//
//    establisherTest(
//      (1 to 3).map {
//        i =>
//          val company = CompanyBuilder(EstablisherCompanyType).build()
//          EstablisherCompany(
//            company,
//            (1 to i).map {
//              _ => IndividualBuilder(CompanyDirectorType)
//                .build()
//            }
//          )
//      },
//      Nil
//    )
//
//  }
//
//  it should "read an individual with minimal details" in {
//
//    individualEstablisherTest(
//      IndividualBuilder(EstablisherIndividualType)
//        .build()
//    )
//
//  }
//
//  it should "read multiple individuals" in {
//
//    establisherTest(
//      (1 to 3).map { _ =>
//        EstablisherIndividual(IndividualBuilder(EstablisherIndividualType).build())
//      },
//      Nil
//    )
//
//  }
//
//  it should "read an individual with a Nino" in {
//
//    individualEstablisherTest(
//      IndividualBuilder(EstablisherIndividualType)
//        .withNino()
//        .build()
//    )
//
//  }
//
//  it should "read an individual with a UTR" in {
//
//    individualEstablisherTest(
//      IndividualBuilder(EstablisherIndividualType)
//        .withUtr()
//        .build()
//    )
//
//  }
//
//  it should "read an individual with a previous address" in {
//
//    individualEstablisherTest(
//      IndividualBuilder(EstablisherIndividualType)
//        .withPreviousAddress()
//        .build()
//    )
//
//  }
//
//  it should "read a mix of company and individual establishers" in {
//
//    val company = CompanyBuilder(EstablisherCompanyType).build()
//    val individual = IndividualBuilder(EstablisherIndividualType).build()
//
//    establisherTest(
//      Seq(
//        EstablisherCompany(
//          company,
//          (1 to 2).map { _ =>
//            IndividualBuilder(CompanyDirectorType)
//              .build()
//          }
//        ),
//        EstablisherIndividual(individual)
//      ),
//      Nil
//    )
//
//  }
//
//  it should "read a trustee company" in {
//
//    companyTrusteeTest(
//      CompanyBuilder(TrusteeCompanyType)
//        .build()
//    )
//
//  }
//
//  it should "read multiple trustee companies" in {
//
//    establisherTest(
//      Nil,
//      (1 to 3).map {
//        _ =>
//          TrusteeCompany(
//            CompanyBuilder(TrusteeCompanyType)
//            .build()
//          )
//      }
//    )
//
//  }
//
//  it should "read a trustee company with a UTR" in {
//
//    companyTrusteeTest(
//      CompanyBuilder(TrusteeCompanyType)
//        .withUtr()
//        .build()
//    )
//
//  }
//
//  it should "read a trustee company with a CRN" in {
//
//    companyTrusteeTest(
//      CompanyBuilder(TrusteeCompanyType)
//        .withCrn()
//        .build()
//    )
//
//  }
//
//  it should "read a trustee company with a VAT number" in {
//
//    companyTrusteeTest(
//      CompanyBuilder(TrusteeCompanyType)
//        .withVat()
//        .build()
//    )
//
//  }
//
//  it should "read a trustee company with a PAYE reference" in {
//
//    companyTrusteeTest(
//      CompanyBuilder(TrusteeCompanyType)
//        .withPaye()
//        .build()
//    )
//
//  }
//
//  it should "read a trustee company with a previous address" in {
//
//    companyTrusteeTest(
//      CompanyBuilder(TrusteeCompanyType)
//        .withPreviousAddress()
//        .build()
//    )
//
//  }
//
//  it should "read a trustee invidual with minimal details" in {
//
//    individualTrusteeTest(
//      IndividualBuilder(TrusteeIndividualType)
//        .build()
//    )
//
//  }
//
//  it should "read multiple trustee individuals" in {
//
//    establisherTest(
//      Nil,
//      (1 to 3).map(
//        _ =>
//          TrusteeIndividual(
//            IndividualBuilder(TrusteeIndividualType)
//              .build()
//          )
//      )
//    )
//
//  }
//
//  it should "read a trustee individual with a Nino" in {
//
//    individualTrusteeTest(
//      IndividualBuilder(TrusteeIndividualType)
//        .withNino()
//        .build()
//    )
//
//  }
//
//  it should "read a trustee individual with a UTR" in {
//
//    individualTrusteeTest(
//      IndividualBuilder(TrusteeIndividualType)
//        .withUtr()
//        .build()
//    )
//
//  }
//
//  it should "read a trustee individual with a previous address" in {
//
//    individualTrusteeTest(
//      IndividualBuilder(TrusteeIndividualType)
//        .withPreviousAddress()
//        .build()
//    )
//
//  }
//
//  it should "read a mix of company and individual trustees" in {
//
//    val company = CompanyBuilder(TrusteeCompanyType).build()
//    val individual = IndividualBuilder(TrusteeIndividualType).build()
//
//    establisherTest(
//      Nil,
//      Seq(
//        TrusteeCompany(company),
//        TrusteeIndividual(individual)
//      )
//    )
//
//  }
//
//  it should "read when there are no establishers or trustees" in {
//
//    establisherTest(Nil, Nil)
//
//  }
//
//  it should "read a mix of company and individual establishers and company and individual trustees" in {
//
//    val companyWithDirectors = CompanyBuilder(EstablisherCompanyType).build()
//
//    val establishers = Seq(
//      EstablisherCompany(
//        companyWithDirectors,
//        (1 to 3).map { _ =>
//          IndividualBuilder(CompanyDirectorType)
//            .build()
//        }
//      ),
//      EstablisherIndividual(
//        IndividualBuilder(EstablisherIndividualType).build()
//      ),
//      EstablisherCompany(
//        CompanyBuilder(EstablisherCompanyType).build(),
//        Nil
//      ),
//      EstablisherIndividual(
//        IndividualBuilder(EstablisherIndividualType).build()
//      )
//    )
//
//    val trustees = Seq(
//      TrusteeIndividual(
//        IndividualBuilder(TrusteeIndividualType).build()
//      ),
//      TrusteeCompany(
//        CompanyBuilder(TrusteeCompanyType).build()
//      ),
//      TrusteeCompany(
//        CompanyBuilder(TrusteeCompanyType).build()
//      ),
//      TrusteeIndividual(
//        IndividualBuilder(TrusteeIndividualType).build()
//      )
//    )
//
//    establisherTest(establishers, trustees)
//
//  }
//
//}
//
//object ReadsEstablisherDetailsSpec extends FlatSpec with Matchers {
//
//  def companyEstablisherTest(company: EstablisherDetails, directors: Seq[EstablisherDetails] = Nil): Assertion =
//    establisherTest(Seq(EstablisherCompany(company, directors)), Nil)
//
//  def individualEstablisherTest(individual: EstablisherDetails): Assertion =
//    establisherTest(Seq(EstablisherIndividual(individual)), Nil)
//
//  def companyTrusteeTest(company: EstablisherDetails): Assertion =
//    establisherTest(Nil, Seq(TrusteeCompany(company)))
//
//  def individualTrusteeTest(individual: EstablisherDetails): Assertion =
//    establisherTest(Nil, Seq(TrusteeIndividual(individual)))
//
//  def establisherTest(establishers: Seq[SchemeEstablisher], trustees: Seq[Trustee]): Assertion = {
//
//    val json = Json.obj(
//      "establishers" ->
//        establishers.foldLeft(Json.arr())((json, establisher) => json :+ establisher.json),
//      "trustees" ->
//        trustees.foldLeft(Json.arr())((json, trustee) => json :+ trustee.json)
//    )
//
//    val expected = (establishers ++ trustees).flatMap(_.establishers)
//
//    json.validate(readsEstablisherDetails).fold(
//      errors => fail(s"JSON errors: $errors"),
//      actual => actual should contain theSameElementsAs expected
//    )
//
//  }
//
//}
