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

class SchemeConnectorSpec extends SpecBase with MockitoSugar with BeforeAndAfter with PatienceConfiguration {

  val httpClient = mock[HttpClient]
  val schemeConnector = new SchemeConnectorImpl(httpClient, appConfig)
  implicit val hc = HeaderCarrier()
  val failureResponse: JsObject = Json.obj(
    "code" -> "INVALID_PAYLOAD",
    "reason" -> "Submission has not passed validation. Invalid PAYLOAD"
  )

  before(reset(httpClient))

  "registerScheme" must {
    val url = appConfig.schemeRegistrationUrl.format("A2000001")
    "return OK when Des/Etmp returns successfully" in {
      val successResponse: JsObject = Json.obj("processingDate" -> LocalDate.now, "schemeReferenceNumber" -> "S0123456789")
      val validData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")

      when(httpClient.POST[JsValue, HttpResponse](Matchers.eq(url), Matchers.eq(validData), any())(any(), any(), any(), any())).
        thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

      val result = schemeConnector.registerScheme("A2000001", validData)
      ScalaFutures.whenReady(result) { res =>
        res.status mustBe OK
      }
    }

    "throw BadRequestException when bad request returned from Des/Etmp" in {
      val validData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")
      when(httpClient.POST[JsValue, HttpResponse](Matchers.eq(url), Matchers.eq(validData), any())(any(), any(), any(), any())).
        thenReturn(Future.failed(new BadRequestException(failureResponse.toString())))

      val result = schemeConnector.registerScheme("A2000001", validData)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe failureResponse.toString()
      }
    }
  }

  "register PSA" must {
    val schemeAdminRegisterUrl = appConfig.schemeAdminRegistrationUrl
    "return OK when DES/Etmp returns successfully" in {
      val inputRequestData = readJsonFromFile("/data/validPsaRequest.json")
      val successResponse = Json.obj(
        "processingDate" -> LocalDate.now,
        "formBundle" -> "1121313",
        "psaId" -> "A21999999"
      )
      when(httpClient.POST[JsValue, HttpResponse](Matchers.eq(schemeAdminRegisterUrl), Matchers.eq(inputRequestData), any())(any(), any(), any(), any())).
        thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

      val result = schemeConnector.registerPSA(inputRequestData)
      ScalaFutures.whenReady(result) { res =>
        res.status mustBe OK
      }
    }

    "throw BadRequestException when DES/Etmp throws Bad Request" in {
      val invalidData = Json.obj("data" -> "invalid")
      when(httpClient.POST[JsValue, HttpResponse](Matchers.eq(schemeAdminRegisterUrl), Matchers.eq(invalidData), any())(any(), any(), any(), any())).
        thenReturn(Future.failed(new BadRequestException(failureResponse.toString())))

      val result = schemeConnector.registerPSA(invalidData)
      ScalaFutures.whenReady(result.failed) { e =>
        e mustBe a[BadRequestException]
        e.getMessage mustBe failureResponse.toString()
      }
    }
  }
}
