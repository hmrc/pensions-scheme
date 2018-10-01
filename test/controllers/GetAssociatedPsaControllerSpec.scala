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

class GetAssociatedPsaControllerSpec extends AsyncWordSpec with MockitoSugar
  with BeforeAndAfter with MustMatchers with JsonFileReader with RecoverMethods with Samples {

  val mockSchemeConnector: SchemeConnector = mock[SchemeConnector]
  val associatedPsaController = new GetAssociatedPsaController(mockSchemeConnector)
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

    "return OK when the scheme is registered successfully" in {

      val successResponse = Json.toJson(psaDetailsSample)
      when(mockSchemeConnector.getSchemeDetails(Matchers.eq("srn"), Matchers.eq(schemeReferenceNumber))(any(), any(), any())).thenReturn(
        Future.successful(Right(psaSchemeDetailsSample)))

      associatedPsaController.getSchemeDetails()(fakeRequest) map { result =>
        result.header.status mustBe OK
        result.body mustBe successResponse
      }
    }
  }

  "throw BadRequestException when the Scheme Reference Number is not present in the header" in {

//    e.getMessage mustBe "Bad Request with missing parameters PSA Id or SRN"
//    verify(mockSchemeConnector, never()).getSchemeDetails(Matchers.any(),
//      Matchers.any())(any(), any(), any())

    recoverToSucceededIf[BadRequestException] {
      associatedPsaController.getSchemeDetails()(FakeRequest("GET", "/").withHeaders(("psaIdNumber", psaIdNumber)))
    }

  }
}