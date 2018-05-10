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

import akka.stream.Materializer
import base.SpecBase
import connector.RegistrationConnector
import models._
import org.joda.time.LocalDate
import org.mockito.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.BeforeAndAfter
import play.api.libs.json.{JsNull, JsObject, JsValue, Json}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, Retrievals}
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

class RegistrationControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfter {

  val dataFromFrontend = readJsonFromFile("/data/validRegistrationNoIDOrganisationFE.json")
  val dataToEmtp = readJsonFromFile("/data/validRegistrationNoIDOrganisationToEMTP.json").as[OrganisationRegistrant]

  val fakeRequest = FakeRequest("POST", "/")

  val mockRegistrationConnector = mock[RegistrationConnector]

  def registrationController(authNino: Option[String] = Some("AB100100A")): RegistrationController = new RegistrationController(
    new FakeAuthConnector(authNino), mockRegistrationConnector)

  before(reset(mockRegistrationConnector))
  implicit val mat: Materializer = app.materializer

  "register With Id Individual" must {
    val inputRequestData = Json.obj("regime" -> "PODS", "requiresNameMatch" -> false, "isAnAgent" -> false)
    "return OK when the registration with id is successful for Individual" in {
      val output = readJsonFromFile("/data/validRegisterWithIdIndividualResponse.json")
      val successResponse = Json.toJson(readJsonFromFile("/data/validRegisterWithIdIndividualResponse.json").as[SuccessResponse])
      when(mockRegistrationConnector.registerWithIdIndividual(Matchers.eq("AB100100A"), Matchers.eq(inputRequestData))(
        any(), any())).thenReturn(Future.successful(HttpResponse(OK, Some(output))))

      val result = registrationController().registerWithIdIndividual(fakeRequest.withJsonBody(inputRequestData))
      ScalaFutures.whenReady(result) { res =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual successResponse
      }
    }

    "throw BadRequestException when the bad data returned in the response from DES/ETMP" in {
      val output = Json.obj("bad" -> "data")
      when(mockRegistrationConnector.registerWithIdIndividual(Matchers.eq("AB100100A"), Matchers.eq(inputRequestData))(
        any(), any())).thenReturn(Future.successful(HttpResponse(OK, Some(output))))

      val result = registrationController().registerWithIdIndividual(fakeRequest.withJsonBody(inputRequestData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
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
    "return OK when the registration with id is successful for Organisation" in {
      val input = readJsonFromFile("/data/validRegisterWithIdOrganisationResponse.json")
      val successResponse = Json.toJson(readJsonFromFile("/data/validRegisterWithIdOrganisationResponse.json").as[SuccessResponse])
      when(mockRegistrationConnector.registerWithIdOrganisation(Matchers.eq("1100000000"), any())(
        any(), any())).thenReturn(Future.successful(HttpResponse(OK, Some(input))))

      val result = registrationController().registerWithIdOrganisation(fakeRequest.withJsonBody(inputData))
      ScalaFutures.whenReady(result) { res =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual successResponse
      }
    }

    "return Bad Request when the bad data returned from frontend in the request" in {
      val inputData = Json.obj("bad" -> "data")

      val result = registrationController().registerWithIdOrganisation(fakeRequest.withJsonBody(inputData))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
      }
    }

    "return Bad Request when there is no body in the request" in {
      val result = registrationController().registerWithIdOrganisation(fakeRequest)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustEqual "No request body received for Organisation"
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

  "registrationNoIdOrganisation" must {
    def fakeRequest(data: JsValue): FakeRequest[JsValue] = FakeRequest("POST", "/").withBody(data)

    "return a success response when valid data is posted" in {
      val successResponse: JsObject = Json.obj("processingDate" -> LocalDate.now,
        "sapNumber" -> "1234567890",
        "safeId" -> "XE0001234567890"
      )
      when(mockRegistrationConnector.registrationNoIdOrganisation(Matchers.eq(dataToEmtp))(Matchers.any(), Matchers.any())).thenReturn(
        Future.successful(HttpResponse(OK, Some(successResponse))))

      val result = call(registrationController().registrationNoIdOrganisation, fakeRequest(dataFromFrontend))
      ScalaFutures.whenReady(result) { res =>
        status(result) mustBe OK
        verify(mockRegistrationConnector, times(1)).registrationNoIdOrganisation(Matchers.eq(dataToEmtp))(Matchers.any(), Matchers.any())
      }
    }

    "throw BadRequestException when no data is present in the request" in {
      val result = call(registrationController().registrationNoIdOrganisation, FakeRequest("POST", "/").withBody(JsNull))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        verify(mockRegistrationConnector, never()).registrationNoIdOrganisation(Matchers.eq(dataToEmtp))(Matchers.any(), Matchers.any())
      }
    }

    "throw BadRequestException when bad data received in request from frontend" in {
      val dataFromFrontend = Json.obj("bad" -> "data")
      val result = call(registrationController().registrationNoIdOrganisation, fakeRequest(dataFromFrontend))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        verify(mockRegistrationConnector, never()).registrationNoIdOrganisation(Matchers.eq(dataToEmtp))(Matchers.any(), Matchers.any())
      }
    }

    "throw BadRequestException when bad request returned from Des" in {
      val invalidPayload: JsObject = Json.obj(
        "code" -> "INVALID_PAYLOAD",
        "reason" -> "Submission has not passed validation. Invalid PAYLOAD"
      )
      when(mockRegistrationConnector.registrationNoIdOrganisation(Matchers.eq(dataToEmtp))(Matchers.any(), Matchers.any())).thenReturn(
        Future.failed(new BadRequestException(invalidPayload.toString())))

      val result = call(registrationController().registrationNoIdOrganisation,fakeRequest(dataFromFrontend))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe invalidPayload.toString()
        verify(mockRegistrationConnector, times(1)).registrationNoIdOrganisation(Matchers.eq(dataToEmtp))(Matchers.any(), Matchers.any())
      }
    }

    "throw Upstream4xxResponse when UpStream4XXResponse returned from Des" in {
      val invalidSubmission: JsObject = Json.obj(
        "code" -> "INVALID_SUBMISSION",
        "reason" -> "Duplicate submission acknowledgement reference from remote endpoint returned."
      )
      when(mockRegistrationConnector.registrationNoIdOrganisation(Matchers.eq(dataToEmtp))(Matchers.any(), Matchers.any())).thenReturn(
        Future.failed(new Upstream4xxResponse(invalidSubmission.toString(), CONFLICT, CONFLICT)))

      val result = call(registrationController().registrationNoIdOrganisation,fakeRequest(dataFromFrontend))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Upstream4xxResponse]
        e.getMessage mustBe invalidSubmission.toString()
        verify(mockRegistrationConnector, times(1)).registrationNoIdOrganisation(Matchers.eq(dataToEmtp))(Matchers.any(), Matchers.any())
      }
    }

