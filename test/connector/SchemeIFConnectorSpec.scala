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

import audit.{AuditService, PspSchemeDetailsAuditEvent, SchemeDetailsAuditEvent}
import base.JsonFileReader
import com.github.tomakehurst.wiremock.client.WireMock._
import models.FeatureToggle.Enabled
import models.FeatureToggleName.IntegrationFrameworkGetSchemeDetails
import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest._
import org.scalatestplus.mockito.MockitoSugar
import play.api.LoggerLike
import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import service.FeatureToggleService
import uk.gov.hmrc.http.HeaderCarrier
import utils.WireMockHelper

import scala.concurrent.Future

class SchemeIFConnectorSpec extends AsyncFlatSpec
  with WireMockHelper
  with OptionValues
  with MockitoSugar
  with RecoverMethods
  with EitherValues
  with ConnectorBehaviours with JsonFileReader{

  import SchemeConnectorSpec._

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val rh: RequestHeader = FakeRequest("", "")

  private val mockFeatureToggleService = mock[FeatureToggleService]
  override def beforeEach(): Unit = {
    auditService.reset()
    when(mockFeatureToggleService.get(Matchers.any())).thenReturn(Future.successful(Enabled(IntegrationFrameworkGetSchemeDetails)))

    super.beforeEach()
  }
  override protected def portConfigKey: String = "microservice.services.if-hod.port"
  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[AuditService].toInstance(auditService),
      bind[LoggerLike].toInstance(logger),
      bind[FeatureToggleService].toInstance(mockFeatureToggleService)
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
      auditService.verifySent(
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

    connector.getPspSchemeDetails(pspId, pstr).map { response =>
      auditService.verifySent(
        PspSchemeDetailsAuditEvent(pspId, 404, Some(Json.parse(expectedResponse)))
      ) shouldBe true
    }
  }
}


