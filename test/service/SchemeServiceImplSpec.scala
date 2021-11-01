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
import audit.{RACDACDeclarationAuditEvent, SchemeAuditService, SchemeSubscription, SchemeUpdate, SchemeType => AuditSchemeType}
import base.SpecBase
import connector.{BarsConnector, SchemeConnector}
import models._
import models.enumeration.SchemeType
import models.userAnswersToEtmp.establisher.EstablisherDetails
import models.userAnswersToEtmp.trustee.TrusteeDetails
import models.userAnswersToEtmp.{BankAccount, CustomerAndSchemeDetails, PensionSchemeDeclaration, PensionsScheme}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.MockitoSugar._
import org.scalatest.EitherValues
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.http.Status
import play.api.libs.json._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}

import scala.concurrent.Future

class SchemeServiceImplSpec
  extends AsyncFlatSpec
    with Matchers
    with EitherValues {

  import SchemeServiceImplSpec._

  "listOfSchemes" should "return the list of schemes from the connector" in {
    when(schemeConnector.listOfSchemes(any(), any())(any(), any(), any())).thenReturn(Future.successful(Right(listOfSchemesJson)))
    schemeService.listOfSchemes("PSA", psaId).map { httpResponse =>
      httpResponse.right.value shouldBe listOfSchemesJson
    }
  }

  "haveInvalidBank" must "set the pension scheme's haveInvalidBank to true if the bank account is invalid" in {
    val account = bankAccount(invalidAccountNumber)
    when(barsConnector.invalidBankAccount(any(), any())(any(), any(), any())).thenReturn(Future.successful(true))

    schemeService.haveInvalidBank(Some(account), pensionsScheme, psaId).map {
      scheme =>
        scheme.customerAndSchemeDetails.haveInvalidBank mustBe true
    }
  }

  it must "set the pension scheme's haveInvalidBank to false if the bank account is not invalid" in {
    val account = bankAccount(notInvalidAccountNumber)
    when(barsConnector.invalidBankAccount(any(), any())(any(), any(), any())).thenReturn(Future.successful(false))

    schemeService.haveInvalidBank(Some(account), pensionsScheme, psaId).map {
      scheme =>
        scheme.customerAndSchemeDetails.haveInvalidBank mustBe false
    }
  }

  it must "set the pension scheme's haveInvalidBank to false if the scheme does not have a bank account" in {

    schemeService.haveInvalidBank(None, pensionsScheme, psaId).map {
      scheme =>
        scheme.customerAndSchemeDetails.haveInvalidBank mustBe false
    }
  }

  "readBankAccount" must "return a bank account where it exists in json" in {
    val json = bankDetailsJson(notInvalidAccountNumber)

    val actual = schemeService.readBankAccount(json)
    actual mustBe Right(Some(bankAccount(notInvalidAccountNumber)))
  }

  it must "return None where no account exists in json" in {
    val actual = schemeService.readBankAccount(Json.obj())
    actual mustBe Right(None)
  }

  it must "return bad request exception where uKBankDetails present but account invalid" in {
    val actual = schemeService.readBankAccount(Json.obj("uKBankDetails" -> "invalid"))
    actual.isLeft mustBe true
    actual.left.toOption.map(_.message).getOrElse("") mustBe "Invalid bank account details"
  }

  "registerScheme" must "return the result of submitting a normal " +
    "(non RAC/DAC) pensions scheme and contain no racdacScheme node" in {
    reset(schemeConnector)
    val regDataWithRacDacNode = schemeJsValue.as[JsObject]
    when(schemeConnector.registerScheme(any(), any())(any(), any(), any())).
      thenReturn(Future.successful(Right(schemeRegistrationResponseJson)))
    schemeService.registerScheme(psaId, pensionsSchemeJson).map {
      response =>
        val json = response.right.value

        json.transform((__ \ 'pensionSchemeDeclaration \ 'declaration1).json.pick).asOpt mustBe None
        verify(schemeConnector, times(1)).registerScheme(any(), eqTo(regDataWithRacDacNode))(any(), any(), any())
        json.validate[SchemeRegistrationResponse] mustBe JsSuccess(schemeRegistrationResponse)
    }
  }

  "registerScheme" must "return the result of submitting a RAC/DAC pensions scheme" in {
    reset(schemeConnector)
    when(schemeConnector.registerScheme(any(), any())(any(), any(), any())).
      thenReturn(Future.successful(Right(schemeRegistrationResponseJson)))

    schemeService.registerScheme(psaId, racDACPensionsSchemeJson).map {
      response =>
        val json = response.right.value
        verify(schemeConnector, times(1)).registerScheme(any(), eqTo(racDacRegisterData))(any(), any(), any())
        json.validate[SchemeRegistrationResponse] mustBe JsSuccess(schemeRegistrationResponse)
    }
  }

  "register scheme" must "send a SchemeSubscription audit event following a successful submission" in {
    reset(schemeConnector)
    when(schemeConnector.registerScheme(any(), any())(any(), any(), any())).
      thenReturn(Future.successful(Right(schemeRegistrationResponseJson)))
    schemeService.registerScheme(psaId, pensionsSchemeJson).map {
      response =>
        val json = response.right.value
        val expected = schemeSubscription.copy(
          hasIndividualEstablisher = true,
          status = Status.OK,
          request = expectedJsonForAudit,
          response = Some(json)
        )
        auditService.verifySent(expected) mustBe true
    }
  }

  it must "send a SchemeSubscription audit event following an unsuccessful submission" in {
    reset(schemeConnector)
    when(schemeConnector.registerScheme(any(), any())(any(), any(), any())).
      thenReturn(Future.failed(new BadRequestException("bad request")))

    schemeService.registerScheme(psaId, pensionsSchemeJson)
      .map(_ => fail("Expected failure"))
      .recover {
        case _: BadRequestException =>
          val expected = schemeSubscription.copy(
            hasIndividualEstablisher = true,
            status = Status.BAD_REQUEST,
            request = expectedJsonForAudit,
            response = None
          )
          auditService.verifySent(expected) mustBe true
      }
  }

  "register RAC DAC scheme" must "send a RACDACDeclaration audit event following a successful submission" in {
    reset(schemeConnector)
    when(schemeConnector.registerScheme(any(), any())(any(), any(), any())).
      thenReturn(Future.successful(Right(schemeRegistrationResponseJson)))
    schemeService.registerScheme(psaId, racDACPensionsSchemeJson).map {
      response =>
        val json = response.right.value
        val expected = RACDACDeclarationAuditEvent(
          psaIdentifier = psaId,
          status = Status.OK,
          request = racDacRegisterData,
          response = Some(json)
        )
        auditService.verifyExtendedSent(expected) mustBe true
    }
  }

  it must "send a RACDACDeclaration audit event following an unsuccessful submission" in {
    reset(schemeConnector)
    when(schemeConnector.registerScheme(any(), any())(any(), any(), any())).
      thenReturn(Future.failed(new BadRequestException("bad request")))

    schemeService.registerScheme(psaId, racDACPensionsSchemeJson)
      .map(_ => fail("Expected failure"))
      .recover {
        case _: BadRequestException =>
          val expected = RACDACDeclarationAuditEvent(
            psaIdentifier = psaId,
            status = Status.BAD_REQUEST,
            request = racDacRegisterData,
            response = None
          )
          auditService.verifyExtendedSent(expected) mustBe true
      }
  }

  "updateScheme" must "return the result of submitting the pensions scheme and have the right declaration type" in {
    reset(schemeConnector)
    when(schemeConnector.updateSchemeDetails(any(), any())(any(), any(), any())).
      thenReturn(Future.successful(Right(schemeRegistrationResponseJson)))
    schemeService.updateScheme(pstr, psaId, pensionsSchemeJson).map {
      response =>
        val json = response.right.value
        verify(schemeConnector, times(1)).updateSchemeDetails(
          any(), eqTo(Json.parse(updateSchemeRegisterData)))(any(), any(), any())
        json.validate[SchemeRegistrationResponse] mustBe JsSuccess(schemeRegistrationResponse)
    }
  }

  it must "send a SchemeUpdate audit event following a successful submission" in {
    reset(schemeConnector)
    when(schemeConnector.updateSchemeDetails(any(), any())(any(), any(), any())).
      thenReturn(Future.successful(Right(schemeRegistrationResponseJson)))
    schemeService.updateScheme(pstr, psaId, pensionsSchemeJson).map { _ =>
      val expectedAuditEvent =
        SchemeUpdate(psaIdentifier = psaId,
          schemeType = Some(audit.SchemeType.singleTrust),
          status = Status.OK,
          request = schemeUpdateRequestJson,
          response = Some(schemeRegistrationResponseJson))

      auditService.verifySent(expectedAuditEvent) mustBe true
    }
  }

  it must "send a SchemeUpdate audit event following an unsuccessful submission" in {
    reset(schemeConnector)
    when(schemeConnector.updateSchemeDetails(any(), any())(any(), any(), any())).
      thenReturn(Future.failed(new BadRequestException("bad request")))
    schemeService.updateScheme(pstr, psaId, pensionsSchemeJson)
      .map(_ => fail("Expected failure"))
      .recover {
        case _: BadRequestException =>
          val expectedAuditEvent =
            SchemeUpdate(psaIdentifier = psaId,
              schemeType = Some(audit.SchemeType.singleTrust),
              status = Status.BAD_REQUEST,
              request = schemeUpdateRequestJson,
              response = None
            )
          auditService.verifySent(expectedAuditEvent) mustBe true
      }
  }
}

