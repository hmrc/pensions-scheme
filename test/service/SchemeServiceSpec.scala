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

package service

import audit.testdoubles.StubSuccessfulAuditService
import audit.{SchemeSubscription, SchemeType => AuditSchemeType}
import base.SpecBase
import models.Reads.establishers.{CompanyEstablisherBuilder, IndividualBuilder, PartnershipBuilder}
import models.enumeration.SchemeType
import models.{EstablisherDetails, PensionsScheme, _}
import org.scalatest.{AsyncFlatSpec, Matchers}
import play.api.http.Status
import play.api.libs.json.Reads._
import play.api.libs.json.{__, _}
import play.api.mvc.{AnyContentAsEmpty, RequestHeader}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse}
import utils.Lens

import scala.concurrent.{ExecutionContext, Future}

class SchemeServiceSpec extends AsyncFlatSpec with Matchers {

  import FakeSchemeConnector._
  import SchemeServiceSpec._

  "haveInvalidBank" should "set the pension scheme's haveInvalidBank to true if the bank account is invalid" in {

    val account = bankAccount(invalidAccountNumber)

    testFixture().schemeService.haveInvalidBank(Some(account), pensionsScheme, psaId).map {
      scheme =>
        scheme.customerAndSchemeDetails.haveInvalidBank shouldBe true
    }

  }

  it should "set the pension scheme's haveInvalidBank to false if the bank account is not invalid" in {

    val account = bankAccount(notInvalidAccountNumber)

    testFixture().schemeService.haveInvalidBank(Some(account), pensionsScheme, psaId).map {
      scheme =>
        scheme.customerAndSchemeDetails.haveInvalidBank shouldBe false
    }

  }

  it should "set the pension scheme's haveInvalidBank to false if the scheme does not have a bank account" in {

    testFixture().schemeService.haveInvalidBank(None, pensionsScheme, psaId).map {
      scheme =>
        scheme.customerAndSchemeDetails.haveInvalidBank shouldBe false
    }

  }

  "readBankAccount" should "return a bank account where it exists in json" in {

    val json = bankDetailsJson(notInvalidAccountNumber)

    val actual = testFixture().schemeService.readBankAccount(json)
    actual shouldBe Right(Some(bankAccount(notInvalidAccountNumber)))

  }

  it should "return None where no account exists in json" in {

    val actual = testFixture().schemeService.readBankAccount(Json.obj())
    actual shouldBe Right(None)

  }

  "transformJsonToModel" should "return a pensions scheme object given valid JSON" in {

    testFixture().schemeService.transformJsonToModel(pensionsSchemeJson, PensionSchemeDeclaration.apiReads) shouldBe a[Right[_, PensionsScheme]]

  }

  it should "return a pensions scheme object given valid JSON with correct register scheme declaration reads" in {

    val result = testFixture().schemeService.transformJsonToModel(pensionsSchemeJson, PensionSchemeDeclaration.apiReads)

    Json.toJson(result.right.get).transform((__ \ 'pensionSchemeDeclaration \ 'declaration1).json.pick).asOpt shouldBe None

  }

  it should "return a pensions scheme object given valid JSON with correct update scheme declaration reads" in {

    val result = testFixture().schemeService.transformJsonToModel(pensionsSchemeJson, PensionSchemeUpdateDeclaration.reads)

    Json.toJson(result.right.get).transform((__ \ 'pensionSchemeDeclaration \ 'declaration1).json.pick).asOpt shouldBe Some(JsBoolean(false))

  }

  it should "return a flag that says whether there has been any changes on establishers or trustee details" in {
    val inputWithUpdatedTrusteesOrEstablishers = pensionsSchemeJson.as[JsObject] ++ Json.obj("changeOfEstablisherOrTrustDetails" -> true)

    val result = testFixture().schemeService.transformJsonToModel(inputWithUpdatedTrusteesOrEstablishers, PensionSchemeDeclaration.apiReads)

    result.right.get.changeOfEstablisherOrTrustDetails mustBe Some(true)
  }

