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

package models.Reads

import models._
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._

class CorrespondenceCommonDetailReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples {
  "JSON payload containing correspondence common details" should {
    "correclty parse to a CorrespondenceCommonDetails object" when {
      val details = Json.obj("directorContactDetails" -> Json.obj("email" -> "test@test.com",
        "phone" -> "07592113"),
        "directorAddress" -> Json.obj("lines" -> JsArray(Seq(JsString("line1"),JsString("line2"),JsString("line3"),
          JsString("line4"))), "postcode" -> JsString("NE1"), "country" -> Json.obj("name" -> JsString("IT"))))
      "We have Contact Details" in {
        val result = details.as[CorrespondenceCommonDetail](CorrespondenceCommonDetail.apiReads)

        result.contactDetail.telephone mustBe correspondenceCommonDetails.contactDetail.telephone
      }

      "We have an address" in {
        val result = details.as[CorrespondenceCommonDetail](CorrespondenceCommonDetail.apiReads)

        result.addressDetail mustBe correspondenceCommonDetails.addressDetail
      }
    }
  }
}
