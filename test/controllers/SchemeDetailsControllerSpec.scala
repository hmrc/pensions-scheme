/*
 * Copyright 2025 HM Revenue & Customs
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
import connector.SchemeDetailsConnector
import controllers.actions.{PsaPspSchemeAuthAction, PsaSchemeAuthAction}
import models.PsaInvitationInfoResponse
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, reset, verify, when}
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories._
import service.SchemeService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{BadRequestException, UpstreamErrorResponse}
import utils.AuthUtils
import utils.AuthUtils.{FakePsaPspSchemeAuthAction, FakePsaSchemeAuthAction}

import scala.concurrent.Future

class SchemeDetailsControllerSpec
  extends SpecBase
    with MockitoSugar
    with BeforeAndAfter
    with ScalaFutures {

  private val mockSchemeService: SchemeService = mock[SchemeService]
  private val mockSchemeConnector: SchemeDetailsConnector = mock[SchemeDetailsConnector]
  private val mockSchemeDetailsCache: SchemeDetailsWithIdCacheRepository = mock[SchemeDetailsWithIdCacheRepository]
  private val schemeIdType = "srn"
  private val psaId = AuthUtils.psaId
  private val pspId = AuthUtils.pspId
  private val userAnswersResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsUserAnswers.json")
  private val srn = AuthUtils.srn.id
  private val mockAuthConnector = mock[AuthConnector]

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[SchemeDetailsConnector].toInstance(mockSchemeConnector),
      bind[SchemeService].toInstance(mockSchemeService),
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[PsaSchemeAuthAction].to[FakePsaSchemeAuthAction],
      bind[PsaPspSchemeAuthAction].to[FakePsaPspSchemeAuthAction],
      bind[LockRepository].toInstance(mock[LockRepository]),
      bind[RacdacSchemeSubscriptionCacheRepository].toInstance(mock[RacdacSchemeSubscriptionCacheRepository]),
      bind[SchemeCacheRepository].toInstance(mock[SchemeCacheRepository]),
      bind[SchemeDetailsCacheRepository].toInstance(mock[SchemeDetailsCacheRepository]),
      bind[SchemeDetailsWithIdCacheRepository].toInstance(mockSchemeDetailsCache),
      bind[SchemeSubscriptionCacheRepository].toInstance(mock[SchemeSubscriptionCacheRepository]),
      bind[UpdateSchemeCacheRepository].toInstance(mock[UpdateSchemeCacheRepository])
    )

  private val schemeDetailsController: SchemeDetailsController = injector.instanceOf[SchemeDetailsController]

  before {
    reset(mockSchemeConnector)
    reset(mockAuthConnector)
    AuthUtils.authStub(mockAuthConnector)
    when(mockSchemeService.getPstrFromSrn(any(), any(), any())(any(), any(), any())).thenReturn(Future.successful(srn))
    when(mockSchemeDetailsCache.get(any())).thenReturn(Future.successful(None))
    when(mockSchemeDetailsCache.upsert(any(), any())).thenReturn(Future.successful(true))
  }

  "getSchemeDetails" must {

    def fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest("GET", "/").withHeaders(
        ("schemeIdType", schemeIdType),
        ("idNumber", srn),
        ("PSAId", psaId)
      )

    "return OK when the scheme is registered successfully, no data is found in cache and caching toggle is on" in {

      val successResponse = userAnswersResponse
      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Right(successResponse.as[JsObject])))

      val result = schemeDetailsController.getSchemeDetails()(fakeRequest)

      status(result).mustBe(OK)
      contentAsJson(result) mustBe successResponse
    }

    "return OK when the scheme is registered successfully, data is found in cache" in {

      val successResponse = userAnswersResponse
      when(mockSchemeDetailsCache.get(any())).thenReturn(Future.successful(Some(successResponse)))
      val result = schemeDetailsController.getSchemeDetails()(fakeRequest)

      status(result).mustBe(OK)
      contentAsJson(result) mustBe successResponse
    }

    "throw BadRequestException when SchemeIdNumber is not present in the header" in {
      val result = schemeDetailsController.getSchemeDetails()(FakeRequest("GET", "/").withHeaders(
        ("schemeIdType", schemeIdType),
        ("PSAId", psaId)
      ))

      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage.mustBe("Bad Request with missing parameters idType, idNumber or PSAId")
        verify(mockSchemeConnector, never).getSchemeDetails(any(), any(), any())(any(), any(), any())
      }
    }

    "throw BadRequestException when SchemeIdType is not present in the header" in {

      val result = schemeDetailsController.getSchemeDetails()(FakeRequest("GET", "/").withHeaders(
        ("idNumber", srn),
        ("PSAId", psaId)
      ))

      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage.mustBe("Bad Request with missing parameters idType, idNumber or PSAId")
        verify(mockSchemeConnector, never).getSchemeDetails(any(), any(), any())(any(), any(), any())
      }
    }

    "throw BadRequestException when PSAId is not present in the header" in {

      val result = schemeDetailsController.getSchemeDetails()(FakeRequest("GET", "/").withHeaders(
        ("idNumber", srn),
        ("schemeIdType", schemeIdType)
      ))

      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage.mustBe("Bad Request with missing parameters idType, idNumber or PSAId")
        verify(mockSchemeConnector, never).getSchemeDetails(any(), any(), any())(any(), any(), any())
      }
    }


    "throw BadRequestException when bad request with INVALID_IDTYPE returned from If" in {
      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(
          Future.failed(new BadRequestException(errorResponse("INVALID_IDTYPE")))
        )

      val result = schemeDetailsController.getSchemeDetails()(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_IDTYPE")
      }
    }

    "throw BadRequestException when bad request with INVALID_SRN returned from If" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(
          Future.failed(new BadRequestException(errorResponse("INVALID_SRN")))
        )

      val result = schemeDetailsController.getSchemeDetails()(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_SRN")
      }
    }

    "throw BadRequestException when bad request with INVALID_PSTR returned from If" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(
          Future.failed(new BadRequestException(errorResponse("INVALID_PSTR")))
        )

      val result = schemeDetailsController.getSchemeDetails()(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_PSTR")
      }
    }

    "throw BadRequestException when bad request with INVALID_CORRELATIONID returned from If" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(
          Future.failed(new BadRequestException(errorResponse("INVALID_CORRELATIONID")))
        )

      val result = schemeDetailsController.getSchemeDetails()(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_CORRELATIONID")
      }
    }

    "throw Upstream4xxResponse when UpStream4XXResponse returned from If" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any())).thenReturn(
        Future.failed(UpstreamErrorResponse(errorResponse("NOT_FOUND"), NOT_FOUND, NOT_FOUND))
      )

      val result = schemeDetailsController.getSchemeDetails()(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe errorResponse("NOT_FOUND")
      }
    }

    "throw Upstream5xxResponse when UpStream5XXResponse with SERVICE_UNAVAILABLE returned from If" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(
          Future.failed(UpstreamErrorResponse(errorResponse("NOT_FOUND"), SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE))
        )

      val result = schemeDetailsController.getSchemeDetails()(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe errorResponse("NOT_FOUND")
      }
    }

    "throw Upstream5xxResponse when UpStream5XXResponse with INTERNAL_SERVER_ERROR returned from If" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(
          Future.failed(UpstreamErrorResponse(errorResponse("NOT_FOUND"), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))
        )

      val result = schemeDetailsController.getSchemeDetails()(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe errorResponse("NOT_FOUND")
      }
    }

    "throw generic exception when any other exception returned from If" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(new Exception("Generic Exception")))

      val result = schemeDetailsController.getSchemeDetails()(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe "Generic Exception"
      }
    }
  }

  "getSchemeDetailsSrn" must {

    def fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest("GET", "/").withHeaders(
        ("schemeIdType", schemeIdType),
        ("idNumber", srn)
      )

    "return OK when the scheme is registered successfully, no data is found in cache and caching toggle is on" in {

      val successResponse = userAnswersResponse
      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Right(successResponse.as[JsObject])))

      val result = schemeDetailsController.getSchemeDetailsSrn(srn)(fakeRequest)

      status(result).mustBe(OK)
      contentAsJson(result) mustBe successResponse
    }

    "return OK when the scheme is registered successfully, data is found in cache" in {

      val successResponse = userAnswersResponse
      when(mockSchemeDetailsCache.get(any())).thenReturn(Future.successful(Some(successResponse)))
      val result = schemeDetailsController.getSchemeDetailsSrn(srn)(fakeRequest)

      status(result).mustBe(OK)
      contentAsJson(result) mustBe successResponse
    }

    "throw BadRequestException when SchemeIdNumber is not present in the header" in {
      val result = schemeDetailsController.getSchemeDetailsSrn(srn)(FakeRequest("GET", "/").withHeaders(
        ("schemeIdType", schemeIdType),
        ("PSAId", psaId)
      ))

      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage.mustBe("Bad Request with missing parameters idType, idNumber or PSAId")
        verify(mockSchemeConnector, never).getSchemeDetails(any(), any(), any())(any(), any(), any())
      }
    }

    "throw BadRequestException when SchemeIdType is not present in the header" in {

      val result = schemeDetailsController.getSchemeDetailsSrn(srn)(FakeRequest("GET", "/").withHeaders(
        ("idNumber", srn),
        ("PSAId", psaId)
      ))

      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage.mustBe("Bad Request with missing parameters idType, idNumber or PSAId")
        verify(mockSchemeConnector, never).getSchemeDetails(any(), any(), any())(any(), any(), any())
      }
    }

    "throw BadRequestException when bad request with INVALID_IDTYPE returned from If" in {
      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(
          Future.failed(new BadRequestException(errorResponse("INVALID_IDTYPE")))
        )

      val result = schemeDetailsController.getSchemeDetailsSrn(srn)(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_IDTYPE")
      }
    }

    "throw BadRequestException when bad request with INVALID_SRN returned from If" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(
          Future.failed(new BadRequestException(errorResponse("INVALID_SRN")))
        )

      val result = schemeDetailsController.getSchemeDetailsSrn(srn)(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_SRN")
      }
    }

    "throw BadRequestException when bad request with INVALID_PSTR returned from If" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(
          Future.failed(new BadRequestException(errorResponse("INVALID_PSTR")))
        )

      val result = schemeDetailsController.getSchemeDetailsSrn(srn)(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_PSTR")
      }
    }

    "throw BadRequestException when bad request with INVALID_CORRELATIONID returned from If" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(
          Future.failed(new BadRequestException(errorResponse("INVALID_CORRELATIONID")))
        )

      val result = schemeDetailsController.getSchemeDetailsSrn(srn)(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_CORRELATIONID")
      }
    }

    "throw Upstream4xxResponse when UpStream4XXResponse returned from If" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any())).thenReturn(
        Future.failed(UpstreamErrorResponse(errorResponse("NOT_FOUND"), NOT_FOUND, NOT_FOUND))
      )

      val result = schemeDetailsController.getSchemeDetailsSrn(srn)(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe errorResponse("NOT_FOUND")
      }
    }

    "throw Upstream5xxResponse when UpStream5XXResponse with SERVICE_UNAVAILABLE returned from If" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(
          Future.failed(UpstreamErrorResponse(errorResponse("NOT_FOUND"), SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE))
        )

      val result = schemeDetailsController.getSchemeDetailsSrn(srn)(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe errorResponse("NOT_FOUND")
      }
    }

    "throw Upstream5xxResponse when UpStream5XXResponse with INTERNAL_SERVER_ERROR returned from If" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(
          Future.failed(UpstreamErrorResponse(errorResponse("NOT_FOUND"), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))
        )

      val result = schemeDetailsController.getSchemeDetailsSrn(srn)(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe errorResponse("NOT_FOUND")
      }
    }

    "throw generic exception when any other exception returned from If" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(new Exception("Generic Exception")))

      val result = schemeDetailsController.getSchemeDetailsSrn(srn)(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage.mustBe("Generic Exception")
      }
    }
  }

  "getSchemePsaInvitationInfo" must {

    def fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest("GET", "/").withHeaders(
        ("schemeIdType", schemeIdType),
        ("idNumber", srn)
      )

    val successResponse = PsaInvitationInfoResponse(
      Some("12345678AB"),
      Some("Benefits Scheme"),
      Some(Json.parse(
        """
          |{
          |    "schemeTypeDetails": " ",
          |    "name": "master"
          |  }
          |""".stripMargin).as[JsObject]),
      None
    )

    "return OK when the scheme is registered successfully, no data is found in cache and caching toggle is on" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Right(userAnswersResponse.as[JsObject])))

      val result = schemeDetailsController.getSchemePsaInvitationInfo()(fakeRequest)

      status(result).mustBe(OK)
      contentAsJson(result) mustBe Json.toJson(successResponse)
    }

    "return OK when the scheme is registered successfully, data is found in cache" in {

      when(mockSchemeDetailsCache.get(any())).thenReturn(Future.successful(Some(userAnswersResponse)))
      val result = schemeDetailsController.getSchemePsaInvitationInfo()(fakeRequest)

      status(result).mustBe(OK)
      contentAsJson(result) mustBe Json.toJson(successResponse)
    }

    "throw BadRequestException when idNumber is not present in the header" in {
      val result = schemeDetailsController.getSchemePsaInvitationInfo()(FakeRequest("GET", "/").withHeaders(
        ("schemeIdType", schemeIdType)
      ))

      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage.mustBe("Bad Request with missing parameters schemeIdType, idNumber")
        verify(mockSchemeConnector, never).getSchemeDetails(any(), any(), any())(any(), any(), any())
      }
    }

    "throw BadRequestException when SchemeIdType is not present in the header" in {

      val result = schemeDetailsController.getSchemePsaInvitationInfo()(FakeRequest("GET", "/").withHeaders(
        ("idNumber", srn)
      ))

      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage.mustBe("Bad Request with missing parameters schemeIdType, idNumber")
        verify(mockSchemeConnector, never).getSchemeDetails(any(), any(), any())(any(), any(), any())
      }
    }

    "throw BadRequestException when bad request with INVALID_IDTYPE returned from If" in {
      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(
          Future.failed(new BadRequestException(errorResponse("INVALID_IDTYPE")))
        )

      val result = schemeDetailsController.getSchemePsaInvitationInfo()(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_IDTYPE")
      }
    }

    "throw BadRequestException when bad request with INVALID_SRN returned from If" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(
          Future.failed(new BadRequestException(errorResponse("INVALID_SRN")))
        )

      val result = schemeDetailsController.getSchemePsaInvitationInfo()(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_SRN")
      }
    }

    "throw BadRequestException when bad request with INVALID_PSTR returned from If" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(
          Future.failed(new BadRequestException(errorResponse("INVALID_PSTR")))
        )

      val result = schemeDetailsController.getSchemePsaInvitationInfo()(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_PSTR")
      }
    }

    "throw BadRequestException when bad request with INVALID_CORRELATIONID returned from If" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(
          Future.failed(new BadRequestException(errorResponse("INVALID_CORRELATIONID")))
        )

      val result = schemeDetailsController.getSchemePsaInvitationInfo()(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_CORRELATIONID")
      }
    }

    "throw Upstream4xxResponse when UpStream4XXResponse returned from If" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any())).thenReturn(
        Future.failed(UpstreamErrorResponse(errorResponse("NOT_FOUND"), NOT_FOUND, NOT_FOUND))
      )

      val result = schemeDetailsController.getSchemePsaInvitationInfo()(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe errorResponse("NOT_FOUND")
      }
    }

    "throw Upstream5xxResponse when UpStream5XXResponse with SERVICE_UNAVAILABLE returned from If" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(
          Future.failed(UpstreamErrorResponse(errorResponse("NOT_FOUND"), SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE))
        )

      val result = schemeDetailsController.getSchemePsaInvitationInfo()(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe errorResponse("NOT_FOUND")
      }
    }

    "throw Upstream5xxResponse when UpStream5XXResponse with INTERNAL_SERVER_ERROR returned from If" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(
          Future.failed(UpstreamErrorResponse(errorResponse("NOT_FOUND"), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR))
        )

      val result = schemeDetailsController.getSchemePsaInvitationInfo()(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe errorResponse("NOT_FOUND")
      }
    }

    "throw generic exception when any other exception returned from If" in {

      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(new Exception("Generic Exception")))

      val result = schemeDetailsController.getSchemePsaInvitationInfo()(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe "Generic Exception"
      }
    }
  }

  "getPspSchemeDetails" must {

    def fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest("GET", "/").withHeaders(("srn", srn), ("pspId", pspId))

    "return OK when the scheme is registered successfully" in {
      reset(mockAuthConnector)
      AuthUtils.authStubPsp(mockAuthConnector)
      val successResponse = userAnswersResponse
      when(mockSchemeConnector.getPspSchemeDetails(ArgumentMatchers.eq(pspId), ArgumentMatchers.eq(srn))(any(), any(), any())).thenReturn(
        Future.successful(Right(successResponse.as[JsObject])))

      val result = schemeDetailsController.getPspSchemeDetails()(fakeRequest)

      status(result).mustBe(OK)
      contentAsJson(result) mustBe successResponse
    }

    "throw BadRequestException when SchemeIdNumber is not present in the header" in {
      reset(mockAuthConnector)
      AuthUtils.authStubPsp(mockAuthConnector)
      val result = schemeDetailsController.getPspSchemeDetails()(FakeRequest("GET", "/").withHeaders(("schemeIdType", schemeIdType), ("PSAId", psaId)))
      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage.mustBe("Bad Request with missing parameters idType, idNumber or PSAId")
        verify(mockSchemeConnector, never).getPspSchemeDetails(ArgumentMatchers.any(),
          ArgumentMatchers.any())(any(), any(), any())
      }
    }

    "throw BadRequestException when SchemeIdType is not present in the header" in {
      reset(mockAuthConnector)
      AuthUtils.authStubPsp(mockAuthConnector)
      val result = schemeDetailsController.getPspSchemeDetails()(FakeRequest("GET", "/").withHeaders(("idNumber", srn), ("PSAId", psaId)))
      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage.mustBe("Bad Request with missing parameters idType, idNumber or PSAId")
        verify(mockSchemeConnector, never).getPspSchemeDetails(ArgumentMatchers.any(),
          ArgumentMatchers.any())(any(), any(), any())
      }
    }

    "throw BadRequestException when PSAId is not present in the header" in {
      reset(mockAuthConnector)
      AuthUtils.authStubPsp(mockAuthConnector)
      val result = schemeDetailsController.getPspSchemeDetails()(FakeRequest("GET", "/").withHeaders(("idNumber", srn), ("schemeIdType", schemeIdType)))
      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage.mustBe("Bad Request with missing parameters idType, idNumber or PSAId")
        verify(mockSchemeConnector, never).getPspSchemeDetails(ArgumentMatchers.any(),
          ArgumentMatchers.any())(any(), any(), any())
      }
    }


    "throw BadRequestException when bad request with INVALID_IDTYPE returned from If" in {
      reset(mockAuthConnector)
      AuthUtils.authStubPsp(mockAuthConnector)
      when(mockSchemeConnector.getPspSchemeDetails(ArgumentMatchers.eq(pspId), ArgumentMatchers.eq(srn))(any(), any(), any())).thenReturn(
        Future.failed(new BadRequestException(errorResponse("INVALID_IDTYPE"))))

      val result = schemeDetailsController.getPspSchemeDetails()(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_IDTYPE")
      }
    }

    "throw BadRequestException when bad request with INVALID_SRN returned from If" in {
      reset(mockAuthConnector)
      AuthUtils.authStubPsp(mockAuthConnector)
      when(mockSchemeConnector.getPspSchemeDetails(ArgumentMatchers.eq(pspId), ArgumentMatchers.eq(srn))(any(), any(), any())).thenReturn(
        Future.failed(new BadRequestException(errorResponse("INVALID_SRN"))))

      val result = schemeDetailsController.getPspSchemeDetails()(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_SRN")
      }
    }

    "throw BadRequestException when bad request with INVALID_PSTR returned from If" in {
      reset(mockAuthConnector)
      AuthUtils.authStubPsp(mockAuthConnector)
      when(mockSchemeConnector.getPspSchemeDetails(ArgumentMatchers.eq(pspId), ArgumentMatchers.eq(srn))(any(), any(), any())).thenReturn(
        Future.failed(new BadRequestException(errorResponse("INVALID_PSTR"))))

      val result = schemeDetailsController.getPspSchemeDetails()(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_PSTR")
      }
    }

    "throw BadRequestException when bad request with INVALID_CORRELATIONID returned from If" in {
      reset(mockAuthConnector)
      AuthUtils.authStubPsp(mockAuthConnector)
      when(mockSchemeConnector.getPspSchemeDetails(ArgumentMatchers.eq(pspId), ArgumentMatchers.eq(srn))(any(), any(), any())).thenReturn(
        Future.failed(new BadRequestException(errorResponse("INVALID_CORRELATIONID"))))

      val result = schemeDetailsController.getPspSchemeDetails()(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_CORRELATIONID")
      }
    }

    "throw Upstream4xxResponse when UpStream4XXResponse returned from If" in {
      reset(mockAuthConnector)
      AuthUtils.authStubPsp(mockAuthConnector)
      when(mockSchemeConnector.getPspSchemeDetails(ArgumentMatchers.eq(pspId), ArgumentMatchers.eq(srn))(any(), any(), any())).thenReturn(
        Future.failed(UpstreamErrorResponse(errorResponse("NOT_FOUND"), NOT_FOUND, NOT_FOUND)))

      val result = schemeDetailsController.getPspSchemeDetails()(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe errorResponse("NOT_FOUND")
      }
    }

    "throw Upstream5xxResponse when UpStream5XXResponse with SERVICE_UNAVAILABLE returned from If" in {
      reset(mockAuthConnector)
      AuthUtils.authStubPsp(mockAuthConnector)
      when(mockSchemeConnector.getPspSchemeDetails(ArgumentMatchers.eq(pspId), ArgumentMatchers.eq(srn))(any(), any(), any())).thenReturn(
        Future.failed(UpstreamErrorResponse(errorResponse("NOT_FOUND"), SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)))

      val result = schemeDetailsController.getPspSchemeDetails()(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe errorResponse("NOT_FOUND")
      }
    }

    "throw Upstream5xxResponse when UpStream5XXResponse with INTERNAL_SERVER_ERROR returned from If" in {
      reset(mockAuthConnector)
      AuthUtils.authStubPsp(mockAuthConnector)
      when(mockSchemeConnector.getPspSchemeDetails(ArgumentMatchers.eq(pspId), ArgumentMatchers.eq(srn))(any(), any(), any())).thenReturn(
        Future.failed(UpstreamErrorResponse(errorResponse("NOT_FOUND"), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      val result = schemeDetailsController.getPspSchemeDetails()(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe errorResponse("NOT_FOUND")
      }
    }

    "throw generic exception when any other exception returned from If" in {
      reset(mockAuthConnector)
      AuthUtils.authStubPsp(mockAuthConnector)
      when(mockSchemeConnector.getPspSchemeDetails(ArgumentMatchers.eq(pspId), ArgumentMatchers.eq(srn))(any(), any(), any())).thenReturn(
        Future.failed(new Exception("Generic Exception")))

      val result = schemeDetailsController.getPspSchemeDetails()(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe "Generic Exception"
      }
    }
  }

  "getPspSchemeDetailsSrn" must {

    def fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest("GET", "/").withHeaders(("srn", srn))

    "return OK when the scheme is registered successfully" in {
      reset(mockAuthConnector)
      AuthUtils.authStubPsp(mockAuthConnector)
      val successResponse = userAnswersResponse
      when(mockSchemeConnector.getPspSchemeDetails(ArgumentMatchers.eq(pspId), ArgumentMatchers.eq(srn))(any(), any(), any())).thenReturn(
        Future.successful(Right(successResponse.as[JsObject])))

      val result = schemeDetailsController.getPspSchemeDetailsSrn(srn)(fakeRequest)

      status(result).mustBe(OK)
      contentAsJson(result) mustBe successResponse
    }

    "throw BadRequestException when SchemeIdNumber is not present in the header" in {
      reset(mockAuthConnector)
      AuthUtils.authStubPsp(mockAuthConnector)
      val result = schemeDetailsController.getPspSchemeDetailsSrn(srn)(FakeRequest("GET", "/").withHeaders(("schemeIdType", schemeIdType)))
      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage.mustBe("Bad Request with missing parameters idType, idNumber or PSAId")
        verify(mockSchemeConnector, never).getPspSchemeDetails(ArgumentMatchers.any(),
          ArgumentMatchers.any())(any(), any(), any())
      }
    }

    "throw BadRequestException when SchemeIdType is not present in the header" in {
      reset(mockAuthConnector)
      AuthUtils.authStubPsp(mockAuthConnector)
      val result = schemeDetailsController.getPspSchemeDetailsSrn(srn)(FakeRequest("GET", "/").withHeaders(("idNumber", srn)))
      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage.mustBe("Bad Request with missing parameters idType, idNumber or PSAId")
        verify(mockSchemeConnector, never).getPspSchemeDetails(ArgumentMatchers.any(),
          ArgumentMatchers.any())(any(), any(), any())
      }
    }

    "throw BadRequestException when bad request with INVALID_IDTYPE returned from If" in {
      reset(mockAuthConnector)
      AuthUtils.authStubPsp(mockAuthConnector)
      when(mockSchemeConnector.getPspSchemeDetails(ArgumentMatchers.eq(pspId), ArgumentMatchers.eq(srn))(any(), any(), any())).thenReturn(
        Future.failed(new BadRequestException(errorResponse("INVALID_IDTYPE"))))

      val result = schemeDetailsController.getPspSchemeDetailsSrn(srn)(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_IDTYPE")
      }
    }

    "throw BadRequestException when bad request with INVALID_SRN returned from If" in {
      reset(mockAuthConnector)
      AuthUtils.authStubPsp(mockAuthConnector)
      when(mockSchemeConnector.getPspSchemeDetails(ArgumentMatchers.eq(pspId), ArgumentMatchers.eq(srn))(any(), any(), any())).thenReturn(
        Future.failed(new BadRequestException(errorResponse("INVALID_SRN"))))

      val result = schemeDetailsController.getPspSchemeDetailsSrn(srn)(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_SRN")
      }
    }

    "throw BadRequestException when bad request with INVALID_PSTR returned from If" in {
      reset(mockAuthConnector)
      AuthUtils.authStubPsp(mockAuthConnector)
      when(mockSchemeConnector.getPspSchemeDetails(ArgumentMatchers.eq(pspId), ArgumentMatchers.eq(srn))(any(), any(), any())).thenReturn(
        Future.failed(new BadRequestException(errorResponse("INVALID_PSTR"))))

      val result = schemeDetailsController.getPspSchemeDetailsSrn(srn)(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_PSTR")
      }
    }

    "throw BadRequestException when bad request with INVALID_CORRELATIONID returned from If" in {
      reset(mockAuthConnector)
      AuthUtils.authStubPsp(mockAuthConnector)
      when(mockSchemeConnector.getPspSchemeDetails(ArgumentMatchers.eq(pspId), ArgumentMatchers.eq(srn))(any(), any(), any())).thenReturn(
        Future.failed(new BadRequestException(errorResponse("INVALID_CORRELATIONID"))))

      val result = schemeDetailsController.getPspSchemeDetailsSrn(srn)(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_CORRELATIONID")
      }
    }

    "throw Upstream4xxResponse when UpStream4XXResponse returned from If" in {
      reset(mockAuthConnector)
      AuthUtils.authStubPsp(mockAuthConnector)
      when(mockSchemeConnector.getPspSchemeDetails(ArgumentMatchers.eq(pspId), ArgumentMatchers.eq(srn))(any(), any(), any())).thenReturn(
        Future.failed(UpstreamErrorResponse(errorResponse("NOT_FOUND"), NOT_FOUND, NOT_FOUND)))

      val result = schemeDetailsController.getPspSchemeDetailsSrn(srn)(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe errorResponse("NOT_FOUND")
      }
    }

    "throw Upstream5xxResponse when UpStream5XXResponse with SERVICE_UNAVAILABLE returned from If" in {
      reset(mockAuthConnector)
      AuthUtils.authStubPsp(mockAuthConnector)
      when(mockSchemeConnector.getPspSchemeDetails(ArgumentMatchers.eq(pspId), ArgumentMatchers.eq(srn))(any(), any(), any())).thenReturn(
        Future.failed(UpstreamErrorResponse(errorResponse("NOT_FOUND"), SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)))

      val result = schemeDetailsController.getPspSchemeDetailsSrn(srn)(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe errorResponse("NOT_FOUND")
      }
    }

    "throw Upstream5xxResponse when UpStream5XXResponse with INTERNAL_SERVER_ERROR returned from If" in {
      reset(mockAuthConnector)
      AuthUtils.authStubPsp(mockAuthConnector)
      when(mockSchemeConnector.getPspSchemeDetails(ArgumentMatchers.eq(pspId), ArgumentMatchers.eq(srn))(any(), any(), any())).thenReturn(
        Future.failed(UpstreamErrorResponse(errorResponse("NOT_FOUND"), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      val result = schemeDetailsController.getPspSchemeDetailsSrn(srn)(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e.getMessage mustBe errorResponse("NOT_FOUND")
      }
    }

    "throw generic exception when any other exception returned from If" in {
      reset(mockAuthConnector)
      AuthUtils.authStubPsp(mockAuthConnector)
      when(mockSchemeConnector.getPspSchemeDetails(ArgumentMatchers.eq(pspId), ArgumentMatchers.eq(srn))(any(), any(), any())).thenReturn(
        Future.failed(new Exception("Generic Exception")))

      val result = schemeDetailsController.getPspSchemeDetailsSrn(srn)(fakeRequest)
      whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe "Generic Exception"
      }
    }
  }
}
