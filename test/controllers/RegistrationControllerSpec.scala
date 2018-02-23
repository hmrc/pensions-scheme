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
import connector.RegistrationConnector
import models._
import org.mockito.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.BeforeAndAfter
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, Retrievals}
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

class RegistrationControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfter {

  val fakeRequest = FakeRequest("POST", "/")

  val mockRegistrationConnector = mock[RegistrationConnector]

  def registrationController(authNino: Option[String] = Some("AB100100A")): RegistrationController = new RegistrationController(
    new FakeAuthConnector(authNino), mockRegistrationConnector)

  before(reset(mockRegistrationConnector))

  "register With Id Individual" must {
    val inputRequestData = Json.obj("regime" -> "PODS", "requiresNameMatch" -> false, "isAnAgent" -> false)
    "return OK when the registration with id is successful for Individual" in {
      val successResponse = Json.toJson(readJsonFromFile("/data/validRegisterWithIdIndividualResponse.json").as[SuccessResponse])
      when(mockRegistrationConnector.registerWithIdIndividual(Matchers.eq("AB100100A"), Matchers.eq(inputRequestData))(
        any(), any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

      val result = registrationController().registerWithIdIndividual(fakeRequest.withJsonBody(inputRequestData))
      ScalaFutures.whenReady(result) { res =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual successResponse
      }
    }

    "return Upstream4XXResponse when auth record does not contain Nino" in {
      val result = registrationController(None).registerWithIdIndividual(fakeRequest.withJsonBody(inputRequestData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Upstream4xxResponse]
        e.getMessage mustBe "Nino not found in auth record"
        verify(mockRegistrationConnector, never()).registerWithIdIndividual(any(), any())(any(), any())
      }
    }

    "throw Upstream4xxResponse when DES/ETMP returns Upstream4xxResponse" in {
      val failureResponse = Json.obj("code" -> "INVALID_PAYLOAD",
        "reason" -> "Submission has not passed validation. Invalid PAYLOAD")
      when(mockRegistrationConnector.registerWithIdIndividual(Matchers.eq("AB100100A"),
        Matchers.eq(inputRequestData))(any(), any())).
        thenReturn(Future.failed(new Upstream4xxResponse(failureResponse.toString(), CONFLICT, CONFLICT)))

      val result = registrationController().registerWithIdIndividual(fakeRequest.withJsonBody(inputRequestData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Upstream4xxResponse]
        e.getMessage mustBe failureResponse.toString()
        verify(mockRegistrationConnector, times(1)).registerWithIdIndividual(Matchers.eq("AB100100A"),
          Matchers.eq(inputRequestData))(any(), any())
      }
    }

    "throw Upstream5xxResponse when DES/ETMP returns Upstream5xxResponse" in {
      val failureResponse = Json.obj("code" -> "SERVER_ERROR",
        "reason" -> "DES is currently experiencing problems that require live service intervention.")
      when(mockRegistrationConnector.registerWithIdIndividual(Matchers.eq("AB100100A"),
        Matchers.eq(inputRequestData))(any(), any())).thenReturn(
        Future.failed(new Upstream5xxResponse(failureResponse.toString(), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      val result = registrationController().registerWithIdIndividual(fakeRequest.withJsonBody(inputRequestData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Upstream5xxResponse]
        e.getMessage mustBe failureResponse.toString()
        verify(mockRegistrationConnector, times(1)).registerWithIdIndividual(Matchers.eq("AB100100A"),
          Matchers.eq(inputRequestData))(any(), any())
      }
    }

    "throw generic exception when any other exception returned from Des" in {
      when(mockRegistrationConnector.registerWithIdIndividual(Matchers.eq("AB100100A"),
        Matchers.eq(inputRequestData))(Matchers.any(), Matchers.any())).thenReturn(Future.failed(new Exception("Generic Exception")))

      val result = registrationController().registerWithIdIndividual(fakeRequest.withJsonBody(inputRequestData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe "Generic Exception"
        verify(mockRegistrationConnector, times(1)).registerWithIdIndividual(
          Matchers.eq("AB100100A"), Matchers.eq(inputRequestData))(any(), any())
      }
    }
  }

  "register With Id Organisation" must {
    val inputData = Json.obj("utr" -> "1100000000", "organisationName" -> "Test Ltd", "organisationType" -> "LLP")
    val finalRequestData = Json.obj(
      "regime" -> "PODS", "requiresNameMatch" -> true, "isAnAgent" -> false,
      "organisation" -> Json.toJson(Json.obj(
        "organisationName" -> "Test Ltd", "organisationType" -> "LLP").as[Organisation]
      ))
    "return OK when the registration with id is successful for Organisation" in {
      val successResponse = Json.toJson(readJsonFromFile("/data/validRegisterWithIdOrganisationResponse.json").as[SuccessResponse])
      when(mockRegistrationConnector.registerWithIdOrganisation(Matchers.eq("1100000000"), any())(
        any(), any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

      val result = registrationController().registerWithIdOrganisation(fakeRequest.withJsonBody(inputData))
      ScalaFutures.whenReady(result) { res =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual successResponse
      }
    }

    "throw Upstream4xxResponse when DES/ETMP returns Upstream4xxResponse" in {
      val failureResponse = Json.obj("code" -> "INVALID_PAYLOAD",
        "reason" -> "Submission has not passed validation. Invalid PAYLOAD")
      when(mockRegistrationConnector.registerWithIdOrganisation(Matchers.eq("1100000000"),
        any())(any(), any())).
        thenReturn(Future.failed(new Upstream4xxResponse(failureResponse.toString(), CONFLICT, CONFLICT)))

      val result = registrationController().registerWithIdOrganisation(fakeRequest.withJsonBody(inputData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Upstream4xxResponse]
        e.getMessage mustBe failureResponse.toString()
        verify(mockRegistrationConnector, times(1)).registerWithIdOrganisation(Matchers.eq("1100000000"),
          any())(any(), any())
      }
    }

    "throw Upstream5xxResponse when DES/ETMP returns Upstream5xxResponse" in {
      val failureResponse = Json.obj("code" -> "SERVER_ERROR",
        "reason" -> "DES is currently experiencing problems that require live service intervention.")
      when(mockRegistrationConnector.registerWithIdOrganisation(Matchers.eq("1100000000"),
        any())(any(), any())).thenReturn(
        Future.failed(new Upstream5xxResponse(failureResponse.toString(), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      val result = registrationController().registerWithIdOrganisation(fakeRequest.withJsonBody(inputData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Upstream5xxResponse]
        e.getMessage mustBe failureResponse.toString()
        verify(mockRegistrationConnector, times(1)).registerWithIdOrganisation(Matchers.eq("1100000000"),
          any())(any(), any())
      }
    }

    "throw generic exception when any other exception returned from Des" in {
      when(mockRegistrationConnector.registerWithIdOrganisation(Matchers.eq("1100000000"),
        any())(Matchers.any(), Matchers.any())).thenReturn(Future.failed(new Exception("Generic Exception")))

      val result = registrationController().registerWithIdOrganisation(fakeRequest.withJsonBody(inputData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe "Generic Exception"
        verify(mockRegistrationConnector, times(1)).registerWithIdOrganisation(
          Matchers.eq("1100000000"), any())(any(), any())
      }
    }
  }
}

class FakeAuthConnector(authNino: Option[String]) extends AuthConnector {
  def success: Option[String] = authNino

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(
    implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {
    Future.successful(success.asInstanceOf[A])
  }
}
