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
import play.api.libs.json._


class DirectorOrPartnerDetailTypeItemReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples {
  "JSON Payload of a Director" should  {
    "Map correctly into a DirectorOrPartnerDetailTypeItem" when {
      
      val directors = Json.obj("directorDetails" -> Json.obj("firstName" -> JsString("John"),
        "lastName" -> JsString("Doe"),
        "middleName" -> JsString("Does Does"),
        "dateOfBirth" -> JsString("2019-01-31")),
        "directorNino" -> Json.obj("hasNino" -> JsBoolean(true), "nino" -> JsString("SL211111A")),
        "directorUtr" -> Json.obj("hasUtr" -> JsBoolean(true), "utr" -> JsString("123456789")),
        "directorAddressYears" -> JsString("over_a_year")
      ) + ("directorContactDetails" -> Json.obj("email" -> "test@test.com", "phone" -> "07592113")) + ("directorAddress"->
        Json.obj("lines" -> JsArray(Seq(JsString("line1"),JsString("line2"))),
          "country" -> JsObject(Map("name" -> JsString("IT")))))

      "We have director user details" when {
        "We have a first name" in {
          val result = directors.as[DirectorOrPartnerDetailTypeItem](DirectorOrPartnerDetailTypeItem.apiReads)

          result.firstName mustBe director.firstName
        }

        "We have a last name" in {
          val result = directors.as[DirectorOrPartnerDetailTypeItem](DirectorOrPartnerDetailTypeItem.apiReads)

          result.lastName mustBe director.lastName
        }

        "We have a middle name " in {
          val result = directors.as[DirectorOrPartnerDetailTypeItem](DirectorOrPartnerDetailTypeItem.apiReads)

          result.middleName mustBe director.middleName
        }

        "We have a date of birth" in {
          val result = directors.as[DirectorOrPartnerDetailTypeItem](DirectorOrPartnerDetailTypeItem.apiReads)

          result.dateOfBirth mustBe director.dateOfBirth
        }
      }

      "We have director NINO details" when {
        "We have a director nino" in {
          val result = directors.as[DirectorOrPartnerDetailTypeItem](DirectorOrPartnerDetailTypeItem.apiReads)

          result.referenceOrNino mustBe director.referenceOrNino
        }

        "We don't have a nino" in {
          val directorNoNino = directors + ("directorNino" -> Json.obj("hasNino" -> JsBoolean(false)))

          val result = directorNoNino.as[DirectorOrPartnerDetailTypeItem](DirectorOrPartnerDetailTypeItem.apiReads)

          result.referenceOrNino mustBe None
        }

        "We have a reason for not having nino" in {
          val directorNoNino = directors + ("directorNino" -> Json.obj("reason" -> JsString("he can't find it")))

          val result = directorNoNino.as[DirectorOrPartnerDetailTypeItem](DirectorOrPartnerDetailTypeItem.apiReads)

          result.noNinoReason mustBe director.noNinoReason
        }
      }

      "We have director UTR details" when {
        "We have a director utr" in {
          val result = directors.as[DirectorOrPartnerDetailTypeItem](DirectorOrPartnerDetailTypeItem.apiReads)

          result.utr mustBe director.utr
        }

        "There is no UTR" in {
          val directorNoUtr = directors + ("directorUtr" -> Json.obj("hasUtr" -> JsBoolean(false)))

          val result = directorNoUtr.as[DirectorOrPartnerDetailTypeItem](DirectorOrPartnerDetailTypeItem.apiReads)

          result.utr mustBe None
        }

        "We have a reason for not having utr" in {
          val directorNoUtr = directors + ("directorUtr" -> Json.obj("reason" -> JsString("he can't find it")))

          val result = directorNoUtr.as[DirectorOrPartnerDetailTypeItem](DirectorOrPartnerDetailTypeItem.apiReads)

          result.noUtrReason mustBe director.noUtrReason
        }
      }

      "We have a previous address detail" in {
        val directorWithPreviousAddress: JsValue = directors +
          ("directorAddressYears" -> JsString("under_a_year")) +
          ("directorPreviousAddress"->  Json.obj("lines" -> JsArray(Seq(JsString("line1"),JsString("line2"))),
          "country" -> JsObject(Map("name" -> JsString("IT")))))

        val result = directorWithPreviousAddress.as[DirectorOrPartnerDetailTypeItem](DirectorOrPartnerDetailTypeItem.apiReads)
        val expectedDirector = director.copy(previousAddressDetail = PreviousAddressDetails(true,Some(ForeignAddress("line1",Some("line2"),countryCode = "IT"))))

        result.previousAddressDetail mustBe expectedDirector.previousAddressDetail
      }

      "We have a correspondence common detail" in {
        val directorWithCorrespondenceCommonDetail = directors + ("directorContactDetails" -> Json.obj("email" -> "test@test.com", "phone" -> "07592113")) + ("directorAddress"->
          Json.obj("lines" -> JsArray(Seq(JsString("line1"),JsString("line2"),JsString("line3"),JsString("line4"))),
          "country" -> JsObject(Map("name" -> JsString("IT"))), "postcode" -> JsString("NE1")))

        val result = directorWithCorrespondenceCommonDetail.as[DirectorOrPartnerDetailTypeItem](DirectorOrPartnerDetailTypeItem.apiReads)
        val expectedDirector = director.copy(correspondenceCommonDetail = correspondenceCommonDetails)

        result.correspondenceCommonDetail mustBe expectedDirector.correspondenceCommonDetail
      }
    }
  }
}
