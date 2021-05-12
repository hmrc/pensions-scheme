/*
 * Copyright 2021 HM Revenue & Customs
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
import audit.{SchemeSubscription, SchemeUpdate, SchemeType => AuditSchemeType}
import base.SpecBase
import models.FeatureToggle.Disabled
import models.FeatureToggle.Enabled
import models.FeatureToggleName.{TCMP, RACDAC}
import models.enumeration.SchemeType
import models.userAnswersToEtmp._
import models.userAnswersToEtmp.establisher.{Partnership, CompanyEstablisher, EstablisherDetails}
import models.userAnswersToEtmp.reads.CommonGenerator.{establisherPartnershipGenerator, establisherCompanyGenerator, establisherIndividualGenerator}
import models.userAnswersToEtmp.trustee.TrusteeDetails
import org.mockito.Mockito.when
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.http.Status
import play.api.libs.json.{__, _}
import play.api.mvc.{RequestHeader, AnyContentAsEmpty}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{BadRequestException, HttpResponse, HeaderCarrier}
import utils.Lens

import scala.concurrent.{ExecutionContext, Future}

class SchemeServiceSpec extends AsyncFlatSpec with ScalaCheckDrivenPropertyChecks with BeforeAndAfterEach {

  import FakeSchemeConnector._
  import SchemeServiceSpec._

  override def beforeEach(): Unit = {
    org.mockito.Mockito.reset(featureToggleService)

    when(featureToggleService.get(org.mockito.Matchers.any())).thenReturn(Future.successful(Disabled(TCMP)))
    super.beforeEach()
  }

  //"haveInvalidBank" must "set the pension scheme's haveInvalidBank to true if the bank account is invalid" in {
  //
  //  val account = bankAccount(invalidAccountNumber)
  //
  //  testFixture().schemeService.haveInvalidBank(Some(account), pensionsScheme, psaId).map {
  //    scheme =>
  //      scheme.customerAndSchemeDetails.haveInvalidBank mustBe true
  //  }
  //
  //}
  //
  //it must "set the pension scheme's haveInvalidBank to false if the bank account is not invalid" in {
  //
  //  val account = bankAccount(notInvalidAccountNumber)
  //
  //  testFixture().schemeService.haveInvalidBank(Some(account), pensionsScheme, psaId).map {
  //    scheme =>
  //      scheme.customerAndSchemeDetails.haveInvalidBank mustBe false
  //  }
  //
  //}
  //
  //it must "set the pension scheme's haveInvalidBank to false if the scheme does not have a bank account" in {
  //
  //  testFixture().schemeService.haveInvalidBank(None, pensionsScheme, psaId).map {
  //    scheme =>
  //      scheme.customerAndSchemeDetails.haveInvalidBank mustBe false
  //  }
  //
  //}
  //
  //"readBankAccount" must "return a bank account where it exists in json" in {
  //
  //  val json = bankDetailsJson(notInvalidAccountNumber)
  //
  //  val actual = testFixture().schemeService.readBankAccount(json)
  //  actual mustBe Right(Some(bankAccount(notInvalidAccountNumber)))
  //
  //}
  //
  //it must "return None where no account exists in json" in {
  //
  //  val actual = testFixture().schemeService.readBankAccount(Json.obj())
  //  actual mustBe Right(None)
  //
  //}
  //
  //it must "return bad request exception where uKBankDetails present but account invalid" in {
  //  val actual = testFixture().schemeService.readBankAccount(Json.obj("uKBankDetails" -> "invalid"))
  //  actual.isLeft mustBe true
  //  actual.left.toOption.map(_.message).getOrElse("") mustBe "Invalid bank account details"
  //}
  //
  //"registerScheme" must "return the result of submitting the pensions scheme" in {
  //
  //  testFixture().schemeService.registerScheme(psaId, pensionsSchemeJson).map {
  //    response =>
  //      response.status mustBe Status.OK
  //      val json = Json.parse(response.body)
  //
  //      json.transform((__ \ 'pensionSchemeDeclaration \ 'declaration1).json.pick).asOpt mustBe None
  //
  //      json.validate[SchemeRegistrationResponse] mustBe JsSuccess(schemeRegistrationResponse)
  //
  //
  //  }
  //
  //}

  "registerScheme (when RAC DAC toggled on)" must "return the result of submitting a normal (non RAC/DAC) pensions scheme" in {
    when(featureToggleService.get(org.mockito.Matchers.eq(RACDAC))).thenReturn(Future.successful(Enabled(RACDAC)))
    testFixture().schemeService.registerScheme(psaId, pensionsSchemeJson).map {
      response =>
        response.status mustBe Status.OK
        val json = Json.parse(response.body)

        json.transform((__ \ 'pensionSchemeDeclaration \ 'declaration1).json.pick).asOpt mustBe None

        json.validate[SchemeRegistrationResponse] mustBe JsSuccess(schemeRegistrationResponse)
    }
  }

  "registerScheme (when RAC DAC toggled on)" must "return the result of submitting a RAC/DAC pensions scheme" in {
    when(featureToggleService.get(org.mockito.Matchers.eq(RACDAC))).thenReturn(Future.successful(Enabled(RACDAC)))
    testFixture().schemeService.registerScheme(psaId, racDACPensionsSchemeJson).map {
      response =>
        response.status mustBe Status.OK

        val json = Json.parse(response.body)
        json.validate[SchemeRegistrationResponse] mustBe JsSuccess(schemeRegistrationResponse)
    }
  }

  //it must "send a SchemeSubscription audit event following a successful submission" in {
  //  val fixture = testFixture()
  //  fixture.schemeService.registerScheme(psaId, pensionsSchemeJson).map {
  //    httpResponse =>
  //      val expected = schemeSubscription.copy(
  //        hasIndividualEstablisher = true,
  //        status = Status.OK,
  //        request = expectedJsonForAudit,
  //        response = Some(httpResponse.json)
  //      )
  //      fixture.auditService.lastEvent mustBe Some(expected)
  //  }
  //}
  //
  //it must "not send a SchemeSubscription audit event following an unsuccessful submission" in {
  //
  //  val fixture = testFixture()
  //
  //  fixture.schemeConnector.setRegisterSchemeResponse(Future.failed(new BadRequestException("bad request")))
  //
  //  fixture.schemeService.registerScheme(psaId, pensionsSchemeJson)
  //    .map(_ => fail("Expected failure"))
  //    .recover {
  //      case _: BadRequestException =>
  //        val expected = schemeSubscription.copy(
  //          hasIndividualEstablisher = true,
  //          status = Status.BAD_REQUEST,
  //          request = expectedJsonForAudit,
  //          response = None
  //        )
  //
  //        fixture.auditService.lastEvent mustBe Some(expected)
  //    }
  //
  //}
  //
  //"translateSchemeSubscriptionEvent" must "translate a master trust scheme" in {
  //
  //  val scheme = PensionsSchemeIsSchemeMasterTrust.set(pensionsScheme, true)
  //
  //  val actual = testFixture().schemeService.translateSchemeSubscriptionEvent(psaId, scheme, hasBankDetails = false, status = Status.OK, response = None)
  //
  //  val expected = schemeSubscription.copy(
  //    schemeType = Some(AuditSchemeType.masterTrust),
  //    request = Json.toJson(scheme)
  //  )
  //
  //  actual mustBe expected
  //
  //}
  //
  //it must "translate a single trust scheme" in {
  //
  //  val scheme = PensionsSchemeSchemeStructure.set(pensionsScheme, Some(SchemeType.single.value))
  //
  //  val actual = testFixture().schemeService.translateSchemeSubscriptionEvent(psaId, scheme, hasBankDetails = false, Status.OK, None)
  //
  //  val expected = schemeSubscription.copy(
  //    request = Json.toJson(scheme)
  //  )
  //
  //  actual mustBe expected
  //
  //}
  //
  //it must "translate a group Life/Death scheme" in {
  //
  //  val scheme = PensionsSchemeSchemeStructure.set(pensionsScheme, Some(SchemeType.group.value))
  //
  //  val actual = testFixture().schemeService.translateSchemeSubscriptionEvent(psaId, scheme, hasBankDetails = false, Status.OK, None)
  //
  //  val expected = schemeSubscription.copy(
  //    schemeType = Some(AuditSchemeType.groupLifeDeath),
  //    request = Json.toJson(scheme)
  //  )
  //
  //  actual mustBe expected
  //
  //}
  //
  //it must "translate a body corporate scheme" in {
  //
  //  val scheme = PensionsSchemeSchemeStructure.set(pensionsScheme, Some(SchemeType.corp.value))
  //
  //  val actual = testFixture().schemeService.translateSchemeSubscriptionEvent(psaId, scheme, hasBankDetails = false, Status.OK, None)
  //
  //  val expected = schemeSubscription.copy(
  //    schemeType = Some(AuditSchemeType.bodyCorporate),
  //    request = Json.toJson(scheme)
  //  )
  //
  //  actual mustBe expected
  //
  //}
  //
  //it must "translate an 'other' scheme" in {
  //
  //  val scheme = PensionsSchemeSchemeStructure.set(pensionsScheme, Some(SchemeType.other.value))
  //
  //  val actual = testFixture().schemeService.translateSchemeSubscriptionEvent(psaId, scheme, hasBankDetails = false, Status.OK, None)
  //
  //  val expected = schemeSubscription.copy(
  //    schemeType = Some(AuditSchemeType.other),
  //    request = Json.toJson(scheme)
  //  )
  //
  //  actual mustBe expected
  //
  //}
  //
  //it must "translate a scheme with individual establishers" in {
  //
  //  forAll(establisherIndividualGenerator()) {
  //    json =>
  //      val individual = json.as[Individual](Individual.readsEstablisherIndividual)
  //      val scheme =
  //        PensionsSchemeSchemeStructure
  //          .set(pensionsScheme, Some(SchemeType.single.value))
  //          .copy(establisherDetails =
  //            EstablisherDetails(
  //              companyOrOrganization = Nil,
  //              individual = Seq(individual),
  //              partnership = Nil
  //            )
  //          )
  //
  //      val actual = testFixture().schemeService.translateSchemeSubscriptionEvent(psaId, scheme, hasBankDetails = false, Status.OK, None)
  //
  //      val expected = schemeSubscription.copy(
  //        hasIndividualEstablisher = true,
  //        request = Json.toJson(scheme)
  //      )
  //
  //      actual mustBe expected
  //
  //  }
  //}
  //
  //it must "translate a scheme with company establishers" in {
  //  forAll(establisherCompanyGenerator()) {
  //    json =>
  //      val estCom = json.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)
  //      val scheme =
  //        PensionsSchemeSchemeStructure
  //          .set(pensionsScheme, Some(SchemeType.single.value))
  //          .copy(establisherDetails =
  //            EstablisherDetails(
  //              companyOrOrganization = Seq(estCom),
  //              individual = Nil,
  //              partnership = Nil
  //            )
  //          )
  //
  //      val actual = testFixture().schemeService.translateSchemeSubscriptionEvent(psaId, scheme, hasBankDetails = false, Status.OK, None)
  //
  //      val expected = schemeSubscription.copy(
  //        hasCompanyEstablisher = true,
  //        request = Json.toJson(scheme)
  //      )
  //
  //      actual mustBe expected
  //
  //  }
  //}
  //
  //it must "translate a scheme with partnership establishers" in {
  //  forAll(establisherPartnershipGenerator()) {
  //    json =>
  //      val estPart = json.as[Partnership](Partnership.readsEstablisherPartnership)
  //      val scheme =
  //        PensionsSchemeSchemeStructure
  //          .set(pensionsScheme, Some(SchemeType.single.value))
  //          .copy(establisherDetails =
  //            EstablisherDetails(
  //              companyOrOrganization = Nil,
  //              individual = Nil,
  //              partnership = Seq(estPart)
  //            )
  //          )
  //
  //      val actual = testFixture().schemeService.translateSchemeSubscriptionEvent(psaId, scheme, hasBankDetails = false, Status.OK, None)
  //
  //      val expected = schemeSubscription.copy(
  //        hasPartnershipEstablisher = true,
  //        request = Json.toJson(scheme)
  //      )
  //
  //      actual mustBe expected
  //  }
  //}
  //
  //it must "translate a scheme with dormant company, bank details, and invalid bank details" in {
  //
  //  val declaration = pensionsScheme.pensionSchemeDeclaration.asInstanceOf[PensionSchemeDeclaration]
  //
  //  val scheme = pensionsScheme.copy(
  //    customerAndSchemeDetails = pensionsScheme.customerAndSchemeDetails.copy(haveInvalidBank = true),
  //    pensionSchemeDeclaration = declaration.copy(box5 = Some(true))
  //  )
  //
  //  val actual = testFixture().schemeService.translateSchemeSubscriptionEvent(psaId, scheme, hasBankDetails = true, Status.OK, None)
  //
  //  val expected = schemeSubscription.copy(
  //    schemeType = Some(AuditSchemeType.singleTrust),
  //    hasDormantCompany = true,
  //    hasBankDetails = true,
  //    hasValidBankDetails = false,
  //    request = Json.toJson(scheme)
  //  )
  //
  //  actual mustBe expected
  //
  //}
  //
  //"updateScheme" must "return the result of submitting the pensions scheme and have the right declaration type" in {
  //  val f = new UpdateTestFixture() {}
  //
  //  f.schemeService.updateScheme(pstr, psaId, pensionsSchemeJson).map {
  //    response =>
  //      response.status mustBe Status.OK
  //      val declaration1Value = f.schemeConnector.lastUpdateSchemeDetailsdata
  //        .transform((__ \ "pensionSchemeDeclaration" \ "declaration1").json.pick)
  //      declaration1Value.asOpt mustBe Some(JsBoolean(false))
  //  }
  //}
  //
  //it must "send a SchemeUpdate audit event following a successful submission" in {
  //  val f = new UpdateTestFixture() {}
  //
  //  f.schemeConnector.setUpdateSchemeResponse(Future.successful(HttpResponse.apply(Status.OK, testResponse.toString())))
  //  f.schemeService.updateScheme(pstr, psaId, pensionsSchemeJson).map { _ =>
  //    val expectedAuditEvent =
  //      SchemeUpdate(psaIdentifier = "test-psa-id",
  //        schemeType = Some(audit.SchemeType.singleTrust),
  //        status = Status.OK,
  //        request = schemeUpdateRequestJson,
  //        response = Some(testResponse))
  //
  //    f.auditService.lastEvent mustBe Some(expectedAuditEvent)
  //  }
  //}
  //
  //it must "send a SchemeUpdate audit event following an unsuccessful submission" in {
  //  val f = new UpdateTestFixture() {}
  //
  //  f.schemeConnector.setUpdateSchemeResponse(Future.failed(new BadRequestException("bad request")))
  //  f.schemeService.updateScheme(pstr, psaId, pensionsSchemeJson)
  //    .map(_ => fail("Expected failure"))
  //    .recover {
  //      case _: BadRequestException =>
  //        val expectedAuditEvent =
  //          SchemeUpdate(psaIdentifier = "test-psa-id",
  //            schemeType = Some(audit.SchemeType.singleTrust),
  //            status = Status.BAD_REQUEST,
  //            request = schemeUpdateRequestJson,
  //            response = None
  //          )
  //        f.auditService.lastEvent mustBe Some(expectedAuditEvent)
  //    }
  //}
}

object SchemeServiceSpec extends SpecBase with MockitoSugar {
  private val testResponse = Json.obj(
    "testDesResponse" -> "a response"
  )

  private val featureToggleService: FeatureToggleService = mock[FeatureToggleService]

  class FakeSchemeConnectorStoreJson extends FakeSchemeConnector {
    var lastUpdateSchemeDetailsdata: JsValue = JsNull

    override def updateSchemeDetails(pstr: String, data: JsValue, tcmpToggle: Boolean)(
      implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[HttpResponse] = {
      lastUpdateSchemeDetailsdata = data
      updateSchemeResponse
    }
  }

  trait UpdateTestFixture {
    val schemeConnector: FakeSchemeConnectorStoreJson = new FakeSchemeConnectorStoreJson()
    val barsConnector: FakeBarsConnector = new FakeBarsConnector()
    val auditService: StubSuccessfulAuditService = new StubSuccessfulAuditService()
    val schemeService: SchemeServiceImpl = new SchemeServiceImpl(
      schemeConnector, barsConnector, auditService, featureToggleService)
  }

  trait TestFixture {
    val schemeConnector: FakeSchemeConnector = new FakeSchemeConnector()
    val barsConnector: FakeBarsConnector = new FakeBarsConnector()
    val auditService: StubSuccessfulAuditService = new StubSuccessfulAuditService()
    val schemeService: SchemeServiceImpl = new SchemeServiceImpl(
      schemeConnector, barsConnector, auditService, featureToggleService)
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

  val pensionsScheme: PensionsScheme = PensionsScheme(
    CustomerAndSchemeDetails(
      schemeName = "test-pensions-scheme",
      isSchemeMasterTrust = false,
      schemeStructure = Some(SchemeType.single.value),
      currentSchemeMembers = "test-current-scheme-members",
      futureSchemeMembers = "test-future-scheme-members",
      isRegulatedSchemeInvestment = false,
      isOccupationalPensionScheme = false,
      areBenefitsSecuredContractInsuranceCompany = false,
      doesSchemeProvideBenefits = "test-does-scheme-provide-benefits",
      tcmpBenefitType = Some("01"),
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

  val expectedJsonForAudit: JsValue = Json.parse(
    """{
   "customerAndSchemeDetails":{
      "schemeName":"test-scheme-name",
      "isSchemeMasterTrust":false,
      "schemeStructure":"A single trust under which all of the assets are held for the benefit of all members of the scheme",
      "currentSchemeMembers":"0",
      "futureSchemeMembers":"0",
      "isRegulatedSchemeInvestment":false,
      "isOccupationalPensionScheme":false,
      "areBenefitsSecuredContractInsuranceCompany":false,
      "doesSchemeProvideBenefits":"Money Purchase benefits only (defined contribution)",
      "schemeEstablishedCountry":"test-scheme-established-country",
      "haveInvalidBank":false,
      "insuranceCompanyName":"Test insurance company name",
      "policyNumber":"Test insurance policy number"
   },
   "pensionSchemeDeclaration":{
      "box1":false,
      "box2":false,
      "box6":false,
      "box7":false,
      "box8":false,
      "box9":false,
      "box10":true
   },
   "establisherDetails":{
      "individual":[
         {
            "personalDetails":{
               "firstName":"test-first-name",
               "lastName":"test-last-name",
               "dateOfBirth":"1969-07-20"
            },
            "correspondenceAddressDetails":{
               "addressDetails":{
                  "line1":"test-address-line-1",
                  "countryCode":"test-country",
                  "addressType":"NON-UK"
               }
            },
            "correspondenceContactDetails":{
               "contactDetails":{
                  "telephone":"test-phone-number",
                  "email":"test-email-address"
               }
            }
         }
      ],
      "companyOrOrganization":[

      ],
      "partnership":[

      ]
   },
   "trusteeDetails":{
      "individualTrusteeDetail":[

      ],
      "companyTrusteeDetail":[

      ],
      "partnershipTrusteeDetail":[

      ]
   }
  }""")

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
          "lastName" -> "test-last-name"
        ),
        "dateOfBirth" -> "1969-07-20",
        "contactDetails" -> Json.obj(
          "emailAddress" -> "test-email-address",
          "phoneNumber" -> "test-phone-number"
        ),
        "address" -> Json.obj(
          "addressLine1" -> "test-address-line-1",
          "country" -> "test-country"
        ),
        "addressYears" -> "test-address-years",
        "establisherKind" -> "individual"
      )
    )
  )

  private val racDACPensionsSchemeJson: JsValue = Json.obj(
    "racdac" -> Json.obj(
      "name" -> "test-scheme-name",
      "contractOrPolicyNumber" -> "121212",
      "declaration" -> true
    )
  )

  val schemeUpdateRequestJson: JsValue = Json.parse(
    """
      |{
      |   "customerAndSchemeDetails":{
      |      "schemeName":"test-scheme-name",
      |      "isSchemeMasterTrust":false,
      |      "schemeStructure":"A single trust under which all of the assets are held for the benefit of all members of the scheme",
      |      "currentSchemeMembers":"0",
      |      "futureSchemeMembers":"0",
      |      "isRegulatedSchemeInvestment":false,
      |      "isOccupationalPensionScheme":false,
      |      "areBenefitsSecuredContractInsuranceCompany":false,
      |      "doesSchemeProvideBenefits":"Money Purchase benefits only (defined contribution)",
      |      "schemeEstablishedCountry":"test-scheme-established-country",
      |      "haveInvalidBank":false,
      |      "insuranceCompanyName":"Test insurance company name",
      |      "policyNumber":"Test insurance policy number"
      |   },
      |   "pensionSchemeDeclaration":{
      |      "declaration1":false
      |   },
      |   "establisherDetails":{
      |      "individual":[
      |         {
      |            "personalDetails":{
      |               "firstName":"test-first-name",
      |               "lastName":"test-last-name",
      |               "dateOfBirth":"1969-07-20"
      |            },
      |            "correspondenceAddressDetails":{
      |               "addressDetails":{
      |                  "line1":"test-address-line-1",
      |                  "countryCode":"test-country",
      |                  "addressType":"NON-UK"
      |               }
      |            },
      |            "correspondenceContactDetails":{
      |               "contactDetails":{
      |                  "telephone":"test-phone-number",
      |                  "email":"test-email-address"
      |               }
      |            }
      |         }
      |      ],
      |      "companyOrOrganization":[
      |
      |      ],
      |      "partnership":[
      |
      |      ]
      |   },
      |   "trusteeDetails":{
      |      "individualTrusteeDetail":[
      |
      |      ],
      |      "companyTrusteeDetail":[
      |
      |      ],
      |      "partnershipTrusteeDetail":[
      |
      |      ]
      |   }
      |}
    """.stripMargin)

  val schemeSubscription: SchemeSubscription = SchemeSubscription(
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

  val schemeUpdate: SchemeUpdate = SchemeUpdate(
    psaIdentifier = psaId,
    schemeType = Some(AuditSchemeType.singleTrust),
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
