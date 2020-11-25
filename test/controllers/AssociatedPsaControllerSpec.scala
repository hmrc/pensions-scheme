/*
 * Copyright 2020 HM Revenue & Customs
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

import base.{JsonFileReader, SpecBase}
import connector.SchemeConnector
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.{never, reset, verify, when}
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.BadRequestException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AssociatedPsaControllerSpec
  extends SpecBase
    with MockitoSugar
    with BeforeAndAfter
    with PatienceConfiguration
    with JsonFileReader {

  private val mockSchemeConnector: SchemeConnector = mock[SchemeConnector]
  private val associatedPsaController = new AssociatedPsaController(mockSchemeConnector, stubControllerComponents())
  private val schemeIdNumber = "S999999999"
  private val schemeIdType = "srn"
  private val userIdNumber = "A0000001"
  private val userIdType = "PSAID"
  private val userAnswersResponse: JsValue = readJsonFromFile("/data/validGetSchemeDetailsUserAnswers.json")

  before {
    reset(mockSchemeConnector)
  }

  "getAssociatedPsa" must {

    "return OK" when {

      "the psa we retrieve exists in the list of PSAs we receive from getSchemeDetails" in {
        val associatedPsaController = new AssociatedPsaController(mockSchemeConnector, stubControllerComponents())

        val request = FakeRequest("GET", "/").withHeaders(
          ("userIdNumber", userIdNumber),
          ("schemeIdType", schemeIdType),
          ("schemeIdNumber", schemeIdNumber)
        )

        when(mockSchemeConnector.getSchemeDetails(
          userIdNumber = Matchers.eq(userIdNumber),
          schemeIdNumber = Matchers.eq(schemeIdNumber),
          schemeIdType = Matchers.eq(schemeIdType)
        )(any(), any(), any())).thenReturn(
          Future.successful(Right(userAnswersResponse))
        )

        val result = associatedPsaController.isPsaAssociated()(request)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(true)
      }

      "the psa we retrieve dont exists in the list of PSAs we receive from getSchemeDetails as its empty" in {
        val associatedPsaController = new AssociatedPsaController(mockSchemeConnector, stubControllerComponents())

        val request = FakeRequest("GET", "/").withHeaders(
          ("userIdNumber", userIdNumber),
          ("schemeIdType", schemeIdType),
          ("schemeIdNumber", schemeIdNumber)
        )

        val emptyPsa = (userAnswersResponse.as[JsObject] - "psaDetails")

        when(mockSchemeConnector.getSchemeDetails(
          userIdNumber = Matchers.eq(userIdNumber),
          schemeIdNumber = Matchers.eq(schemeIdNumber),
          schemeIdType = Matchers.eq(schemeIdType)
        )(any(), any(), any())).thenReturn(
          Future.successful(Right(emptyPsa))
        )

        val result = associatedPsaController.isPsaAssociated()(request)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(false)
      }
    }
  }

  "throw BadRequestException" when {
    "the Scheme Reference Number is not present in the header" in {
      val result = associatedPsaController.isPsaAssociated()(FakeRequest("GET", "/").withHeaders(
        ("psaIdNumber", userIdNumber)
      ))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Bad Request with missing parameters PSA Id or SRN"
        verify(mockSchemeConnector, never()).getSchemeDetails(
          userIdNumber = Matchers.any(),
          schemeIdNumber = Matchers.any(),
          schemeIdType = Matchers.any()
        )(any(), any(), any())
      }
    }


    "the PsaId is not present in the header" in {
      val result = associatedPsaController.isPsaAssociated()(FakeRequest("GET", "/").withHeaders(
        (schemeIdType, schemeIdNumber)
      ))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Bad Request with missing parameters PSA Id or SRN"
        verify(mockSchemeConnector, never()).getSchemeDetails(
          userIdNumber = Matchers.any(),
          schemeIdNumber = Matchers.any(),
          schemeIdType = Matchers.any()
        )(any(), any(), any())
      }
    }

    "there is no PsaId or SRN" in {
      val result = associatedPsaController.isPsaAssociated()(FakeRequest("GET", "/"))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Bad Request with missing parameters PSA Id or SRN"
        verify(mockSchemeConnector, never()).getSchemeDetails(
          userIdNumber = Matchers.any(),
          schemeIdNumber = Matchers.any(),
          schemeIdType = Matchers.any()
        )(any(), any(), any())
      }
    }

    "we receive INVALID_IDTYPE returned from Des" in {
      when(mockSchemeConnector.getSchemeDetails(
        userIdNumber = Matchers.eq(userIdNumber),
        schemeIdNumber = Matchers.eq(schemeIdNumber),
        schemeIdType = Matchers.eq(schemeIdType)
      )(any(), any(), any())).thenReturn(
        Future.failed(new BadRequestException(errorResponse("INVALID_IDTYPE")))
      )

      val result = associatedPsaController.isPsaAssociated()(FakeRequest("GET", "/").withHeaders(
        ("userIdNumber", userIdNumber),
        ("schemeIdType", schemeIdType),
        ("schemeIdNumber", schemeIdNumber)
      ))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_IDTYPE")
      }
    }
  }
}