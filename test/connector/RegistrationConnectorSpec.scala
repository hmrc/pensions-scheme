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

package connector

import base.SpecBase
import models.{FailureResponse, FailureResponseElement, SuccessResponse}
import org.joda.time.LocalDate
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.{JsObject, JsValue, Json}
import play.mvc.Http.Status.OK
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RegistrationConnectorSpec extends SpecBase with MockitoSugar with BeforeAndAfter with PatienceConfiguration {

  val httpClient = mock[HttpClient]
  val registrationConnector = new RegistrationConnectorImpl(httpClient, appConfig)
  implicit val hc = HeaderCarrier()
  val failureResponse: JsObject = Json.obj(
    "code" -> "INVALID_PAYLOAD",
    "reason" -> "Submission has not passed validation. Invalid PAYLOAD"
  )

  before(reset(httpClient))

  "register with id" must {
    "return OK when Des/ETMP returns successfully" in {
      val inputRequestData = readJsonFromFile("/data/validRegisterWithIdIndividualRequest.json")
      val validSuccessResponse = readJsonFromFile("/data/validRegisterWithIdIndividualResponse.json")

      when(httpClient.POST[JsValue, HttpResponse](Matchers.eq(appConfig.registerWithIdUrl.format("nino", "AB100100A")),
        Matchers.eq(inputRequestData), any())(any(), any(), Matchers.eq(hc), any())).
        thenReturn(Future.successful(HttpResponse(OK, Some(Json.toJson(
          validSuccessResponse.as[SuccessResponse])))))

      val result = registrationConnector.registerWithId("nino", "AB100100A", inputRequestData)
      ScalaFutures.whenReady(result) { res =>
        res.status mustBe OK
        res.body mustEqual Json.prettyPrint(Json.toJson(validSuccessResponse.as[SuccessResponse]))
      }
    }

    "throw BadRequest when DES/ETMP throws Bad Request" in {
      val invalidData = Json.obj("data" -> "invalid")
      val failureResponse = Json.toJson(FailureResponse(Some(FailureResponseElement(code = "INVALID_PAYLOAD",
          reason = "Submission has not passed validation. Invalid PAYLOAD"))))

      when(httpClient.POST[JsValue, HttpResponse](Matchers.eq(appConfig.registerWithIdUrl.format("nino", "AB100100A")),
        Matchers.eq(invalidData), any())(any(), any(), any(), any())).thenReturn(
        Future.failed(new BadRequestException(failureResponse.toString())))

      val result = registrationConnector.registerWithId("nino", "AB100100A", invalidData)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustEqual failureResponse.toString()
      }
    }
  }
}
