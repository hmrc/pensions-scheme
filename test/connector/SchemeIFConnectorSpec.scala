/*
 * Copyright 2020 HM Revenue & Customs
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
import base.JsonFileReader
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest._
import play.api.LoggerLike
import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import utils.WireMockHelper

class SchemeIFConnectorSpec extends AsyncFlatSpec
  with Matchers
  with WireMockHelper
  with OptionValues
  with RecoverMethods
  with EitherValues
  with ConnectorBehaviours with JsonFileReader{

  import SchemeConnectorSpec._

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val rh: RequestHeader = FakeRequest("", "")

  override def beforeEach(): Unit = {
    auditService.reset()
    super.beforeEach()
  }
  override protected def portConfigKey: String = "microservice.services.if-hod.port"

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[AuditService].toInstance(auditService),
      bind[LoggerLike].toInstance(logger)
    )
  def connector: SchemeIFConnector = app.injector.instanceOf[SchemeIFConnector]

  private val psaType = "PSA"
  private val pspType = "PSP"
  private val validListOfSchemeIFResponse = readJsonFromFile("/data/validListOfSchemesIFResponse.json")
  def listOfSchemesIFUrl(idType: String = psaType) = s"/pension-online/subscriptions/schemes/list/PODS/$idType/$idValue"

  "SchemeConnector listOfScheme from IF" should "return OK with the list of schemes response for PSA" in {
    server.stubFor(
      get(listOfSchemesIFUrl())
        .willReturn(
          ok(Json.stringify(validListOfSchemeIFResponse))
        )
    )

    connector.listOfSchemes(psaType, idValue).map { response =>
      response.status shouldBe OK
      response.body shouldBe Json.stringify(validListOfSchemeIFResponse)
    }
  }

  it should "return OK with the list of schemes response for PSP" in {
    server.stubFor(
      get(listOfSchemesIFUrl(pspType))
        .willReturn(
          ok(Json.stringify(validListOfSchemeIFResponse))
        )
    )

    connector.listOfSchemes(pspType, idValue).map { response =>
      response.status shouldBe OK
      response.body shouldBe Json.stringify(validListOfSchemeIFResponse)
    }
  }

  it should "throw Bad Request Exception when DES/ETMP throws BadRequestException" in {
    server.stubFor(
      get(listOfSchemesIFUrl())
        .willReturn(
          badRequest()
        )
    )

    connector.listOfSchemes(psaType, idValue).map { response =>
      response.status shouldBe BAD_REQUEST
    }
  }

  it should "throw NotFoundException when DES/ETMP throws NotFoundException" in {
    server.stubFor(
      get(listOfSchemesIFUrl(pspType))
        .willReturn(
          notFound()
        )
    )

    connector.listOfSchemes(pspType, idValue).map { response =>
      response.status shouldBe NOT_FOUND
    }
  }

  it should "throw UpStream5XXResponse when DES/ETMP throws Server error" in {
    server.stubFor(
      get(listOfSchemesIFUrl(psaType))
        .willReturn(
          serverError()
        )
    )

    connector.listOfSchemes(psaType, idValue).map { response =>
      response.status shouldBe INTERNAL_SERVER_ERROR
    }
  }
}


