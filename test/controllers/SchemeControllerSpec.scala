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
import org.joda.time.LocalDate
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers
import org.scalatest.BeforeAndAfter
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.FakeRequest
import uk.gov.hmrc.http._
import play.api.mvc.AnyContentAsJson
import play.api.test.Helpers._
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import scala.concurrent.Future

class SchemeControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfter with PatienceConfiguration {

  def fakeRequest(data: JsValue): FakeRequest[AnyContentAsJson] = FakeRequest("POST", "/").withJsonBody(data).withHeaders(("psaId", "A2000001"))
  val mockSchemeConnector: SchemeConnector = mock[SchemeConnector]
  val schemeController = new SchemeController(mockSchemeConnector)

  before(reset(mockSchemeConnector))

  "registerScheme" must {

    "return OK when the scheme is registered successfully" in {
      val validData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")
      val successResponse: JsObject = Json.obj("processingDate" -> LocalDate.now, "schemeReferenceNumber" -> "S0123456789")
      when(mockSchemeConnector.registerScheme(Matchers.eq("A2000001"), Matchers.eq(validData))(Matchers.any(), Matchers.any())).thenReturn(
        Future.successful(HttpResponse(OK, Some(successResponse))))

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

    "throw Upstream4xxResponse when not found exception returned from Des" in {
      val validData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")
      when(mockSchemeConnector.registerScheme(Matchers.eq("A2000001"), Matchers.eq(validData))(Matchers.any(), Matchers.any())).thenReturn(
        Future.failed(new NotFoundException("Not Found Exception")))

      val result = schemeController.registerScheme()(fakeRequest(validData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Upstream4xxResponse]
        e.getMessage mustBe "Not Found Exception"
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
        Future.failed(new Upstream5xxResponse(serviceUnavailable.toString(), CONFLICT, CONFLICT)))

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
}
