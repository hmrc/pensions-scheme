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

package connector

import audit.AuditService
import audit.testdoubles.StubSuccessfulAuditService
import base.JsonFileReader
import com.github.tomakehurst.wiremock.client.WireMock._
import org.joda.time.LocalDate
import org.scalatest._
import org.slf4j.event.Level
import play.api.LoggerLike
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.http._
import utils.{StubLogger, WireMockHelper}
import play.api.http.Status._

class SchemeConnectorSpec extends AsyncFlatSpec
  with Matchers
  with WireMockHelper
  with OptionValues
  with RecoverMethods
  with EitherValues
  with ConnectorBehaviours {

  import SchemeConnectorSpec._

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

  "SchemeConnector registerPSA" should "handle OK (200)" in {
    val successResponse = Json.obj(
      "processingDate" -> LocalDate.now,
      "formBundle" -> "1121313",
      "psaId" -> "A21999999"
    )
    server.stubFor(
      post(urlEqualTo(registerPsaUrl))
        .withHeader("Content-Type", equalTo("application/json"))
        .withRequestBody(equalToJson(Json.stringify(registerPsaData)))
        .willReturn(
          ok(Json.stringify(successResponse))
            .withHeader("Content-Type", "application/json")
        )
    )
    connector.registerPSA(registerPsaData).map { response =>
      response.right.value shouldBe successResponse
    }
  }

  it should "return a BadRequestException for a 400 INVALID_CORRELATION_ID response" in {
    server.stubFor(
      post(urlEqualTo(registerPsaUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(invalidCorrelationIdResponse)
        )
    )
    connector.registerPSA(registerPsaData).map {
      response =>
        response.left.value shouldBe a[BadRequestException]
        response.left.value.message should include("INVALID_CORRELATION_ID")
    }
  }

  it should "log details of an INVALID_PAYLOAD for a 400 BAD request" in {
    server.stubFor(
      post(urlEqualTo(registerPsaUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody("INVALID_PAYLOAD")
        )
    )

    logger.reset()
    connector.registerPSA(registerPsaData).map {
      _ =>
        logger.getLogEntries.size shouldBe 1
        logger.getLogEntries.head.level shouldBe Level.WARN
    }
  }

  it should "return a ForbiddenException for a 403 INVALID_BUSINESS_PARTNER response" in {
    server.stubFor(
      post(urlEqualTo(registerPsaUrl))
        .willReturn(
          forbidden
            .withHeader("Content-Type", "application/json")
            .withBody(invalidBusinessPartnerResponse)
        )
    )

    connector.registerPSA(registerPsaData).map {
      response =>
        response.left.value shouldBe a[ForbiddenException]
        response.left.value.message should include("INVALID_BUSINESS_PARTNER")
    }
  }

  it should behave like errorHandlerForApiFailures(
    connector.registerPSA(registerPsaData),
    registerPsaUrl
  )

  it should "return a ConflictException for a 409 DUPLICATE_SUBMISSION response" in {
    server.stubFor(
      post(urlEqualTo(registerPsaUrl))
        .willReturn(
          aResponse()
            .withStatus(Status.CONFLICT)
            .withHeader("Content-Type", "application/json")
            .withBody(duplicateSubmissionResponse)
        )
    )
    connector.registerPSA(registerPsaData).map {
      response =>
        response.left.value shouldBe a[ConflictException]
        response.left.value.message should include("DUPLICATE_SUBMISSION")
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
}

object SchemeConnectorSpec extends JsonFileReader {
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val rh: RequestHeader = FakeRequest("", "")
  val psaId = "test"
  private val registerSchemeData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")
  private val registerPsaData = readJsonFromFile("/data/validPsaRequest.json")
  val registerPsaUrl = "/pension-online/subscription"
  val schemeUrl = s"/pension-online/scheme-subscription/$psaId"
  val listOfSchemeUrl = s"/pension-online/subscription/$psaId/list"
  private val validListOfSchemeResponse = readJsonFromFile("/data/validListOfSchemesResponse.json")

  private val invalidBusinessPartnerResponse =
    Json.stringify(
      Json.obj(
        "code" -> "INVALID_BUSINESS_PARTNER",
        "reason" -> "test-reason"
      )
    )

  private val invalidCorrelationIdResponse =
    Json.stringify(
      Json.obj(
        "code" -> "INVALID_CORRELATION_ID",
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
  val auditService = new StubSuccessfulAuditService()
  val logger = new StubLogger()
}
