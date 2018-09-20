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

package models.Reads.schemes

import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._
import play.api.libs.functional.syntax._

class SchemeDetailsReadsSpec extends WordSpec with MustMatchers with OptionValues {
  "A JSON payload containing scheme details" should {

    val schemeDetails = Json.obj("srn" -> JsString("AAABA932JASDA"),
      "pstr" -> JsString("A3DCADAA"),
      "schemeStatus" -> "Pending",
      "schemeName" -> "Test Scheme",
      "isSchemeMasterTrust" -> JsBoolean(true),
      "pensionSchemeStructure" -> "Other",
      "otherPensionSchemeStructure" -> "Other type",
      "hasMoreThanTenTrustees" -> JsBoolean(true))

    "correctly parse to a model of SchemeDetails" when {
      "we have a srn" in {
        val output = schemeDetails.as[SchemeDetails]

        output.srn.value mustBe (schemeDetails \ "srn").as[String]
      }

      "we don't have an srn" in {
        val output =  (schemeDetails - "srn").as[SchemeDetails]

        output.srn mustBe None
      }

      "we have a pstr" in {
        val output = schemeDetails.as[SchemeDetails]

        output.pstr.value mustBe (schemeDetails \ "pstr").as[String]
      }

      "we don't have pstr" in {
        val output = (schemeDetails - "pstr").as[SchemeDetails]

        output.pstr mustBe None
      }

      "we have a status" in {
        val output = schemeDetails.as[SchemeDetails]

        output.status mustBe (schemeDetails \ "schemeStatus").as[String]
      }

      "we have a name" in {
        val output = schemeDetails.as[SchemeDetails]

        output.name mustBe (schemeDetails \ "schemeName").as[String]
      }

      "we have a flag to say if it is a master trust" in {
        val output = schemeDetails.as[SchemeDetails]

        output.isMasterTrust mustBe (schemeDetails \ "isSchemeMasterTrust").as[Boolean]
      }

      "there is no flag to say it is a master trust so we assume it is not" in {
        val output = (schemeDetails - "isSchemeMasterTrust").as[SchemeDetails]

        output.isMasterTrust mustBe false
      }

      "we have a type of scheme" in {
        val output = schemeDetails.as[SchemeDetails]

        output.typeOfScheme.value mustBe (schemeDetails \ "pensionSchemeStructure").as[String]
      }

      "we don't have a type of scheme" in {
        val output = (schemeDetails - "pensionSchemeStructure").as[SchemeDetails]

        output.typeOfScheme mustBe None
      }

      "we have other types of schemes" in {
        val output = schemeDetails.as[SchemeDetails]

        output.otherTypeOfScheme.value mustBe (schemeDetails \ "otherPensionSchemeStructure").as[String]
      }

      "we don't have other types of scheme" in {
        val output = (schemeDetails - "otherPensionSchemeStructure").as[SchemeDetails]

        output.otherTypeOfScheme mustBe None
      }

      "we have a flag that tells us if there is more than 10 trustees" in {
        val output = schemeDetails.as[SchemeDetails]

        output.hasMoreThanTenTrustees mustBe (schemeDetails \ "hasMoreThanTenTrustees").as[Boolean]
      }

      "we don't have a flag that tells us if there is more than 10 trustees so we assume we haven't" in {
        val output = (schemeDetails - "hasMoreThanTenTrustees").as[SchemeDetails]

        output.hasMoreThanTenTrustees mustBe false
      }
    }
  }
}

case class SchemeDetails(srn: Option[String], pstr: Option[String], status: String, name: String, isMasterTrust: Boolean, typeOfScheme: Option[String], otherTypeOfScheme: Option[String], hasMoreThanTenTrustees: Boolean)

object SchemeDetails {
  implicit val reads : Reads[SchemeDetails] = (
    (JsPath \ "srn").readNullable[String] and
      (JsPath \ "pstr").readNullable[String] and
    (JsPath \ "schemeStatus").read[String] and
      (JsPath \ "schemeName").read[String] and
      (JsPath \ "isSchemeMasterTrust").readNullable[Boolean] and
      (JsPath \ "pensionSchemeStructure").readNullable[String] and
      (JsPath \ "otherPensionSchemeStructure").readNullable[String] and
      (JsPath \ "hasMoreThanTenTrustees").readNullable[Boolean]
  )((srn,pstr,status,name,isMasterTrust,typeOfScheme,otherTypeOfScheme,moreThan10Trustees) => SchemeDetails(srn,pstr,status,name,isMasterTrust.getOrElse(false),typeOfScheme,otherTypeOfScheme,moreThan10Trustees.getOrElse(false)))
  implicit val writes : Writes[SchemeDetails] = Json.writes[SchemeDetails]
}
