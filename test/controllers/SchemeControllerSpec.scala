/*
 * Copyright 2023 HM Revenue & Customs
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
import models.enumeration.SchemeJourneyType
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, JsResultException, JsString, JsValue, Json}
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories._
import service.SchemeService
import uk.gov.hmrc.http._

import java.time.LocalDate
import scala.concurrent.Future

class SchemeControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfter with PatienceConfiguration {
  val mockSchemeService: SchemeService = mock[SchemeService]
  val validSchemeUpdateData: JsValue = readJsonFromFile("/data/validSchemeUpdateRequest.json")
  private val schemeController: SchemeController = injector.instanceOf[SchemeController]

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[SchemeService].toInstance(mockSchemeService),
      bind[AdminDataRepository].toInstance(mock[AdminDataRepository]),
      bind[LockRepository].toInstance(mock[LockRepository]),
      bind[RacdacSchemeSubscriptionCacheRepository].toInstance(mock[RacdacSchemeSubscriptionCacheRepository]),
      bind[SchemeCacheRepository].toInstance(mock[SchemeCacheRepository]),
      bind[SchemeDetailsCacheRepository].toInstance(mock[SchemeDetailsCacheRepository]),
      bind[SchemeDetailsWithIdCacheRepository].toInstance(mock[SchemeDetailsWithIdCacheRepository]),
      bind[SchemeSubscriptionCacheRepository].toInstance(mock[SchemeSubscriptionCacheRepository]),
      bind[UpdateSchemeCacheRepository].toInstance(mock[UpdateSchemeCacheRepository])
    )

  before {
    reset(mockSchemeService)
  }

  "registerScheme" must {

    def fakeRequest(data: JsValue): FakeRequest[AnyContentAsJson] = FakeRequest("POST", "/").withJsonBody(data).withHeaders(("psaId", "A2000001"))

    "return OK when the scheme is registered successfully" in {
      val validData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")
      val successResponse: JsObject = Json.obj("processingDate" -> LocalDate.now, "schemeReferenceNumber" -> "S0123456789")
      when(mockSchemeService.registerScheme(any(), meq(validData))(any(), any(), any())).thenReturn(
        Future.successful(Right(successResponse)))

      val result = schemeController.registerScheme(SchemeJourneyType.NON_RAC_DAC_SCHEME)(fakeRequest(validData))
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        contentAsJson(result) mustBe successResponse
      }
    }

    "throw BadRequestException when PSAId is not present in the header" in {
      val validData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")

      val result = schemeController.registerScheme(SchemeJourneyType.NON_RAC_DAC_SCHEME)(FakeRequest("POST", "/").withJsonBody(validData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Bad Request without PSAId or request body"
        verify(mockSchemeService, never).registerScheme(any(),
          any())(any(), any(), any())
      }
    }

    "throw BadRequestException when no data is not present in the request" in {
      val result = schemeController.registerScheme(SchemeJourneyType.NON_RAC_DAC_SCHEME)(FakeRequest("POST", "/").withHeaders(("psaId", "A2000001")))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Bad Request without PSAId or request body"
        verify(mockSchemeService, never).registerScheme(any(),
          any())(any(), any(), any())
      }
    }

    "throw BadRequestException when bad request returned from If" in {
      val validData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")
      val invalidPayload: JsObject = Json.obj(
        "code" -> "INVALID_PAYLOAD",
        "reason" -> "Submission has not passed validation. Invalid PAYLOAD"
      )
      when(mockSchemeService.registerScheme(any(), any())(any(), any(), any())).thenReturn(
        Future.failed(new BadRequestException(invalidPayload.toString())))

      val result = schemeController.registerScheme(SchemeJourneyType.NON_RAC_DAC_SCHEME)(fakeRequest(validData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe invalidPayload.toString()
      }
    }

    "throw Upstream4xxResponse when UpStream4XXResponse returned from If" in {
      val validData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")
      val invalidSubmission: JsObject = Json.obj(
        "code" -> "INVALID_SUBMISSION",
        "reason" -> "Duplicate submission acknowledgement reference from remote endpoint returned."
      )
      when(mockSchemeService.registerScheme(any(), any())(any(), any(), any())).thenReturn(
        Future.failed(UpstreamErrorResponse(invalidSubmission.toString(), CONFLICT, CONFLICT)))

      val result = schemeController.registerScheme(SchemeJourneyType.NON_RAC_DAC_SCHEME)(fakeRequest(validData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe invalidSubmission.toString()
      }
    }

    "throw Upstream5xxResponse when UpStream5XXResponse returned from If" in {
      val validData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")
      val serviceUnavailable: JsObject = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "Dependent systems are currently not responding."
      )
      when(mockSchemeService.registerScheme(any(), any())(any(), any(), any())).thenReturn(
        Future.failed(UpstreamErrorResponse(serviceUnavailable.toString(), SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)))

      val result = schemeController.registerScheme(SchemeJourneyType.NON_RAC_DAC_SCHEME)(fakeRequest(validData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe serviceUnavailable.toString()
      }
    }

    "throw generic exception when any other exception returned from If" in {
      val validData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")
      when(mockSchemeService.registerScheme(any(), any())(any(), any(), any())).thenReturn(
        Future.failed(new Exception("Generic Exception")))

      val result = schemeController.registerScheme(SchemeJourneyType.NON_RAC_DAC_SCHEME)(fakeRequest(validData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe "Generic Exception"
      }
    }
  }

  "list of schemes" must {
    val fakeRequest = FakeRequest("GET", "/").withHeaders(("idType", "PSA"), ("idValue", "A2000001"))

    "return OK with sorted list of schemes for PSA when If/ETMP returns it successfully" in {
      val validResponse = readJsonFromFile("/data/validListOfSchemesIFResponseAlphabetical.json")
      when(mockSchemeService.listOfSchemes(meq("PSA"), meq("A2000001"))(any(), any(), any()))
        .thenReturn(Future.successful(Right(validResponse)))
      val result = schemeController.listOfSchemes(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual validResponse
        verify(mockSchemeService, times(1)).listOfSchemes(any(), any())(any(), any(), any())
      }
    }

    "return OK with sorted list of schemes for PSP when If/ETMP returns it successfully" in {
      val fakeRequest = FakeRequest("GET", "/").withHeaders(("idType", "PSP"), ("idValue", "A2200001"))
      val validResponse = readJsonFromFile("/data/validListOfSchemesIFResponseAlphabetical.json")
      when(mockSchemeService.listOfSchemes(meq("PSP"), meq("A2200001"))(any(), any(), any()))
        .thenReturn(Future.successful(Right(validResponse)))
      val result = schemeController.listOfSchemes(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual validResponse
        verify(mockSchemeService, times(1)).listOfSchemes(any(), any())(any(), any(), any())
      }
    }

    "throw BadRequestException when PSAId is not present in the header" in {
      val result = schemeController.listOfSchemes(FakeRequest("GET", "/"))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Bad Request with no ID type or value"
        verify(mockSchemeService, never).listOfSchemes(any(), any())(any(), any(), any())
      }
    }

    "throw JsResultException when the invalid data returned from If/ETMP" in {
      val validResponse = Json.obj("invalid" -> "data")
      when(mockSchemeService.listOfSchemes(meq("PSA"), meq("A2000001"))(any(), any(), any()))
        .thenReturn(Future.successful(Right(validResponse)))
      val result = schemeController.listOfSchemes(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[JsResultException]
        verify(mockSchemeService, times(1)).listOfSchemes(any(), any())(any(), any(), any())
      }
    }

    "throw BadRequestException when bad request returned from If" in {
      val invalidPayload: JsObject = Json.obj(
        "code" -> "INVALID_PSAID",
        "reason" -> "Submission has not passed validation. Invalid parameter PSAID."
      )
      when(mockSchemeService.listOfSchemes(meq("PSA"), meq("A2000001"))(any(), any(), any())).thenReturn(
        Future.failed(new BadRequestException(invalidPayload.toString())))

      val result = schemeController.listOfSchemes(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe invalidPayload.toString()
        verify(mockSchemeService, times(1)).listOfSchemes(meq("PSA"), meq("A2000001"))(any(), any(), any())
      }
    }

    "throw Upstream5xxResponse when UpStream5XXResponse returned" in {
      val serviceUnavailable: JsObject = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "Dependent systems are currently not responding."
      )
      when(mockSchemeService.listOfSchemes(meq("PSA"), meq("A2000001"))(any(), any(), any())).thenReturn(
        Future.failed(UpstreamErrorResponse(serviceUnavailable.toString(), SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)))

      val result = schemeController.listOfSchemes(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe serviceUnavailable.toString()
        verify(mockSchemeService, times(1)).listOfSchemes(meq("PSA"), meq("A2000001"))(any(), any(), any())
      }
    }

    "throw generic exception when any other exception returned from If" in {
      when(mockSchemeService.listOfSchemes(meq("PSA"), meq("A2000001"))(any(), any(), any())).thenReturn(
        Future.failed(new Exception("Generic Exception")))

      val result = schemeController.listOfSchemes(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe "Generic Exception"
        verify(mockSchemeService, times(1)).listOfSchemes(meq("PSA"), meq("A2000001"))(any(), any(), any())
      }
    }
  }

  "openDateScheme" must {
    val fakeRequest = FakeRequest("GET", "/").withHeaders(("idType", "PSA"), ("idValue", "A2000001"), ("pstr", "24000001IN"))

       "return OK with openDate for PSA when If/ETMP returns it successfully" in {
      val validResponse = readJsonFromFile("/data/validListOfSchemesIFResponse.json")
      when(mockSchemeService.listOfSchemes(meq("PSA"), meq("A2000001"))(any(), any(), any()))
        .thenReturn(Future.successful(Right(validResponse)))
      val result = schemeController.openDateScheme(fakeRequest)
         ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual JsString("2017-12-17")
        verify(mockSchemeService, times(1)).listOfSchemes(any(), any())(any(), any(), any())
      }
    }

    "return OK with openDate for PSP when If/ETMP returns it successfully" in {
      val fakeRequest = FakeRequest("GET", "/").withHeaders(("idType", "PSP"), ("idValue", "A2200001"), ("pstr", "24000001IN"))
      val validResponse = readJsonFromFile("/data/validListOfSchemesIFResponse.json")
      when(mockSchemeService.listOfSchemes(meq("PSP"), meq("A2200001"))(any(), any(), any()))
        .thenReturn(Future.successful(Right(validResponse)))
      val result = schemeController.openDateScheme(fakeRequest)
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual JsString("2017-12-17")
        verify(mockSchemeService, times(1)).listOfSchemes(any(), any())(any(), any(), any())
      }
    }

    "throw BadRequestException when PSAId is not present in the header" in {
      val result = schemeController.listOfSchemes(FakeRequest("GET", "/"))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Bad Request with no ID type or value"
        verify(mockSchemeService, never).listOfSchemes(any(), any())(any(), any(), any())
      }
    }
  }

  "updateScheme" must {

    def fakeRequest(data: JsValue): FakeRequest[AnyContentAsJson] = FakeRequest("POST", "/").withJsonBody(
      data).withHeaders(("pstr", "20010010AA"), ("psaId", "A2000001"))

    "return OK when the scheme is updated successfully" in {
      val successResponse: JsObject = Json.obj("processingDate" -> LocalDate.now)
      when(mockSchemeService.updateScheme(any(), any(), meq(validSchemeUpdateData))(any(), any(), any())).thenReturn(
        Future.successful(Right(successResponse)))

      val result = schemeController.updateScheme()(fakeRequest(validSchemeUpdateData))
      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        contentAsJson(result) mustBe successResponse
      }
    }

    "throw BadRequestException when PSTR is not present in the header" in {

      val result = schemeController.updateScheme()(FakeRequest("POST", "/").withJsonBody(validSchemeUpdateData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Bad Request without PSTR or PSAId or request body"
        verify(mockSchemeService, never).updateScheme(any(), any(), any())(any(), any(), any())
      }
    }

    "throw BadRequestException when no data is not present in the request" in {
      val result = schemeController.updateScheme()(FakeRequest("POST", "/").withHeaders(("pstr", "20010010AA")))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Bad Request without PSTR or PSAId or request body"
        verify(mockSchemeService, never).updateScheme(any(), any(), any())(any(), any(), any())
      }
    }

    "throw BadRequestException when bad request returned from If" in {
      val invalidPayload: JsObject = Json.obj(
        "code" -> "INVALID_PAYLOAD",
        "reason" -> "Submission has not passed validation. Invalid PAYLOAD"
      )
      when(mockSchemeService.updateScheme(any(), any(), any())(any(), any(), any())).thenReturn(
        Future.failed(new BadRequestException(invalidPayload.toString())))

      val result = schemeController.updateScheme()(fakeRequest(validSchemeUpdateData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe invalidPayload.toString()
      }
    }

    "throw Upstream4xxResponse when UpStream4XXResponse returned from If" in {
      val invalidSubmission: JsObject = Json.obj(
        "code" -> "DUPLICATE_SUBMISSION",
        "reason" -> "The back end has indicated that duplicate submission or acknowledgement reference."
      )
      when(mockSchemeService.updateScheme(any(), any(), any())(any(), any(), any())).thenReturn(
        Future.failed(UpstreamErrorResponse(invalidSubmission.toString(), CONFLICT, CONFLICT)))

      val result = schemeController.updateScheme()(fakeRequest(validSchemeUpdateData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[ConflictException]
        e.getMessage mustBe invalidSubmission.toString()
      }
    }

    "throw Upstream5xxResponse when UpStream5XXResponse returned from If" in {
      val serviceUnavailable: JsObject = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "Dependent systems are currently not responding."
      )
      when(mockSchemeService.updateScheme(any(), any(), any())(any(), any(), any())).thenReturn(
        Future.failed(UpstreamErrorResponse(serviceUnavailable.toString(), SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)))

      val result = schemeController.updateScheme()(fakeRequest(validSchemeUpdateData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe serviceUnavailable.toString()
      }
    }

    "throw generic exception when any other exception returned from If" in {
      when(mockSchemeService.updateScheme(any(), any(), any())(any(), any(), any())).thenReturn(
        Future.failed(new Exception("Generic Exception")))

      val result = schemeController.updateScheme()(fakeRequest(validSchemeUpdateData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe "Generic Exception"
      }
    }
  }
}
