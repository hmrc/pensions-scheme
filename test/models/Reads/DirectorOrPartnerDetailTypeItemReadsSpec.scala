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

import models.DirectorOrPartnerDetailTypeItem
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
        "directorUtr" -> Json.obj("hasUtr" -> JsBoolean(true), "utr" -> JsString("123456789"))
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
            "directorUtr" -> Json.obj("hasUtr" -> JsBoolean(false)))

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
            "directorNino" -> Json.obj("hasNino" -> JsBoolean(false)),
            "directorUtr" -> Json.obj("hasUtr" -> JsBoolean(false), "reason" -> JsString("he can't find it")))

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
    }
  }

  val apiReads : Reads[DirectorOrPartnerDetailTypeItem] = (
    (JsPath \ "directorDetails" \ "firstName").read[String] and
      (JsPath \ "directorDetails" \ "lastName").read[String] and
      (JsPath \ "directorDetails" \ "middleName").readNullable[String] and
      (JsPath \ "directorDetails" \ "dateOfBirth").read[DateTime] and
      (JsPath \ "directorNino" \ "hasNino").readNullable[Boolean] and
      (JsPath \ "directorNino" \ "nino").readNullable[String] and
      (JsPath \ "directorNino" \ "reason").readNullable[String] and
      (JsPath \ "directorUtr" \ "hasUtr").readNullable[Boolean] and
      (JsPath \ "directorUtr" \ "utr").readNullable[String] and
      (JsPath \ "directorUtr" \ "reason").readNullable[String]
  )((name,lastName,middleName,dateOfBirth,hasNino,nino,reasonForNoNino, hasUtr,utr,reasonForNoUtr)=>DirectorOrPartnerDetailTypeItem(sequenceId = "",
    entityType = "",
    title = None,
    firstName = name,
    middleName = middleName,
    lastName = lastName,
    dateOfBirth = dateOfBirth,
    referenceOrNino = if (hasNino.fold(false)(c=>c)) nino else None,
    noNinoReason = if (hasNino.fold(false)(c=>c) == false) reasonForNoNino else None,
    utr = if (hasUtr.fold(false)(c=>c)) utr else None,
    noUtrReason = if (hasUtr.fold(false)(c=>c) == false) reasonForNoUtr else None,
    correspondenceCommonDetail = None,
    previousAddressDetail = None))


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
    previousAddressDetail = None)
}
