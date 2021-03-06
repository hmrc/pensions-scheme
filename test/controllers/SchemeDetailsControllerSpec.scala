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

package controllers

import base.SpecBase
import connector.SchemeConnector
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.JsValue
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import service.SchemeService
import uk.gov.hmrc.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SchemeDetailsControllerSpec
  extends SpecBase
    with MockitoSugar
    with BeforeAndAfter
    with PatienceConfiguration {


  private val mockSchemeService: SchemeService = mock[SchemeService]
  private val mockSchemeConnector: SchemeConnector = mock[SchemeConnector]
  private val schemeDetailsController = new SchemeDetailsController(mockSchemeConnector, mockSchemeService, stubControllerComponents())
  private val schemeIdType = "srn"
  private val idNumber = "00000000AA"
  private val psaId = "000"
  private val pspId = "000"
  private val userAnswersResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsUserAnswers.json")

  before {
    reset(mockSchemeConnector)
    when(mockSchemeService.getPstrFromSrn(any(), any(), any())(any(), any(), any())).thenReturn(Future.successful(idNumber))
  }

  "getSchemeDetails" must {

    def fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest("GET", "/").withHeaders(
        ("schemeIdType", schemeIdType),
        ("idNumber", idNumber),
        ("PSAId", psaId)
      )

    "return OK when the scheme is registered successfully" in {

      val successResponse = userAnswersResponse
      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Right(successResponse)))

      val result = schemeDetailsController.getSchemeDetails()(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result) mustBe successResponse
    }

    "throw BadRequestException when SchemeIdNumber is not present in the header" in {
      val result = schemeDetailsController.getSchemeDetails()(FakeRequest("GET", "/").withHeaders(
        ("schemeIdType", schemeIdType),
        ("PSAId", psaId)
      ))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Bad Request with missing parameters idType, idNumber or PSAId"
        verify(mockSchemeConnector, never()).getSchemeDetails(any(), any(), any())(any(), any(), any())
      }
    }

    "throw BadRequestException when SchemeIdType is not present in the header" in {

      val result = schemeDetailsController.getSchemeDetails()(FakeRequest("GET", "/").withHeaders(
        ("idNumber", idNumber),
        ("PSAId", psaId)
      ))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Bad Request with missing parameters idType, idNumber or PSAId"
        verify(mockSchemeConnector, never()).getSchemeDetails(any(), any(), any())(any(), any(), any())
      }
    }

    "throw BadRequestException when PSAId is not present in the header" in {

      val result = schemeDetailsController.getSchemeDetails()(FakeRequest("GET", "/").withHeaders(
        ("idNumber", idNumber),
        ("schemeIdType", schemeIdType)
      ))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Bad Request with missing parameters idType, idNumber or PSAId"
        verify(mockSchemeConnector, never()).getSchemeDetails(any(), any(), any())(any(), any(), any())
      }
    }


    "throw BadRequestException when bad request with INVALID_IDTYPE returned from Des" in {
      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(
          Future.failed(new BadRequestException(errorResponse("INVALID_IDTYPE")))
        )

      val result = schemeDetailsController.getSchemeDetails()(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_IDTYPE")
      }
    }

    "throw BadRequestException when bad request with INVALID_SRN returned from Des" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(
          Future.failed(new BadRequestException(errorResponse("INVALID_SRN")))
        )

      val result = schemeDetailsController.getSchemeDetails()(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_SRN")
      }
    }

    "throw BadRequestException when bad request with INVALID_PSTR returned from Des" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(
          Future.failed(new BadRequestException(errorResponse("INVALID_PSTR")))
        )

      val result = schemeDetailsController.getSchemeDetails()(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_PSTR")
      }
    }

    "throw BadRequestException when bad request with INVALID_CORRELATIONID returned from Des" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(
          Future.failed(new BadRequestException(errorResponse("INVALID_CORRELATIONID")))
        )

      val result = schemeDetailsController.getSchemeDetails()(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_CORRELATIONID")
      }
    }

    "throw Upstream4xxResponse when UpStream4XXResponse returned from Des" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any())).thenReturn(
        Future.failed(UpstreamErrorResponse(errorResponse("NOT_FOUND"), NOT_FOUND, NOT_FOUND))
      )

      val result = schemeDetailsController.getSchemeDetails()(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe errorResponse("NOT_FOUND")
      }
    }

    "throw Upstream5xxResponse when UpStream5XXResponse with SERVICE_UNAVAILABLE returned from Des" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(
          Future.failed(UpstreamErrorResponse(errorResponse("NOT_FOUND"), SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE))
        )

      val result = schemeDetailsController.getSchemeDetails()(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe errorResponse("NOT_FOUND")
      }
    }

    "throw Upstream5xxResponse when UpStream5XXResponse with INTERNAL_SERVER_ERROR returned from Des" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(
          Future.failed(UpstreamErrorResponse(errorResponse("NOT_FOUND"), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))
        )

      val result = schemeDetailsController.getSchemeDetails()(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe errorResponse("NOT_FOUND")
      }
    }

    "throw generic exception when any other exception returned from Des" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(new Exception("Generic Exception")))

      val result = schemeDetailsController.getSchemeDetails()(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe "Generic Exception"
      }
    }
  }

  "getPspSchemeDetails" must {

    def fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest("GET", "/").withHeaders(("srn", idNumber), ("pspId", pspId))

    "return OK when the scheme is registered successfully" in {

      val successResponse = userAnswersResponse
      when(mockSchemeConnector.getPspSchemeDetails(Matchers.eq(pspId), Matchers.eq("00000000AA"))(any(), any(), any())).thenReturn(
        Future.successful(Right(successResponse)))

      val result = schemeDetailsController.getPspSchemeDetails()(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result) mustBe successResponse
    }

    "throw BadRequestException when SchemeIdNumber is not present in the header" in {
      val result = schemeDetailsController.getPspSchemeDetails()(FakeRequest("GET", "/").withHeaders(("schemeIdType", schemeIdType), ("PSAId", psaId)))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Bad Request with missing parameters idType, idNumber or PSAId"
        verify(mockSchemeConnector, never()).getPspSchemeDetails(Matchers.any(),
          Matchers.any())(any(), any(), any())
      }
    }

    "throw BadRequestException when SchemeIdType is not present in the header" in {

      val result = schemeDetailsController.getPspSchemeDetails()(FakeRequest("GET", "/").withHeaders(("idNumber", idNumber), ("PSAId", psaId)))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Bad Request with missing parameters idType, idNumber or PSAId"
        verify(mockSchemeConnector, never()).getPspSchemeDetails(Matchers.any(),
          Matchers.any())(any(), any(), any())
      }
    }

    "throw BadRequestException when PSAId is not present in the header" in {

      val result = schemeDetailsController.getPspSchemeDetails()(FakeRequest("GET", "/").withHeaders(("idNumber", idNumber), ("schemeIdType", schemeIdType)))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Bad Request with missing parameters idType, idNumber or PSAId"
        verify(mockSchemeConnector, never()).getPspSchemeDetails(Matchers.any(),
          Matchers.any())(any(), any(), any())
      }
    }


    "throw BadRequestException when bad request with INVALID_IDTYPE returned from Des" in {
      when(mockSchemeConnector.getPspSchemeDetails(Matchers.eq(psaId), Matchers.eq(idNumber))(any(), any(), any())).thenReturn(
        Future.failed(new BadRequestException(errorResponse("INVALID_IDTYPE"))))

      val result = schemeDetailsController.getPspSchemeDetails()(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_IDTYPE")
      }
    }

    "throw BadRequestException when bad request with INVALID_SRN returned from Des" in {

      when(mockSchemeConnector.getPspSchemeDetails(Matchers.eq(psaId), Matchers.eq(idNumber))(any(), any(), any())).thenReturn(
        Future.failed(new BadRequestException(errorResponse("INVALID_SRN"))))

      val result = schemeDetailsController.getPspSchemeDetails()(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_SRN")
      }
    }

    "throw BadRequestException when bad request with INVALID_PSTR returned from Des" in {

      when(mockSchemeConnector.getPspSchemeDetails(Matchers.eq(psaId), Matchers.eq(idNumber))(any(), any(), any())).thenReturn(
        Future.failed(new BadRequestException(errorResponse("INVALID_PSTR"))))

      val result = schemeDetailsController.getPspSchemeDetails()(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_PSTR")
      }
    }

    "throw BadRequestException when bad request with INVALID_CORRELATIONID returned from Des" in {

      when(mockSchemeConnector.getPspSchemeDetails(Matchers.eq(psaId), Matchers.eq(idNumber))(any(), any(), any())).thenReturn(
        Future.failed(new BadRequestException(errorResponse("INVALID_CORRELATIONID"))))

      val result = schemeDetailsController.getPspSchemeDetails()(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_CORRELATIONID")
      }
    }

    "throw Upstream4xxResponse when UpStream4XXResponse returned from Des" in {

      when(mockSchemeConnector.getPspSchemeDetails(Matchers.eq(psaId), Matchers.eq(idNumber))(any(), any(), any())).thenReturn(
        Future.failed(UpstreamErrorResponse(errorResponse("NOT_FOUND"), NOT_FOUND, NOT_FOUND)))

      val result = schemeDetailsController.getPspSchemeDetails()(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe errorResponse("NOT_FOUND")
      }
    }

    "throw Upstream5xxResponse when UpStream5XXResponse with SERVICE_UNAVAILABLE returned from Des" in {

      when(mockSchemeConnector.getPspSchemeDetails(Matchers.eq(psaId), Matchers.eq(idNumber))(any(), any(), any())).thenReturn(
        Future.failed(UpstreamErrorResponse(errorResponse("NOT_FOUND"), SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)))

      val result = schemeDetailsController.getPspSchemeDetails()(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe errorResponse("NOT_FOUND")
      }
    }

    "throw Upstream5xxResponse when UpStream5XXResponse with INTERNAL_SERVER_ERROR returned from Des" in {

      when(mockSchemeConnector.getPspSchemeDetails(Matchers.eq(psaId), Matchers.eq(idNumber))(any(), any(), any())).thenReturn(
        Future.failed(UpstreamErrorResponse(errorResponse("NOT_FOUND"), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      val result = schemeDetailsController.getPspSchemeDetails()(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe errorResponse("NOT_FOUND")
      }
    }

    "throw generic exception when any other exception returned from Des" in {

      when(mockSchemeConnector.getPspSchemeDetails(Matchers.eq(pspId), Matchers.eq(idNumber))(any(), any(), any())).thenReturn(
        Future.failed(new Exception("Generic Exception")))

      val result = schemeDetailsController.getPspSchemeDetails()(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe "Generic Exception"
      }
    }
  }
}