  it should "return a BadRequestException if the JSON is invalid" in {

    testFixture().schemeService.transformJsonToModel(Json.obj(), PensionSchemeDeclaration.apiReads) shouldBe a[Left[BadRequestException, _]]

  }

  "registerScheme" should "return the result of submitting the pensions scheme" in {

    testFixture().schemeService.registerScheme(psaId, pensionsSchemeJson).map {
      response =>
        response.status shouldBe Status.OK
        val json = Json.parse(response.body)

        json.transform((__ \ 'pensionSchemeDeclaration \ 'declaration1).json.pick).asOpt mustBe None

        json.validate[SchemeRegistrationResponse] shouldBe JsSuccess(schemeRegistrationResponse)


    }

  }

  it should "send a SchemeSubscription audit event following a successful submission" in {

    val fixture = testFixture()

    fixture.schemeService.registerScheme(psaId, pensionsSchemeJson).map {
      httpResponse =>
        val expected = schemeSubscription.copy(
          hasIndividualEstablisher = true,
          status = Status.OK,
          request = schemeSubscriptionRequestJson(pensionsSchemeJson, fixture.schemeService),
          response = Some(httpResponse.json)
        )

        fixture.auditService.lastEvent shouldBe Some(expected)
    }

  }

  it should "not send a SchemeSubscription audit event following an unsuccessful submission" in {

    val fixture = testFixture()

    fixture.schemeConnector.setRegisterSchemeResponse(Future.failed(new BadRequestException("bad request")))

    fixture.schemeService.registerScheme(psaId, pensionsSchemeJson)
      .map(_ => fail("Expected failure"))
      .recover {
        case _: BadRequestException =>
          val expected = schemeSubscription.copy(
            hasIndividualEstablisher = true,
            status = Status.BAD_REQUEST,
            request = schemeSubscriptionRequestJson(pensionsSchemeJson, fixture.schemeService),
            response = None
          )

          fixture.auditService.lastEvent shouldBe Some(expected)
      }

  }

  "translateSchemeSubscriptionEvent" should "translate a master trust scheme" in {

    val scheme = PensionsSchemeIsSchemeMasterTrust.set(pensionsScheme, true)

    val actual = testFixture().schemeService.translateSchemeSubscriptionEvent(psaId, scheme, false, Status.OK, None)

    val expected = schemeSubscription.copy(
      schemeType = Some(AuditSchemeType.masterTrust),
      request = Json.toJson(scheme)
    )

    actual shouldBe expected

  }

  it should "translate a single trust scheme" in {

    val scheme = PensionsSchemeSchemeStructure.set(pensionsScheme, Some(SchemeType.single.value))

    val actual = testFixture().schemeService.translateSchemeSubscriptionEvent(psaId, scheme, false, Status.OK, None)

    val expected = schemeSubscription.copy(
      request = Json.toJson(scheme)
    )

    actual shouldBe expected

  }

  it should "translate a group Life/Death scheme" in {

    val scheme = PensionsSchemeSchemeStructure.set(pensionsScheme, Some(SchemeType.group.value))

    val actual = testFixture().schemeService.translateSchemeSubscriptionEvent(psaId, scheme, false, Status.OK, None)

    val expected = schemeSubscription.copy(
      schemeType = Some(AuditSchemeType.groupLifeDeath),
      request = Json.toJson(scheme)
    )

    actual shouldBe expected

  }

  it should "translate a body corporate scheme" in {

    val scheme = PensionsSchemeSchemeStructure.set(pensionsScheme, Some(SchemeType.corp.value))

    val actual = testFixture().schemeService.translateSchemeSubscriptionEvent(psaId, scheme, false, Status.OK, None)

    val expected = schemeSubscription.copy(
      schemeType = Some(AuditSchemeType.bodyCorporate),
      request = Json.toJson(scheme)
    )

    actual shouldBe expected

  }

  it should "translate an 'other' scheme" in {

    val scheme = PensionsSchemeSchemeStructure.set(pensionsScheme, Some(SchemeType.other.value))

    val actual = testFixture().schemeService.translateSchemeSubscriptionEvent(psaId, scheme, false, Status.OK, None)

    val expected = schemeSubscription.copy(
      schemeType = Some(AuditSchemeType.other),
      request = Json.toJson(scheme)
    )

    actual shouldBe expected

  }

