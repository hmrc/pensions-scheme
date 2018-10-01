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

import base.JsonFileReader
import connector.SchemeConnector
import models.Samples
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{AsyncWordSpec, BeforeAndAfter, MustMatchers, RecoverMethods}
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.BadRequestException

import scala.concurrent.Future

class AssociatedPsaControllerSpec extends AsyncWordSpec with MockitoSugar
  with BeforeAndAfter with MustMatchers with JsonFileReader with RecoverMethods with Samples {

  val mockSchemeConnector: SchemeConnector = mock[SchemeConnector]
  val associatedPsaController = new AssociatedPsaController(mockSchemeConnector)
  private val schemeReferenceNumber = "S999999999"
  private val psaIdNumber = "A1234567"

  def errorResponse(code: String): String = {
    Json.stringify(
      Json.obj(
        "code" -> code,
        "reason" -> s"Reason for $code"
      )
    )
  }

  before {
    reset(mockSchemeConnector)
  }

  "getAssociatedPsa" must {

    def fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest("GET", "/").withHeaders(("psaId", psaIdNumber), ("schemeReferenceNumber", schemeReferenceNumber))

    "return OK when we retrieve whether if the psa is associated or not" in {
      when(mockSchemeConnector.getSchemeDetails(Matchers.eq("srn"), Matchers.eq(schemeReferenceNumber))(any(), any(), any())).thenReturn(
        Future.successful(Right(psaSchemeDetailsSample)))

      val result = associatedPsaController.isPsaAssociated()(fakeRequest)

      status(result) mustBe OK
    }

    "return false when the psa we retrieve does not exist within the list of PSAs we receive from getSchemeDetails" in {
      when(mockSchemeConnector.getSchemeDetails(Matchers.eq("srn"), Matchers.eq(schemeReferenceNumber))(any(), any(), any())).thenReturn(
        Future.successful(Right(psaSchemeDetailsSample)))

      val result = associatedPsaController.isPsaAssociated()(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(false)
    }

    "return false if we have no psa id available in the headers" in {
      val request = FakeRequest("GET", "/").withHeaders(("schemeReferenceNumber", schemeReferenceNumber))

      when(mockSchemeConnector.getSchemeDetails(Matchers.eq("srn"), Matchers.eq(schemeReferenceNumber))(any(), any(), any())).thenReturn(
        Future.successful(Right(psaSchemeDetailsSample)))

      val result = associatedPsaController.isPsaAssociated()(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(false)
    }

    "return true when the psa we retrieve exists in the list of PSAs we receive from getSchemeDetails" in {
      val request = FakeRequest("GET", "/").withHeaders(("psaId", "A0000001"), ("schemeReferenceNumber", schemeReferenceNumber))

      when(mockSchemeConnector.getSchemeDetails(Matchers.eq("srn"), Matchers.eq(schemeReferenceNumber))(any(), any(), any())).thenReturn(
        Future.successful(Right(psaSchemeDetailsSample)))

      val result = associatedPsaController.isPsaAssociated()(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(true)
    }
  }

  "throw BadRequestException when the Scheme Reference Number is not present in the header" in {

//    e.getMessage mustBe "Bad Request with missing parameters PSA Id or SRN"
//    verify(mockSchemeConnector, never()).getSchemeDetails(Matchers.any(),
//      Matchers.any())(any(), any(), any())

    recoverToSucceededIf[BadRequestException] {
      associatedPsaController.isPsaAssociated()(FakeRequest("GET", "/").withHeaders(("psaIdNumber", psaIdNumber)))
    }

  }
}