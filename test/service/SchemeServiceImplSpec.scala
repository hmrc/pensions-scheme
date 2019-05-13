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

import audit.SchemeList
import audit.testdoubles.StubSuccessfulAuditService
import base.SpecBase
import connector.{BarsConnector, SchemeConnector}
import models.Reads.schemes.SchemeDetailsStubData
import models._
import models.schemes.PsaSchemeDetails
import org.scalatest.{AsyncFlatSpec, EitherValues, Matchers}
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, RequestHeader}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{
  BadRequestException,
  HeaderCarrier,
  HttpException,
  HttpResponse
}

import scala.concurrent.{ExecutionContext, Future}

class SchemeServiceImplSpec
  extends AsyncFlatSpec
    with Matchers
    with EitherValues {

  import FakeSchemeConnector._
  import SchemeServiceImplSpec._

  "listOfSchemes" should "return the list of schemes from the connector" in {

    testFixture().schemeService.listOfSchemes(psaId).map { httpResponse =>
      httpResponse.status shouldBe Status.OK
      httpResponse.json shouldBe listOfSchemesJson
    }

  }

  it should "send an audit event on success" in {

    val fixture = testFixture()

    fixture.schemeService.listOfSchemes(psaId).map { httpResponse =>
      fixture.auditService.verifySent(
        SchemeList(psaId, Status.OK, Some(httpResponse.json))) shouldBe true
    }

  }

  it should "send an audit event on failure" in {

    val fixture = testFixture()

    fixture.schemeConnector.setListOfSchemesResponse(
      Future.failed(new BadRequestException("bad request")))

    fixture.schemeService
      .listOfSchemes(psaId)
      .map(_ => fail("Expected failure"))
      .recover {
        case _: BadRequestException =>
          fixture.auditService.verifySent(
            SchemeList(psaId, Status.BAD_REQUEST, None)) shouldBe true
      }

  }

}

object SchemeServiceImplSpec extends SpecBase {

  trait TestFixture {
    val schemeConnector: FakeSchemeConnector = new FakeSchemeConnector()
    val barsConnector: FakeBarsConnector = new FakeBarsConnector()
    val auditService: StubSuccessfulAuditService = new StubSuccessfulAuditService()

    val schemeService: SchemeServiceImpl = new SchemeServiceImpl(
      schemeConnector,
      barsConnector,
      auditService,
      appConfig) {
      override def registerScheme(psaId: String, json: JsValue)(
        implicit headerCarrier: HeaderCarrier,
        ec: ExecutionContext,
        request: RequestHeader): Future[HttpResponse] = {
        throw new NotImplementedError()
      }
    }
  }

  def testFixture(): TestFixture = new TestFixture {}

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("", "")

  val psaId: String = "test-psa-id"

}

class FakeSchemeConnector extends SchemeConnector {

  import FakeSchemeConnector._

  private var registerSchemeResponse = Future.successful(
    HttpResponse(Status.OK, Some(schemeRegistrationResponseJson)))

  protected var updateSchemeResponse = Future.successful(HttpResponse.apply(responseStatus = Status.OK))

  private var listOfSchemesResponse =
    Future.successful(HttpResponse(Status.OK, Some(listOfSchemesJson)))

  def setRegisterSchemeResponse(response: Future[HttpResponse]): Unit =
    this.registerSchemeResponse = response

  def setUpdateSchemeResponse(response: Future[HttpResponse]): Unit =
    this.updateSchemeResponse = response

  def setListOfSchemesResponse(response: Future[HttpResponse]): Unit =
    this.listOfSchemesResponse = response

  override def registerScheme(psaId: String, registerData: JsValue)(
    implicit
    headerCarrier: HeaderCarrier,
    ec: ExecutionContext,
    request: RequestHeader): Future[HttpResponse] = registerSchemeResponse

  override def listOfSchemes(psaId: String)(
    implicit
    headerCarrier: HeaderCarrier,
    ec: ExecutionContext,
    request: RequestHeader): Future[HttpResponse] = listOfSchemesResponse

  override def getSchemeDetails(psaId: String, schemeIdType: String, idNumber: String)(
    implicit headerCarrier: HeaderCarrier,
    ec: ExecutionContext,
    request: RequestHeader): Future[Either[HttpException, JsValue]] =
    validSchemeDetailsResponse

  override def getCorrelationId(requestId: Option[String]): String =
    "4725c81192514c069b8ff1d84659b2df"

  override def updateSchemeDetails(pstr: String, data: JsValue)(
    implicit headerCarrier: HeaderCarrier, ec: ExecutionContext, request: RequestHeader): Future[HttpResponse] = updateSchemeResponse
}

object FakeSchemeConnector extends SchemeDetailsStubData {

  val schemeRegistrationResponse = SchemeRegistrationResponse(
    "test-processing-date",
    "test-scheme-reference-number")
  val schemeRegistrationResponseJson: JsValue =
    Json.toJson(schemeRegistrationResponse)

  val schemeUpdateResponseJson: JsValue =
    Json.toJson(schemeRegistrationResponse)

  val listOfSchemes: ListOfSchemes =
    ListOfSchemes(
      processingDate = "1969-07-20",
      totalSchemesRegistered = "0",
      schemeDetail = None
    )

  val listOfSchemesJson: JsValue = Json.toJson(listOfSchemes)

  private val validSchemeDetailsResponse =
    Future.successful(Right(Json.toJson(psaSchemeDetailsSample)))
}

class FakeBarsConnector extends BarsConnector {

  import SchemeServiceSpec._

  override def invalidBankAccount(bankAccount: BankAccount, psaId: String)(
    implicit ec: ExecutionContext,
    hc: HeaderCarrier,
    rh: RequestHeader): Future[Boolean] = {
    bankAccount match {
      case BankAccount(_, accountNumber)
        if accountNumber == invalidAccountNumber =>
        Future.successful(true)
      case BankAccount(_, accountNumber)
        if accountNumber == notInvalidAccountNumber =>
        Future.successful(false)
      case _ =>
        throw new IllegalArgumentException(
          s"No stub behaviour for bank account: $bankAccount")
    }
  }

}