  it should "translate a scheme with individual establishers" in {

    val scheme =
      PensionsSchemeSchemeStructure
        .set(pensionsScheme, Some(SchemeType.single.value))
        .copy(establisherDetails =
          EstablisherDetails(
            companyOrOrganization = Nil,
            individual = Seq(IndividualBuilder().build()),
            partnership = Nil
          )
        )

    val actual = testFixture().schemeService.translateSchemeSubscriptionEvent(psaId, scheme, false, Status.OK, None)

    val expected = schemeSubscription.copy(
      hasIndividualEstablisher = true,
      request = Json.toJson(scheme)
    )

    actual shouldBe expected

  }

  it should "translate a scheme with company establishers" in {

    val scheme =
      PensionsSchemeSchemeStructure
        .set(pensionsScheme, Some(SchemeType.single.value))
        .copy(establisherDetails =
          EstablisherDetails(
            companyOrOrganization = Seq(CompanyEstablisherBuilder().build()),
            individual = Nil,
            partnership = Nil
          )
        )

    val actual = testFixture().schemeService.translateSchemeSubscriptionEvent(psaId, scheme, false, Status.OK, None)

    val expected = schemeSubscription.copy(
      hasCompanyEstablisher = true,
      request = Json.toJson(scheme)
    )

    actual shouldBe expected

  }

  it should "translate a scheme with partnership establishers" in {

    val scheme =
      PensionsSchemeSchemeStructure
        .set(pensionsScheme, Some(SchemeType.single.value))
        .copy(establisherDetails =
          EstablisherDetails(
            companyOrOrganization = Nil,
            individual = Nil,
            partnership = Seq(PartnershipBuilder().build())
          )
        )

    val actual = testFixture().schemeService.translateSchemeSubscriptionEvent(psaId, scheme, false, Status.OK, None)

    val expected = schemeSubscription.copy(
      hasPartnershipEstablisher = true,
      request = Json.toJson(scheme)
    )

    actual shouldBe expected

  }

  it should "translate a scheme with dormant company, bank details, and invalid bank details" in {

    val declaration = pensionsScheme.pensionSchemeDeclaration.asInstanceOf[PensionSchemeDeclaration]

    val scheme = pensionsScheme.copy(
      customerAndSchemeDetails = pensionsScheme.customerAndSchemeDetails.copy(haveInvalidBank = true),
      pensionSchemeDeclaration = declaration.copy(box5 = Some(true))
    )

    val actual = testFixture().schemeService.translateSchemeSubscriptionEvent(psaId, scheme, true, Status.OK, None)

    val expected = schemeSubscription.copy(
      schemeType = Some(AuditSchemeType.singleTrust),
      hasDormantCompany = true,
      hasBankDetails = true,
      hasValidBankDetails = false,
      request = Json.toJson(scheme)
    )

    actual shouldBe expected

  }

  "updateScheme" should "return the result of submitting the pensions scheme and have the right declaration type" in {
    class FakeSchemeConnectorStoreJson extends FakeSchemeConnector {
      var lastUpdateSchemeDetailsdata: JsValue = JsNull

      override def updateSchemeDetails(pstr: String, data: JsValue)(
        implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[HttpResponse] = {
        lastUpdateSchemeDetailsdata = data
        updateSchemeResponse
      }
    }

    trait TestFixture {
      val schemeConnector: FakeSchemeConnectorStoreJson = new FakeSchemeConnectorStoreJson()
      val barsConnector: FakeBarsConnector = new FakeBarsConnector()
      val auditService: StubSuccessfulAuditService = new StubSuccessfulAuditService()
      val schemeService: SchemeServiceImpl = new SchemeServiceImpl(
        schemeConnector, barsConnector, auditService, appConfig)
    }

    val f = new TestFixture() {}

    f.schemeService.updateScheme(pstr, psaId, pensionsSchemeJson).map {
      response =>
        response.status shouldBe Status.OK
        val declaration1Value = f.schemeConnector.lastUpdateSchemeDetailsdata
          .transform((__ \ "pensionSchemeDeclaration" \ "declaration1").json.pick)
        declaration1Value.asOpt shouldBe Some(JsBoolean(false))
    }
  }
}

object SchemeServiceSpec extends SpecBase {