object SchemeServiceImplSpec extends SpecBase {

  private val schemeConnector: SchemeConnector = mock[SchemeConnector]
  private val barsConnector: BarsConnector = mock[BarsConnector]
  private val auditService: StubSuccessfulAuditService = new StubSuccessfulAuditService()

  private val schemeService: SchemeServiceImpl = new SchemeServiceImpl(
    schemeConnector,
    barsConnector,
    auditService,
    new SchemeAuditService
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("", "")

  val psaId: String = "test-psa-id"
  val listOfSchemes: ListOfSchemes =
    ListOfSchemes(
      processingDate = "1969-07-20",
      totalSchemesRegistered = "0",
      schemeDetails = None
    )

  val listOfSchemesJson: JsValue = Json.toJson(listOfSchemes)
  val pstr: String = "test-pstr"

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

  val racDacRegisterData = Json.obj(
    "racdacScheme" -> true,
    "racdacSchemeDetails" -> Json.obj(
      "racdacName" -> "test-scheme-name",
      "contractOrPolicyNumber" -> "121212"
    ),
    "racdacSchemeDeclaration" -> Json.obj(
      "box12" -> true,
      "box13" -> true,
      "box14" -> true
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

  def bankAccount(accountNumber: String): BankAccount =
    BankAccount("001100", accountNumber)

  val invalidAccountNumber: String = "111"
  val notInvalidAccountNumber: String = "112"

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
    "moneyPurchaseBenefits" -> "05",
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

  private val schemeJsValue: JsValue =
    Json.parse(
      """
        |{
        |  "establisherDetails": {
        |    "individual": [
        |      {
        |        "personalDetails": {
        |          "firstName": "test-first-name",
        |          "lastName": "test-last-name",
        |          "dateOfBirth": "1969-07-20"
        |        },
        |        "correspondenceAddressDetails": {
        |          "addressDetails": {
        |            "line1": "test-address-line-1",
        |            "countryCode": "test-country",
        |            "addressType": "NON-UK"
        |          }
        |        },
        |        "correspondenceContactDetails": {
        |          "contactDetails": {
        |            "telephone": "test-phone-number",
        |            "email": "test-email-address"
        |          }
        |        }
        |      }
        |    ],
        |    "companyOrOrganization": [],
        |    "partnership": []
        |  },
        |  "customerAndSchemeDetails": {
        |    "schemeName": "test-scheme-name",
        |    "isSchemeMasterTrust": false,
        |    "schemeStructure": "A single trust under which all of the assets are held for the benefit of all members of the scheme",
        |    "currentSchemeMembers": "0",
        |    "futureSchemeMembers": "0",
        |    "isRegulatedSchemeInvestment": false,
        |    "isOccupationalPensionScheme": false,
        |    "areBenefitsSecuredContractInsuranceCompany": false,
        |    "doesSchemeProvideBenefits": "Money Purchase benefits only (defined contribution)",
        |    "tcmpBenefitType": "05",
        |    "schemeEstablishedCountry": "test-scheme-established-country",
        |    "haveInvalidBank": false,
        |    "insuranceCompanyName": "Test insurance company name",
        |    "policyNumber": "Test insurance policy number"
        |  },
        |  "pensionSchemeDeclaration": {
        |    "box1": false,
        |    "box2": false,
        |    "box6": false,
        |    "box7": false,
        |    "box8": false,
        |    "box9": false,
        |    "box10": true
        |  },
        |  "trusteeDetails": {
        |    "individualTrusteeDetail": [],
        |    "companyTrusteeDetail": [],
        |    "partnershipTrusteeDetail": []
        |  }
        |}
        |""".stripMargin)

  private val racDACPensionsSchemeJson: JsValue = Json.obj(
    "racdac" -> Json.obj(
      "name" -> "test-scheme-name",
      "contractOrPolicyNumber" -> "121212",
      "declaration" -> true
    )
  )

  val schemeRegistrationResponse: SchemeRegistrationResponse = SchemeRegistrationResponse(
    "test-processing-date",
    "test-scheme-reference-number")
  val schemeRegistrationResponseJson: JsValue =
    Json.toJson(schemeRegistrationResponse)

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
      "tcmpBenefitType":"05",
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
      |      "tcmpBenefitType":"05",
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

  private val updateSchemeRegisterData =
    """{
      |  "schemeDetails": {
      |    "changeOfschemeDetails": false,
      |    "psaid": "test-psa-id",
      |    "schemeName": "test-scheme-name",
      |    "schemeStatus": "Open",
      |    "isSchemeMasterTrust": false,
      |    "pensionSchemeStructure": "A single trust under which all of the assets are held for the benefit of all members of the scheme",
      |    "currentSchemeMembers": "0",
      |    "futureSchemeMembers": "0",
      |    "isRegulatedSchemeInvestment": false,
      |    "isOccupationalPensionScheme": false,
      |    "schemeProvideBenefits": "Money Purchase benefits only (defined contribution)",
      |    "tcmpBenefitType": "05",
      |    "schemeEstablishedCountry": "test-scheme-established-country",
      |    "insuranceCompanyDetails": {
      |      "isInsuranceDetailsChanged": false,
      |      "isSchemeBenefitsInsuranceCompany": false,
      |      "insuranceCompanyName": "Test insurance company name",
      |      "policyNumber": "Test insurance policy number"
      |    }
      |  },
      |  "pensionSchemeDeclaration": {
      |    "declaration1": false
      |  },
      |  "establisherAndTrustDetailsType": {
      |    "changeOfEstablisherOrTrustDetails": false,
      |    "haveMoreThanTenTrustees": false,
      |    "establisherDetails": {
      |      "individualDetails": [
      |        {
      |          "personalDetails": {
      |            "firstName": "test-first-name",
      |            "lastName": "test-last-name",
      |            "dateOfBirth": "1969-07-20"
      |          },
      |          "previousAddressDetails": {
      |            "isPreviousAddressLast12Month": false
      |          },
      |          "correspondenceContactDetails": {
      |            "contactDetails": {
      |              "telephone": "test-phone-number",
      |              "email": "test-email-address"
      |            }
      |          },
      |          "correspondenceAddressDetails": {
      |            "addressDetails": {
      |              "line1": "test-address-line-1",
      |              "countryCode": "test-country",
      |              "nonUKAddress": true
      |            }
      |          }
      |        }
      |      ]
      |    }
      |  }
      |}""".stripMargin

}

case class SchemeRegistrationResponse(processingDate: String, schemeReferenceNumber: String)

object SchemeRegistrationResponse {
  implicit val formatsSchemeRegistrationResponse: Format[SchemeRegistrationResponse] = Json.format[SchemeRegistrationResponse]
}


