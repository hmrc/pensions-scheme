/*
 * Copyright 2019 HM Revenue & Customs
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

import models.AdviserDetails
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.Json

class AdviserDetailsSpec extends WordSpec with MustMatchers {
  "A JSON payload containing contact details" should {
    "Map to a valid ContactDetails object" when {
      val input = Json.obj("adviserName" -> "abc", "phoneNumber" -> "0758237281", "emailAddress" -> "test@test.com")

      "We have a name" in {
        val result: AdviserDetails = input.as[AdviserDetails](AdviserDetails.readsAdviserDetails)
        result.adviserName mustBe "abc"
      }
      "We have a telephone number" in {
        val result: AdviserDetails = input.as[AdviserDetails](AdviserDetails.readsAdviserDetails)
        result.phoneNumber mustBe "0758237281"
      }

      "We have an email address" in {
        val result: AdviserDetails = input.as[AdviserDetails](AdviserDetails.readsAdviserDetails)
        result.emailAddress mustBe "test@test.com"
      }
    }
  }

}