    "throw Upstream5xxResponse when UpStream5XXResponse returned from Des" in {
      val serviceUnavailable: JsObject = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "Dependent systems are currently not responding."
      )
      when(mockRegistrationConnector.registrationNoIdOrganisation(Matchers.eq(dataToEmtp))(Matchers.any(), Matchers.any())).thenReturn(
        Future.failed(new Upstream5xxResponse(serviceUnavailable.toString(), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      val result = call(registrationController().registrationNoIdOrganisation,fakeRequest(dataFromFrontend))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Upstream5xxResponse]
        e.getMessage mustBe serviceUnavailable.toString()
        verify(mockRegistrationConnector, times(1)).registrationNoIdOrganisation(Matchers.eq(dataToEmtp))(Matchers.any(), Matchers.any())
      }
    }

    "throw generic exception when any other exception returned from Des" in {
      when(mockRegistrationConnector.registrationNoIdOrganisation(Matchers.eq(dataToEmtp))(Matchers.any(), Matchers.any())).thenReturn(
        Future.failed(new Exception("Generic Exception")))

      val result = call(registrationController().registrationNoIdOrganisation,fakeRequest(dataFromFrontend))
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe "Generic Exception"
        verify(mockRegistrationConnector, times(1)).registrationNoIdOrganisation(Matchers.eq(dataToEmtp))(Matchers.any(), Matchers.any())
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
