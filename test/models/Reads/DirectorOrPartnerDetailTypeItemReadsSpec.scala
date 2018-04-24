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

import models.{DirectorOrPartnerDetailTypeItem, ForeignAddress, PreviousAddressDetails}
import org.joda.time.DateTime
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._
import play.api.libs.functional.syntax._


class DirectorOrPartnerDetailTypeItemReadsSpec extends WordSpec with MustMatchers with OptionValues {
  "JSON Payload of a Director" should  {
    "Map correctly into a DirectorOrPartnerDetailTypeItem" when {

      val directors = Json.obj("directorDetails" -> Json.obj("firstName" -> JsString("John"),
        "lastName" -> JsString("Doe"),
        "middleName" -> JsString("Does Does"),
        "dateOfBirth" -> JsString("2019-01-31")),
        "directorNino" -> Json.obj("hasNino" -> JsBoolean(true), "nino" -> JsString("SL211111A")),
        "directorUtr" -> Json.obj("hasUtr" -> JsBoolean(true), "utr" -> JsString("123456789")),
        "directorAddressYears" -> JsString("over_a_year")
      )

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
            "directorAddressYears" -> JsString("over_a_year"))

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
            "directorAddressYears" -> JsString("over_a_year"))

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
    }
  }

  val directorPersonalDetailsReads : Reads[(String,String,Option[String],DateTime)] = (
    (JsPath \ "firstName").read[String] and
      (JsPath \ "lastName").read[String] and
      (JsPath \ "middleName").readNullable[String] and
      (JsPath \ "dateOfBirth").read[DateTime]
  )((name,lastName,middleName,dateOfBirth) => (name,lastName,middleName,dateOfBirth))

  def directorReferenceReads(referenceFlag : String, referenceName: String) : Reads[(Option[Boolean],Option[String],Option[String])] = (
    (JsPath \ referenceFlag).readNullable[Boolean] and
      (JsPath \ referenceName).readNullable[String] and
      (JsPath \ "reason").readNullable[String]
    )((hasNino,nino,reason)=>(hasNino,nino,reason))

  val apiReads : Reads[DirectorOrPartnerDetailTypeItem] = (
    (JsPath \ "directorDetails").read(directorPersonalDetailsReads) and
      (JsPath \ "directorNino").readNullable(directorReferenceReads("hasNino","nino")) and
      (JsPath \ "directorUtr").readNullable(directorReferenceReads("hasUtr","utr")) and
      (JsPath).read(PreviousAddressDetails.apiReads("director"))
  )((directorPersonalDetails,ninoDetails,utrDetails,previousAddress)=>DirectorOrPartnerDetailTypeItem(sequenceId = "",
    entityType = "",
    title = None,
    firstName = directorPersonalDetails._1,
    middleName = directorPersonalDetails._3,
    lastName = directorPersonalDetails._2,
    dateOfBirth = directorPersonalDetails._4,
    referenceOrNino = ninoDetails.flatMap(details => if (details._1.fold(false)(c=>c)) details._2 else None),
    noNinoReason = ninoDetails.flatMap(details => if (details._1.fold(false)(c=>c) == false) details._3 else None),
    utr = utrDetails.flatMap(details => if (details._1.fold(false)(c=>c)) details._2 else None),
    noUtrReason = utrDetails.flatMap(details => if (details._1.fold(false)(c=>c) == false) details._3 else None),
    correspondenceCommonDetail = None,
    previousAddressDetail = previousAddress))

  val director = DirectorOrPartnerDetailTypeItem(sequenceId = "",
    entityType = "",
    title = None,
    firstName = "John",
    middleName = Some("Does Does"),
    lastName = "Doe",
    dateOfBirth = DateTime.parse("2019-01-31"),
    referenceOrNino = Some("SL211111A"),
    noNinoReason = Some("he can't find it"),
    utr = Some("123456789"),
    noUtrReason = Some("he can't find it"),
    correspondenceCommonDetail = None,
    previousAddressDetail = PreviousAddressDetails(isPreviousAddressLast12Month = false))
}
