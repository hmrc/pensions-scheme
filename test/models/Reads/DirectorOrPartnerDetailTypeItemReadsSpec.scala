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
      
      val director = Json.obj("directorDetails" -> Json.obj("firstName" -> JsString("John"),
        "lastName" -> JsString("Doe"),
        "middleName" -> JsString("Does Does"),
        "dateOfBirth" -> JsString("2019-01-31")),
        "directorNino" -> Json.obj("hasNino" -> JsBoolean(true), "nino" -> JsString("SL211111A")),
        "directorUtr" -> Json.obj("hasUtr" -> JsBoolean(true), "utr" -> JsString("123456789")),
        "directorAddressYears" -> JsString("over_a_year")
      ) + ("directorContactDetails" -> Json.obj("email" -> "test@test.com", "phone" -> "07592113")) + ("directorAddress"->
        Json.obj("addressLine1" -> JsString("line1"), "addressLine2" -> JsString("line2"), "country" -> JsString("IT")))

      val directors = JsArray(Seq(director,director))

      "We have director user details" when {
        "We have a list of Directors" in {
          val result = directors.as[List[DirectorOrPartnerDetailTypeItem]](DirectorOrPartnerDetailTypeItem.apiReads)

          result.head.lastName mustBe directorSample.lastName
        }

        "We have a sequence id" in {
          val result = directors.as[List[DirectorOrPartnerDetailTypeItem]](DirectorOrPartnerDetailTypeItem.apiReads)
          result.head.sequenceId mustBe directorSample.sequenceId
        }

        "We have 10 directors" in {
          val directors = JsArray(Seq.tabulate(10)(_ => director))
          val result = directors.as[List[DirectorOrPartnerDetailTypeItem]](DirectorOrPartnerDetailTypeItem.apiReads)
          result.last.sequenceId mustBe "009"
        }

        "We have 100 directors" in {
          val directors = JsArray(Seq.tabulate(100)(_ => director))
          val result = directors.as[List[DirectorOrPartnerDetailTypeItem]](DirectorOrPartnerDetailTypeItem.apiReads)
          result.last.sequenceId mustBe "099"
        }

        "We have 101 directors" in {
          val directors = JsArray(Seq.tabulate(101)(_ => director))
          val result = directors.as[List[DirectorOrPartnerDetailTypeItem]](DirectorOrPartnerDetailTypeItem.apiReads)
          result.last.sequenceId mustBe "100"
        }

        "We have individual details" in {
          val result = directors.as[List[DirectorOrPartnerDetailTypeItem]](DirectorOrPartnerDetailTypeItem.apiReads)

          result.head.firstName mustBe directorSample.firstName
        }
      }

      "We have director NINO details" when {
        "We have a director nino" in {
          val result = directors.as[List[DirectorOrPartnerDetailTypeItem]](DirectorOrPartnerDetailTypeItem.apiReads)

          result.head.referenceOrNino mustBe directorSample.referenceOrNino
        }

        "We don't have a nino" in {
          val directorsNoNino = directors.value :+ (director + ("directorNino" -> Json.obj("hasNino" -> JsBoolean(false))))

          val result = JsArray(directorsNoNino).as[List[DirectorOrPartnerDetailTypeItem]](DirectorOrPartnerDetailTypeItem.apiReads)

          result.last.referenceOrNino mustBe None
        }

        "We have a reason for not having nino" in {
          val directorsNoNino = directors.value :+ (director + ("directorNino" -> Json.obj("reason" -> JsString("he can't find it"))))

          val result = JsArray(directorsNoNino).as[List[DirectorOrPartnerDetailTypeItem]](DirectorOrPartnerDetailTypeItem.apiReads)

          result.last.noNinoReason mustBe directorSample.noNinoReason
        }
      }

      "We have director UTR details" when {
        "We have a director utr" in {
          val result = directors.as[List[DirectorOrPartnerDetailTypeItem]](DirectorOrPartnerDetailTypeItem.apiReads)

          result.head.utr mustBe directorSample.utr
        }

        "There is no UTR" in {
          val directorNoUtr = directors.value :+ (director + ("directorUtr" -> Json.obj("hasUtr" -> JsBoolean(false))))

          val result = JsArray(directorNoUtr).as[List[DirectorOrPartnerDetailTypeItem]](DirectorOrPartnerDetailTypeItem.apiReads)

          result.last.utr mustBe None
        }

        "We have a reason for not having utr" in {
          val directorNoUtr = directors.value :+ (director + ("directorUtr" -> Json.obj("reason" -> JsString("he can't find it"))))

          val result = JsArray(directorNoUtr).as[List[DirectorOrPartnerDetailTypeItem]](DirectorOrPartnerDetailTypeItem.apiReads)

          result.last.noUtrReason mustBe directorSample.noUtrReason
        }
      }

      "We have entity type as Director" in {
        val result = directors.as[List[DirectorOrPartnerDetailTypeItem]](DirectorOrPartnerDetailTypeItem.apiReads)

        result.head.entityType mustBe directorSample.entityType
      }

      "We have a previous address detail" in {
        val directorWithPreviousAddress = directors.value :+ (director + ("directorAddressYears" -> JsString("under_a_year")) +
          ("directorPreviousAddress"->  Json.obj("addressLine1" -> JsString("line1"), "addressLine2" -> JsString("line2"), "country" -> JsString("IT"))))


        val result = JsArray(directorWithPreviousAddress).as[List[DirectorOrPartnerDetailTypeItem]](DirectorOrPartnerDetailTypeItem.apiReads)
        val expectedDirector = directorSample.copy(previousAddressDetail = PreviousAddressDetails(true,Some(InternationalAddress("line1",Some("line2"),countryCode = "IT"))))

        result.last.previousAddressDetail mustBe expectedDirector.previousAddressDetail
      }

      "We have a correspondence common detail" in {
        val directorWithCorrespondenceCommonDetail = directors.value :+ (director + ("directorContactDetails" -> Json.obj("email" -> "test@test.com", "phone" -> "07592113")) + ("directorAddress"->
          Json.obj("addressLine1" -> JsString("line1"), "addressLine2" -> JsString("line2"),
            "addressLine3" -> JsString("line3"), "addressLine4" -> JsString("line4"),"postcode" -> JsString("NE1"),"country" -> JsString("IT"))))

        val result = JsArray(directorWithCorrespondenceCommonDetail).as[List[DirectorOrPartnerDetailTypeItem]](DirectorOrPartnerDetailTypeItem.apiReads)
        val expectedDirector = directorSample.copy(correspondenceCommonDetail = correspondenceCommonDetails)

        result.last.correspondenceCommonDetail mustBe expectedDirector.correspondenceCommonDetail
      }
    }
  }
}
