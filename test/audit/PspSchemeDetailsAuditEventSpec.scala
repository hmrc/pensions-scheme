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

package audit

import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{JsValue, Json}

class PspSchemeDetailsAuditEventSpec extends WordSpec with MustMatchers {

  private val pspId = "pspId"
  private val status = 200
  private val inputPayload: JsValue = Json.parse(
    """ {
      |  "pstr": "24000040IN",
      |  "srn": "S2400000040",
      |  "pspDetails": {
      |    "relationshipStartDate": "2019-03-29",
      |    "pspClientReference": "1234345",
      |    "authorisingPSA": {
      |      "organisationOrPartnershipName": "Authorised PSA Organisation Name"
      |    },
      |    "id": "21000005",
      |    "individual": {
      |      "firstName": "Nigel",
      |      "lastName": "Smith",
      |      "middleName": "Robert"
      |    },
      |    "authorisingPSAID": "A1090099"
      |  }
      |}
      |""".stripMargin)


  private val outputPayload: JsValue = Json.parse(
    """  {
      |    "pensionSchemeTaxReference": "24000040IN",
      |    "schemeReferenceNumber": "S2400000040",
      |    "pensionSchemePractitionerDetails": {
      |      "relationshipStartDate": "2019-03-29",
      |      "pensionSchemePractitionerClientReference": "1234345",
      |      "authorisingPensionSchemeAdministrator": {
      |        "organisationOrPartnershipName": "Authorised PSA Organisation Name"
      |      },
      |      "id": "21000005",
      |      "individual": {
      |        "firstName": "Nigel",
      |        "lastName": "Smith",
      |        "middleName": "Robert"
      |      },
      |      "authorisingPensionSchemeAdministratorID": "A1090099"
      |    }
      |  }
      |""".stripMargin)


  private val event = PspSchemeDetailsAuditEvent(
    pspId = pspId,
    status = status,
    payload = Some(inputPayload)
  )

  private val expectedDetails: Map[String, String] = Map(
    "pensionSchemePractitionerId" -> pspId,
    "status" -> status.toString,
    "payload" -> outputPayload.toString
  )

  "calling PspSchemeDetailsAuditEvent" must {

    "returns correct event object" in {

      event.auditType mustBe "GetPensionSchemePractitionerSchemeDetails"

      event.details mustBe expectedDetails
    }
  }
}