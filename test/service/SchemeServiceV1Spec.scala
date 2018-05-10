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

package service

import connector.{BarsConnector, SchemeConnector}
import models._
import org.scalatest.{AsyncFlatSpec, Matchers}
import play.api.http.Status
import play.api.libs.json._
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class SchemeServiceV1Spec extends AsyncFlatSpec with Matchers {

  import SchemeServiceV1Spec._

  "haveInvalidBank" should "set the pension scheme's haveInvalidBank to true if the bank account is invalid" in {

    val json = bankDetailsJson(invalidAccountNumber)

    schemeService.haveInvalidBank(json, pensionsScheme).map {
      result =>
        result.customerAndSchemeDetails.haveInvalidBank shouldBe true
    }

  }

  it should "set the pension scheme's haveInvalidBank to false if the bank account is not invalid" in {

    val json = bankDetailsJson(notInvalidAccountNumber)

    schemeService.haveInvalidBank(json, pensionsScheme).map {
      result =>
        result.customerAndSchemeDetails.haveInvalidBank shouldBe false
    }

  }

  it should "set the pension scheme's haveInvalidBank to false if the scheme does not have a bank account" in {

    val json = Json.obj()

    schemeService.haveInvalidBank(json, pensionsScheme).map {
      result =>
        result.customerAndSchemeDetails.haveInvalidBank shouldBe false
    }

  }

  it should "return a BadRequestException if the bank details JSON cannot be parsed" in {

    val json = Json.obj(
      "uKBankDetails" -> Json.obj()
    )

    recoverToSucceededIf[BadRequestException] {
      schemeService.haveInvalidBank(json, pensionsScheme)
    }

  }

  "jsonToPensionsSchemeModel" should "return a pensions scheme object given valid JSON" in {

    schemeService.jsonToPensionsSchemeModel(pensionsSchemeJson) shouldBe a[Right[_, PensionsScheme]]

  }

  it should "return a BadRequestException if the JSON is invalid" in {

    schemeService.jsonToPensionsSchemeModel(Json.obj()) shouldBe a[Left[BadRequestException, _]]

  }

  "registerScheme" should "return the result of submitting the pensions scheme" in {

    schemeService.registerScheme("test-psa-id", pensionsSchemeJson).map {
      response =>
        response.status shouldBe Status.OK
        val json = Json.parse(response.body)
        json.validate[SchemeRegistrationResponse] shouldBe JsSuccess(schemeRegistrationResponse)
    }

  }

}

object SchemeServiceV1Spec {

  val schemeService: SchemeServiceV1 = new SchemeServiceV1(FakeSchemeConnector, FakeBarsConnector)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val invalidAccountNumber: String = "111"
  val notInvalidAccountNumber: String = "112"

  val pensionsSchemeJson: JsValue = Json.obj(
    "schemeDetails" -> Json.obj(
      "schemeName" -> "test-scheme-name",
      "schemeType" -> Json.obj(
        "name" -> "single"
      )
    ),
    "membership" -> "opt1",
    "membershipFuture" -> "opt1",
    "investmentRegulated" -> false,
    "occupationalPensionScheme" -> false,
    "securedBenefits" -> false,
    "benefits" -> "opt1",
    "schemeEstablishedCountry" -> "test-scheme-established-country",
    "uKBankAccount" -> false,
    "declaration" -> false,
    "establishers" -> Json.arr(
      Json.obj(
        "establisherDetails" -> Json.obj(
          "firstName" -> "test-first-name",
          "lastName" -> "test-last-name",
          "date" -> "1969-07-20"
        ),
        "contactDetails" -> Json.obj(
          "emailAddress" -> "test-email-address",
          "phoneNumber" -> "test-phone-number"
        ),
        "uniqueTaxReference" -> Json.obj(),
        "address" -> Json.obj(
          "addressLine1" -> "test-address-line-1",
          "country" -> "test-country"
        ),
        "addressYears" -> "test-address-years",
        "establisherNino" -> Json.obj()
      )
    )
  )

  val pensionsScheme = PensionsScheme(
    CustomerAndSchemeDetails(
      schemeName = "test-pensions-scheme",
      isSchemeMasterTrust = false,
      schemeStructure = "test-scheme-structure",
      currentSchemeMembers = "test-current-scheme-members",
      futureSchemeMembers = "test-future-scheme-members",
      isReguledSchemeInvestment = false,
      isOccupationalPensionScheme = false,
      areBenefitsSecuredContractInsuranceCompany = false,
      doesSchemeProvideBenefits = "test-does-scheme-provide-benefits",
      schemeEstablishedCountry = "test-scheme-established-country",
      haveInvalidBank = false
    ),
    PensionSchemeDeclaration(
      box1 = false,
      box2 = false,
      box6 = false,
      box7 = false,
      box8 = false,
      box9 = false
    ),
    List.empty[EstablisherDetails]
  )

  def bankDetailsJson(accountNumber: String ): JsValue =
    Json.obj(
      "uKBankDetails" -> Json.obj(
        "bankName" -> "my bank name",
        "accountName" -> "my account name",
        "sortCode" -> Json.obj(
        "first" -> "00",
        "second" -> "11",
        "third" -> "00"
        ),
        "accountNumber" -> accountNumber,
        "date" -> "2010-02-02"
      )
    )

  val schemeRegistrationResponse = SchemeRegistrationResponse("test-processing-date", "test-scheme-reference-number")
  val schemeRegistrationResponseJson: JsValue = Json.toJson(schemeRegistrationResponse)

}

//noinspection NotImplementedCode
object FakeSchemeConnector extends SchemeConnector {

  import SchemeServiceV1Spec.schemeRegistrationResponseJson

  override def registerScheme(psaId: String, registerData: JsValue)(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    Future.successful(HttpResponse(Status.OK, Some(schemeRegistrationResponseJson)))
  }

  override def registerPSA(registerData: JsValue)(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = ???

  override def listOfSchemes(psaId: String)(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = ???

}

object FakeBarsConnector extends BarsConnector {

  import SchemeServiceV1Spec._

  override def invalidBankAccount(bankAccount: BankAccount)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Boolean] = {
    bankAccount match {
      case BankAccount(_, accountNumber) if accountNumber == invalidAccountNumber => Future.successful(true)
      case BankAccount(_, accountNumber) if accountNumber == notInvalidAccountNumber => Future.successful(false)
      case _ => throw new IllegalArgumentException(s"No stub behaviour for bank account: $bankAccount")
    }
  }

}

case class SchemeRegistrationResponse(processingDate: String, schemeReferenceNumber: String)

object SchemeRegistrationResponse {
  implicit val formatsSchemeRegistrationResponse: Format[SchemeRegistrationResponse] = Json.format[SchemeRegistrationResponse]
}
