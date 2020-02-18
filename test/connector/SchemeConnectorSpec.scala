/*
 * Copyright 2020 HM Revenue & Customs
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

package connector

import audit.testdoubles.StubSuccessfulAuditService
import audit.{AuditService, SchemeDetailsAuditEvent}
import base.JsonFileReader
import com.github.tomakehurst.wiremock.client.WireMock._
import org.joda.time.LocalDate
import org.scalatest._
import org.slf4j.event.Level
import play.api.LoggerLike
import play.api.http.Status
import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.JodaWrites._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.http._
import utils.{StubLogger, WireMockHelper}

class SchemeConnectorSpec extends AsyncFlatSpec
  with Matchers
  with WireMockHelper
  with OptionValues
  with RecoverMethods
  with EitherValues
  with ConnectorBehaviours with JsonFileReader{

  import SchemeConnectorSpec._

  override def beforeEach(): Unit = {
    auditService.reset()
    super.beforeEach()
  }
  override protected def portConfigKey: String = "microservice.services.des-hod.port"

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[AuditService].toInstance(auditService),
      bind[LoggerLike].toInstance(logger)
    )
  def connector: SchemeConnector = app.injector.instanceOf[SchemeConnector]

  "SchemeConnector registerScheme" should "handle OK (200)" in {
    val successResponse: JsObject = Json.obj("processingDate" -> LocalDate.now, "schemeReferenceNumber" -> "S0123456789")
    server.stubFor(
      post(urlEqualTo(schemeUrl))
        .withHeader("Content-Type", equalTo("application/json"))
        .withRequestBody(equalToJson(Json.stringify(registerSchemeData)))
        .willReturn(
          ok(Json.stringify(successResponse))
            .withHeader("Content-Type", "application/json")
        )
    )

    connector.registerScheme(psaId, registerSchemeData).map { response =>
      response.status shouldBe OK
    }
  }

  it should "handle FORBIDDEN (403) - INVALID_BUSINESS_PARTNER" in {
    server.stubFor(
      post(urlEqualTo(schemeUrl))
        .willReturn(
          forbidden
            .withHeader("Content-Type", "application/json")
            .withBody(invalidBusinessPartnerResponse)
        )
    )

    recoverToSucceededIf[Upstream4xxResponse] {
      connector.registerScheme(psaId, registerSchemeData)
    }
  }

  it should "handle CONFLICT (409) - DUPLICATE_SUBMISSION" in {
    server.stubFor(
      post(urlEqualTo(schemeUrl))
        .willReturn(
          aResponse()
            .withStatus(Status.CONFLICT)
            .withHeader("Content-Type", "application/json")
            .withBody(duplicateSubmissionResponse)
        )
    )

    recoverToSucceededIf[Upstream4xxResponse] {
      connector.registerScheme(psaId, registerSchemeData)
    }
  }

  it should "handle BAD_REQUEST (400)" in {
    server.stubFor(
      post(urlEqualTo(schemeUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody("Bad Request")
        )
    )

    recoverToSucceededIf[BadRequestException] {
      connector.registerScheme(psaId, registerSchemeData)
    }
  }

  it should "throw NotFoundException for NOT_FOUND (404) response" in {
    server.stubFor(
      post(urlEqualTo(schemeUrl))
        .willReturn(
          notFound
            .withHeader("Content-Type", "application/json")
            .withBody("Not Found")
        )
    )

    recoverToSucceededIf[NotFoundException] {
      connector.registerScheme(psaId, registerSchemeData)
    }
  }

  it should "log details of an INVALID_PAYLOAD for a BAD request (400) response" in {
    server.stubFor(
      post(urlEqualTo(schemeUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody("INVALID_PAYLOAD")
        )
    )

    logger.reset()

    connector.registerScheme(psaId, registerSchemeData).map(_ => fail("Expected failure"))
      .recover {
        case _: BadRequestException =>
          logger.getLogEntries.size shouldBe 1
          logger.getLogEntries.head.level shouldBe Level.WARN
        case _ => fail("Expected BadRequestException")
      }
  }

  "SchemeConnector listOfScheme" should "return OK with the list of schemes response" in {
    server.stubFor(
      get(listOfSchemeUrl)
        .willReturn(
          ok(Json.stringify(validListOfSchemeResponse))
        )
    )

    connector.listOfSchemes(psaId).map { response =>
      response.status shouldBe OK
      response.body shouldBe Json.stringify(validListOfSchemeResponse)
    }
  }

  it should "throw Bad Request Exception when DES/ETMP throws BadRequestException" in {
    server.stubFor(
      get(listOfSchemeUrl)
        .willReturn(
          badRequest()
        )
    )

    recoverToSucceededIf[BadRequestException] {
      connector.listOfSchemes(psaId)
    }
  }

  it should "throw NotFoundException when DES/ETMP throws NotFoundException" in {
    server.stubFor(
      get(listOfSchemeUrl)
        .willReturn(
          notFound()
        )
    )

    recoverToSucceededIf[NotFoundException] {
      connector.listOfSchemes(psaId)
    }
  }

  it should "throw UpStream5XXResponse when DES/ETMP throws Server error" in {
    server.stubFor(
      get(listOfSchemeUrl)
        .willReturn(
          serverError()
        )
    )

    recoverToSucceededIf[Upstream5xxResponse] {
      connector.listOfSchemes(psaId)
    }
  }

  "SchemeConnector getSchemeDetails" should "return user answer json" in {
    val desResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsResponse.json")
    val userAnswersResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsUserAnswers.json")

    server.stubFor(
      get(urlEqualTo(schemeDetailsUrl))
        .willReturn(
          ok
            .withHeader("Content-Type", "application/json")
            .withBody(desResponse.toString())
        )
    )
    connector.getSchemeDetails(psaId, schemeIdType, idNumber).map { response =>
      response.right.value shouldBe userAnswersResponse
    }
  }

  it should "return a BadRequestException for a 400 INVALID_IDTYPE response" in {
    server.stubFor(
      get(urlEqualTo(schemeDetailsUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_IDTYPE"))
        )
    )
    connector.getSchemeDetails(psaId, schemeIdType, idNumber).map {
      response =>
        response.left.value shouldBe a[BadRequestException]
        response.left.value.message should include("INVALID_IDTYPE")
    }
  }

  it should "return bad request - 400 if body contains INVALID_SRN" in {
    server.stubFor(
      get(urlEqualTo(schemeDetailsUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_SRN"))
        )
    )

    connector.getSchemeDetails(psaId, schemeIdType, idNumber) map {
      response =>
        response.left.value shouldBe a[BadRequestException]
        response.left.value.message should include("INVALID_SRN")
    }
  }

  it should "return bad request - 400 if body contains INVALID_PSTR" in {
    server.stubFor(
      get(urlEqualTo(schemeDetailsUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_PSTR"))
        )
    )

    connector.getSchemeDetails(psaId, schemeIdType, idNumber) map {
      response =>
        response.left.value shouldBe a[BadRequestException]
        response.left.value.message should include("INVALID_PSTR")
    }
  }

  it should "return bad request - 400 if body contains INVALID_CORRELATIONID and log the event as warn" in {
    server.stubFor(
      get(urlEqualTo(schemeDetailsUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_CORRELATIONID"))
        )
    )

    connector.getSchemeDetails(psaId, schemeIdType, idNumber) map {
      response =>
        response.left.value shouldBe a[BadRequestException]
        response.left.value.message should include("INVALID_CORRELATIONID")
    }
  }

  it should "throw upstream4xx - if any other 400 and log the event as error" in {

    server.stubFor(
      get(urlEqualTo(schemeDetailsUrl))
        .willReturn(
          badRequest
            .withBody(errorResponse("not valid"))
        )
    )

    recoverToExceptionIf[Upstream4xxResponse] (connector.getSchemeDetails(psaId, schemeIdType, idNumber)) map {
      ex =>
        ex.upstreamResponseCode shouldBe BAD_REQUEST
        ex.message should include ("not valid")
        ex.reportAs shouldBe BAD_REQUEST
    }
  }
  it should "return Not Found - 404" in {
    server.stubFor(
      get(urlEqualTo(schemeDetailsUrl))
        .willReturn(
          notFound
            .withBody(errorResponse("NOT_FOUND"))
        )
    )
    connector.getSchemeDetails(psaId, schemeIdType, idNumber).map { response =>
      response.left.value shouldBe a[NotFoundException]
      response.left.value.message should include("NOT_FOUND")
    }
  }
  it should "throw Upstream4XX for server unavailable - 403" in {

    server.stubFor(
      get(urlEqualTo(schemeDetailsUrl))
        .willReturn(
          forbidden
            .withBody(errorResponse("FORBIDDEN"))
        )
    )
    recoverToExceptionIf[Upstream4xxResponse] (connector.getSchemeDetails(psaId, schemeIdType, idNumber)) map {
      ex =>
        ex.upstreamResponseCode shouldBe FORBIDDEN
        ex.message should include ("FORBIDDEN")
    }
  }

  it should "throw Upstream5XX for internal server error - 500 and log the event as error" in {

    server.stubFor(
      get(urlEqualTo(schemeDetailsUrl))
        .willReturn(
          serverError
            .withBody(errorResponse("SERVER_ERROR"))
        )
    )

    recoverToExceptionIf[Upstream5xxResponse] (connector.getSchemeDetails(psaId, schemeIdType, idNumber)) map {
      ex =>
        ex.upstreamResponseCode shouldBe INTERNAL_SERVER_ERROR
        ex.message should include("SERVER_ERROR")
        ex.reportAs shouldBe BAD_GATEWAY
    }
  }

  it should "send audit event for successful response" in {
    val desResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsResponse.json")
    val userAnswersResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsUserAnswers.json")

    server.stubFor(
      get(urlEqualTo(schemeDetailsUrl))
        .willReturn(
          ok
            .withHeader("Content-Type", "application/json")
            .withBody(desResponse.toString())
        )
    )
    connector.getSchemeDetails(psaId, schemeIdType, idNumber).map { _ =>
      auditService.verifySent(
        SchemeDetailsAuditEvent(psaId, 200, Some(userAnswersResponse))
      ) shouldBe true
    }
  }

  it should "send audit event for error response" in {

    val expectedResponse = errorResponse("NOT_FOUND")

    server.stubFor(
      get(urlEqualTo(schemeDetailsUrl))
        .willReturn(
          notFound
            .withBody(expectedResponse)
        )
    )

    connector.getSchemeDetails(psaId, schemeIdType, idNumber).map { response =>
      auditService.verifySent(
        SchemeDetailsAuditEvent(psaId, 404, Some(Json.parse(expectedResponse)))
      ) shouldBe true
    }
  }

  "SchemeConnector getCorrelationId" should "return the correct CorrelationId when the request Id is more than 32 characters" in {
    val requestId = Some("govuk-tax-4725c811-9251-4c06-9b8f-f1d84659b2dfe")
    val result = connector.getCorrelationId(requestId)
    result shouldBe "4725c81192514c069b8ff1d84659b2df"
  }

  it should "return the correct CorrelationId when the request Id is less than 32 characters" in {
    val requestId = Some("govuk-tax-4725c811-9251-4c06-9b8f-f1")
    val result = connector.getCorrelationId(requestId)
    result shouldBe "4725c81192514c069b8ff1"
  }

  it should "return the correct CorrelationId when the request Id does not have gov-uk-tax or -" in {
    val requestId = Some("4725c81192514c069b8ff1")
    val result = connector.getCorrelationId(requestId)
    result shouldBe "4725c81192514c069b8ff1"
  }

  "SchemeConnector updateSchemeDetails" should "handle OK (200)" in {
    val successResponse: JsObject = Json.obj("processingDate" -> LocalDate.now)
    server.stubFor(
      post(urlEqualTo(updateSchemeUrl))
        .withHeader("Content-Type", equalTo("application/json"))
        .withRequestBody(equalToJson(Json.stringify(updateSchemeData)))
        .willReturn(
          ok(Json.stringify(successResponse))
            .withHeader("Content-Type", "application/json")
        )
    )

    connector.updateSchemeDetails(pstr, updateSchemeData).map { response =>
      response.status shouldBe OK
    }
  }

  it should "handle FORBIDDEN (403) - INVALID_VARIATION" in {
    server.stubFor(
      post(urlEqualTo(updateSchemeUrl))
        .willReturn(
          forbidden
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_VARIATION"))
        )
    )

    recoverToSucceededIf[Upstream4xxResponse] {
      connector.updateSchemeDetails(pstr, updateSchemeData)
    }
  }

  it should "handle CONFLICT (409) - DUPLICATE_SUBMISSION" in {
    server.stubFor(
      post(urlEqualTo(updateSchemeUrl))
        .willReturn(
          aResponse()
            .withStatus(Status.CONFLICT)
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("DUPLICATE_SUBMISSION"))
        )
    )

    recoverToSucceededIf[Upstream4xxResponse] {
      connector.updateSchemeDetails(pstr, updateSchemeData)
    }
  }

  it should "handle INVALID_PSTR (400)" in {
    server.stubFor(
      post(urlEqualTo(updateSchemeUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_PSTR"))
        )
    )

    recoverToSucceededIf[BadRequestException] {
      connector.updateSchemeDetails(pstr, updateSchemeData)
    }
  }

  it should "handle INVALID_CORRELATION_ID (400)" in {
    server.stubFor(
      post(urlEqualTo(updateSchemeUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_CORRELATION_ID"))
        )
    )

    recoverToSucceededIf[BadRequestException] {
      connector.updateSchemeDetails(pstr, updateSchemeData)
    }
  }

  it should "log details of an INVALID_PAYLOAD for a BAD request (400) response" in {
    server.stubFor(
      post(urlEqualTo(updateSchemeUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody("INVALID_PAYLOAD")
        )
    )

    logger.reset()

    connector.updateSchemeDetails(pstr, updateSchemeData).map(_ => fail("Expected failure"))
      .recover {
        case _: BadRequestException =>
          logger.getLogEntries.size shouldBe 1
          logger.getLogEntries.head.level shouldBe Level.WARN
        case _ => fail("Expected BadRequestException")
      }
  }
}

object SchemeConnectorSpec extends JsonFileReader {
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val rh: RequestHeader = FakeRequest("", "")
  val psaId = "test"
  val schemeIdType = "srn"
  val idNumber = "S1234567890"
  val pstr = "20010010AA"
  private val registerSchemeData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")
  private val updateSchemeData = readJsonFromFile("/data/validSchemeUpdateRequest.json")
  val schemeUrl = s"/pension-online/scheme-subscription/$psaId"
  val listOfSchemeUrl = s"/pension-online/subscription/$psaId/list"
  val schemeDetailsUrl = s"/pension-online/scheme-details/$schemeIdType/$idNumber"
  val updateSchemeUrl = s"/pension-online/scheme-variation/pstr/$pstr"
  private val validListOfSchemeResponse = readJsonFromFile("/data/validListOfSchemesResponse.json")

  private val invalidBusinessPartnerResponse =
    Json.stringify(
      Json.obj(
        "code" -> "INVALID_BUSINESS_PARTNER",
        "reason" -> "test-reason"
      )
    )

  private val duplicateSubmissionResponse =
    Json.stringify(
      Json.obj(
        "code" -> "DUPLICATE_SUBMISSION",
        "reason" -> "test-reason"
      )
    )

  def errorResponse(code: String): String = {
    Json.stringify(
        Json.obj(
        "code" -> code,
        "reason" -> s"Reason for $code"
      )
    )
  }

  val auditService = new StubSuccessfulAuditService()
  val logger = new StubLogger()
}
