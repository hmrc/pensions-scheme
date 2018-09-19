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

import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsString, Json, Reads, Writes}

class SchemeDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues {
  "A JSON payload containing scheme details" should {

    val schemeDetails = Json.obj("srn" -> JsString("AAABA932JASDA"), "pstr" -> JsString("A3DCADAA"), "schemeStatus" -> "Pending")

    "correctly parse to a model of SchemeDetails" when {
      "we have a srn" in {
        val output = schemeDetails.as[SchemeDetails]

        output.srn.value mustBe (schemeDetails \ "srn").as[String]
      }

      "we don't have an srn" in {
        val output = Json.obj("test" -> "test").as[SchemeDetails]

        output.srn mustBe None
      }

      "we have a pstr" in {
        val output = schemeDetails.as[SchemeDetails]

        output.pstr.value mustBe (schemeDetails \ "pstr").as[String]
      }

      "we don't have pstr" in {
        val output = Json.obj("test" -> "test").as[SchemeDetails]

        output.srn mustBe None
      }

      "we have a status" in {
        val output = schemeDetails.as[SchemeDetails]

        output.status mustBe (schemeDetails \ "status").as[String]
      }
    }
  }
}

case class SchemeDetails(srn: Option[String], pstr: Option[String], status: String)

object SchemeDetails {
  implicit val reads : Reads[SchemeDetails] = Json.reads[SchemeDetails]
  implicit val writes : Writes[SchemeDetails] = Json.writes[SchemeDetails]
}
