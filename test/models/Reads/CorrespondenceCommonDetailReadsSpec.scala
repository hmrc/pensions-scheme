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

  import CorrespondenceCommonDetailReadsSpec._

  "JSON payload containing correspondence common details" should {
    Seq("director", "partner").foreach { personType =>

      s"correctly parse to a ${personType} CorrespondenceCommonDetails object" when {

        "We have Contact Details" in {
          val result = directorOrPartnerDetails(personType).as[CorrespondenceCommonDetail](CorrespondenceCommonDetail.apiReads(personType))

          result.contactDetail.telephone mustBe correspondenceCommonDetails.contactDetail.telephone
        }

        "We have an address" in {
          val result = directorOrPartnerDetails(personType).as[CorrespondenceCommonDetail](CorrespondenceCommonDetail.apiReads(personType))

          result.addressDetail mustBe correspondenceCommonDetails.addressDetail
        }
      }
    }
  }
}

object CorrespondenceCommonDetailReadsSpec {

  private def directorOrPartnerDetails(personType: String) = Json.obj(s"${personType}ContactDetails" -> Json.obj("email" -> "test@test.com",
    "phone" -> "07592113"),
    s"${personType}Address" -> Json.obj("addressLine1" -> JsString("line1"), "addressLine2" -> JsString("line2"),
      "addressLine3" -> JsString("line3"), "addressLine4" -> JsString("line4"), "postcode" -> JsString("NE1"), "country" -> JsString("IT")))
}