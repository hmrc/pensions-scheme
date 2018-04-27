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


import java.time.LocalDate

import models.{DirectorOrPartnerDetailTypeItem, IndividualDetailType, OrganisationDetailType, Samples}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.functional.syntax._
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsPath, JsString, Json, Reads}

class IndividualDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues with Samples {
  "A JSON payload containing the details of an individual" should {
    "Map correctly to an IndividualDetailsType model" when {

      val individual = Json.obj("firstName" -> JsString("John"),
        "lastName" -> JsString("Doe"),
        "middleName" -> JsString("Does Does"),
        "dateOfBirth" -> JsString("2019-01-31"))

      "We have a first name" in {
        val result = individual.as[IndividualDetailType](IndividualDetailType.apiReads)

        result.firstName mustBe individualSample.firstName
      }

      "We have a last name" in {
        val result = individual.as[IndividualDetailType](IndividualDetailType.apiReads)

        result.lastName mustBe individualSample.lastName
      }

      "We have a middle name " in {
        val result = individual.as[IndividualDetailType](IndividualDetailType.apiReads)

        result.middleName mustBe individualSample.middleName
      }

      "We have a date of birth" in {
        val result = individual.as[IndividualDetailType](IndividualDetailType.apiReads)

        result.dateOfBirth mustBe individualSample.dateOfBirth
      }

      "We don't have a title" in {
        val result = individual.as[IndividualDetailType](IndividualDetailType.apiReads)

        result.title mustBe None
      }
    }
  }
}
