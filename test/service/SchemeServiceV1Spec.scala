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

import audit.testdoubles.StubSuccessfulAuditService
import audit.{PSASubscription, SchemeList, SchemeSubscription, SchemeType => AuditSchemeType}
import connector.{BarsConnector, SchemeConnector}
import models._
import models.enumeration.SchemeType
import org.joda.time.LocalDate
import org.scalatest.{AsyncFlatSpec, Matchers}
import play.api.http.Status
import play.api.libs.json._
import play.api.mvc.{AnyContentAsEmpty, RequestHeader}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse}
import utils.Lens

import scala.concurrent.{ExecutionContext, Future}

class SchemeServiceV1Spec extends AsyncFlatSpec with Matchers {

  import SchemeServiceV1Spec._

  "haveInvalidBank" should "set the pension scheme's haveInvalidBank to true if the bank account is invalid" in {

    val json = bankDetailsJson(invalidAccountNumber)

    testFixture().schemeService.haveInvalidBank(json, pensionsScheme).map {
      case (scheme, hasBankDetails) =>
        scheme.customerAndSchemeDetails.haveInvalidBank shouldBe true
        hasBankDetails shouldBe true
    }

  }

  it should "set the pension scheme's haveInvalidBank to false if the bank account is not invalid" in {

    val json = bankDetailsJson(notInvalidAccountNumber)

    testFixture().schemeService.haveInvalidBank(json, pensionsScheme).map {
      case (scheme, hasBankDetails) =>
        scheme.customerAndSchemeDetails.haveInvalidBank shouldBe false
        hasBankDetails shouldBe true
    }

  }

  it should "set the pension scheme's haveInvalidBank to false if the scheme does not have a bank account" in {

    val json = Json.obj()

    testFixture().schemeService.haveInvalidBank(json, pensionsScheme).map {
      case (scheme, hasBankDetails) =>
        scheme.customerAndSchemeDetails.haveInvalidBank shouldBe false
        hasBankDetails shouldBe false
    }

  }

  it should "return a BadRequestException if the bank details JSON cannot be parsed" in {

    val json = Json.obj(
      "uKBankDetails" -> Json.obj()
    )

    recoverToSucceededIf[BadRequestException] {
      testFixture().schemeService.haveInvalidBank(json, pensionsScheme)
    }

  }

  "jsonToPensionsSchemeModel" should "return a pensions scheme object given valid JSON" in {

    testFixture().schemeService.jsonToPensionsSchemeModel(pensionsSchemeJson) shouldBe a[Right[_, PensionsScheme]]

  }

  it should "return a BadRequestException if the JSON is invalid" in {

    testFixture().schemeService.jsonToPensionsSchemeModel(Json.obj()) shouldBe a[Left[BadRequestException, _]]

  }

  "registerScheme" should "return the result of submitting the pensions scheme" in {

    testFixture().schemeService.registerScheme(psaId, pensionsSchemeJson).map {
      response =>
        response.status shouldBe Status.OK
        val json = Json.parse(response.body)
        json.validate[SchemeRegistrationResponse] shouldBe JsSuccess(schemeRegistrationResponse)
    }

  }

  it should "send a SchemeSubscription audit event following a successful submission" in {

    val fixture = testFixture()

    val expected = schemeSubscription.copy(hasIndividualEstablisher = true)

    fixture.schemeService.registerScheme(psaId, pensionsSchemeJson).map {
      _ =>
        fixture.auditService.verifySent(expected) shouldBe true
    }

  }

  it should "not send a SchemeSubscription audit event following an unsuccessful submission" in {

    val fixture = testFixture()

    fixture.schemeConnector.setRegisterSchemeResponse(Future.failed(new BadRequestException("bad request")))

    fixture.schemeService.registerScheme(psaId, pensionsSchemeJson)
        .map(_ => fail("Expected failure"))
        .recover {
          case _: BadRequestException =>
            fixture.auditService.verifyNothingSent() shouldBe true
        }

  }

