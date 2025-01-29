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

package controllers

import base.SpecBase
import connector.SchemeDetailsConnector
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, reset, verify, when}
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories._
import uk.gov.hmrc.http.BadRequestException

import scala.concurrent.Future

class AssociatedPsaControllerSpec
  extends SpecBase
    with MockitoSugar
    with BeforeAndAfter
    with PatienceConfiguration {

  private val mockSchemeConnector: SchemeDetailsConnector = mock[SchemeDetailsConnector]
  private val mockSessionDetailsCache = mock[SchemeDetailsWithIdCacheRepository]
  private val schemeIdNumber = "S999999999"
  private val userIdNumber = "A0000001"
  private val userAnswersResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsUserAnswers.json")

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[SchemeDetailsConnector].toInstance(mockSchemeConnector),
      bind[LockRepository].toInstance(mock[LockRepository]),
      bind[RacdacSchemeSubscriptionCacheRepository].toInstance(mock[RacdacSchemeSubscriptionCacheRepository]),
      bind[SchemeCacheRepository].toInstance(mock[SchemeCacheRepository]),
      bind[SchemeDetailsCacheRepository].toInstance(mock[SchemeDetailsCacheRepository]),
      bind[SchemeSubscriptionCacheRepository].toInstance(mock[SchemeSubscriptionCacheRepository]),
      bind[UpdateSchemeCacheRepository].toInstance(mock[UpdateSchemeCacheRepository]),
      bind[SchemeDetailsWithIdCacheRepository].toInstance(mockSessionDetailsCache)
    )

  private val associatedPsaController: AssociatedPsaController = injector.instanceOf[AssociatedPsaController]

  before {
    reset(mockSchemeConnector)
    reset(mockSessionDetailsCache)
    when(mockSessionDetailsCache.get(any())).thenReturn(Future.successful(None))
    when(mockSessionDetailsCache.upsert(any(), any())).thenReturn(Future.successful(true))
  }

  "getAssociatedPsa" must {
    "return OK" when {
      "the psa we retrieve exists in the list of PSAs we receive from getSchemeDetails and create cache" in {
        val request = FakeRequest("GET", "/").withHeaders(
          ("psaId", userIdNumber),
          ("schemeReferenceNumber", schemeIdNumber)
        )

        when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(Right(userAnswersResponse.as[JsObject])))

        val result = associatedPsaController.isPsaAssociated()(request)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(true)
      }

      "the psa we retrieve dont exists in the list of PSAs we receive from getSchemeDetails as its empty" in {
        val request = FakeRequest("GET", "/").withHeaders(
          ("psaId", userIdNumber),
          ("schemeReferenceNumber", schemeIdNumber)
        )

        val emptyPsa = userAnswersResponse.as[JsObject] - "psaDetails"

        when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(Right(emptyPsa)))

        val result = associatedPsaController.isPsaAssociated()(request)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(false)
        verify(mockSessionDetailsCache).upsert(any(), any())
      }
    }
  }

  "retrieve data from cache when cache is available" in {
    val request = FakeRequest("GET", "/").withHeaders(
      ("psaId", userIdNumber),
      ("schemeReferenceNumber", schemeIdNumber)
    )

    when(mockSessionDetailsCache.get(any())).thenReturn(Future.successful(Some(userAnswersResponse.as[JsObject])))

    val result = associatedPsaController.isPsaAssociated()(request)

    status(result) mustBe OK
    contentAsJson(result) mustBe Json.toJson(true)
    verify(mockSchemeConnector, never).getSchemeDetails(
      userIdNumber = ArgumentMatchers.any(),
      schemeIdType = ArgumentMatchers.any(),
      schemeIdNumber = ArgumentMatchers.any()
    )(any(), any(), any())
  }

  "return BadRequest" when {
    "the Scheme Reference Number is not present in the header" in {
      val result = associatedPsaController.isPsaAssociated()(FakeRequest("GET", "/").withHeaders(
        ("psaId", userIdNumber)
      ))
      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("schemeReferenceNumber")
      verify(mockSchemeConnector, never).getSchemeDetails(
        userIdNumber = ArgumentMatchers.any(),
        schemeIdType = ArgumentMatchers.any(),
        schemeIdNumber = ArgumentMatchers.any()
      )(any(), any(), any())
    }


    "both psaId and pspId not present in the header" in {
      def result: Future[Result] =
        associatedPsaController.isPsaAssociated()(FakeRequest("GET", "/")
          .withHeaders(("schemeReferenceNumber", schemeIdNumber)))

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("pspId")
      contentAsString(result) must include("psaId")

      verify(mockSchemeConnector, never).getSchemeDetails(
        userIdNumber = ArgumentMatchers.any(),
        schemeIdType = ArgumentMatchers.any(),
        schemeIdNumber = ArgumentMatchers.any()
      )(any(), any(), any())
    }

    "we receive INVALID_IDTYPE returned from If" in {
      when(mockSchemeConnector.getSchemeDetails(any(), any(), any())(any(), any(), any()))
        .thenReturn(
          Future.failed(new BadRequestException(errorResponse("INVALID_IDTYPE")))
        )

      val result = associatedPsaController.isPsaAssociated()(FakeRequest("GET", "/").withHeaders(
        ("psaId", userIdNumber),
        ("schemeReferenceNumber", schemeIdNumber)
      ))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_IDTYPE")
      }
    }
  }
}
