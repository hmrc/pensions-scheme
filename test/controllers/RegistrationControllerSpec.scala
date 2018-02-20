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
import models.{FailureResponse, FailureResponseElement, RegisterWithId, SuccessResponse}
import org.mockito.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.BeforeAndAfter
import play.api.libs.json.Json
import uk.gov.hmrc.http.{BadRequestException, HttpResponse, Upstream4xxResponse, Upstream5xxResponse}

import scala.concurrent.Future

class RegistrationControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfter {

  val fakeRequestIndividual = FakeRequest("POST", "/").withHeaders(("idType", "nino"), ("idNumber", "AB100100A"))
  val fakeRequestOrg = FakeRequest("POST", "/").withHeaders(("idType", "utr"), ("idNumber", "1000000000"))

  val mockSchemeConnector = mock[SchemeConnector]
  val registrationController = new RegistrationController(mockSchemeConnector)

  before(reset(mockSchemeConnector))

  "register With Id" must {

    "return OK when the registration with id is successful for Individual" in {
      val validIndividualRequest = Json.toJson(readJsonFromFile("/data/validRegisterWithIdIndividualRequest.json").as[RegisterWithId])
      val successResponse = Json.toJson(readJsonFromFile("/data/validRegisterWithIdIndividualResponse.json").as[SuccessResponse])
      when(mockSchemeConnector.registerWithId(Matchers.eq("nino"), Matchers.eq("AB100100A"), Matchers.eq(validIndividualRequest))(
        any(), any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

      val result = registrationController.registerWithId(fakeRequestIndividual.withJsonBody(validIndividualRequest))
      ScalaFutures.whenReady(result) { res =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual successResponse
      }
    }

    "return OK when the registration with id is successful for Company" in {
      val validOrganisationRequest = Json.toJson(readJsonFromFile("/data/validRegisterWithIdOrganisationRequest.json").as[RegisterWithId])
      val successResponse = Json.toJson(readJsonFromFile("/data/validRegisterWithIdOrganisationResponse.json").as[SuccessResponse])
      when(mockSchemeConnector.registerWithId(Matchers.eq("utr"), Matchers.eq("1000000000"), Matchers.eq(validOrganisationRequest))(any(), any())).thenReturn(
        Future.successful(HttpResponse(OK, Some(successResponse))))

      val result = registrationController.registerWithId(fakeRequestOrg.withJsonBody(validOrganisationRequest))
      ScalaFutures.whenReady(result) { res =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual successResponse
      }
    }

    "return Bad Request when idType and IdNumber is not present in request header" in {
      val validOrganisationRequest = Json.toJson(readJsonFromFile("/data/validRegisterWithIdOrganisationRequest.json").as[RegisterWithId])
      val result = registrationController.registerWithId(FakeRequest("POST", "/").withJsonBody(validOrganisationRequest))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustEqual "Bad Request without proper Id or request body"
      }
    }

    "return Bad Request when no request body is present" in {
      val result = registrationController.registerWithId(fakeRequestOrg)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustEqual "Bad Request without proper Id or request body"
      }
    }

    "throw Upstream4xxResponse when DES/ETMP returns Upstream4xxResponse" in {
      val validOrganisationRequest = Json.toJson(readJsonFromFile("/data/validRegisterWithIdOrganisationRequest.json").as[RegisterWithId])
      val failureResponse = Json.toJson(FailureResponse(Some(FailureResponseElement(code = "INVALID_PAYLOAD",
        reason = "Submission has not passed validation. Invalid PAYLOAD"))))
      when(mockSchemeConnector.registerWithId(Matchers.eq("utr"), Matchers.eq("1000000000"), Matchers.eq(validOrganisationRequest))(any(), any())).
        thenReturn(Future.failed(new Upstream4xxResponse(failureResponse.toString(), CONFLICT, CONFLICT)))

      val result = registrationController.registerWithId(fakeRequestOrg.withJsonBody(validOrganisationRequest))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Upstream4xxResponse]
        e.getMessage mustBe failureResponse.toString()
        verify(mockSchemeConnector, times(1)).registerWithId(Matchers.eq("utr"), Matchers.eq("1000000000"), Matchers.eq(validOrganisationRequest))(any(), any())
      }
    }

    "throw Upstream5xxResponse when DES/ETMP returns Upstream5xxResponse" in {
      val validOrganisationRequest = Json.toJson(readJsonFromFile("/data/validRegisterWithIdOrganisationRequest.json").as[RegisterWithId])
      val failureResponse = Json.toJson(FailureResponse(Some(FailureResponseElement(code = "SERVER_ERROR",
        reason = "DES is currently experiencing problems that require live service intervention."))))
      when(mockSchemeConnector.registerWithId(Matchers.eq("utr"), Matchers.eq("1000000000"), Matchers.eq(validOrganisationRequest))(any(), any())).thenReturn(
        Future.failed(new Upstream5xxResponse(failureResponse.toString(), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      val result = registrationController.registerWithId(fakeRequestOrg.withJsonBody(validOrganisationRequest))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Upstream5xxResponse]
        e.getMessage mustBe failureResponse.toString()
        verify(mockSchemeConnector, times(1)).registerWithId(Matchers.eq("utr"), Matchers.eq("1000000000"), Matchers.eq(validOrganisationRequest))(any(), any())
      }
    }

    "throw generic exception when any other exception returned from Des" in {
      val validOrganisationRequest = Json.toJson(readJsonFromFile("/data/validRegisterWithIdOrganisationRequest.json").as[RegisterWithId])
      when(mockSchemeConnector.registerWithId(Matchers.eq("utr"), Matchers.eq("1000000000"),
        Matchers.eq(validOrganisationRequest))(Matchers.any(), Matchers.any())).thenReturn(Future.failed(new Exception("Generic Exception")))

      val result = registrationController.registerWithId(fakeRequestOrg.withJsonBody(validOrganisationRequest))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe "Generic Exception"
        verify(mockSchemeConnector, times(1)).registerWithId(Matchers.eq("utr"), Matchers.eq("1000000000"), Matchers.eq(validOrganisationRequest))(any(), any())
      }
    }
  }
}
