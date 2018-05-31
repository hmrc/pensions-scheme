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

import audit.AuditService
import audit.testdoubles.StubSuccessfulAuditService
import base.JsonFileReader
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.{AsyncWordSpec, MustMatchers, OptionValues, RecoverMethods}
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.http._
import utils.WireMockHelper
import org.joda.time.LocalDate

class SchemeConnectorSpec extends AsyncWordSpec with MustMatchers with WireMockHelper with OptionValues with RecoverMethods{

  import SchemeConnectorSpec._

  override protected def portConfigKey: String = "microservice.services.des-hod.port"

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[AuditService].toInstance(auditService)
    )

  "SchemeConnector after calling registerScheme" must {
    val psaId = "test"
    val validData = readJsonFromFile("/data/validSchemeRegistrationRequest.json")
   "return 200 Success" in {
      val successResponse: JsObject = Json.obj("processingDate" -> LocalDate.now, "schemeReferenceNumber" -> "S0123456789")
      server.stubFor(
        post(urlEqualTo(s"/pension-online/scheme-subscription/$psaId"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.stringify(validData)))
          .willReturn(
            ok(Json.stringify(successResponse))
              .withHeader("Content-Type", "application/json")
          )
      )

      val connector = injector.instanceOf[SchemeConnector]
      connector.registerScheme(psaId, validData).map { response =>
        response.status mustBe 200
      }
    }

    "throw UpStream4XXResponse for a 403 INVALID_BUSINESS_PARTNER response" in {
      server.stubFor(
        post(urlEqualTo(s"/pension-online/scheme-subscription/$psaId"))
          .willReturn(
            forbidden
              .withHeader("Content-Type", "application/json")
              .withBody(invalidBusinessPartnerResponse)
          )
      )

      val connector = injector.instanceOf[SchemeConnector]

      recoverToSucceededIf[Upstream4xxResponse] {
        connector.registerScheme(psaId, validData)
      }
    }

    "throw UpStream4XXResponse for a 409 DUPLICATE_SUBMISSION response" in {
      server.stubFor(
        post(urlEqualTo(s"/pension-online/scheme-subscription/$psaId"))
          .willReturn(
            aResponse()
              .withStatus(Status.CONFLICT)
              .withHeader("Content-Type", "application/json")
              .withBody(duplicateSubmissionResponse)
          )
      )

      val connector = injector.instanceOf[SchemeConnector]

      recoverToSucceededIf[Upstream4xxResponse] {
        connector.registerScheme(psaId, validData)
      }
    }

    "throw BadRequestException for a 400 BAD request" in {
      server.stubFor(
        post(urlEqualTo(s"/pension-online/scheme-subscription/$psaId"))
          .willReturn(
            badRequest
              .withHeader("Content-Type", "application/json")
              .withBody("Bad Request")
          )
      )

      val connector = injector.instanceOf[SchemeConnector]

      recoverToSucceededIf[BadRequestException] {
        connector.registerScheme(psaId, validData)
      }
    }

    "throw NotFoundException for a 404 Not Found request" in {
      server.stubFor(
        post(urlEqualTo(s"/pension-online/scheme-subscription/$psaId"))
          .willReturn(
            notFound
              .withHeader("Content-Type", "application/json")
              .withBody("Not Found")
          )
      )

      val connector = injector.instanceOf[SchemeConnector]

      recoverToSucceededIf[NotFoundException] {
        connector.registerScheme(psaId, validData)
      }
    }
  }

  "SchemeConnector after calling registerPSA" must {
    val inputRequestData = readJsonFromFile("/data/validPsaRequest.json")
    "return 200 Success" in {
      val successResponse = Json.obj(
        "processingDate" -> LocalDate.now,
        "formBundle" -> "1121313",
        "psaId" -> "A21999999"
      )
      server.stubFor(
        post(urlEqualTo("/pension-online/subscription"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.stringify(inputRequestData)))
          .willReturn(
            ok(Json.stringify(successResponse))
              .withHeader("Content-Type", "application/json")
          )
      )

      val connector = injector.instanceOf[SchemeConnector]
      connector.registerPSA(inputRequestData).map { response =>
        response.status mustBe 200
      }
    }

    "throw UpStream4XXResponse for a 403 INVALID_BUSINESS_PARTNER response" in {
      server.stubFor(
        post(urlEqualTo("/pension-online/subscription"))
          .willReturn(
            forbidden
              .withHeader("Content-Type", "application/json")
              .withBody(invalidBusinessPartnerResponse)
          )
      )

      val connector = injector.instanceOf[SchemeConnector]

      recoverToSucceededIf[Upstream4xxResponse] {
        connector.registerPSA(inputRequestData)
      }
    }

    "throw UpStream4XXResponse for a 409 DUPLICATE_SUBMISSION response" in {
      server.stubFor(
        post(urlEqualTo("/pension-online/subscription"))
          .willReturn(
            aResponse()
              .withStatus(Status.CONFLICT)
              .withHeader("Content-Type", "application/json")
              .withBody(duplicateSubmissionResponse)
          )
      )

      val connector = injector.instanceOf[SchemeConnector]

      recoverToSucceededIf[Upstream4xxResponse] {
        connector.registerPSA(inputRequestData)
      }
    }

    "throw BadRequestException for a 400 BAD request" in {
      server.stubFor(
        post(urlEqualTo("/pension-online/subscription"))
          .willReturn(
            badRequest
              .withHeader("Content-Type", "application/json")
              .withBody("Bad Request")
          )
      )

      val connector = injector.instanceOf[SchemeConnector]

      recoverToSucceededIf[BadRequestException] {
        connector.registerPSA(inputRequestData)
      }
    }

    "throw NotFoundException for a 404 Not Found request" in {
      server.stubFor(
        post(urlEqualTo("/pension-online/subscription"))
          .willReturn(
            notFound
              .withHeader("Content-Type", "application/json")
              .withBody("Not Found")
          )
      )

      val connector = injector.instanceOf[SchemeConnector]

      recoverToSucceededIf[NotFoundException] {
        connector.registerPSA(inputRequestData)
      }
    }
  }

  "SchemeConnector after calling listOfScheme" must {
    val validResponse = readJsonFromFile("/data/validListOfSchemesResponse.json")
    val psaId = "test"

    "return OK with the list of schemes response" in {
      server.stubFor(
        get(s"/pension-online/subscription/$psaId/list")
          .willReturn(
            ok(Json.stringify(validResponse))
        )
      )

      val connector = injector.instanceOf[SchemeConnector]
      connector.listOfSchemes(psaId).map{ response =>
        response.status mustBe 200
        response.body mustEqual Json.stringify(validResponse)
      }
    }

    "throw Bad Request Exception when DES/ETMP throws BadRequestException " in {
      server.stubFor(
        get(s"/pension-online/subscription/$psaId/list")
          .willReturn(
            notFound()
          )
      )

      val connector = injector.instanceOf[SchemeConnector]
      recoverToSucceededIf[NotFoundException]{
        connector.listOfSchemes(psaId)
      }
    }

    "throw UpStream5XXResponse when DES/ETMP throws Server error " in {
      server.stubFor(
        get(s"/pension-online/subscription/$psaId/list")
          .willReturn(
            serverError()
          )
      )

      val connector = injector.instanceOf[SchemeConnector]
      recoverToSucceededIf[Upstream5xxResponse]{
        connector.listOfSchemes(psaId)
      }
    }
  }
}

object SchemeConnectorSpec extends JsonFileReader {
    private implicit val hc: HeaderCarrier = HeaderCarrier()
    private implicit val rh: RequestHeader = FakeRequest("", "")
    private val invalidBusinessPartnerResponse =
      Json.stringify(
        Json.obj(
          "code" -> "INVALID_BUSINESS_PARTNER",
          "reason" -> "test-reason"
        )
      )

    private val duplicateSubmissionResponse =
      Json.stringify(
        Json.obj(
          "code" -> "DUPLICATE_SUBMISSION",
          "reason" -> "test-reason"
        )
      )
  val auditService = new StubSuccessfulAuditService()
}
