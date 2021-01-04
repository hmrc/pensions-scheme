/*
 * Copyright 2021 HM Revenue & Customs
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

import models.userAnswersToEtmp.reads.CommonGenerator.trusteePartnershipGenerator
import models.userAnswersToEtmp.trustee.PartnershipTrustee
import models.userAnswersToEtmp._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Shrink
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsBoolean, JsString, Json}

class ReadsTrusteePartnershipSpec extends WordSpec with MustMatchers with OptionValues {
  private implicit def dontShrink[A]: Shrink[A] = Shrink.shrinkAny

  "A Json payload containing trustee partnership" must {

    "have partnership name read correctly" in {
      forAll(trusteePartnershipGenerator()) { json =>
        val transformedTrustee = json.as[PartnershipTrustee](PartnershipTrustee.readsTrusteePartnership)
        transformedTrustee.organizationName mustBe (json \ "partnershipDetails" \ "name").as[String]
      }
    }

    "read utr when it is present" in {
      forAll(trusteePartnershipGenerator(), arbitrary[String]) {
        (json, utr) =>
          val newJson = json + ("utr" -> Json.obj("value" -> utr))
          val model = newJson.as[PartnershipTrustee](PartnershipTrustee.readsTrusteePartnership)
          model.utr.value mustBe (newJson \ "utr" \ "value").as[String]
      }
    }

    "read no utr reason when it is present" in {
      forAll(trusteePartnershipGenerator(), arbitrary[String]) {
        (json, noUtrReason) =>
          val newJson = json + ("noUtrReason" -> JsString(noUtrReason))
          val model = newJson.as[PartnershipTrustee](PartnershipTrustee.readsTrusteePartnership)
          model.noUtrReason.value mustBe (newJson \ "noUtrReason").as[String]
      }
    }

    "read vat when it is present" in {
      forAll(trusteePartnershipGenerator(), arbitrary[String]) {
        (json, vat) =>
          val newJson = json + ("partnershipVat" -> Json.obj("value" -> vat))
          val model = newJson.as[PartnershipTrustee](PartnershipTrustee.readsTrusteePartnership)
          model.vatRegistrationNumber.value mustBe (newJson \ "partnershipVat" \ "value").as[String]
      }
    }

    "read paye when it is present" in {
      forAll(trusteePartnershipGenerator(), arbitrary[String]) {
        (json, paye) =>
          val newJson = json + ("partnershipPaye" -> Json.obj("value" -> paye))
          val model = newJson.as[PartnershipTrustee](PartnershipTrustee.readsTrusteePartnership)
          model.payeReference.value mustBe (newJson \ "partnershipPaye" \ "value").as[String]
      }
    }

    "read previous address when address years is under a year" in {
      forAll(trusteePartnershipGenerator(), "under_a_year") {
        (json, addressYears) =>
          val newJson = json + ("partnershipAddressYears" -> JsString(addressYears))
          val model = newJson.as[PartnershipTrustee](PartnershipTrustee.readsTrusteePartnership)
          model.previousAddressDetails.value.isPreviousAddressLast12Month mustBe true
          model.previousAddressDetails.value.previousAddressDetails.value mustBe (json \ "partnershipPreviousAddress").as[Address]
      }
    }

    "read no previous address when address years is not under a year" in {
      forAll(trusteePartnershipGenerator()) {
        json =>
          val model = json.as[PartnershipTrustee](PartnershipTrustee.readsTrusteePartnership)
          model.previousAddressDetails mustBe None
      }
    }

    "read company address correctly" in {
      forAll(trusteePartnershipGenerator()) { json =>
        val model = json.as[PartnershipTrustee](PartnershipTrustee.readsTrusteePartnership)

        model.correspondenceAddressDetails.addressDetails mustBe (json \ "partnershipAddress").as[Address]
      }
    }

    "have partnership contact details read correctly" in {
      forAll(trusteePartnershipGenerator()) { json =>
        val model = json.as[PartnershipTrustee](PartnershipTrustee.readsTrusteePartnership)
        model.correspondenceContactDetails.contactDetails.email mustBe
          (json \ "partnershipContactDetails" \ "emailAddress").as[String]

        model.correspondenceContactDetails.contactDetails.telephone mustBe
          (json \ "partnershipContactDetails" \ "phoneNumber").as[String]
      }
    }
  }
}
