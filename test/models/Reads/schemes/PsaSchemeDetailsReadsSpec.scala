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
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.Json
import play.api.libs.json._

class PsaSchemeDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues with PSASchemeDetailsGenerator {
  
  "A JSON payload containing a PsaSchemeDetails" should {
    
    "Parse correctly to a PsaSchemeDetails object" when {
      
      "we have a valid Scheme Details object within it" in {
        forAll(psaSchemeDetailsGenerator) { psaSchemeDetails =>
          psaSchemeDetails.as(PsaSchemeDetails.apiReads).schemeDetails.srn mustBe (
            psaSchemeDetails \ "psaSchemeDetails" \ "schemeDetails" \ "srn").asOpt[String]
        }
      }

      "we have a establisherDetails" in {
        forAll(psaSchemeDetailsGenerator) { psaSchemeDetails =>
          psaSchemeDetails.as(PsaSchemeDetails.apiReads).establisherDetails mustBe (
            psaSchemeDetails \ "psaSchemeDetails" \ "establisherDetails").asOpt(EstablisherInfo.apiReads)
        }
      }

      "we don't have a establisherDetails" in {
        forAll(psaSchemeDetailsGenerator(withAllEmpty = true)) { psaSchemeDetails =>
          psaSchemeDetails.as(PsaSchemeDetails.apiReads).establisherDetails mustBe None
        }
      }

      "we have a trusteeDetails" in {
        forAll(psaSchemeDetailsGenerator) { psaSchemeDetails =>
          psaSchemeDetails.as(PsaSchemeDetails.apiReads).trusteeDetails mustBe (
            psaSchemeDetails \ "psaSchemeDetails" \ "trusteeDetails").asOpt(TrusteeInfo.apiReads)
        }
      }

      "we don't have a trusteeDetails" in {
        forAll(psaSchemeDetailsGenerator(withAllEmpty = true)) { psaSchemeDetails =>
          psaSchemeDetails.as(PsaSchemeDetails.apiReads).trusteeDetails mustBe None
        }
      }

      "we have a valid list of psa details within it" in {
        forAll(psaSchemeDetailsGenerator(noOfElements = 2)) { psaSchemeDetails =>
          psaSchemeDetails.as(PsaSchemeDetails.apiReads).psaDetails.head.id mustBe (
            psaSchemeDetails \ "psaSchemeDetails" \ "psaDetails" \ 0 \ "psaid").as[String]
          psaSchemeDetails.as(PsaSchemeDetails.apiReads).psaDetails(1).id mustBe (
            psaSchemeDetails \ "psaSchemeDetails" \ "psaDetails" \ 1 \ "psaid").as[String]
        }
      }

      "we don't have psa details" in {
        forAll(psaSchemeDetailsGenerator(withAllEmpty = true)) { psaSchemeDetails =>
          psaSchemeDetails.as(PsaSchemeDetails.apiReads).psaDetails mustBe Nil
        }
      }
    }
  }
}