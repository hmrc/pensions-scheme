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

import play.api.libs.functional.syntax._
import models.{OrganisationDetailType, Samples}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsPath, JsString, Json, Reads}

class OrganisationDetailTypeSpec extends WordSpec with MustMatchers with OptionValues with Samples {
  "A JSON Payload containing organisation detials" should {
    "Map correctly to an OrganisationDetailTypeSpec" when {
      val companyDetails = Json.obj("companyDetails" -> Json.obj("vatRegistrationNumber" -> JsString("VAT11111"), "payeEmployerReferenceNumber" -> JsString("PAYE11111")),
        "companyRegistrationNumber" -> JsString("CRN11111"), "businessDetails" -> Json.obj("companyName" -> JsString("Company Test")))

      "We have a name" in {
        val result = companyDetails.as[OrganisationDetailType](OrganisationDetailType.apiReads)

        result.name mustBe companySample.name
      }

      "We have VAT registration number" in {
        val result = companyDetails.as[OrganisationDetailType](OrganisationDetailType.apiReads)

        result.vatRegistrationNumber mustBe companySample.vatRegistrationNumber
      }

      "We have a PAYE employer reference number" in {
        val result = companyDetails.as[OrganisationDetailType](OrganisationDetailType.apiReads)

        result.payeReference mustBe companySample.payeReference
      }

      "We have a Company Registration Number" in {
        val result = companyDetails.as[OrganisationDetailType](OrganisationDetailType.apiReads)

        result.crnNumber mustBe companySample.crnNumber
      }

      "We have no company details" in {
        val orgDetailsWithNoCompanyDetails = companyDetails - "companyDetails"

        val result = orgDetailsWithNoCompanyDetails.as[OrganisationDetailType](OrganisationDetailType.apiReads)

        result.vatRegistrationNumber mustBe None
      }


      "We have no VAT registration number" in {
        val companyDetails = Json.obj("companyDetails" -> Json.obj("payeEmployerReferenceNumber" -> JsString("PAYE11111")),
          "companyRegistrationNumber" -> JsString("CRN11111"), "businessDetails" -> Json.obj("companyName" -> JsString("Company Test")))

        val result = companyDetails.as[OrganisationDetailType](OrganisationDetailType.apiReads)

        result.vatRegistrationNumber mustBe None
      }

      "We have no payeEmployerReferenceNumber" in {
        val companyDetails = Json.obj("companyDetails" -> Json.obj("vatRegistrationNumber" -> JsString("PAYE11111")),
          "companyRegistrationNumber" -> JsString("CRN11111"), "businessDetails" -> Json.obj("companyName" -> JsString("Company Test")))

        val result = companyDetails.as[OrganisationDetailType](OrganisationDetailType.apiReads)

        result.payeReference mustBe None
      }
    }
  }
}
