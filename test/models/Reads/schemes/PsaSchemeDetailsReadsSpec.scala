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

package models.Reads.schemes

import models.schemes.{EstablisherInfo, PsaSchemeDetails, TrusteeInfo}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._

class PsaSchemeDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues with SchemeDetailsStubJsonData {
  "A JSON payload containing a PsaSchemeDetails" should {
    "Parse correctly to a PsaSchemeDetails object" when {

      val actualOutput = psaSchemeDetails.as(PsaSchemeDetails.apiReads)

      "we have a valid Scheme Details object within it" in {
        actualOutput.schemeDetails.srn.value mustBe (psaSchemeDetails \ "psaSchemeDetails" \ "schemeDetails" \ "srn").as[String]
      }

      "we have a establisherDetails" in {
        actualOutput.establisherDetails.value mustBe (psaSchemeDetails \ "psaSchemeDetails" \ "establisherDetails").as(
          EstablisherInfo.apiReads)
      }

      "we don't have a establisherDetails" in {
        val psaSchemeDetails = Json.obj("psaSchemeDetails" -> Json.obj("schemeDetails" -> schemeDetails))
        val output = psaSchemeDetails.as(PsaSchemeDetails.apiReads)

        output.establisherDetails mustBe None
      }

      "we have a trusteeDetails" in {
        actualOutput.trusteeDetails.value mustBe (psaSchemeDetails \ "psaSchemeDetails" \ "trusteeDetails").as(
         TrusteeInfo.apiReads)
      }

      "we don't have a trusteeDetails" in {
        val psaSchemeDetails = Json.obj("psaSchemeDetails" -> Json.obj("schemeDetails" -> schemeDetails))
        val output = psaSchemeDetails.as(PsaSchemeDetails.apiReads)

        output.trusteeDetails mustBe None
      }

      "we have a valid list of psa details within it" in {
        actualOutput.psaDetails.head.id mustBe (psaSchemeDetails \ "psaSchemeDetails" \ "psaDetails" \ 0 \ "psaid").as[String]
        actualOutput.psaDetails(1).id mustBe (psaSchemeDetails \ "psaSchemeDetails" \ "psaDetails" \ 1 \ "psaid").as[String]
      }

      "we don't have psa details" in {
        val psaSchemeDetails = Json.obj("psaSchemeDetails" -> Json.obj("schemeDetails" -> schemeDetails))

        val output = psaSchemeDetails.as(PsaSchemeDetails.apiReads)
        output.psaDetails mustBe Nil
      }
    }
  }
}