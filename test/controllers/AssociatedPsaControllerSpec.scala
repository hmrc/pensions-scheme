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
import models.Reads.schemes.SchemeDetailsStubData
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.{never, reset, verify, when}
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.BadRequestException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AssociatedPsaControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfter with PatienceConfiguration with SchemeDetailsStubData {

  val mockSchemeConnector: SchemeConnector = mock[SchemeConnector]
  val associatedPsaController = new AssociatedPsaController(mockSchemeConnector)
  private val schemeReferenceNumber = "S999999999"
  private val psaIdNumber = "A1234567"
  val srnRequest = "srn"

  before {
    reset(mockSchemeConnector)
  }

  "getAssociatedPsa" must {

    def fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest("GET", "/").withHeaders(("psaId", psaIdNumber), ("schemeReferenceNumber", schemeReferenceNumber))

    "return OK" when {
      "we retrieve whether if the psa is associated or not" in {
        when(mockSchemeConnector.getSchemeDetails(Matchers.eq(psaIdNumber),
          Matchers.eq(srnRequest), Matchers.eq(schemeReferenceNumber))(any(), any(), any())).thenReturn(
          Future.successful(Right(psaSchemeDetailsSample)))

        val result = associatedPsaController.isPsaAssociated()(fakeRequest)

        status(result) mustBe OK
      }

      "the psa we retrieve does not exist within the list of PSAs we receive from getSchemeDetails" in {
        when(mockSchemeConnector.getSchemeDetails(Matchers.eq(psaIdNumber),
          Matchers.eq("srn"), Matchers.eq(schemeReferenceNumber))(any(), any(), any())).thenReturn(
          Future.successful(Right(psaSchemeDetailsSample)))

        val result = associatedPsaController.isPsaAssociated()(fakeRequest)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(false)
      }

      "the psa we retrieve exists in the list of PSAs we receive from getSchemeDetails" in {
        val request = FakeRequest("GET", "/").withHeaders(("psaId", "A0000001"), ("schemeReferenceNumber", schemeReferenceNumber))

        when(mockSchemeConnector.getSchemeDetails(Matchers.any(),
          Matchers.eq(srnRequest), Matchers.eq(schemeReferenceNumber))(any(), any(), any())).thenReturn(
          Future.successful(Right(psaSchemeDetailsSample)))

        val result = associatedPsaController.isPsaAssociated()(request)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(true)
      }
    }
  }

  "throw BadRequestException" when {
    "the Scheme Reference Number is not present in the header" in {
      val result = associatedPsaController.isPsaAssociated()(FakeRequest("GET", "/").withHeaders(("psaIdNumber", psaIdNumber)))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Bad Request with missing parameters PSA Id or SRN"
        verify(mockSchemeConnector, never()).getSchemeDetails(Matchers.any(), Matchers.any(),
          Matchers.any())(any(), any(), any())
      }
    }


    "the PsaId is not present in the header" in {
      val result = associatedPsaController.isPsaAssociated()(FakeRequest("GET", "/").withHeaders((srnRequest, schemeReferenceNumber)))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Bad Request with missing parameters PSA Id or SRN"
        verify(mockSchemeConnector, never()).getSchemeDetails(Matchers.any(), Matchers.any(),
          Matchers.any())(any(), any(), any())
      }
    }

    "there is no PsaId or SRN" in {
      val result = associatedPsaController.isPsaAssociated()(FakeRequest("GET", "/"))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Bad Request with missing parameters PSA Id or SRN"
        verify(mockSchemeConnector, never()).getSchemeDetails(Matchers.any(), Matchers.any(),
          Matchers.any())(any(), any(), any())
      }
    }

    "we receive INVALID_IDTYPE returned from Des" in {
      when(mockSchemeConnector.getSchemeDetails(Matchers.eq(psaIdNumber),
        Matchers.eq(srnRequest), Matchers.eq(schemeReferenceNumber))(any(), any(), any())).thenReturn(
        Future.failed(new BadRequestException(errorResponse("INVALID_IDTYPE"))))

      val result = associatedPsaController.isPsaAssociated()(FakeRequest("GET", "/")
        .withHeaders(("psaId", psaIdNumber), ("schemeReferenceNumber", schemeReferenceNumber)))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe errorResponse("INVALID_IDTYPE")
      }
    }
  }
}