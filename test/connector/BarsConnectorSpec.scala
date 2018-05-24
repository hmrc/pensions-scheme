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

import audit.{AuditService, BarsCheck}
import audit.testdoubles.StubSuccessfulAuditService
import org.scalatest.{AsyncFlatSpec, Matchers, OptionValues}
import utils.WireMockHelper
import com.github.tomakehurst.wiremock.client.WireMock._
import models.{BankAccount, ValidateBankDetailsRequest}
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext


class BarsConnectorSpec
  extends AsyncFlatSpec with Matchers with OptionValues with WireMockHelper {

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val rh: RequestHeader = FakeRequest("", "")

  private val auditService = new StubSuccessfulAuditService()

  val notInvalid = false
  val invalid = true
  val sortCode = "991122"
  val accountNumber = "12345678"
  val psaIdentifier = "test-psa-id"

  "BarsConnector after calling invalidBankAccount" should "return invalid if accountNumberWithSortCode is invalid and sort code is not present on EISCID" in {
    val response =
      """ {
        "accountNumberWithSortCodeIsValid": false,
        "nonStandardAccountDetailsRequiredForBacs": "no",
        "sortCodeIsPresentOnEISCD":"no",
        "supportsBACS":"yes"
      } """

    server.stubFor(
      post(urlEqualTo("/validateBankDetails"))
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
            .withHeader("Content-Type", "application/json")
            .withBody(response)
        )
    )

    val connector = injector.instanceOf[BarsConnector]
    connector.invalidBankAccount(BankAccount(sortCode, accountNumber), psaIdentifier).map { response =>
      response shouldBe invalid
    }

  }

  it should "return not invalid if accountNumberWithSortCode is invalid and sort code is not present on EISCID" in {
    val response =
      """ {
        "accountNumberWithSortCodeIsValid": true,
        "nonStandardAccountDetailsRequiredForBacs": "no",
        "sortCodeIsPresentOnEISCD":"no",
        "supportsBACS":"yes"
      } """

    server.stubFor(
      post(urlEqualTo("/validateBankDetails"))
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
            .withHeader("Content-Type", "application/json")
            .withBody(response)
        )
    )

    val connector = injector.instanceOf[BarsConnector]
    connector.invalidBankAccount(BankAccount(sortCode, accountNumber), psaIdentifier).map { response =>
      response shouldBe notInvalid
    }

  }

  it should "return not invalid if accountNumberWithSortCode is notInvalid and can't check sort code on EISCID" in {
    val response =
      """ {
        "accountNumberWithSortCodeIsValid": true,
        "nonStandardAccountDetailsRequiredForBacs": "no",
        "sortCodeIsPresentOnEISCD":"error",
        "supportsBACS":"yes"
      } """

    server.stubFor(
      post(urlEqualTo("/validateBankDetails"))
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
            .withHeader("Content-Type", "application/json")
            .withBody(response)
        )
    )

    val connector = injector.instanceOf[BarsConnector]
    connector.invalidBankAccount(BankAccount(sortCode, accountNumber), psaIdentifier).map { response =>
      response shouldBe notInvalid
    }
  }

  it should "return not invalid if accountNumberWithSortCode is notInvalid and sort code is found on EISCID" in {

    val response =
      """ {
        "accountNumberWithSortCodeIsValid": true,
        "nonStandardAccountDetailsRequiredForBacs": "no",
        "sortCodeIsPresentOnEISCD":"yes",
        "supportsBACS":"yes"
      } """

    server.stubFor(
      post(urlEqualTo("/validateBankDetails"))
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
            .withHeader("Content-Type", "application/json")
            .withBody(response)
        )
    )

    val connector = injector.instanceOf[BarsConnector]
    connector.invalidBankAccount(BankAccount(sortCode, accountNumber), psaIdentifier).map { response =>
      response shouldBe notInvalid
    }
  }

  it should "return not invalid when BARS returns a failure" in {

    val errorResponse = "error"

    server.stubFor(
      post(urlEqualTo("/validateBankDetails"))
        .willReturn(
          serverError()
            .withStatus(Status.BAD_REQUEST)
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse)
        )
    )

    val connector = injector.instanceOf[BarsConnector]
    connector.invalidBankAccount(BankAccount(sortCode, accountNumber), psaIdentifier).map { response =>
      response shouldBe notInvalid
    }
  }

  it should "pass the Content-Type header" in {
    val response =
      """ {
        "accountNumberWithSortCodeIsValid": true,
        "nonStandardAccountDetailsRequiredForBacs": "no",
        "sortCodeIsPresentOnEISCD":"yes",
        "supportsBACS":"yes"
      } """

    val headerName = "Content-Type"
    val headerValue = "application/json"

    server.stubFor(
      post(urlEqualTo("/validateBankDetails"))
        .withHeader(headerName, equalTo(headerValue))
        .willReturn(
          serverError()
            .withStatus(Status.OK)
            .withHeader("Content-Type", "application/json")
            .withBody(response)
        )
    )

    val hc: HeaderCarrier = HeaderCarrier(extraHeaders = Seq((headerName, headerValue)))
    val ec: ExecutionContext = implicitly[ExecutionContext]

    val connector = injector.instanceOf[BarsConnector]
    connector.invalidBankAccount(BankAccount(sortCode, accountNumber), psaIdentifier)(ec, hc, rh).map { _ =>
      succeed
    }
  }

  it should "return pass the correct sortcode and account number" in {
    val request =
      """{
      "account": {
        "sortCode": "991122",
        "accountNumber": "12345678"
      }
    }"""


    val response =
      """ {
        "accountNumberWithSortCodeIsValid": true,
        "nonStandardAccountDetailsRequiredForBacs": "no",
        "sortCodeIsPresentOnEISCD":"yes",
        "supportsBACS":"yes"
      } """

    server.stubFor(
      post(urlEqualTo("/validateBankDetails"))
        .withRequestBody(equalToJson(request))
        .willReturn(
          serverError()
            .withStatus(Status.OK)
            .withHeader("Content-Type", "application/json")
            .withBody(response)
        )
    )

    val connector = injector.instanceOf[BarsConnector]
    connector.invalidBankAccount(BankAccount(sortCode, accountNumber), psaIdentifier).map { _ =>
      succeed
    }
  }

  it should "send an audit event on success" in {

    auditService.reset()

    val response =
      """ {
        "accountNumberWithSortCodeIsValid": false,
        "nonStandardAccountDetailsRequiredForBacs": "no",
        "sortCodeIsPresentOnEISCD":"no",
        "supportsBACS":"yes"
      } """

    val bankAccount = BankAccount(sortCode, accountNumber)

    server.stubFor(
      post(urlEqualTo("/validateBankDetails"))
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
            .withHeader("Content-Type", "application/json")
            .withBody(response)
        )
    )

    val connector = injector.instanceOf[BarsConnector]

    connector.invalidBankAccount(bankAccount, psaIdentifier).map { _ =>
      auditService.verifySent(
        BarsCheck(
          psaIdentifier,
          Status.OK,
          ValidateBankDetailsRequest(bankAccount),
          Some(Json.parse(response: String))
        )
      ) shouldBe true
    }

  }

  it should "send an audit event on failure" in {

    auditService.reset()

    val errorResponse = "error"
    val bankAccount = BankAccount(sortCode, accountNumber)

    server.stubFor(
      post(urlEqualTo("/validateBankDetails"))
        .willReturn(
          serverError()
            .withStatus(Status.BAD_REQUEST)
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse)
        )
    )

    val connector = injector.instanceOf[BarsConnector]

    connector.invalidBankAccount(bankAccount, psaIdentifier).map { _ =>
      auditService.verifySent(
        BarsCheck(
          psaIdentifier,
          Status.BAD_REQUEST,
          ValidateBankDetailsRequest(bankAccount),
          None
        )
      ) shouldBe true
    }

  }

  override protected def portConfigKey: String = "microservice.services.bank-account-reputation.port"

  override protected def bindings: Seq[GuiceableModule] =
    Seq(
      bind[AuditService].toInstance(auditService)
    )

}
