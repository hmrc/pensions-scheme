/*
 * Copyright 2021 HM Revenue & Customs
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

///*
// * Copyright 2021 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package controllers
//
//import base.SpecBase
//import org.mockito.Matchers.{any, eq => meq}
//import org.mockito.Mockito._
//import org.scalatest.BeforeAndAfter
//import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
//import org.scalatestplus.mockito.MockitoSugar
//import play.api.libs.json.{JsObject, JsResultException, Json}
//import play.api.test.FakeRequest
//import play.api.test.Helpers._
//import service.SchemeService
//import uk.gov.hmrc.http._
//
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.Future
//
//class SchemeIFControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfter with PatienceConfiguration {
//  val mockSchemeService: SchemeService = mock[SchemeService]
//  val controller = new SchemeIFController(mockSchemeService, stubControllerComponents())
//
//  before {
//    reset(mockSchemeService)
//  }
//
//  "list of schemes" must {
//    val fakeRequest = FakeRequest("GET", "/").withHeaders(("idType", "PSA"),("idValue", "A2000001"))
//
//    "return OK with list of schemes for PSA when DES/ETMP returns it successfully" in {
//      val validResponse = readJsonFromFile("/data/validListOfSchemesIFResponse.json")
//      when(mockSchemeService.listOfSchemes(meq("PSA"), meq("A2000001"))(any(), any(), any()))
//        .thenReturn(Future.successful(HttpResponse(OK, validResponse.toString())))
//      val result = controller.listOfSchemes(fakeRequest)
//      ScalaFutures.whenReady(result) { _ =>
//        status(result) mustBe OK
//        contentAsJson(result) mustEqual validResponse
//        verify(mockSchemeService, times(1)).listOfSchemes(any(), any())(any(), any(), any())
//      }
//    }
//
//    "return OK with list of schemes for PSP when DES/ETMP returns it successfully" in {
//      val fakeRequest = FakeRequest("GET", "/").withHeaders(("idType", "PSP"),("idValue", "A2200001"))
//      val validResponse = readJsonFromFile("/data/validListOfSchemesIFResponse.json")
//      when(mockSchemeService.listOfSchemes(meq("PSP"), meq("A2200001"))(any(), any(), any()))
//        .thenReturn(Future.successful(HttpResponse(OK, validResponse.toString())))
//      val result = controller.listOfSchemes(fakeRequest)
//      ScalaFutures.whenReady(result) { _ =>
//        status(result) mustBe OK
//        contentAsJson(result) mustEqual validResponse
//        verify(mockSchemeService, times(1)).listOfSchemes(any(), any())(any(), any(), any())
//      }
//    }
//
//    "throw BadRequestException when PSAId is not present in the header" in {
//      val result = controller.listOfSchemes(FakeRequest("GET", "/"))
//      ScalaFutures.whenReady(result.failed) { e =>
//        e mustBe a[BadRequestException]
//        e.getMessage mustBe "Bad Request with no ID type or value"
//        verify(mockSchemeService, never()).listOfSchemes(any(), any())(any(), any(), any())
//      }
//    }
//
//    "throw JsResultException when the invalid data returned from DES/ETMP" in {
//      val validResponse = Json.obj("invalid" -> "data")
//      when(mockSchemeService.listOfSchemes(meq("PSA"), meq("A2000001"))(any(), any(), any()))
//        .thenReturn(Future.successful(HttpResponse(OK, validResponse.toString())))
//      val result = controller.listOfSchemes(fakeRequest)
//      ScalaFutures.whenReady(result.failed) { e =>
//        e mustBe a[JsResultException]
//        verify(mockSchemeService, times(1)).listOfSchemes(any(), any())(any(), any(), any())
//      }
//    }
//
//    "throw BadRequestException when bad request returned from Des" in {
//      val invalidPayload: JsObject = Json.obj(
//        "code" -> "INVALID_PSAID",
//        "reason" -> "Submission has not passed validation. Invalid parameter PSAID."
//      )
//      when(mockSchemeService.listOfSchemes(meq("PSA"), meq("A2000001"))(any(), any(), any())).thenReturn(
//        Future.failed(new BadRequestException(invalidPayload.toString())))
//
//      val result = controller.listOfSchemes(fakeRequest)
//      ScalaFutures.whenReady(result.failed) { e =>
//        e mustBe a[BadRequestException]
//        e.getMessage mustBe invalidPayload.toString()
//        verify(mockSchemeService, times(1)).listOfSchemes(meq("PSA"), meq("A2000001"))(any(), any(), any())
//      }
//    }
//
//    "throw Upstream5xxResponse when UpStream5XXResponse returned" in {
//      val serviceUnavailable: JsObject = Json.obj(
//        "code" -> "SERVICE_UNAVAILABLE",
//        "reason" -> "Dependent systems are currently not responding."
//      )
//      when(mockSchemeService.listOfSchemes(meq("PSA"), meq("A2000001"))(any(), any(), any())).thenReturn(
//        Future.failed(UpstreamErrorResponse(serviceUnavailable.toString(), SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)))
//
//      val result = controller.listOfSchemes(fakeRequest)
//      ScalaFutures.whenReady(result.failed) { e =>
//        e mustBe a[UpstreamErrorResponse]
//        e.getMessage mustBe serviceUnavailable.toString()
//        verify(mockSchemeService, times(1)).listOfSchemes(meq("PSA"), meq("A2000001"))(any(), any(), any())
//      }
//    }
//
//    "throw generic exception when any other exception returned from Des" in {
//      when(mockSchemeService.listOfSchemes(meq("PSA"), meq("A2000001"))(any(), any(), any())).thenReturn(
//        Future.failed(new Exception("Generic Exception")))
//
//      val result = controller.listOfSchemes(fakeRequest)
//      ScalaFutures.whenReady(result.failed) { e =>
//        e mustBe a[Exception]
//        e.getMessage mustBe "Generic Exception"
//        verify(mockSchemeService, times(1)).listOfSchemes(meq("PSA"), meq("A2000001"))(any(), any(), any())
//      }
//    }
//  }
//}