  "translateSchemeSubscriptionEvent" should "translate a master trust scheme" in {

    val scheme = PensionsSchemeIsSchemeMasterTrust.set(pensionsScheme, true)
    val actual = testFixture().schemeService.translateSchemeSubscriptionEvent(psaId, scheme, false)
    val expected = schemeSubscription.copy(schemeType = AuditSchemeType.masterTrust)

    actual shouldBe expected

  }

  it should "translate a single trust scheme" in {

    val scheme = PensionsSchemeSchemeStructure.set(pensionsScheme, SchemeType.single.value)
    val actual = testFixture().schemeService.translateSchemeSubscriptionEvent(psaId, scheme, false)
    val expected = schemeSubscription

    actual shouldBe expected

  }

  it should "translate a group Life/Death scheme" in {

    val scheme = PensionsSchemeSchemeStructure.set(pensionsScheme, SchemeType.group.value)
    val actual = testFixture().schemeService.translateSchemeSubscriptionEvent(psaId, scheme, false)
    val expected = schemeSubscription.copy(schemeType = AuditSchemeType.groupLifeDeath)

    actual shouldBe expected

  }

  it should "translate a body corporate scheme" in {

    val scheme = PensionsSchemeSchemeStructure.set(pensionsScheme, SchemeType.corp.value)
    val actual = testFixture().schemeService.translateSchemeSubscriptionEvent(psaId, scheme, false)
    val expected = schemeSubscription.copy(schemeType = AuditSchemeType.bodyCorporate)

    actual shouldBe expected

  }

  it should "translate an 'other' scheme" in {

    val scheme = PensionsSchemeSchemeStructure.set(pensionsScheme, SchemeType.other.value)
    val actual = testFixture().schemeService.translateSchemeSubscriptionEvent(psaId, scheme, false)
    val expected = schemeSubscription.copy(schemeType = AuditSchemeType.other)

    actual shouldBe expected

  }

  it should "translate a scheme with individual establishers" in {

    val scheme =
      PensionsSchemeSchemeStructure
        .set(pensionsScheme, SchemeType.single.value)
        .copy(establisherDetails = List(establisherDetails("Individual")))
    val actual = testFixture().schemeService.translateSchemeSubscriptionEvent(psaId, scheme, false)
    val expected = schemeSubscription.copy(hasIndividualEstablisher = true)

    actual shouldBe expected

  }

  it should "translate a scheme with company establishers" in {

    val scheme =
      PensionsSchemeSchemeStructure
        .set(pensionsScheme, SchemeType.single.value)
        .copy(establisherDetails = List(establisherDetails("Company/Org")))
    val actual = testFixture().schemeService.translateSchemeSubscriptionEvent(psaId, scheme, false)
    val expected = schemeSubscription.copy(hasCompanyEstablisher = true)

    actual shouldBe expected

  }

  it should "translate a scheme with partnership establishers" in {

    val scheme =
      PensionsSchemeSchemeStructure
        .set(pensionsScheme, SchemeType.single.value)
        .copy(establisherDetails = List(establisherDetails("Partnership")))
    val actual = testFixture().schemeService.translateSchemeSubscriptionEvent(psaId, scheme, false)
    val expected = schemeSubscription.copy(hasPartnershipEstablisher = true)

    actual shouldBe expected

  }

  it should "translate a scheme with dormant company, bank details, and invalid bank details" in {

    val scheme = pensionsScheme.copy(
      customerAndSchemeDetails = pensionsScheme.customerAndSchemeDetails.copy(haveInvalidBank = true),
      pensionSchemeDeclaration = pensionsScheme.pensionSchemeDeclaration.copy(box5 = Some(true))
    )

    val actual = testFixture().schemeService.translateSchemeSubscriptionEvent(psaId, scheme, true)

    val expected = schemeSubscription.copy(
      schemeType = AuditSchemeType.singleTrust,
      hasDormantCompany = true,
      hasBankDetails = true,
      hasValidBankDetails = false
    )

    actual shouldBe expected

  }

  "listOfSchemes" should "return the list of schemes from the connector" in {

    testFixture().schemeService.listOfSchemes(psaId).map {
      httpResponse =>
        httpResponse.status shouldBe Status.OK
        httpResponse.json shouldBe listOfSchemesJson
    }

  }

