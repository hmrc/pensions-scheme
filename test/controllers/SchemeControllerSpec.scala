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

package controllers

import base.SpecBase
import connector.SchemeConnector
import models.{PensionSchemeAdministrator, PensionsScheme}
import org.joda.time.LocalDate
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers
import org.mockito.Matchers._
import org.scalatest.BeforeAndAfter
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.FakeRequest
import uk.gov.hmrc.http._
import play.api.mvc.AnyContentAsJson
import play.api.test.Helpers._
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import service.SchemeService

import scala.concurrent.Future

class SchemeControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfter with PatienceConfiguration {
  val mockSchemeConnector: SchemeConnector = mock[SchemeConnector]
  val mockSchemeService: SchemeService = mock[SchemeService]
  val schemeController = new SchemeController(mockSchemeConnector, mockSchemeService)

  before(reset(mockSchemeConnector))

  "registerScheme" must {

    def fakeRequest(data: JsValue): FakeRequest[AnyContentAsJson] = FakeRequest("POST", "/").withJsonBody(data).withHeaders(("psaId", "A2000001"))

    "return OK when the scheme is registered successfully" in {
      val validData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")
      val successResponse: JsObject = Json.obj("processingDate" -> LocalDate.now, "schemeReferenceNumber" -> "S0123456789")
      when(mockSchemeConnector.registerScheme(Matchers.eq("A2000001"), Matchers.eq(validData))(Matchers.any(), Matchers.any())).thenReturn(
        Future.successful(HttpResponse(OK, Some(successResponse))))
      when(mockSchemeService.retrievePensionScheme(Matchers.eq(validData))(Matchers.any(), Matchers.any())).thenReturn(
        Future.successful(validData.as[PensionsScheme]))

      val result = schemeController.registerScheme()(fakeRequest(validData))
      ScalaFutures.whenReady(result) { res =>
        status(result) mustBe OK
        verify(mockSchemeConnector, times(1)).registerScheme(Matchers.eq("A2000001"),
          Matchers.eq(validData))(Matchers.any(), Matchers.any())
      }
    }

    "throw BadRequestException when PSAId is not present in the header" in {
      val validData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")

      val result = schemeController.registerScheme()(FakeRequest("POST", "/").withJsonBody(validData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Bad Request without PSAId or request body"
        verify(mockSchemeConnector, never()).registerScheme(Matchers.any(),
          Matchers.any())(Matchers.any(), Matchers.any())
      }
    }

    "throw BadRequestException when no data is not present in the request" in {
      val result = schemeController.registerScheme()(FakeRequest("POST", "/").withHeaders(("psaId", "A2000001")))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Bad Request without PSAId or request body"
        verify(mockSchemeConnector, never()).registerScheme(Matchers.any(),
          Matchers.any())(Matchers.any(), Matchers.any())
      }
    }

    "throw BadRequestException when bad request returned from Des" in {
      val validData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")
      val invalidPayload: JsObject = Json.obj(
        "code" -> "INVALID_PAYLOAD",
        "reason" -> "Submission has not passed validation. Invalid PAYLOAD"
      )
      when(mockSchemeConnector.registerScheme(Matchers.eq("A2000001"), Matchers.eq(validData))(Matchers.any(), Matchers.any())).thenReturn(
        Future.failed(new BadRequestException(invalidPayload.toString())))

      val result = schemeController.registerScheme()(fakeRequest(validData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe invalidPayload.toString()
        verify(mockSchemeConnector, times(1)).registerScheme(Matchers.any(),
          Matchers.any())(Matchers.any(), Matchers.any())
      }
    }

    "throw Upstream4xxResponse when UpStream4XXResponse returned from Des" in {
      val validData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")
      val invalidSubmission: JsObject = Json.obj(
        "code" -> "INVALID_SUBMISSION",
        "reason" -> "Duplicate submission acknowledgement reference from remote endpoint returned."
      )
      when(mockSchemeConnector.registerScheme(Matchers.eq("A2000001"), Matchers.eq(validData))(Matchers.any(), Matchers.any())).thenReturn(
        Future.failed(new Upstream4xxResponse(invalidSubmission.toString(), CONFLICT, CONFLICT)))

      val result = schemeController.registerScheme()(fakeRequest(validData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Upstream4xxResponse]
        e.getMessage mustBe invalidSubmission.toString()
        verify(mockSchemeConnector, times(1)).registerScheme(Matchers.any(),
          Matchers.any())(Matchers.any(), Matchers.any())
      }
    }

    "throw Upstream5xxResponse when UpStream5XXResponse returned from Des" in {
      val validData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")
      val serviceUnavailable: JsObject = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "Dependent systems are currently not responding."
      )
      when(mockSchemeConnector.registerScheme(Matchers.eq("A2000001"), Matchers.eq(validData))(Matchers.any(), Matchers.any())).thenReturn(
        Future.failed(new Upstream5xxResponse(serviceUnavailable.toString(), SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)))

      val result = schemeController.registerScheme()(fakeRequest(validData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Upstream5xxResponse]
        e.getMessage mustBe serviceUnavailable.toString()
        verify(mockSchemeConnector, times(1)).registerScheme(Matchers.any(),
          Matchers.any())(Matchers.any(), Matchers.any())
      }
    }

    "throw generic exception when any other exception returned from Des" in {
      val validData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")
      when(mockSchemeConnector.registerScheme(Matchers.eq("A2000001"), Matchers.eq(validData))(Matchers.any(), Matchers.any())).thenReturn(
        Future.failed(new Exception("Generic Exception")))

      val result = schemeController.registerScheme()(fakeRequest(validData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe "Generic Exception"
        verify(mockSchemeConnector, times(1)).registerScheme(Matchers.any(),
          Matchers.any())(Matchers.any(), Matchers.any())
      }
    }
  }

  //TODO: Fixing as part of hooking up reads.
  "registerPSA" ignore  {

    def fakeRequest(data: JsValue): FakeRequest[AnyContentAsJson] = FakeRequest("POST", "/").withJsonBody(data)

    "returns OK when ETMP/DES returns successfully" in {
      val validRequestData = readJsonFromFile("/data/validPsaRequest.json")
      val successResponse = Json.obj("processingDate" -> LocalDate.now, "formBundle" -> "1000000", "psaId" -> "A2000000")

      when(mockSchemeConnector.registerPSA(Matchers.eq(Json.toJson(
        validRequestData.as[PensionSchemeAdministrator])))(Matchers.any(), Matchers.any())).thenReturn(
        Future.successful(HttpResponse(OK, Some(successResponse))))

      val result = schemeController.registerPSA(fakeRequest(validRequestData))
      ScalaFutures.whenReady(result) { res =>
        status(result) mustBe OK
        verify(mockSchemeConnector, times(1)).registerPSA(Matchers.any())(Matchers.any(), Matchers.any())
      }
    }

    "throw BadRequestException when ETMP/DES return bad request" in {
      val invalidRequest = readJsonFromFile("/data/validPsaRequest.json")
      val failureResponse = Json.obj("code" -> "INVALID_PAYLOAD", "reason" -> "Submission has not passed validation. Invalid PAYLOAD.")

      when(mockSchemeConnector.registerPSA(Matchers.eq(Json.toJson(
        invalidRequest.as[PensionSchemeAdministrator])))(Matchers.any(), Matchers.any())).thenReturn(
        Future.failed(new BadRequestException(failureResponse.toString()))
      )
      val result = schemeController.registerPSA(fakeRequest(invalidRequest))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe failureResponse.toString()
        verify(mockSchemeConnector, times(1)).registerPSA(Matchers.any())(Matchers.any(), Matchers.any())
      }
    }

    "throw BadRequestException when there is no data in the request" in {
      val result = schemeController.registerPSA(FakeRequest("POST", "/"))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Bad Request with no request body"
        verify(mockSchemeConnector, never()).registerPSA(Matchers.any())(Matchers.any(), Matchers.any())
      }
    }

