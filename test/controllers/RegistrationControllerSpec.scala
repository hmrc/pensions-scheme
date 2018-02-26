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
import uk.gov.hmrc.http.{BadRequestException, HttpResponse, Upstream4xxResponse, Upstream5xxResponse}
import play.api.test.Helpers._

import scala.concurrent.Future

class RegistrationControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfter with PatienceConfiguration {

  val dataFromFrontend = readJsonFromFile("/data/validRegistrationNoIDOrganisationFE.json")
  val dataToEmtp = readJsonFromFile("/data/validRegistrationNoIDOrganisationToEMTP.json")
  val mockEtmpConnector: EtmpConnector = mock[EtmpConnector]
  val registrationController = new RegistrationController(mockEtmpConnector)
  before(reset(mockEtmpConnector))

  "registrationNoIdOrganisation" must {
    def fakeRequest(data: JsValue): FakeRequest[AnyContentAsJson] = FakeRequest("POST", "/").withJsonBody(data)

    "return a success response when valid data is posted" in {

      val successResponse: JsObject = Json.obj("processingDate" -> LocalDate.now,
        "sapNumber" -> "1234567890",
        "safeId" -> "XE0001234567890"
      )

      when(mockEtmpConnector.registrationNoIdOrganisation(Matchers.eq(dataToEmtp))(Matchers.any(), Matchers.any())).thenReturn(
        Future.successful(HttpResponse(OK, Some(successResponse))))
      val result = registrationController.registrationNoIdOrganisation(fakeRequest(dataFromFrontend))
      ScalaFutures.whenReady(result) { res =>
        status(result) mustBe OK
        verify(mockEtmpConnector, times(1)).registrationNoIdOrganisation(Matchers.eq(dataToEmtp))(Matchers.any(), Matchers.any())
      }
    }

    "throw BadRequestException when no data is not present in the request" in {
      val result = registrationController.registrationNoIdOrganisation()(FakeRequest("POST", "/"))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe "Bad Request without request body"
        verify(mockEtmpConnector, never()).registrationNoIdOrganisation(Matchers.any())(Matchers.any(), Matchers.any())
      }
    }

    "throw BadRequestException when bad request returned from Des" in {
      val invalidPayload: JsObject = Json.obj(
        "code" -> "INVALID_PAYLOAD",
        "reason" -> "Submission has not passed validation. Invalid PAYLOAD"
      )
      when(mockEtmpConnector.registrationNoIdOrganisation(Matchers.eq(dataToEmtp))(Matchers.any(), Matchers.any())).thenReturn(
        Future.failed(new BadRequestException(invalidPayload.toString())))

      val result = registrationController.registrationNoIdOrganisation()(fakeRequest(dataFromFrontend))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe invalidPayload.toString()
        verify(mockEtmpConnector, times(1)).registrationNoIdOrganisation(Matchers.eq(dataToEmtp))(Matchers.any(), Matchers.any())
      }
    }

    "throw Upstream4xxResponse when UpStream4XXResponse returned from Des" in {
      val invalidSubmission: JsObject = Json.obj(
        "code" -> "INVALID_SUBMISSION",
        "reason" -> "Duplicate submission acknowledgement reference from remote endpoint returned."
      )
      when(mockEtmpConnector.registrationNoIdOrganisation(Matchers.eq(dataToEmtp))(Matchers.any(), Matchers.any())).thenReturn(
        Future.failed(new Upstream4xxResponse(invalidSubmission.toString(), CONFLICT, CONFLICT)))

      val result = registrationController.registrationNoIdOrganisation()(fakeRequest(dataFromFrontend))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Upstream4xxResponse]
        e.getMessage mustBe invalidSubmission.toString()
        verify(mockEtmpConnector, times(1)).registrationNoIdOrganisation(Matchers.eq(dataToEmtp))(Matchers.any(), Matchers.any())
      }
    }

    "throw Upstream5xxResponse when UpStream5XXResponse returned from Des" in {
      val serviceUnavailable: JsObject = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "Dependent systems are currently not responding."
      )
      when(mockEtmpConnector.registrationNoIdOrganisation(Matchers.eq(dataToEmtp))(Matchers.any(), Matchers.any())).thenReturn(
        Future.failed(new Upstream5xxResponse(serviceUnavailable.toString(), CONFLICT, CONFLICT)))

      val result = registrationController.registrationNoIdOrganisation()(fakeRequest(dataFromFrontend))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Upstream5xxResponse]
        e.getMessage mustBe serviceUnavailable.toString()
        verify(mockEtmpConnector, times(1)).registrationNoIdOrganisation(Matchers.eq(dataToEmtp))(Matchers.any(), Matchers.any())
      }
    }

    "throw generic exception when any other exception returned from Des" in {
      when(mockEtmpConnector.registrationNoIdOrganisation(Matchers.eq(dataToEmtp))(Matchers.any(), Matchers.any())).thenReturn(
        Future.failed(new Exception("Generic Exception")))

      val result = registrationController.registrationNoIdOrganisation()(fakeRequest(dataFromFrontend))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe "Generic Exception"
        verify(mockEtmpConnector, times(1)).registrationNoIdOrganisation(Matchers.eq(dataToEmtp))(Matchers.any(), Matchers.any())
      }
    }


  }
}



