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
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.BeforeAndAfter
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.http._
import utils.FakeAuthConnector

import scala.concurrent.Future

class RegistrationControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfter {

  import RegistrationControllerSpec._

  private val dataFromFrontend = readJsonFromFile("/data/validRegistrationNoIDOrganisationFE.json")
  private val dataToEmtp = readJsonFromFile("/data/validRegistrationNoIDOrganisationToEMTP.json").as[OrganisationRegistrant]

  private val mockRegistrationConnector = mock[RegistrationConnector]

  implicit val mat: Materializer = app.materializer

  private def registrationController(retrievals: Future[_]): RegistrationController =
    new RegistrationController(
      new FakeAuthConnector(retrievals),
      mockRegistrationConnector
    )

  before(reset(mockRegistrationConnector))

  "register With Id Individual" must {

    val inputRequestData = Json.obj("regime" -> "PODA", "requiresNameMatch" -> false, "isAnAgent" -> false)

    "return OK when the registration with id is successful for Individual" in {

      val successResponse = Json.toJson(readJsonFromFile("/data/validRegisterWithIdIndividualResponse.json").as[SuccessResponse])

      when(mockRegistrationConnector.registerWithIdIndividual(Matchers.eq(nino), any(), Matchers.eq(inputRequestData))(any(), any(), any()))
        .thenReturn(Future.successful(Right(successResponse)))

      val result = registrationController(individualRetrievals).registerWithIdIndividual(fakeRequest.withJsonBody(inputRequestData))

      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual successResponse
      }
    }