  it should "send an audit event on success" in {

    val fixture = testFixture()

    fixture.schemeService.listOfSchemes(psaId).map {
      _ =>
        fixture.auditService.verifySent(SchemeList(psaId)) shouldBe true
    }

  }

  it should "not send on audit event on failure" in {

    val fixture = testFixture()

    fixture.schemeConnector.setListOfSchemesResponse(Future.failed(new BadRequestException("bad request")))

    fixture.schemeService.listOfSchemes(psaId)
      .map(_ => fail("Expected failure"))
      .recover {
        case _: BadRequestException =>
          fixture.auditService.verifyNothingSent() shouldBe true
      }

  }

  "registerPSA" should "return the result from the connector" in {

    val fixture = testFixture()

    fixture.schemeService.registerPSA(psaJson).map {
      httpResponse =>
        httpResponse.status shouldBe Status.OK
        httpResponse.json shouldBe registerPsaResponseJson
    }

  }

  it should "throw BadRequestException if the JSON cannot be parsed as PensionSchemeAdministrator" in {

    val fixture = testFixture()

    recoverToSucceededIf[BadRequestException] {
      fixture.schemeService.registerPSA(Json.obj())
    }

  }

  it should "send an audit event on success" in {

    val fixture = testFixture()

    fixture.schemeService.registerPSA(psaJson).map {
      _ =>
        fixture.auditService.verifySent(
          PSASubscription(
            existingUser = false,
            success = true,
            legalStatus = "test-legal-status"
          )
        ) shouldBe true
    }

  }

  it should "send an audit event on failure" in {

    val fixture = testFixture()

    fixture.schemeConnector.setRegisterPsaResponse(Future.failed(new BadRequestException("bad request")))

    fixture.schemeService.registerPSA(psaJson)
      .map(_ => fail("Expected failure"))
      .recover {
        case _: BadRequestException =>
          fixture.auditService.verifySent(
            PSASubscription(
              existingUser = false,
              success = false,
              legalStatus = "test-legal-status"
            )
          ) shouldBe true
      }

  }

}

class TestFixture {
  val schemeConnector: FakeSchemeConnector = new FakeSchemeConnector()
  val barsConnector: FakeBarsConnector = new FakeBarsConnector()
  val auditService: StubSuccessfulAuditService = new StubSuccessfulAuditService()
  val schemeService: SchemeServiceV1 = new SchemeServiceV1(schemeConnector, barsConnector, auditService)
}

object SchemeServiceV1Spec {

  def testFixture(): TestFixture = new TestFixture()

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  val psaId: String = "test-psa-id"
  val invalidAccountNumber: String = "111"
  val notInvalidAccountNumber: String = "112"