  trait TestFixture {
    val schemeConnector: FakeSchemeConnector = new FakeSchemeConnector()
    val barsConnector: FakeBarsConnector = new FakeBarsConnector()
    val auditService: StubSuccessfulAuditService = new StubSuccessfulAuditService()
    val schemeService: SchemeServiceImpl = new SchemeServiceImpl(
      schemeConnector, barsConnector, auditService, appConfig)
  }

  def testFixture(): TestFixture = new TestFixture() {}

  val psaId: String = "test-psa-id"
  val pstr: String = "test-pstr"
  val invalidAccountNumber: String = "111"
  val notInvalidAccountNumber: String = "112"

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  def bankAccount(accountNumber: String): BankAccount =
    BankAccount("001100", accountNumber)

  def bankDetailsJson(accountNumber: String): JsValue =
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

  val pensionsScheme = PensionsScheme(
    CustomerAndSchemeDetails(
      schemeName = "test-pensions-scheme",
      isSchemeMasterTrust = false,
      schemeStructure = Some(SchemeType.single.value),
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
    EstablisherDetails(
      Nil,
      Nil,
      Nil
    ),
    TrusteeDetails(
      Nil,
      Nil,
      Nil
    )
  )

  val pensionsSchemeJson: JsValue = Json.obj(
    "schemeName" -> "test-scheme-name",
    "isSchemeMasterTrust" -> false,
    "schemeType" -> Json.obj(
      "name" -> SchemeType.single.name
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
    "declarationDuties" -> true,
    "insuranceCompanyName" -> "Test insurance company name",
    "insurancePolicyNumber" -> "Test insurance policy number",
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

  def schemeSubscriptionRequestJson(pensionsSchemeJson: JsValue, service: SchemeServiceImpl): JsValue = {

    service.transformJsonToModel(pensionsSchemeJson, PensionSchemeDeclaration.apiReads).fold(
      ex => throw ex,
      scheme => Json.toJson(scheme)
    )

  }

  val schemeSubscription = SchemeSubscription(
    psaIdentifier = psaId,
    schemeType = Some(AuditSchemeType.singleTrust),
    hasIndividualEstablisher = false,
    hasCompanyEstablisher = false,
    hasPartnershipEstablisher = false,
    hasDormantCompany = false,
    hasBankDetails = false,
    hasValidBankDetails = false,
    status = Status.OK,
    request = Json.obj(),
    response = None
  )

  object PensionsSchemeIsSchemeMasterTrust extends Lens[PensionsScheme, Boolean] {

    override def get: PensionsScheme => Boolean = pensionsScheme => pensionsScheme.customerAndSchemeDetails.isSchemeMasterTrust

    override def set: (PensionsScheme, Boolean) => PensionsScheme = (pensionsScheme, isSchemeMasterTrust) =>
      pensionsScheme.copy(customerAndSchemeDetails = pensionsScheme.customerAndSchemeDetails.copy(isSchemeMasterTrust = isSchemeMasterTrust))

  }

  object PensionsSchemeSchemeStructure extends Lens[PensionsScheme, Option[String]] {

    override def get: PensionsScheme => Option[String] = pensionsScheme => pensionsScheme.customerAndSchemeDetails.schemeStructure

    override def set: (PensionsScheme, Option[String]) => PensionsScheme = (pensionsScheme, schemeStructure) =>
      pensionsScheme.copy(customerAndSchemeDetails = pensionsScheme.customerAndSchemeDetails.copy(schemeStructure = schemeStructure))

  }

}

case class SchemeRegistrationResponse(processingDate: String, schemeReferenceNumber: String)

object SchemeRegistrationResponse {
  implicit val formatsSchemeRegistrationResponse: Format[SchemeRegistrationResponse] = Json.format[SchemeRegistrationResponse]
}