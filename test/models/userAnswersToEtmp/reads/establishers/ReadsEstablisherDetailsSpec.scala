/*
 * Copyright 2020 HM Revenue & Customs
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

package models.userAnswersToEtmp.reads.establishers

import models.userAnswersToEtmp.reads.CommonGenerator.establishersGen
import models.userAnswersToEtmp.establisher.EstablisherDetails
import models.userAnswersToEtmp.reads.CommonGenerator
import models.userAnswersToEtmp.reads.CommonGenerator.establisherCompanyGenerator
import models.userAnswersToEtmp.reads.CommonGenerator.establisherPartnershipGenerator
import models.userAnswersToEtmp.reads.CommonGenerator.establisherIndividualGenerator
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{OptionValues, MustMatchers, WordSpec}
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.JsPath
import play.api.libs.json.Json

class ReadsEstablisherDetailsSpec
  extends WordSpec
    with MustMatchers
    with OptionValues {

  "ReadsEstablisherDetails" must {

    "read multiple establishers with filtering out the deleted ones" in {
      forAll(establishersGen) { json =>
        val estDetails = json.as[EstablisherDetails](EstablisherDetails.readsEstablisherDetails)

        estDetails.companyOrOrganization.head.organizationName mustBe
          (json \ "establishers" \ 0 \ "companyDetails" \ "companyName").as[String]

        estDetails.individual.head.personalDetails.firstName mustBe
          (json \ "establishers" \ 3 \ "establisherDetails" \ "firstName").as[String]

        estDetails.partnership.head.organizationName mustBe
          (json \ "establishers" \ 4 \ "partnershipDetails" \ "name").as[String]
      }
    }

    "read individual establisher which includes a companyDetails node" in {
      forAll(establisherIndividualGenerator()) { individualEstablisher =>
        val json = Json.obj(
          "establishers" -> Json.arr(
            individualEstablisher ++ Json.obj(
              "companyDetails" -> Json.obj(
                "companyName" -> "bla"
              )
            )
          )
        )

        val estDetails = json.as[EstablisherDetails](EstablisherDetails.readsEstablisherDetails)

        estDetails.individual.head.personalDetails.firstName mustBe
          (json \ "establishers" \ 0 \ "establisherDetails" \ "firstName").as[String]
      }
    }

    "read individual establisher which includes a partnershipDetails node" in {
      forAll(establisherIndividualGenerator()) { individualEstablisher =>
        val json = Json.obj(
          "establishers" -> Json.arr(
            individualEstablisher ++ Json.obj(
              "partnershipDetails" -> Json.obj(
                "name" -> "bla"
              )
            )
          )
        )

        val estDetails = json.as[EstablisherDetails](EstablisherDetails.readsEstablisherDetails)

        estDetails.individual.head.personalDetails.firstName mustBe
          (json \ "establishers" \ 0 \ "establisherDetails" \ "firstName").as[String]
      }
    }


    "read company establisher which includes an establisherDetails node" in {
      forAll(establisherCompanyGenerator()) { companyEstablisher =>
        val json = Json.obj(
          "establishers" -> Json.arr(
            companyEstablisher ++ Json.obj(
              "establisherDetails" -> Json.obj(
                "firstName" -> "bla",
                "lastName" -> "bla"
              )
            )
          )
        )

        val estDetails = json.as[EstablisherDetails](EstablisherDetails.readsEstablisherDetails)

        estDetails.companyOrOrganization.head.organizationName mustBe
          (json \ "establishers" \ 0 \ "companyDetails" \ "companyName").as[String]
      }
    }

    "read partnership establisher which includes an establisherDetails node" in {
      forAll(establisherPartnershipGenerator()) { partnershipEstablisher =>
        val json = Json.obj(
          "establishers" -> Json.arr(
            partnershipEstablisher ++ Json.obj(
              "establisherDetails" -> Json.obj(
                "firstName" -> "bla",
                "lastName" -> "bla"
              )
            )
          )
        )

        val estDetails = json.as[EstablisherDetails](EstablisherDetails.readsEstablisherDetails)

        estDetails.partnership.head.organizationName mustBe
          (json \ "establishers" \ 0 \ "partnershipDetails" \ "name").as[String]
      }
    }
  }
}
