/*
 * Copyright 2022 HM Revenue & Customs
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
import audit.{AuditService, ListOfSchemesAudit}
import base.JsonFileReader
import com.github.tomakehurst.wiremock.client.WireMock._
import org.mockito.MockitoSugar
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers._
import org.scalatest.{EitherValues, OptionValues, RecoverMethods}
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers.{NOT_FOUND, _}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, NotFoundException, UpstreamErrorResponse}
import utils.WireMockHelper

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
    )

  def connector: SchemeConnector = app.injector.instanceOf[SchemeConnector]

  "SchemeConnector listOfScheme" should "return OK with the list of schemes response for PSA" in {
    server.stubFor(
      get(listOfSchemesIFUrl())
        .willReturn(
          ok(Json.stringify(validListOfSchemeIFResponse))
        )
    )

    connector.listOfSchemes(psaType, idValue).map { response =>
      response.right.value shouldBe validListOfSchemeIFResponse
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
      response.right.value shouldBe validListOfSchemeIFResponse
    }
  }

  it should "throw NotFoundException when if/ETMP throws NotFoundException" in {
    server.stubFor(
      get(listOfSchemesIFUrl(pspType))
        .willReturn(
          notFound()
        )
    )

    recoverToExceptionIf[NotFoundException] {
      connector.listOfSchemes(psaType, idValue)
    } map {
      _.responseCode shouldBe NOT_FOUND
    }
  }

  it should "throw UpStream5XXResponse when if/ETMP throws Server error" in {
    server.stubFor(
      get(listOfSchemesIFUrl(psaType))
        .willReturn(
          serverError()
        )
    )

    recoverToExceptionIf[UpstreamErrorResponse] {
      connector.listOfSchemes(psaType, idValue)
    } map {
      _.statusCode shouldBe INTERNAL_SERVER_ERROR
    }
  }

  it should "send an audit event on success" in {
    server.stubFor(
      get(listOfSchemesIFUrl())
        .willReturn(
          ok(Json.stringify(validListOfSchemeIFResponse))
        )
    )

    connector.listOfSchemes(psaType, idValue).map { response =>
      auditService.verifySent(
        ListOfSchemesAudit("PSA", idValue, Status.OK, Some(response.right.value))) shouldBe true
    }
  }

  it should "send an audit event on failure" in {
    server.stubFor(
      get(listOfSchemesIFUrl())
        .willReturn(
          notFound()
        )
    )
    connector.listOfSchemes(psaType, idValue) .map(_ => fail("Expected failure"))
      .recover {
        case _: NotFoundException =>
          auditService.verifySent(
            ListOfSchemesAudit("PSA", idValue, Status.NOT_FOUND, None)) shouldBe true
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
      response.right.value shouldBe successResponse
    }
  }

  it should "handle FORBIDDEN (403)" in {
    server.stubFor(
      post(urlEqualTo(schemeIFUrl))
        .willReturn(
          forbidden
            .withHeader("Content-Type", "application/json")
            .withBody(invalidBusinessPartnerResponse)
        )
    )
    recoverToExceptionIf[UpstreamErrorResponse] {
      connector.registerScheme(idValue, registerSchemeData)
    } map {
      _.statusCode shouldBe FORBIDDEN
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
      response.left.value.responseCode shouldBe CONFLICT
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

    recoverToExceptionIf[BadRequestException] {
      connector.registerScheme(idValue, registerSchemeData)
    } map {
      _.responseCode shouldBe BAD_REQUEST
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

    recoverToExceptionIf[NotFoundException] {
      connector.registerScheme(idValue, registerSchemeData)
    } map {
      _.responseCode shouldBe NOT_FOUND
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
      response.right.value shouldBe successResponse
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

    recoverToExceptionIf[UpstreamErrorResponse] {
      connector.updateSchemeDetails(pstr, updateSchemeData)
    } map {
      _.statusCode shouldBe FORBIDDEN
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
      response.left.value.responseCode shouldBe CONFLICT
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

    recoverToExceptionIf[BadRequestException] {
      connector.updateSchemeDetails(pstr, updateSchemeData)
    } map {
      _.responseCode shouldBe BAD_REQUEST
    }
  }
}

object SchemeConnectorSpec extends JsonFileReader {
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val rh: RequestHeader = FakeRequest("", "")
  private val idValue = "test"
  private val pstr = "20010010AA"
  private val registerSchemeData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")
  private val updateSchemeData = readJsonFromFile("/data/validSchemeUpdateRequest.json")
  private val schemeIFUrl = s"/pension-online/scheme-subscription/pods/$idValue"
  private val updateSchemeIFUrl = s"/pension-online/scheme-variation/pods/$pstr"

  private val psaType = "PSA"
  private val pspType = "PSP"
  private val validListOfSchemeIFResponse = readJsonFromFile("/data/validListOfSchemesIFResponse.json")

  private def listOfSchemesIFUrl(idType: String = psaType): String =
    s"/pension-online/subscriptions/schemes/list/pods/$idType/$idValue"

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
}