    "throw Upstream5xxResponse when given Upstream5xxResponse from connector" in {

      val failureResponse = Json.obj(
        "code" -> "SERVER_ERROR",
        "reason" -> "DES is currently experiencing problems that require live service intervention."
      )

      when(mockRegistrationConnector.registerWithIdIndividual(Matchers.eq(nino), any(), Matchers.eq(inputRequestData))(any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse(failureResponse.toString(), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      val result = registrationController(individualRetrievals).registerWithIdIndividual(fakeRequest.withJsonBody(inputRequestData))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Upstream5xxResponse]
        e.getMessage mustBe failureResponse.toString()

        verify(mockRegistrationConnector, times(1))
          .registerWithIdIndividual(Matchers.eq(nino), any(), Matchers.eq(inputRequestData))(any(), any(), any())
      }
    }

    "throw Exception when any other exception returned from connector" in {

      when(mockRegistrationConnector.registerWithIdIndividual(Matchers.eq(nino), any(),Matchers.eq(inputRequestData))(any(), any(), any()))
        .thenReturn(Future.failed(new Exception("Generic Exception")))

      val result = registrationController(individualRetrievals).registerWithIdIndividual(fakeRequest.withJsonBody(inputRequestData))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe "Generic Exception"

        verify(mockRegistrationConnector, times(1))
          .registerWithIdIndividual(Matchers.eq(nino), any(), Matchers.eq(inputRequestData))(any(), any(), any())
      }
    }
  }

  "register With Id Organisation" must {

    val inputData = Json.obj("utr" -> "1100000000", "organisationName" -> "Test Ltd", "organisationType" -> "LLP")

    "return OK when the registration with id is successful for Organisation" in {

      val input = readJsonFromFile("/data/validRegisterWithIdOrganisationResponse.json")
      val successResponse = Json.toJson(readJsonFromFile("/data/validRegisterWithIdOrganisationResponse.json").as[SuccessResponse])

      when(mockRegistrationConnector.registerWithIdOrganisation(Matchers.eq("1100000000"), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Right(input)))

      val result = registrationController(organisationRetrievals).registerWithIdOrganisation(fakeRequest.withJsonBody(inputData))

      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        contentAsJson(result) mustEqual successResponse
      }
    }

    "return Bad Request" when {
      "the bad data returned from frontend in the request" in {

        val inputData = Json.obj("bad" -> "data")

        val result = registrationController(organisationRetrievals).registerWithIdOrganisation(fakeRequest.withJsonBody(inputData))

        ScalaFutures.whenReady(result.failed) { e =>
          e mustBe a[BadRequestException]
        }
      }

      "there is no body in the request" in {
        val result = registrationController(organisationRetrievals).registerWithIdOrganisation(fakeRequest)

        ScalaFutures.whenReady(result.failed) { e =>
          e mustBe a[BadRequestException]
          e.getMessage mustEqual "No request body received for Organisation"
        }
      }
    }

    "throw Upstream5xxResponse when given Upstream5xxResponse from connector" in {

      val failureResponse = Json.obj(
        "code" -> "SERVER_ERROR",
        "reason" -> "DES is currently experiencing problems that require live service intervention."
      )

      when(mockRegistrationConnector.registerWithIdOrganisation(Matchers.eq("1100000000"), any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse(failureResponse.toString(), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      val result = registrationController(organisationRetrievals).registerWithIdOrganisation(fakeRequest.withJsonBody(inputData))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Upstream5xxResponse]
        e.getMessage mustBe failureResponse.toString()

        verify(mockRegistrationConnector, times(1))
          .registerWithIdOrganisation(Matchers.eq("1100000000"), any(), any())(any(), any(), any())
      }
    }

    "throw Exception when any other exception returned from connector" in {

      when(mockRegistrationConnector.registerWithIdOrganisation(Matchers.eq("1100000000"), any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(new Exception("Generic Exception")))

      val result = registrationController(organisationRetrievals).registerWithIdOrganisation(fakeRequest.withJsonBody(inputData))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe "Generic Exception"

        verify(mockRegistrationConnector, times(1))
          .registerWithIdOrganisation(Matchers.eq("1100000000"), any(), any())(any(), any(), any())
      }
    }
  }

  "registrationNoIdOrganisation" must {

    def fakeRequest(data: JsValue): FakeRequest[JsValue] = FakeRequest("POST", "/").withBody(data)

    "return OK when valid data is posted" in {

      val successResponse: JsObject = Json.obj(
        "processingDate" -> LocalDate.now,
        "sapNumber" -> "1234567890",
        "safeId" -> "XE0001234567890"
      )
      when(mockRegistrationConnector.registrationNoIdOrganisation(any(), Matchers.eq(dataToEmtp))(any(), any(), any()))
        .thenReturn(Future.successful(Right(successResponse)))

      val result = call(registrationController(organisationRetrievals).registrationNoIdOrganisation, fakeRequest(dataFromFrontend))

      ScalaFutures.whenReady(result) { _ =>
        status(result) mustBe OK
        verify(mockRegistrationConnector, times(1)).registrationNoIdOrganisation(any(), Matchers.eq(dataToEmtp))(any(), any(), any())
      }
    }

    "throw Upstream5xxResponse when given UpStream5XXResponse from connector" in {
      val serviceUnavailable: JsObject = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "Dependent systems are currently not responding."
      )

      when(mockRegistrationConnector.registrationNoIdOrganisation(any(), Matchers.eq(dataToEmtp))(any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse(serviceUnavailable.toString(), INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

      val result = call(registrationController(organisationRetrievals).registrationNoIdOrganisation,fakeRequest(dataFromFrontend))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Upstream5xxResponse]
        e.getMessage mustBe serviceUnavailable.toString()

        verify(mockRegistrationConnector, times(1))
          .registrationNoIdOrganisation(any(), Matchers.eq(dataToEmtp))(any(), any(), any())
      }
    }

    "throw Exception when any other exception returned from connector" in {

      when(mockRegistrationConnector.registrationNoIdOrganisation(any(), Matchers.eq(dataToEmtp))(any(), any(), any()))
        .thenReturn(Future.failed(new Exception("Generic Exception")))

      val result = call(registrationController(organisationRetrievals).registrationNoIdOrganisation,fakeRequest(dataFromFrontend))

      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[Exception]
        e.getMessage mustBe "Generic Exception"

        verify(mockRegistrationConnector, times(1))
          .registrationNoIdOrganisation(any(), Matchers.eq(dataToEmtp))(any(), any(), any())
      }
    }
  }

}

object RegistrationControllerSpec {

  private val nino = "test-nino"
  private val externalId = "test-external-id"
  private val fakeRequest = FakeRequest("POST", "/")

  private val individualRetrievals =
    Future.successful(
      new ~(
        new ~(
          Some(nino),
          Some(externalId)
        ),
        Some(AffinityGroup.Individual)
      )
    )

  private val organisationRetrievals =
    Future.successful(
      new ~(
        Some(externalId),
        Some(AffinityGroup.Organisation)
      )
    )

}

trait RegistrationControllerBehaviours {

  def registrationResponseHandler: Unit = ???

}