    "throw Upstream4xxResponse when DES/ETMP returns Upstream4xxResponse" in {
      val invalidRequest = readJsonFromFile("/data/validPsaRequest.json")
      val invalidBusinessPartner: JsObject = Json.obj(
        "code" -> "INVALID_BUSINESS_PARTNER",
        "reason" -> "Business partner already has active subscription for this regime."
      )
      when(mockSchemeConnector.registerPSA(Matchers.eq(Json.toJson(
        invalidRequest.as[PensionSchemeAdministrator])))(Matchers.any(), Matchers.any())).thenReturn(
        Future.failed(new Upstream4xxResponse(invalidBusinessPartner.toString(), FORBIDDEN, FORBIDDEN))
      )
      val result = schemeController.registerPSA(fakeRequest(invalidRequest))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Upstream4xxResponse]
        e.getMessage mustBe invalidBusinessPartner.toString()
        verify(mockSchemeConnector, times(1)).registerPSA(Matchers.any())(Matchers.any(), Matchers.any())
      }
    }

    "throw Upstream5xxResponse when DES/ETMP returns Upstream5xxResponse" in {
      val invalidRequest = readJsonFromFile("/data/validPsaRequest.json")
      val serverError: JsObject = Json.obj(
        "code" -> "SERVER_ERROR",
        "reason" -> "DES is currently experiencing problems that require live service intervention."
      )
      when(mockSchemeConnector.registerPSA(Matchers.eq(Json.toJson(
        invalidRequest.as[PensionSchemeAdministrator])))(Matchers.any(), Matchers.any())).thenReturn(
        Future.failed(new Upstream5xxResponse(serverError.toString(), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))
      )
      val result = schemeController.registerPSA(fakeRequest(invalidRequest))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Upstream5xxResponse]
        e.getMessage mustBe serverError.toString()
        verify(mockSchemeConnector, times(1)).registerPSA(Matchers.any())(Matchers.any(), Matchers.any())
      }
    }
  }

  "list of schems" must {
    val fakeRequest = FakeRequest("GET", "/").withHeaders(("psaId", "A2000001"))

    "return OK with list of schems when DES/ETMP returns it successfully" in {
      val validResponse = readJsonFromFile("/data/validListOfSchemesResponse.json")
      when(mockSchemeConnector.listOfSchemes(Matchers.eq("A2000001"))(any(), any())).thenReturn(Future.successful(
        HttpResponse(OK, Some(validResponse))))
      val result = schemeController.listOfSchemes(fakeRequest)
      ScalaFutures.whenReady(result){ res =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual validResponse
        verify(mockSchemeConnector, times(1)).listOfSchemes(any())(any(), any())
      }
    }

    "throw BadRequestException when PSAId is not present in the header" in {
      val result = schemeController.listOfSchemes(FakeRequest("GET", "/"))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Bad Request with no Psa Id"
        verify(mockSchemeConnector, never()).registerScheme(any(),
          any())(any(), any())
      }
    }

    "throw BadRequestException when bad request returned from Des" in {
      val invalidPayload: JsObject = Json.obj(
        "code" -> "INVALID_PSAID",
        "reason" -> "Submission has not passed validation. Invalid parameter PSAID."
      )
      when(mockSchemeConnector.listOfSchemes(Matchers.eq("A2000001"))(any(), any())).thenReturn(
        Future.failed(new BadRequestException(invalidPayload.toString())))

      val result = schemeController.listOfSchemes(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe invalidPayload.toString()
        verify(mockSchemeConnector, times(1)).listOfSchemes(Matchers.eq("A2000001"))(Matchers.any(), Matchers.any())
      }
    }

    "throw Upstream5xxResponse when UpStream5XXResponse returned" in {
      val serviceUnavailable: JsObject = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "Dependent systems are currently not responding."
      )
      when(mockSchemeConnector.listOfSchemes(Matchers.eq("A2000001"))(any(), any())).thenReturn(
        Future.failed(new Upstream5xxResponse(serviceUnavailable.toString(), SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)))

      val result = schemeController.listOfSchemes(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Upstream5xxResponse]
        e.getMessage mustBe serviceUnavailable.toString()
        verify(mockSchemeConnector, times(1)).listOfSchemes(Matchers.eq("A2000001"))(any(), any())
      }
    }

    "throw generic exception when any other exception returned from Des" in {
      when(mockSchemeConnector.listOfSchemes(Matchers.eq("A2000001"))(any(), any())).thenReturn(
        Future.failed(new Exception("Generic Exception")))

      val result = schemeController.listOfSchemes(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe "Generic Exception"
        verify(mockSchemeConnector, times(1)).listOfSchemes(Matchers.eq("A2000001"))(any(), any())
      }
    }
  }
}
