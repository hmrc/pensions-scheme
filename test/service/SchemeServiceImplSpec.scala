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
import audit.{PSASubscription, SchemeList}
import base.SpecBase
import connector.{BarsConnector, SchemeConnector}
import models._
import org.joda.time.LocalDate
import org.scalatest.{AsyncFlatSpec, Matchers}
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, RequestHeader}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class SchemeServiceImplSpec extends AsyncFlatSpec with Matchers {

  import FakeSchemeConnector._
  import SchemeServiceImplSpec._

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
      httpResponse =>
        fixture.auditService.verifySent(SchemeList(psaId, Status.OK, Some(httpResponse.json))) shouldBe true
    }

  }

  it should "send an audit event on failure" in {

    val fixture = testFixture()

    fixture.schemeConnector.setListOfSchemesResponse(Future.failed(new BadRequestException("bad request")))

    fixture.schemeService.listOfSchemes(psaId)
      .map(_ => fail("Expected failure"))
      .recover {
        case _: BadRequestException =>
          fixture.auditService.verifySent(SchemeList(psaId, Status.BAD_REQUEST, None)) shouldBe true
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
    val requestJson = registerPsaRequestJson(psaJson)

    fixture.schemeService.registerPSA(psaJson).map {
      httpResponse =>
        fixture.auditService.lastEvent shouldBe
          Some(
            PSASubscription(
              existingUser = false,
              legalStatus = "test-legal-status",
              status = Status.OK,
              request = requestJson,
              response = Some(httpResponse.json)
            )
          )
    }

  }

  it should "send an audit event on failure" in {

    val fixture = testFixture()
    val requestJson = registerPsaRequestJson(psaJson)

    fixture.schemeConnector.setRegisterPsaResponse(Future.failed(new BadRequestException("bad request")))

    fixture.schemeService.registerPSA(psaJson)
      .map(_ => fail("Expected failure"))
      .recover {
        case _: BadRequestException =>
          fixture.auditService.lastEvent shouldBe
            Some(
              PSASubscription(
                existingUser = false,
                legalStatus = "test-legal-status",
                status = Status.BAD_REQUEST,
                request = requestJson,
                response = None
              )
            )
      }

  }

}

object SchemeServiceImplSpec extends SpecBase {

  trait TestFixture {
    val schemeConnector: FakeSchemeConnector = new FakeSchemeConnector()
    val barsConnector: FakeBarsConnector = new FakeBarsConnector()
    val auditService: StubSuccessfulAuditService = new StubSuccessfulAuditService()
    val schemeService: SchemeServiceImpl = new SchemeServiceImpl(schemeConnector, barsConnector,auditService, appConfig) {
      override def registerScheme(psaId: String, json: JsValue)
                                 (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[HttpResponse] = {
        throw new NotImplementedError()
      }
    }
  }

  def testFixture(): TestFixture = new TestFixture {}

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  val psaId: String = "test-psa-id"

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
    "individualContactAddress" -> Json.obj(
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

  def registerPsaRequestJson(userAnswersJson: JsValue): JsValue = {
    implicit val contactAddressEnabled: Boolean = true
    val psa = userAnswersJson.as[PensionSchemeAdministrator](PensionSchemeAdministrator.apiReads)
    val requestJson = Json.toJson(psa)(PensionSchemeAdministrator.psaSubmissionWrites)

    requestJson
  }

}

class FakeSchemeConnector extends SchemeConnector {

  import FakeSchemeConnector._

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

  override def getCorrelationId(requestId: Option[String]): String = "4725c81192514c069b8ff1d84659b2df"
}

object FakeSchemeConnector {

  val schemeRegistrationResponse = SchemeRegistrationResponse("test-processing-date", "test-scheme-reference-number")
  val schemeRegistrationResponseJson: JsValue = Json.toJson(schemeRegistrationResponse)

  val listOfSchemes: ListOfSchemes =
    ListOfSchemes(
      processingDate = "1969-07-20",
      totalSchemesRegistered = "0",
      schemeDetail = None
    )

  val listOfSchemesJson: JsValue = Json.toJson(listOfSchemes)

  val registerPsaResponseJson: JsValue =
    Json.obj(
      "processingDate" -> LocalDate.now,
      "formBundle" -> "1121313",
      "psaId" -> "A21999999"
    )

}

class FakeBarsConnector extends BarsConnector {

  import SchemeServiceSpec._

  override def invalidBankAccount(bankAccount: BankAccount, psaId: String)
                                 (implicit ec: ExecutionContext, hc: HeaderCarrier, rh: RequestHeader): Future[Boolean] = {
    bankAccount match {
      case BankAccount(_, accountNumber) if accountNumber == invalidAccountNumber => Future.successful(true)
      case BankAccount(_, accountNumber) if accountNumber == notInvalidAccountNumber => Future.successful(false)
      case _ => throw new IllegalArgumentException(s"No stub behaviour for bank account: $bankAccount")
    }
  }

}
