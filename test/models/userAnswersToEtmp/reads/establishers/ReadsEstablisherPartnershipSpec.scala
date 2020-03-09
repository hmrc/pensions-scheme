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

import models.userAnswersToEtmp.Address
import models.userAnswersToEtmp.reads.CommonGenerator.establisherPartnershipGenerator
import models.userAnswersToEtmp.establisher.Partnership
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Shrink
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._

class ReadsEstablisherPartnershipSpec extends WordSpec with MustMatchers with OptionValues {
  private implicit def dontShrink[A]: Shrink[A] = Shrink.shrinkAny

  "A Json payload containing trustee partnership" should {
    "have partnership name read correctly" in {
      forAll(establisherPartnershipGenerator()) { json =>
        val transformedEstablisher = json.as[Partnership](Partnership.readsEstablisherPartnership)
        transformedEstablisher.organizationName mustBe (json \ "partnershipDetails" \ "name").as[String]
      }
    }

    "must read vat when it is present" in {
      forAll(establisherPartnershipGenerator(), arbitrary[String]) {
        (json, vat) =>
          val newJson = json + ("partnershipVat" -> Json.obj("value" -> vat))
          val model = newJson.as[Partnership](Partnership.readsEstablisherPartnership)
          model.vatRegistrationNumber.value mustBe (newJson \ "partnershipVat" \ "value").as[String]
      }
    }

    "must read paye when it is present" in {
      forAll(establisherPartnershipGenerator(), arbitrary[String]) {
        (json, paye) =>
          val newJson = json + ("partnershipPaye" -> Json.obj("value" -> paye))
          val model = newJson.as[Partnership](Partnership.readsEstablisherPartnership)
          model.payeReference.value mustBe (newJson \ "partnershipPaye" \ "value").as[String]
      }
    }

    "must read utr when it is present" in {
      forAll(establisherPartnershipGenerator(), arbitrary[String]) {
        (json, utr) =>
          val newJson = json + ("utr" -> Json.obj("value" -> utr))
          val model = newJson.as[Partnership](Partnership.readsEstablisherPartnership)
          model.utr.value mustBe (newJson \ "utr" \ "value").as[String]
      }
    }

    "must read no utr reason when it is present" in {
      forAll(establisherPartnershipGenerator(), arbitrary[String]) {
        (json, noUtrReason) =>
          val newJson = json + ("noUtrReason" -> JsString(noUtrReason))
          val model = newJson.as[Partnership](Partnership.readsEstablisherPartnership)
          model.noUtrReason.value mustBe (newJson \ "noUtrReason").as[String]
      }
    }

    "read partnership address correctly" in {
      forAll(establisherPartnershipGenerator()) { json =>
        val model = json.as[Partnership](Partnership.readsEstablisherPartnership)

        model.correspondenceAddressDetails.addressDetails mustBe
          (json \ "partnershipAddress").as[Address]
      }
    }

    "must read previous address when address years is under a year and trading time is true" in {
      forAll(establisherPartnershipGenerator(), "under_a_year", true) {
        (json, addressYears, hasBeenTrading) =>
          val newJson = json + ("partnershipAddressYears" -> JsString(addressYears)) + ("hasBeenTrading" -> JsBoolean(hasBeenTrading))
          val model = newJson.as[Partnership](Partnership.readsEstablisherPartnership)
          model.previousAddressDetails.value.isPreviousAddressLast12Month mustBe true
          model.previousAddressDetails.value.previousAddressDetails.value mustBe (json \ "partnershipPreviousAddress").as[Address]
      }
    }

    "must not read previous address when address years is not under a year" in {
      forAll(establisherPartnershipGenerator()) {
        json =>
          val model = json.as[Partnership](Partnership.readsEstablisherPartnership)
          model.previousAddressDetails mustBe None
      }
    }

    "must not read previous address when address years is under a year but trading time is false" in {
      forAll(establisherPartnershipGenerator(), false) {
        (json, hasBeenTrading) =>
          val newJson = json + ("hasBeenTrading" -> JsBoolean(hasBeenTrading))
          val model = newJson.as[Partnership](Partnership.readsEstablisherPartnership)
          model.previousAddressDetails mustBe None
      }
    }

    "read partnership contact details correctly" in {
      forAll(establisherPartnershipGenerator()) { json =>
        val model = json.as[Partnership](Partnership.readsEstablisherPartnership)

        model.correspondenceContactDetails.contactDetails.email mustBe
          (json \ "partnershipContactDetails" \ "emailAddress").as[String]

        model.correspondenceContactDetails.contactDetails.telephone mustBe
          (json \ "partnershipContactDetails" \ "phoneNumber").as[String]
      }
    }

    "read other partners flag correctly" in {
      forAll(establisherPartnershipGenerator()) { json =>
        val model = json.as[Partnership](Partnership.readsEstablisherPartnership)

        model.haveMoreThanTenDirectorOrPartner mustBe (json \ "otherPartners").as[Boolean]
      }
    }

    "read partners correctly" in {
      forAll(establisherPartnershipGenerator()) { json =>
        val model = json.as[Partnership](Partnership.readsEstablisherPartnership)

        model.partnerDetails.head.personalDetails.firstName mustBe
          (json \ "partner" \ 0 \ "partnerDetails" \ "firstName").as[String]

        model.partnerDetails(1).personalDetails.firstName mustBe
          (json \ "partner" \ 1 \ "partnerDetails" \ "firstName").as[String]
      }
    }
  }
}
