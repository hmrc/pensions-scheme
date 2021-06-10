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
import audit.{SchemeDetailsAuditEvent, AuditService}
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
import uk.gov.hmrc.http._
import utils.{StubLogger, WireMockHelper}

import java.time.LocalDate

class SchemeConnectorSpec
  extends AsyncFlatSpec
    with WireMockHelper
    with OptionValues
    with EitherValues
    with MockitoSugar
    with JsonFileReader {

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

  "SchemeConnector listOfSchemes" should "return OK with the list of schemes response" in {
    server.stubFor(
      get(listOfSchemeUrl)
        .willReturn(
          ok(Json.stringify(validListOfSchemeResponse))
        )
    )

    connector.listOfSchemes(idValue).map { response =>
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

    connector.listOfSchemes(idValue).map { response =>
      response.status shouldBe BAD_REQUEST
    }
  }

  it should "throw NotFoundException when DES/ETMP throws NotFoundException" in {
    server.stubFor(
      get(listOfSchemeUrl)
        .willReturn(
          notFound()
        )
    )

    connector.listOfSchemes(idValue).map { response =>
      response.status shouldBe NOT_FOUND
    }
  }

  it should "throw UpStream5XXResponse when DES/ETMP throws Server error" in {
    server.stubFor(
      get(listOfSchemeUrl)
        .willReturn(
          serverError()
        )
    )

    connector.listOfSchemes(idValue).map { response =>
      response.status shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "SchemeConnector getSchemeDetails" should "return user answer json" in {
    val desResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsResponseDES.json")
    val userAnswersResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsUserAnswers.json")

    server.stubFor(
      get(urlEqualTo(schemeDetailsUrl))
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
      get(urlEqualTo(schemeDetailsUrl))
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
      get(urlEqualTo(schemeDetailsUrl))
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
      get(urlEqualTo(schemeDetailsUrl))
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
      get(urlEqualTo(schemeDetailsUrl))
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
      get(urlEqualTo(schemeDetailsUrl))
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
      get(urlEqualTo(schemeDetailsUrl))
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
      get(urlEqualTo(schemeDetailsUrl))
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
      get(urlEqualTo(schemeDetailsUrl))
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
    val desResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsResponseDES.json")
    val userAnswersResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsUserAnswers.json")

    server.stubFor(
      get(urlEqualTo(schemeDetailsUrl))
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
      get(urlEqualTo(schemeDetailsUrl))
        .willReturn(
          notFound
            .withBody(expectedResponse)
        )
    )

    connector.getSchemeDetails(idValue, schemeIdType, idNumber).map { _ =>
      auditService.verifySent(
        SchemeDetailsAuditEvent(idValue, 404, Some(Json.parse(expectedResponse)))
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

    connector.updateSchemeDetails(pstr, updateSchemeData).map { response =>
      response.status shouldBe FORBIDDEN
    }
  }

  it should "handle CONFLICT (409) - DUPLICATE_SUBMISSION" in {
    server.stubFor(
      post(urlEqualTo(updateSchemeUrl))
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
      post(urlEqualTo(updateSchemeUrl))
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
      post(urlEqualTo(updateSchemeUrl))
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
  val idValue = "test"
  val schemeIdType = "srn"
  val idNumber = "S1234567890"
  val pstr = "20010010AA"
  val registerSchemeData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")
  val updateSchemeData = readJsonFromFile("/data/validSchemeUpdateRequest.json")
  val schemeUrl = s"/pension-online/scheme-subscription/$idValue"
  val schemeIFUrl = s"/pension-online/scheme-subscription/pods/$idValue"
  val listOfSchemeUrl: String = s"/pension-online/subscription/$idValue/list"
  val schemeDetailsUrl = s"/pension-online/scheme-details/$schemeIdType/$idNumber"
  val updateSchemeUrl = s"/pension-online/scheme-variation/pstr/$pstr"
  val updateSchemeIFUrl = s"/pension-online/scheme-variation/pods/$pstr"
  val validListOfSchemeResponse = readJsonFromFile("/data/validListOfSchemesResponse.json")

  val invalidBusinessPartnerResponse =
    Json.stringify(
      Json.obj(
        "code" -> "INVALID_BUSINESS_PARTNER",
        "reason" -> "test-reason"
      )
    )

  val duplicateSubmissionResponse =
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
