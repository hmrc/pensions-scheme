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
import audit.{AuditService, PspSchemeDetailsAuditEvent, SchemeDetailsAuditEvent}
import base.JsonFileReader
import com.github.tomakehurst.wiremock.client.WireMock._
import org.mockito.MockitoSugar
import org.scalatest._
import org.scalatest.matchers.should.Matchers._
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, NotFoundException, UpstreamErrorResponse}
import utils.WireMockHelper
import org.scalatest.flatspec.AsyncFlatSpec

class SchemeDetailsConnectorSpec
  extends AsyncFlatSpec
    with WireMockHelper
    with OptionValues
    with MockitoSugar
    with RecoverMethods
    with EitherValues
    with JsonFileReader {

  import SchemeDetailsConnectorSpec._

  override def beforeEach(): Unit = {
    auditService.reset()
    super.beforeEach()
  }

  override protected def portConfigKey: String = "microservice.services.if-hod.port"

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[AuditService].toInstance(auditService),
    )

  def connector: SchemeDetailsConnector = app.injector.instanceOf[SchemeDetailsConnector]

  "SchemeConnector getSchemeDetails" should "return user answer json" in {
    val IfResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsResponse.json")
    val userAnswersResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsIFUserAnswers.json")

    server.stubFor(
      get(urlEqualTo(schemeDetailsIFUrl))
        .willReturn(
          ok
            .withHeader("Content-Type", "application/json")
            .withBody(IfResponse.toString())
        )
    )
    connector.getSchemeDetails(idValue, schemeIdType, idNumber).map { response =>
      response.right.value shouldBe userAnswersResponse
    }
  }

  "SchemeConnector getSchemeDetails with no SRN" should "return user answer json" in {
    val IfResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsResponseNoSrn.json")
    val userAnswersResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsIFUserAnswersNoSrn.json")

    server.stubFor(
      get(urlEqualTo(schemeDetailsIFUrl))
        .willReturn(
          ok
            .withHeader("Content-Type", "application/json")
            .withBody(IfResponse.toString())
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

    recoverToExceptionIf[BadRequestException] {
      connector.getSchemeDetails(idValue, schemeIdType, idNumber)
    } map {
      _.responseCode shouldBe BAD_REQUEST
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
    recoverToExceptionIf[NotFoundException] {
      connector.getSchemeDetails(idValue, schemeIdType, idNumber)
    } map {
      _.responseCode shouldBe NOT_FOUND
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

    recoverToExceptionIf[UpstreamErrorResponse] {
      connector.getSchemeDetails(idValue, schemeIdType, idNumber)
    } map {
      _.statusCode shouldBe FORBIDDEN
    }
  }

  it should "send audit event for successful response" in {
    val IfResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsResponse.json")
    val userAnswersResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsIFUserAnswers.json")

    server.stubFor(
      get(urlEqualTo(schemeDetailsIFUrl))
        .willReturn(
          ok
            .withHeader("Content-Type", "application/json")
            .withBody(IfResponse.toString())
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

    recoverToExceptionIf[NotFoundException] {
      connector.getSchemeDetails(idValue, schemeIdType, idNumber)
    } map {_ =>
      auditService.verifySent(
        SchemeDetailsAuditEvent(idValue, 404, None)
      ) shouldBe true
    }
  }

  "SchemeConnector getPspSchemeDetails" should "return user answer json" in {
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

  it should "throw Upstream5XX for internal server error - 500 and log the event as error" in {

    server.stubFor(
      get(urlEqualTo(pspSchemeDetailsIFUrl))
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
    val IfResponse: JsValue = readJsonFromFile("/data/validGetPspSchemeDetailsResponse.json")
    val userAnswersResponse: JsValue = readJsonFromFile("/data/validGetPspSchemeDetailsUserAnswers.json")

    server.stubFor(
      get(urlEqualTo(pspSchemeDetailsIFUrl))
        .willReturn(
          ok
            .withHeader("Content-Type", "application/json")
            .withBody(IfResponse.toString())
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

    recoverToExceptionIf[NotFoundException] {
      connector.getPspSchemeDetails(pspId, pstr)
    } map {_ =>
      auditService.verifyExtendedSent(
        PspSchemeDetailsAuditEvent(pspId, 404, None)
      ) shouldBe true
    }
  }
}

object SchemeDetailsConnectorSpec extends JsonFileReader {
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val rh: RequestHeader = FakeRequest("", "")
  private val idValue = "test"
  private val schemeIdType = "srn"
  private val idNumber = "S1234567890"
  private val pstr = "20010010AA"

  private val pspId = "psp-id"

  private val schemeDetailsIFUrl: String = s"/pension-online/scheme-details/pods/$schemeIdType/$idNumber"
  private val pspSchemeDetailsIFUrl: String = s"/pension-online/psp-scheme-details/pods/$pspId/$pstr"

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



