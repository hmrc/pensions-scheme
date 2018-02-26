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
import connector.EtmpConnector
import org.joda.time.LocalDate
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeRequest
import play.api.test.Helpers.{OK, status}
import uk.gov.hmrc.http.{BadRequestException, HttpResponse}
import play.api.test.Helpers._

import scala.concurrent.Future

class RegistrationControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfter with PatienceConfiguration {
  val mockEtmpConnector: EtmpConnector = mock[EtmpConnector]
  val registrationController = new RegistrationController(mockEtmpConnector)
  before(reset(mockEtmpConnector))

  "registrationNoIdOrganisation" must {

      "return a success response when valid data is posted" in {

        def fakeRequest(data: JsValue): FakeRequest[AnyContentAsJson] = FakeRequest("POST", "/").withJsonBody(data)

        val validData = readJsonFromFile("/data/validRegistrationNoIDOrganisation.json")
        val successResponse: JsObject = Json.obj("processingDate" -> LocalDate.now,
          "sapNumber" -> "1234567890",
          "safeId" -> "XE0001234567890"
        )
        when(mockEtmpConnector.registrationNoIdOrganisation(Matchers.eq(validData))(Matchers.any(), Matchers.any())).thenReturn(
          Future.successful(HttpResponse(OK, Some(successResponse))))
        val result = registrationController.registrationNoIdOrganisation(fakeRequest(validData))
        ScalaFutures.whenReady(result) { res =>
          status(result) mustBe OK
          verify(mockEtmpConnector, times(1)).registrationNoIdOrganisation(Matchers.eq(validData))(Matchers.any(), Matchers.any())
        }
      }

      "throw BadRequestException when no data is not present in the request" in {
        val result = registrationController.registrationNoIdOrganisation()(FakeRequest("POST", "/"))
        ScalaFutures.whenReady(result.failed) { e =>
          e mustBe a[BadRequestException]
          e.getMessage mustBe "Bad Request without request body"
          verify(mockEtmpConnector, never()).registerScheme(Matchers.any(),
            Matchers.any())(Matchers.any(), Matchers.any())
        }
      }
    }
}



