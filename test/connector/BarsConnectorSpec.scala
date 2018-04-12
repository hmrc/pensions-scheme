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

import org.scalatest.{AsyncFlatSpec, Matchers, OptionValues}
import utils.WireMockHelper
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status
import uk.gov.hmrc.http.HeaderCarrier


class BarsConnectorSpec
  extends AsyncFlatSpec with Matchers with OptionValues with WireMockHelper {

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  val notInvalid = false
  val invalid = true

  "BarsConnector after calling invalidBankAccount" should "return invalid if accountNumberWithSortCode is invalid and sort code is not present on EISCID" in {
    val response = """ {
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
    connector.invalidBankAccount("", "").map { response =>
      response shouldBe invalid
    }

  }

    it should "return not invalid if accountNumberWithSortCode is invalid and sort code is not present on EISCID" in {
      val response = """ {
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
      connector.invalidBankAccount("", "").map { response =>
        response shouldBe notInvalid
      }

    }

    it should "return not invalid if accountNumberWithSortCode is notInvalid and can't check sort code on EISCID" in {
      val response = """ {
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
      connector.invalidBankAccount("", "").map { response =>
        response shouldBe notInvalid
      }
    }

    it should "return not invalid if accountNumberWithSortCode is notInvalid and sort code is found on EISCID" in {

      val response = """ {
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
      connector.invalidBankAccount("", "").map { response =>
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
      connector.invalidBankAccount("", "").map { response =>
        response shouldBe notInvalid
      }
    }

  override protected def portConfigKey: String = "microservice.services.bank-account-reputation.port"
}
