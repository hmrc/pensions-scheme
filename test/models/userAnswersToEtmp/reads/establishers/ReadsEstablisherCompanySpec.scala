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
import models.userAnswersToEtmp.establisher.CompanyEstablisher
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Shrink
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._
import models.userAnswersToEtmp.reads.CommonGenerator.establisherCompanyGenerator

class ReadsEstablisherCompanySpec extends WordSpec with MustMatchers with OptionValues {

  implicit def dontShrink[A]: Shrink[A] = Shrink.shrinkAny

  "A Json payload containing establisher company" should {

    "read company name correctly" in {
      forAll(establisherCompanyGenerator()) { json =>
        val model = json.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)

        model.organizationName mustBe (json \ "companyDetails" \ "companyName").as[String]
      }
    }

    "must read vat when it is present" in {
      forAll(establisherCompanyGenerator(), arbitrary[String]) {
        (json, vat) =>
          val newJson = json + ("companyVat" -> Json.obj("value" -> vat))
          val model = newJson.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)
          model.vatRegistrationNumber.value mustBe (newJson \ "companyVat" \ "value").as[String]
      }
    }

    "must read paye when it is present" in {
      forAll(establisherCompanyGenerator(), arbitrary[String]) {
        (json, paye) =>
          val newJson = json + ("companyPaye" -> Json.obj("value" -> paye))
          val model = newJson.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)
          model.payeReference.value mustBe (newJson \ "companyPaye" \ "value").as[String]
      }
    }

    "must read utr when it is present" in {
      forAll(establisherCompanyGenerator(), arbitrary[String]) {
        (json, utr) =>
          val newJson = json + ("utr" -> Json.obj("value" -> utr))
          val model = newJson.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)
          model.utr.value mustBe (newJson \ "utr" \ "value").as[String]
      }
    }

    "must read no utr reason when it is present" in {
      forAll(establisherCompanyGenerator(), arbitrary[String]) {
        (json, noUtrReason) =>
          val newJson = json + ("noUtrReason" -> JsString(noUtrReason))
          val model = newJson.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)
          model.noUtrReason.value mustBe (newJson \ "noUtrReason").as[String]
      }
    }

    "must read crn when it is present" in {
      forAll(establisherCompanyGenerator(), arbitrary[String]) {
        (json, vat) =>
          val newJson = json + ("companyRegistrationNumber" -> Json.obj("value" -> vat))
          val model = newJson.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)
          model.crnNumber.value mustBe (newJson \ "companyRegistrationNumber" \ "value").as[String]
      }
    }

    "must read no crn reason when it is present" in {
      forAll(establisherCompanyGenerator(), arbitrary[String]) {
        (json, noUtrReason) =>
          val newJson = json + ("noCrnReason" -> JsString(noUtrReason))
          val model = newJson.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)
          model.noCrnReason.value mustBe (newJson \ "noCrnReason").as[String]
      }
    }

    "read company address correctly" in {
      forAll(establisherCompanyGenerator()) { json =>
        val model = json.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)

        model.correspondenceAddressDetails.addressDetails mustBe
          (json \ "companyAddress").as[Address]
      }
    }

    "must read previous address when address years is under a year and trading time is true" in {
      forAll(establisherCompanyGenerator(), "under_a_year", true) {
        (json, addressYears, hasBeenTrading) =>
          val newJson = json + ("companyAddressYears" -> JsString(addressYears)) + ("hasBeenTrading" -> JsBoolean(hasBeenTrading))
          val model = newJson.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)
          model.previousAddressDetails.value.isPreviousAddressLast12Month mustBe true
          model.previousAddressDetails.value.previousAddressDetails.value mustBe (json \ "companyPreviousAddress").as[Address]
      }
    }

    "must not read previous address when address years is not under a year" in {
      forAll(establisherCompanyGenerator()) {
        json =>
          val model = json.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)
          model.previousAddressDetails mustBe None
      }
    }

    "must not read previous address when address years is under a year but trading time is false" in {
      forAll(establisherCompanyGenerator(), false) {
        (json, hasBeenTrading) =>
          val newJson = json + ("hasBeenTrading" -> JsBoolean(hasBeenTrading))
          val model = newJson.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)
          model.previousAddressDetails mustBe None
      }
    }

    "read company contact details correctly" in {
      forAll(establisherCompanyGenerator()) { json =>
        val model = json.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)

        model.correspondenceContactDetails.contactDetails.email mustBe
          (json \ "companyContactDetails" \ "emailAddress").as[String]

        model.correspondenceContactDetails.contactDetails.telephone mustBe
          (json \ "companyContactDetails" \ "phoneNumber").as[String]
      }
    }

    "read other directors flag correctly" in {
      forAll(establisherCompanyGenerator()) { json =>
        val model = json.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)

        model.haveMoreThanTenDirectorOrPartner mustBe (json \ "otherDirectors").as[Boolean]
      }
    }

    "read directors correctly" in {
      forAll(establisherCompanyGenerator()) { json =>
        val model = json.as[CompanyEstablisher](CompanyEstablisher.readsEstablisherCompany)

        model.directorDetails.head.personalDetails.firstName mustBe
          (json \ "director" \ 0 \ "directorDetails" \ "firstName").as[String]

        model.directorDetails(1).personalDetails.firstName mustBe
          (json \ "director" \ 2 \ "directorDetails" \ "firstName").as[String]
      }
    }
  }
}