  val pensionsSchemeJson: JsValue = Json.obj(
    "schemeDetails" -> Json.obj(
      "schemeName" -> "test-scheme-name",
      "schemeType" -> Json.obj(
        "name" -> SchemeType.single.name
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
      schemeStructure = SchemeType.single.value,
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

  val schemeSubscription = SchemeSubscription(
    psaIdentifier = psaId,
    schemeType = AuditSchemeType.singleTrust,
    hasIndividualEstablisher = false,
    hasCompanyEstablisher = false,
    hasPartnershipEstablisher = false,
    hasDormantCompany = false,
    hasBankDetails = false,
    hasValidBankDetails = false
  )

  def establisherDetails(establisherType: String): EstablisherDetails =
    EstablisherDetails(
      `type` = establisherType,
      correspondenceAddressDetails = CorrespondenceAddressDetails(
        addressDetails = UkAddress(
          addressLine1 = "test-address-line-1",
          countryCode = "test-country-code",
          postalCode = "test-postal-code"
        )
      ),
      correspondenceContactDetails = CorrespondenceContactDetails(
        contactDetails = ContactDetails(
          telephone = "test-telephone",
          email = "test-email"
        )
      )
    )

  val listOfSchemes: ListOfSchemes =
    ListOfSchemes(
      processingDate = "1969-07-20",
      totalSchemesRegistered = "0",
      schemeDetail = None
    )

  val listOfSchemesJson: JsValue = Json.toJson(listOfSchemes)

  val psaJson: JsValue = Json.obj(
    "registrationInfo" -> Json.obj(
      "legalStatus" -> "test-legal-status",
      "sapNumber" -> "test-sap-number",
      "noIdentifier" -> false,
      "customerType" -> "test-customer-type"
    ),
    "individualContactDetails" -> Json.obj(
      "phone" -> "test-phone",
      "email" -> "test-email"
    ),
    "individualAddress" -> Json.obj(
      "addressLine1" -> "test-address-line-1",
      "countryCode" -> "GB",
      "postalCode" -> "test-postal-code"
    ),
    "individualAddressYears" -> "test-individual-address-years",
    "existingPSA" -> Json.obj(
      "isExistingPSA" -> false
    ),
    "individualDetails" -> Json.obj(
      "firstName" -> "test-first-name",
      "lastName" -> "test-last-name",
      "dateOfBirth" -> "2000-01-01"
    ),
    "declaration" -> true,
    "declarationWorkingKnowledge" -> "test-declaration-working-knowledge",
    "declarationFitAndProper" -> true
  )

  val registerPsaResponseJson: JsValue =
    Json.obj(
      "processingDate" -> LocalDate.now,
      "formBundle" -> "1121313",
      "psaId" -> "A21999999"
    )

}

class FakeSchemeConnector extends SchemeConnector {

  import SchemeServiceV1Spec.{listOfSchemesJson, registerPsaResponseJson, schemeRegistrationResponseJson}

  private var registerSchemeResponse = Future.successful(HttpResponse(Status.OK, Some(schemeRegistrationResponseJson)))
  private var listOfSchemesResponse = Future.successful(HttpResponse(Status.OK, Some(listOfSchemesJson)))
  private var registerPsaResponse = Future.successful(HttpResponse(Status.OK, Some(registerPsaResponseJson)))

  def setRegisterSchemeResponse(response: Future[HttpResponse]): Unit = this.registerSchemeResponse = response
  def setListOfSchemesResponse(response: Future[HttpResponse]): Unit = this.listOfSchemesResponse = response
  def setRegisterPsaResponse(response: Future[HttpResponse]): Unit = this.registerPsaResponse = response

  override def registerScheme(psaId: String, registerData: JsValue)(implicit
                                                        headerCarrier: HeaderCarrier,
                                                        ec: ExecutionContext,
                                                        request: RequestHeader): Future[HttpResponse] = registerSchemeResponse

  override def registerPSA(registerData: JsValue)(implicit
                                         headerCarrier: HeaderCarrier,
                                         ec: ExecutionContext,
                                         request: RequestHeader): Future[HttpResponse] = registerPsaResponse

  override def listOfSchemes(psaId: String)(implicit
                                   headerCarrier: HeaderCarrier,
                                   ec: ExecutionContext,
                                   request: RequestHeader): Future[HttpResponse] = listOfSchemesResponse
}

class FakeBarsConnector extends BarsConnector {

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

object PensionsSchemeIsSchemeMasterTrust extends Lens[PensionsScheme, Boolean] {

  override def get: PensionsScheme => Boolean = pensionsScheme => pensionsScheme.customerAndSchemeDetails.isSchemeMasterTrust

  override def set: (PensionsScheme, Boolean) => PensionsScheme = (pensionsScheme, isSchemeMasterTrust) =>
    pensionsScheme.copy(customerAndSchemeDetails = pensionsScheme.customerAndSchemeDetails.copy(isSchemeMasterTrust = isSchemeMasterTrust))

}

object PensionsSchemeSchemeStructure extends Lens[PensionsScheme, String] {

  override def get: PensionsScheme => String = pensionsScheme => pensionsScheme.customerAndSchemeDetails.schemeStructure

  override def set: (PensionsScheme, String) => PensionsScheme = (pensionsScheme, schemeStructure) =>
    pensionsScheme.copy(customerAndSchemeDetails = pensionsScheme.customerAndSchemeDetails.copy(schemeStructure = schemeStructure))

}
