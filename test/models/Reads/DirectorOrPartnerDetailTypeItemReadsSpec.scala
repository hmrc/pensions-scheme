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
import org.joda.time.DateTime
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._
import play.api.libs.functional.syntax._


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
          val result = directors.as[DirectorOrPartnerDetailTypeItem](apiReads)

          result.firstName mustBe director.firstName
        }

        "We have a last name" in {
          val result = directors.as[DirectorOrPartnerDetailTypeItem](apiReads)

          result.lastName mustBe director.lastName
        }

        "We have a middle name " in {
          val result = directors.as[DirectorOrPartnerDetailTypeItem](apiReads)

          result.middleName mustBe director.middleName
        }

        "We have a date of birth" in {
          val result = directors.as[DirectorOrPartnerDetailTypeItem](apiReads)

          result.dateOfBirth mustBe director.dateOfBirth
        }
      }

      "We have director NINO details" when {
        "We have a director nino" in {
          val result = directors.as[DirectorOrPartnerDetailTypeItem](apiReads)

          result.referenceOrNino mustBe director.referenceOrNino
        }

        "Has nino flag is set to false" when {
          val directors = Json.obj("directorDetails" -> Json.obj("firstName" -> JsString("John"),
            "lastName" -> JsString("Doe"),
            "middleName" -> JsString("Does Does"),
            "dateOfBirth" -> JsString("2019-01-31")),
            "directorNino" -> Json.obj("hasNino" -> JsBoolean(false), "reason" -> JsString("he can't find it")),
            "directorAddressYears" -> JsString("over_a_year")) + ("directorContactDetails" -> Json.obj("email" -> "test@test.com", "phone" -> "07592113")) + ("directorAddress"->
            Json.obj("lines" -> JsArray(Seq(JsString("line1"),JsString("line2"))),
              "country" -> JsObject(Map("name" -> JsString("IT")))))

          "Nino is not displayed" in {
            val result = directors.as[DirectorOrPartnerDetailTypeItem](apiReads)

            result.referenceOrNino mustBe None
          }

          "We have a reason for not having nino" in {
            val result = directors.as[DirectorOrPartnerDetailTypeItem](apiReads)

            result.noNinoReason mustBe director.noNinoReason
          }
        }
      }

      "We have director UTR details" when {
        "We have a director utr" in {
          val result = directors.as[DirectorOrPartnerDetailTypeItem](apiReads)

          result.utr mustBe director.utr
        }

        "Has utr flag is set to false" when {
          val directors = Json.obj("directorDetails" -> Json.obj("firstName" -> JsString("John"),
            "lastName" -> JsString("Doe"),
            "middleName" -> JsString("Does Does"),
            "dateOfBirth" -> JsString("2019-01-31")),
            "directorUtr" -> Json.obj("hasUtr" -> JsBoolean(false), "reason" -> JsString("he can't find it")),
            "directorAddressYears" -> JsString("over_a_year")) + ("directorContactDetails" -> Json.obj("email" -> "test@test.com", "phone" -> "07592113")) + ("directorAddress"->
            Json.obj("lines" -> JsArray(Seq(JsString("line1"),JsString("line2"))),
              "country" -> JsObject(Map("name" -> JsString("IT")))))

          "Utr is not displayed" in {
            val result = directors.as[DirectorOrPartnerDetailTypeItem](apiReads)

            result.utr mustBe None
          }

          "We have a reason for not having utr" in {
            val result = directors.as[DirectorOrPartnerDetailTypeItem](apiReads)

            result.noUtrReason mustBe director.noUtrReason
          }
        }
      }

      "We have a previous address detail" in {
        val directorWithPreviousAddress: JsValue = directors +
          ("directorAddressYears" -> JsString("under_a_year")) +
          ("directorPreviousAddress"->  Json.obj("lines" -> JsArray(Seq(JsString("line1"),JsString("line2"))),
          "country" -> JsObject(Map("name" -> JsString("IT")))))

        val result = directorWithPreviousAddress.as[DirectorOrPartnerDetailTypeItem](apiReads)
        val expectedDirector = director.copy(previousAddressDetail = PreviousAddressDetails(true,Some(ForeignAddress("line1",Some("line2"),countryCode = "IT"))))

        result.previousAddressDetail mustBe expectedDirector.previousAddressDetail
      }

      "We have a correspondence common detail" in {
        val directorWithCorrespondenceCommonDetail = directors + ("directorContactDetails" -> Json.obj("email" -> "test@test.com", "phone" -> "07592113")) + ("directorAddress"->
          Json.obj("lines" -> JsArray(Seq(JsString("line1"),JsString("line2"),JsString("line3"),JsString("line4"))),
          "country" -> JsObject(Map("name" -> JsString("IT"))), "postcode" -> JsString("NE1")))

        val result = directorWithCorrespondenceCommonDetail.as[DirectorOrPartnerDetailTypeItem](apiReads)
        val expectedDirector = director.copy(correspondenceCommonDetail = correspondenceCommonDetails)

        result.correspondenceCommonDetail mustBe expectedDirector.correspondenceCommonDetail
      }
    }
  }

  val directorPersonalDetailsReads : Reads[(String,String,Option[String],DateTime)] = (
    (JsPath \ "firstName").read[String] and
      (JsPath \ "lastName").read[String] and
      (JsPath \ "middleName").readNullable[String] and
      (JsPath \ "dateOfBirth").read[DateTime]
  )((name,lastName,middleName,dateOfBirth) => (name,lastName,middleName,dateOfBirth))

  def directorReferenceReads(referenceFlag : String, referenceName: String) : Reads[(Option[String],Option[String])] = (
      (JsPath \ referenceName).readNullable[String] and
      (JsPath \ "reason").readNullable[String]
    )((referenceNumber,reason)=>(referenceNumber,reason))

  val apiReads : Reads[DirectorOrPartnerDetailTypeItem] = (
    (JsPath \ "directorDetails").read(directorPersonalDetailsReads) and
      (JsPath \ "directorNino").readNullable(directorReferenceReads("hasNino","nino")) and
      (JsPath \ "directorUtr").readNullable(directorReferenceReads("hasUtr","utr")) and
      (JsPath).read(PreviousAddressDetails.apiReads("director")) and
      (JsPath).read(CorrespondenceCommonDetail.apiReads)
  )((directorPersonalDetails,ninoDetails,utrDetails,previousAddress, addressCommonDetails)=>DirectorOrPartnerDetailTypeItem(sequenceId = "",
    entityType = "",
    title = None,
    firstName = directorPersonalDetails._1,
    middleName = directorPersonalDetails._3,
    lastName = directorPersonalDetails._2,
    dateOfBirth = directorPersonalDetails._4,
    referenceOrNino = ninoDetails.flatMap(_._1),
    noNinoReason = ninoDetails.flatMap(_._2),
    utr = utrDetails.flatMap(_._1),
    noUtrReason = utrDetails.flatMap(_._2),
    correspondenceCommonDetail = addressCommonDetails,
    previousAddressDetail = previousAddress))
}
