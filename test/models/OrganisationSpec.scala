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

package models

import base.SpecBase
import play.api.libs.json.Json

class OrganisationSpec extends SpecBase {

  "Reads Organisation" must {
    "return Organisation" in {
      val json = Json.obj("organisationName" -> "Test Ltd", "organisationType" -> "Corporate Body")

      json.as[Organisation] mustEqual Organisation("Test Ltd", OrganisationTypeEnum.CorporateBody)
    }
  }

  "Writes Organisation" must {
    "return json" in {
      val org = Organisation("Test Ltd", OrganisationTypeEnum.CorporateBody)

      val jsonResult = Json.obj("organisationName" -> "Test Ltd", "organisationType" -> "Corporate Body")
      Json.toJson[Organisation](org) mustEqual jsonResult
    }
  }
}