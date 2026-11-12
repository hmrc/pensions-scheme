/*
 * Copyright 2024 HM Revenue & Customs
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
import audit.{AuditService, PspSchemeDetailsAuditEvent, SchemeDetailsAuditEvent}
import base.JsonFileReader
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalatest.*
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers.*
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.*
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, NotFoundException, UpstreamErrorResponse}
import utils.WireMockHelper

class HipSchemeDetailsConnectorSpec
  extends AsyncFlatSpec
    with WireMockHelper
    with OptionValues
    with MockitoSugar
    with RecoverMethods
    with EitherValues
    with JsonFileReader {

  import HipSchemeDetailsConnectorSpec.*

  override def beforeEach(): Unit = {
    auditService.reset()
    super.beforeEach()
  }

  override protected def portConfigKey: String = "microservice.services.hip-hod.port"

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[AuditService].toInstance(auditService),
      bind[LockRepository].toInstance(mock[LockRepository]),
      bind[RacdacSchemeSubscriptionCacheRepository].toInstance(mock[RacdacSchemeSubscriptionCacheRepository]),
      bind[SchemeCacheRepository].toInstance(mock[SchemeCacheRepository]),
      bind[SchemeDetailsCacheRepository].toInstance(mock[SchemeDetailsCacheRepository]),
      bind[SchemeDetailsWithIdCacheRepository].toInstance(mock[SchemeDetailsWithIdCacheRepository]),
      bind[SchemeSubscriptionCacheRepository].toInstance(mock[SchemeSubscriptionCacheRepository]),
      bind[UpdateSchemeCacheRepository].toInstance(mock[UpdateSchemeCacheRepository])
    )

  def connector: HipSchemeDetailsConnector = app.injector.instanceOf[HipSchemeDetailsConnector]

  "SchemeConnector getSchemeDetails" should "return user answer json" in {
    val hipResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsHipResponse.json")
    val userAnswersResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsIFUserAnswers.json")

    server.stubFor(
      get(urlEqualTo(schemeDetailsHipUrl))
        .willReturn(
          ok
            .withHeader("Content-Type", "application/json")
            .withBody(hipResponse.toString())
        )
    )
    connector.getSchemeDetails(idValue, schemeIdType, idNumber).map { response =>
      response.value shouldBe userAnswersResponse
    }
  }

  "SchemeConnector getSchemeDetails with no SRN" should "return user answer json" in {
    val hipResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsResponseHipNoSrn.json")
    val userAnswersResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsIFUserAnswersNoSrn.json")

    server.stubFor(
      get(urlEqualTo(schemeDetailsHipUrl))
        .willReturn(
          ok
            .withHeader("Content-Type", "application/json")
            .withBody(hipResponse.toString())
        )
    )
    connector.getSchemeDetails(idValue, schemeIdType, idNumber).map { response =>
      response.value shouldBe userAnswersResponse
    }
  }

  it should "return a BadRequestException for a 400 INVALID_IDTYPE response" in {
    server.stubFor(
      get(urlEqualTo(schemeDetailsHipUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse("INVALID_IDTYPE"))
        )
    )

    recoverToExceptionIf[BadRequestException] {
      connector.getSchemeDetails(idValue, schemeIdType, idNumber)
    } map {
      _.responseCode shouldBe BAD_REQUEST
    }
  }

  it should "return Not Found - 404" in {
    server.stubFor(
      get(urlEqualTo(schemeDetailsHipUrl))
        .willReturn(
          notFound
            .withBody(errorResponse("NOT_FOUND"))
        )
    )
    recoverToExceptionIf[NotFoundException] {
      connector.getSchemeDetails(idValue, schemeIdType, idNumber)
    } map {
      _.responseCode shouldBe NOT_FOUND
    }
  }

  it should "throw Upstream4XX for server unavailable - 403" in {

    server.stubFor(
      get(urlEqualTo(schemeDetailsHipUrl))
        .willReturn(
          forbidden
            .withBody(errorResponse("FORBIDDEN"))
        )
    )

    recoverToExceptionIf[UpstreamErrorResponse] {
      connector.getSchemeDetails(idValue, schemeIdType, idNumber)
    } map {
      _.statusCode shouldBe FORBIDDEN
    }
  }

  it should "send audit event for successful response" in {
    val hipResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsHipResponse.json")
    val userAnswersResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsIFUserAnswers.json")

    server.stubFor(
      get(urlEqualTo(schemeDetailsHipUrl))
        .willReturn(
          ok
            .withHeader("Content-Type", "application/json")
            .withBody(hipResponse.toString())
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
      get(urlEqualTo(schemeDetailsHipUrl))
        .willReturn(
          notFound
            .withBody(expectedResponse)
        )
    )

    recoverToExceptionIf[NotFoundException] {
      connector.getSchemeDetails(idValue, schemeIdType, idNumber)
    } map { _ =>
      auditService.verifySent(
        SchemeDetailsAuditEvent(idValue, 404, None)
      ) shouldBe true
    }
  }

  "SchemeConnector getPspSchemeDetails" should "return user answer json" in {
    val apiResponse: JsValue = readJsonFromFile("/data/validGetPspSchemeDetailsHipResponse.json")
    val userAnswersResponse: JsValue = readJsonFromFile("/data/validGetPspSchemeDetailsUserAnswers.json")

    server.stubFor(
      get(urlEqualTo(pspSchemeDetailsHipUrl))
        .willReturn(
          ok
            .withHeader("Content-Type", "application/json")
            .withBody(apiResponse.toString())
        )
    )
    connector.getPspSchemeDetails(pspId, pstr).map { response =>
      response.value shouldBe userAnswersResponse
    }
  }

  it should "throw Upstream5XX for internal server error - 500 and log the event as error" in {

    server.stubFor(
      get(urlEqualTo(pspSchemeDetailsHipUrl))
        .willReturn(
          serverError
            .withBody(errorResponse("SERVER_ERROR"))
        )
    )

    recoverToExceptionIf[UpstreamErrorResponse] {
      connector.getPspSchemeDetails(pspId, pstr)
    } map {
      _.statusCode shouldBe INTERNAL_SERVER_ERROR
    }
  }

  it should "send audit event for successful response" in {
    val hipResponse: JsValue = readJsonFromFile("/data/validGetPspSchemeDetailsHipResponse.json")
    val userAnswersResponse: JsValue = readJsonFromFile("/data/validGetPspSchemeDetailsUserAnswers.json")

    server.stubFor(
      get(urlEqualTo(pspSchemeDetailsHipUrl))
        .willReturn(
          ok
            .withHeader("Content-Type", "application/json")
            .withBody(hipResponse.toString())
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
      get(urlEqualTo(pspSchemeDetailsHipUrl))
        .willReturn(
          notFound
            .withBody(expectedResponse)
        )
    )

    recoverToExceptionIf[NotFoundException] {
      connector.getPspSchemeDetails(pspId, pstr)
    } map { _ =>
      auditService.verifyExtendedSent(
        PspSchemeDetailsAuditEvent(pspId, 404, None)
      ) shouldBe true
    }
  }
}

object HipSchemeDetailsConnectorSpec extends JsonFileReader {
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val rh: RequestHeader = FakeRequest("", "")
  private val idValue = "test"
  private val schemeIdType = "srn"
  private val idNumber = "S1234567890"
  private val pstr = "20010010AA"

  private val pspId = "psp-id"

  private val schemeDetailsHipUrl: String = s"/RESTAdapter/pension-online/scheme-details/$schemeIdType/$idNumber"
  private val pspSchemeDetailsHipUrl: String = s"/RESTAdapter/pension-online/psp-scheme-details/$pspId/$pstr"

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



