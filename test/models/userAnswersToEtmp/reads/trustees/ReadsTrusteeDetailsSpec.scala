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

package models.userAnswersToEtmp.reads.trustees

import models.userAnswersToEtmp.reads.CommonGenerator
import models.userAnswersToEtmp.reads.CommonGenerator.trusteesGen
import models.userAnswersToEtmp.trustee.TrusteeDetails
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{OptionValues, MustMatchers, WordSpec}
import play.api.libs.json.Json

class ReadsTrusteeDetailsSpec extends WordSpec with MustMatchers with OptionValues {

  "ReadsTrusteeDetails" must {

    "read multiple trustees with filtering out the deleted ones" in {
      forAll(trusteesGen) { json =>
        val trusteeDetails = json.as[TrusteeDetails](TrusteeDetails.readsTrusteeDetails)

        trusteeDetails.companyTrusteeDetail.head.organizationName mustBe
          (json \ "trustees" \ 0 \ "companyDetails" \ "companyName").as[String]

        trusteeDetails.individualTrusteeDetail.head.personalDetails.firstName mustBe
          (json \ "trustees" \ 3 \ "trusteeDetails" \ "firstName").as[String]

        trusteeDetails.partnershipTrusteeDetail.head.organizationName mustBe
          (json \ "trustees" \ 4 \ "partnershipDetails" \ "name").as[String]
      }
    }



    "read individual trustee which includes a companyDetails node (due to url manipulation - fix for production issue)" in {
      forAll(CommonGenerator.trusteeIndividualGenerator()) { individualTrustee =>
        val json = Json.obj(
          "trustees" -> Json.arr(
            individualTrustee ++ Json.obj(
              "companyDetails" -> Json.obj()
            )
          )
        )

        val trusteeDetails = json.as[TrusteeDetails](TrusteeDetails.readsTrusteeDetails)

        trusteeDetails.individualTrusteeDetail.head.personalDetails.firstName mustBe
          (json \ "trustees" \ 0 \ "trusteeDetails" \ "firstName").as[String]
      }
    }

    "read individual trustee which includes a partnershipDetails node (due to url manipulation - fix for production issue)" in {
      forAll(CommonGenerator.trusteeIndividualGenerator()) { individualTrustee =>
        val json = Json.obj(
          "trustees" -> Json.arr(
            individualTrustee ++ Json.obj(
              "partnershipDetails" -> Json.obj()
            )
          )
        )

        val trusteeDetails = json.as[TrusteeDetails](TrusteeDetails.readsTrusteeDetails)

        trusteeDetails.individualTrusteeDetail.head.personalDetails.firstName mustBe
          (json \ "trustees" \ 0 \ "trusteeDetails" \ "firstName").as[String]
      }
    }

    "read company trustee which includes an trusteeDetails node (due to url manipulation - fix for production issue)" in {
      forAll(CommonGenerator.trusteeCompanyGenerator()) { companyTrustee =>
        val json = Json.obj(
          "trustees" -> Json.arr(
            companyTrustee ++ Json.obj(
              "trusteeDetails" -> Json.obj()
            )
          )
        )

        val trusteeDetails = json.as[TrusteeDetails](TrusteeDetails.readsTrusteeDetails)

        trusteeDetails.companyTrusteeDetail.head.organizationName mustBe
          (json \ "trustees" \ 0 \ "companyDetails" \ "companyName").as[String]
      }
    }

    "read partnership trustee which includes an trusteeDetails node (due to url manipulation - fix for production issue)" in {
      forAll(CommonGenerator.trusteePartnershipGenerator()) { partnershipTrustee =>
        val json = Json.obj(
          "trustees" -> Json.arr(
            partnershipTrustee ++ Json.obj(
              "trusteeDetails" -> Json.obj()
            )
          )
        )

        val trusteeDetails = json.as[TrusteeDetails](TrusteeDetails.readsTrusteeDetails)

        trusteeDetails.partnershipTrusteeDetail.head.organizationName mustBe
          (json \ "trustees" \ 0 \ "partnershipDetails" \ "name").as[String]
      }
    }
  }








}


