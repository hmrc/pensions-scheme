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

package connector

import audit.testdoubles.StubSuccessfulAuditService
import audit.{SchemeDetailsAuditEvent, AuditService, PspSchemeDetailsAuditEvent}
import base.JsonFileReader
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.Matchers.{include, convertToAnyShouldWrapper}
import org.scalatest._
import org.scalatestplus.mockito.MockitoSugar
import org.slf4j.event.Level
import play.api.LoggerLike
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, Json, JsValue}
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import utils.{WireMockHelper, StubLogger}

import java.time.LocalDate
class SchemeConnectorSpec
  extends AsyncFlatSpec
    with WireMockHelper
    with OptionValues
    with MockitoSugar
    with RecoverMethods
    with EitherValues
    with JsonFileReader {

  import SchemeConnectorSpec._

  override def beforeEach(): Unit = {
    auditService.reset()
    super.beforeEach()
  }

  override protected def portConfigKey: String = "microservice.services.if-hod.port"

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[AuditService].toInstance(auditService),
      bind[LoggerLike].toInstance(logger)
    )

  def connector: SchemeConnector = app.injector.instanceOf[SchemeConnector]

  private val psaType = "PSA"
  private val pspType = "PSP"
  private val pspId = "psp-id"
  private val validListOfSchemeIFResponse = readJsonFromFile("/data/validListOfSchemesIFResponse.json")

  def listOfSchemesIFUrl(idType: String = psaType): String =
    s"/pension-online/subscriptions/schemes/list/pods/$idType/$idValue"

  val schemeDetailsIFUrl: String = s"/pension-online/scheme-details/pods/$schemeIdType/$idNumber"
  val pspSchemeDetailsIFUrl: String = s"/pension-online/psp-scheme-details/pods/$pspId/$pstr"

  "SchemeConnector listOfScheme from IF" should "return OK with the list of schemes response for PSA" in {
    server.stubFor(
      get(listOfSchemesIFUrl())
        .willReturn(
          ok(Json.stringify(validListOfSchemeIFResponse))
        )
    )

    connector.listOfSchemes(psaType, idValue).map { response =>
      response.status shouldBe OK
      response.body shouldBe Json.stringify(validListOfSchemeIFResponse)
    }
  }

  it should "return OK with the list of schemes response for PSP" in {
    server.stubFor(
      get(listOfSchemesIFUrl(pspType))
        .willReturn(
          ok(Json.stringify(validListOfSchemeIFResponse))
        )
    )

    connector.listOfSchemes(pspType, idValue).map { response =>
      response.status shouldBe OK
      response.body shouldBe Json.stringify(validListOfSchemeIFResponse)
    }
  }

  it should "throw Bad Request Exception when DES/ETMP throws BadRequestException" in {
    server.stubFor(
      get(listOfSchemesIFUrl())
        .willReturn(
          badRequest()
        )
    )

    connector.listOfSchemes(psaType, idValue).map { response =>
      response.status shouldBe BAD_REQUEST
    }
  }

  it should "throw NotFoundException when DES/ETMP throws NotFoundException" in {
    server.stubFor(
      get(listOfSchemesIFUrl(pspType))
        .willReturn(
          notFound()
        )
    )

    connector.listOfSchemes(pspType, idValue).map { response =>
      response.status shouldBe NOT_FOUND
    }
  }

  it should "throw UpStream5XXResponse when DES/ETMP throws Server error" in {
    server.stubFor(
      get(listOfSchemesIFUrl(psaType))
        .willReturn(
          serverError()
        )
    )

    connector.listOfSchemes(psaType, idValue).map { response =>
      response.status shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "SchemeConnectorIF getSchemeDetails" should "return user answer json" in {
    val desResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsResponse.json")
    val userAnswersResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsIFUserAnswers.json")

    server.stubFor(
      get(urlEqualTo(schemeDetailsIFUrl))
        .willReturn(
          ok
            .withHeader("Content-Type", "application/json")
            .withBody(desResponse.toString())
        )
    )
    connector.getSchemeDetails(idValue, schemeIdType, idNumber).map { response =>
      response.right.value shouldBe userAnswersResponse
    }
  }

  "SchemeConnectorIF getSchemeDetails with no SRN" should "return user answer json" in {
    val desResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsResponseNoSrn.json")
    val userAnswersResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsIFUserAnswersNoSrn.json")

    server.stubFor(
      get(urlEqualTo(schemeDetailsIFUrl))
        .willReturn(
          ok
            .withHeader("Content-Type", "application/json")
            .withBody(desResponse.toString())
        )
    )
    connector.getSchemeDetails(idValue, schemeIdType, idNumber).map { response =>
      response.right.value shouldBe userAnswersResponse
    }
  }

  it should "return a BadRequestException for a 400 INVALID_IDTYPE response" in {
    server.stubFor(
      get(urlEqualTo(schemeDetailsIFUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_IDTYPE"))
        )
    )
    connector.getSchemeDetails(idValue, schemeIdType, idNumber).map {
      response =>
        response.left.value.status shouldBe BAD_REQUEST
        response.left.value.body should include("INVALID_IDTYPE")
    }
  }

  it should "return bad request - 400 if body contains INVALID_SRN" in {
    server.stubFor(
      get(urlEqualTo(schemeDetailsIFUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_SRN"))
        )
    )

    connector.getSchemeDetails(idValue, schemeIdType, idNumber) map {
      response =>
        response.left.value.status shouldBe BAD_REQUEST
        response.left.value.body should include("INVALID_SRN")
    }
  }

  it should "return bad request - 400 if body contains INVALID_PSTR" in {
    server.stubFor(
      get(urlEqualTo(schemeDetailsIFUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_PSTR"))
        )
    )

    connector.getSchemeDetails(idValue, schemeIdType, idNumber) map {
      response =>
        response.left.value.status shouldBe BAD_REQUEST
        response.left.value.body should include("INVALID_PSTR")
    }
  }

  it should "return bad request - 400 if body contains INVALID_CORRELATIONID and log the event as warn" in {
    server.stubFor(
      get(urlEqualTo(schemeDetailsIFUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_CORRELATIONID"))
        )
    )

    connector.getSchemeDetails(idValue, schemeIdType, idNumber) map {
      response =>
        response.left.value.status shouldBe BAD_REQUEST
        response.left.value.body should include("INVALID_CORRELATIONID")
    }
  }

  it should "throw upstream4xx - if any other 400 and log the event as error" in {

    server.stubFor(
      get(urlEqualTo(schemeDetailsIFUrl))
        .willReturn(
          badRequest
            .withBody(errorResponse("not valid"))
        )
    )

    connector.getSchemeDetails(idValue, schemeIdType, idNumber) map {
      response =>
        response.left.value.status shouldBe BAD_REQUEST
        response.left.value.body should include("not valid")
    }
  }

  it should "return Not Found - 404" in {
    server.stubFor(
      get(urlEqualTo(schemeDetailsIFUrl))
        .willReturn(
          notFound
            .withBody(errorResponse("NOT_FOUND"))
        )
    )
    connector.getSchemeDetails(idValue, schemeIdType, idNumber).map { response =>
      response.left.value.status shouldBe NOT_FOUND
      response.left.value.body should include("NOT_FOUND")
    }
  }

  it should "throw Upstream4XX for server unavailable - 403" in {

    server.stubFor(
      get(urlEqualTo(schemeDetailsIFUrl))
        .willReturn(
          forbidden
            .withBody(errorResponse("FORBIDDEN"))
        )
    )

    connector.getSchemeDetails(idValue, schemeIdType, idNumber) map {
      response =>
        response.left.value.status shouldBe FORBIDDEN
        response.left.value.body should include("FORBIDDEN")
    }
  }

  it should "throw Upstream5XX for internal server error - 500 and log the event as error" in {

    server.stubFor(
      get(urlEqualTo(schemeDetailsIFUrl))
        .willReturn(
          serverError
            .withBody(errorResponse("SERVER_ERROR"))
        )
    )

    connector.getSchemeDetails(idValue, schemeIdType, idNumber) map {
      response =>
        response.left.value.status shouldBe INTERNAL_SERVER_ERROR
        response.left.value.body should include("SERVER_ERROR")
    }
  }

  it should "send audit event for successful response" in {
    val desResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsResponse.json")
    val userAnswersResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsIFUserAnswers.json")

    server.stubFor(
      get(urlEqualTo(schemeDetailsIFUrl))
        .willReturn(
          ok
            .withHeader("Content-Type", "application/json")
            .withBody(desResponse.toString())
        )
    )
    connector.getSchemeDetails(idValue, schemeIdType, idNumber).map { _ =>
      auditService.verifySent(
        SchemeDetailsAuditEvent(idValue, 200, Some(userAnswersResponse))
      ) shouldBe true
    }
  }

  it should "send audit event for error response" in {

    val expectedResponse = errorResponse("NOT_FOUND")

    server.stubFor(
      get(urlEqualTo(schemeDetailsIFUrl))
        .willReturn(
          notFound
            .withBody(expectedResponse)
        )
    )

    connector.getSchemeDetails(idValue, schemeIdType, idNumber).map { response =>
      auditService.verifySent(
        SchemeDetailsAuditEvent(idValue, 404, Some(Json.parse(expectedResponse)))
      ) shouldBe true
    }
  }

  "SchemeConnectorIF getPspSchemeDetails" should "return user answer json" in {
    val apiResponse: JsValue = readJsonFromFile("/data/validGetPspSchemeDetailsResponse.json")
    val userAnswersResponse: JsValue = readJsonFromFile("/data/validGetPspSchemeDetailsUserAnswers.json")

    server.stubFor(
      get(urlEqualTo(pspSchemeDetailsIFUrl))
        .willReturn(
          ok
            .withHeader("Content-Type", "application/json")
            .withBody(apiResponse.toString())
        )
    )
    connector.getPspSchemeDetails(pspId, pstr).map { response =>
      response.right.value shouldBe userAnswersResponse
    }
  }

  it should "return a BadRequestException for a 400 INVALID_IDTYPE response" in {
    server.stubFor(
      get(urlEqualTo(pspSchemeDetailsIFUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_IDTYPE"))
        )
    )
    connector.getPspSchemeDetails(pspId, pstr).map {
      response =>
        response.left.value.status shouldBe BAD_REQUEST
        response.left.value.body should include("INVALID_IDTYPE")
    }
  }

  it should "return bad request - 400 if body contains INVALID_SRN" in {
    server.stubFor(
      get(urlEqualTo(pspSchemeDetailsIFUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_SRN"))
        )
    )

    connector.getPspSchemeDetails(pspId, pstr) map {
      response =>
        response.left.value.status shouldBe BAD_REQUEST
        response.left.value.body should include("INVALID_SRN")
    }
  }

  it should "return bad request - 400 if body contains INVALID_PSTR" in {
    server.stubFor(
      get(urlEqualTo(pspSchemeDetailsIFUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_PSTR"))
        )
    )

    connector.getPspSchemeDetails(pspId, pstr) map {
      response =>
        response.left.value.status shouldBe BAD_REQUEST
        response.left.value.body should include("INVALID_PSTR")
    }
  }

  it should "return bad request - 400 if body contains INVALID_CORRELATIONID and log the event as warn" in {
    server.stubFor(
      get(urlEqualTo(pspSchemeDetailsIFUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_CORRELATIONID"))
        )
    )

    connector.getPspSchemeDetails(pspId, pstr) map {
      response =>
        response.left.value.status shouldBe BAD_REQUEST
        response.left.value.body should include("INVALID_CORRELATIONID")
    }
  }

  it should "throw upstream4xx - if any other 400 and log the event as error" in {

    server.stubFor(
      get(urlEqualTo(pspSchemeDetailsIFUrl))
        .willReturn(
          badRequest
            .withBody(errorResponse("not valid"))
        )
    )

    connector.getPspSchemeDetails(pspId, pstr) map {
      response =>
        response.left.value.status shouldBe BAD_REQUEST
        response.left.value.body should include("not valid")
    }
  }

  it should "return Not Found - 404" in {
    server.stubFor(
      get(urlEqualTo(pspSchemeDetailsIFUrl))
        .willReturn(
          notFound
            .withBody(errorResponse("NOT_FOUND"))
        )
    )
    connector.getPspSchemeDetails(pspId, pstr).map { response =>
      response.left.value.status shouldBe NOT_FOUND
      response.left.value.body should include("NOT_FOUND")
    }
  }

  it should "throw Upstream4XX for server unavailable - 403" in {

    server.stubFor(
      get(urlEqualTo(pspSchemeDetailsIFUrl))
        .willReturn(
          forbidden
            .withBody(errorResponse("FORBIDDEN"))
        )
    )

    connector.getPspSchemeDetails(pspId, pstr) map {
      response =>
        response.left.value.status shouldBe FORBIDDEN
        response.left.value.body should include("FORBIDDEN")
    }
  }

  it should "throw Upstream5XX for internal server error - 500 and log the event as error" in {

    server.stubFor(
      get(urlEqualTo(pspSchemeDetailsIFUrl))
        .willReturn(
          serverError
            .withBody(errorResponse("SERVER_ERROR"))
        )
    )

    connector.getPspSchemeDetails(pspId, pstr) map {
      response =>
        response.left.value.status shouldBe INTERNAL_SERVER_ERROR
        response.left.value.body should include("SERVER_ERROR")
    }
  }

  it should "send audit event for successful response" in {
    val desResponse: JsValue = readJsonFromFile("/data/validGetPspSchemeDetailsResponse.json")
    val userAnswersResponse: JsValue = readJsonFromFile("/data/validGetPspSchemeDetailsUserAnswers.json")

    server.stubFor(
      get(urlEqualTo(pspSchemeDetailsIFUrl))
        .willReturn(
          ok
            .withHeader("Content-Type", "application/json")
            .withBody(desResponse.toString())
        )
    )
    connector.getPspSchemeDetails(pspId, pstr).map { _ =>
      auditService.verifyExtendedSent(
        PspSchemeDetailsAuditEvent(pspId, 200, Some(userAnswersResponse))
      ) shouldBe true
    }
  }

  it should "send audit event for error response" in {

    val expectedResponse = errorResponse("NOT_FOUND")

    server.stubFor(
      get(urlEqualTo(pspSchemeDetailsIFUrl))
        .willReturn(
          notFound
            .withBody(expectedResponse)
        )
    )

    connector.getPspSchemeDetails(pspId, pstr).map { _ =>
      auditService.verifyExtendedSent(
        PspSchemeDetailsAuditEvent(pspId, 404, Some(Json.parse(expectedResponse)))
      ) shouldBe true
    }
  }

  "SchemeConnector registerScheme with tcmp toggle on" should "handle OK (200)" in {
    val successResponse: JsObject = Json.obj("processingDate" -> LocalDate.now, "schemeReferenceNumber" -> "S0123456789")
    server.stubFor(
      post(urlEqualTo(schemeIFUrl))
        .withHeader("Content-Type", equalTo("application/json"))
        .withRequestBody(equalToJson(Json.stringify(registerSchemeData)))
        .willReturn(
          ok(Json.stringify(successResponse))
            .withHeader("Content-Type", "application/json")
        )
    )

    connector.registerScheme(idValue, registerSchemeData).map { response =>
      response.status shouldBe OK
    }
  }

  it should "handle FORBIDDEN (403) - INVALID_BUSINESS_PARTNER" in {
    server.stubFor(
      post(urlEqualTo(schemeIFUrl))
        .willReturn(
          forbidden
            .withHeader("Content-Type", "application/json")
            .withBody(invalidBusinessPartnerResponse)
        )
    )

    connector.registerScheme(idValue, registerSchemeData).map { response =>
      response.status shouldBe FORBIDDEN
    }
  }

  it should "handle CONFLICT (409) - DUPLICATE_SUBMISSION" in {
    server.stubFor(
      post(urlEqualTo(schemeIFUrl))
        .willReturn(
          aResponse()
            .withStatus(CONFLICT)
            .withHeader("Content-Type", "application/json")
            .withBody(duplicateSubmissionResponse)
        )
    )

    connector.registerScheme(idValue, registerSchemeData).map { response =>
      response.status shouldBe CONFLICT
    }
  }

  it should "handle BAD_REQUEST (400)" in {
    server.stubFor(
      post(urlEqualTo(schemeIFUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody("Bad Request")
        )
    )

    connector.registerScheme(idValue, registerSchemeData).map { response =>
      response.status shouldBe BAD_REQUEST
    }
  }

  it should "throw NotFoundException for NOT_FOUND (404) response" in {
    server.stubFor(
      post(urlEqualTo(schemeIFUrl))
        .willReturn(
          notFound
            .withHeader("Content-Type", "application/json")
            .withBody("Not Found")
        )
    )

    connector.registerScheme(idValue, registerSchemeData).map { response =>
      response.status shouldBe NOT_FOUND
    }
  }

  it should "log details of an INVALID_PAYLOAD for a BAD request (400) response" in {
    server.stubFor(
      post(urlEqualTo(schemeIFUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody("INVALID_PAYLOAD")
        )
    )

    logger.reset()

    connector.registerScheme(idValue, registerSchemeData).map { response =>
      response.status shouldBe BAD_REQUEST
      logger.getLogEntries.size shouldBe 1
      logger.getLogEntries.head.level shouldBe Level.WARN
    }
  }

  "SchemeConnector updateSchemeDetails with toggle on" should "handle OK (200)" in {
    val successResponse: JsObject = Json.obj("processingDate" -> LocalDate.now)
    server.stubFor(
      post(urlEqualTo(updateSchemeIFUrl))
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
      post(urlEqualTo(updateSchemeIFUrl))
        .willReturn(
          forbidden
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_VARIATION"))
        )
    )

    connector.updateSchemeDetails(pstr, updateSchemeData).map { response =>
      response.status shouldBe FORBIDDEN
    }
  }

  it should "handle CONFLICT (409) - DUPLICATE_SUBMISSION" in {
    server.stubFor(
      post(urlEqualTo(updateSchemeIFUrl))
        .willReturn(
          aResponse()
            .withStatus(CONFLICT)
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("DUPLICATE_SUBMISSION"))
        )
    )

    connector.updateSchemeDetails(pstr, updateSchemeData).map { response =>
      response.status shouldBe CONFLICT
    }
  }

  it should "handle BAD_REQUEST (400)" in {
    server.stubFor(
      post(urlEqualTo(updateSchemeIFUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_PSTR"))
        )
    )

    connector.updateSchemeDetails(pstr, updateSchemeData).map { response =>
      response.status shouldBe BAD_REQUEST
    }
  }

  it should "log details of an INVALID_PAYLOAD for a BAD request (400) response" in {
    server.stubFor(
      post(urlEqualTo(updateSchemeIFUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody("INVALID_PAYLOAD")
        )
    )

    logger.reset()

    connector.updateSchemeDetails(pstr, updateSchemeData).map { response =>
      response.status shouldBe BAD_REQUEST
      logger.getLogEntries.size shouldBe 1
      logger.getLogEntries.head.level shouldBe Level.WARN
    }
  }
}

object SchemeConnectorSpec extends JsonFileReader {
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val rh: RequestHeader = FakeRequest("", "")
  private val idValue = "test"
  private val schemeIdType = "srn"
  private val idNumber = "S1234567890"
  private val pstr = "20010010AA"
  private val registerSchemeData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")
  private val updateSchemeData = readJsonFromFile("/data/validSchemeUpdateRequest.json")
  private val schemeIFUrl = s"/pension-online/scheme-subscription/pods/$idValue"
  private val updateSchemeIFUrl = s"/pension-online/scheme-variation/pods/$pstr"

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

  private def errorResponse(code: String): String = {
    Json.stringify(
      Json.obj(
        "code" -> code,
        "reason" -> s"Reason for $code"
      )
    )
  }

  private val auditService = new StubSuccessfulAuditService()
  private val logger = new StubLogger()
}

