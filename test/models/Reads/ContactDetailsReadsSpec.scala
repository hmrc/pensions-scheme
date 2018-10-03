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

import models.ContactDetails
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsObject, Json}

class ContactDetailsReadsSpec extends CommonContactDetailsReads {

  "A JSON payload containing contact details" should {

    "Map to a valid ContactDetails object" when {

      val input = Json.obj("phone" -> "0758237281", "email" -> "test@test.com")

      behave like commonContactDetails(input)

    }
  }
}

trait CommonContactDetailsReads extends WordSpec with MustMatchers with OptionValues {

  def commonContactDetails(contactDetails: JsObject): Unit ={

    val result =  contactDetails.as[ContactDetails](ContactDetails.apiReads)

    "A JSON payload containing contact details" should {

      "read into a valid contact details object " when {

        "we have a email" in {
          result.email mustBe (contactDetails \ "email").as[String]
        }

        "we have a phone" in {
          result.telephone mustBe (contactDetails \ "phone").as[String]
        }
      }
    }
  }
}
