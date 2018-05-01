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

import models.{Reads => _, _}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{Json, _}

class PensionAdvisorDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples {

  "A JSON Payload containing a pension advisor detail" should {
    "Map correctly to a valid representation of a PensionAdvisorDetail" when {

      val input = Json.obj("advisorDetails" -> Json.obj("name" -> JsString("John"),"phone" -> "07592113", "email" -> "test@test.com"), "advisorAddress" -> Json.obj("addressLine1" -> JsString("line1"), "addressLine2" -> JsString("line2"), "addressLine3" -> JsString("line3"), "addressLine4" -> JsString("line4"),
        "postalCode" -> JsString("NE1"), "countryCode" -> JsString("GB")))

      "We have a name" in {
        val result = input.as[PensionAdvisorDetail](apiReads)

        result.name mustBe pensionAdvisorSample.name
      }

      "We have an address" in {
        val result = input.as[PensionAdvisorDetail](apiReads)

        result.addressDetail mustBe pensionAdvisorSample.addressDetail
      }

      "We have advisor contact details" in {
        val result = input.as[PensionAdvisorDetail](apiReads)

        result.contactDetail mustBe pensionAdvisorSample.contactDetail
      }
    }
  }

  val apiReads : Reads[PensionAdvisorDetail] = (
    (JsPath \ "advisorDetails" \ "name").read[String] and
      (JsPath \ "advisorAddress").read[Address] and
      (JsPath \ "advisorDetails").read(ContactDetails.apiReads)
  )((name,address,contactDetails)=>PensionAdvisorDetail(name,address,contactDetails))

  val pensionAdvisorSample = PensionAdvisorDetail("John",ukAddressSample,contactDetailsSample)
}
