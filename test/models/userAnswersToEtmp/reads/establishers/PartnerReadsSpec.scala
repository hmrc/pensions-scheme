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

package models.userAnswersToEtmp.reads.establishers

import models.userAnswersToEtmp.reads.CommonGenerator
import models.userAnswersToEtmp.reads.CommonGenerator.partnerGenerator
import models.userAnswersToEtmp.{Address, Individual}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Shrink
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import play.api.libs.json.{JsString, Json}

class PartnerReadsSpec extends FreeSpec with MustMatchers with GeneratorDrivenPropertyChecks with OptionValues {

  implicit def dontShrink[A]: Shrink[A] = Shrink.shrinkAny

  "A company partner" - {

    "must read partner details" in {
      forAll(partnerGenerator()){
        json =>
          val model = json.as[Individual](Individual.readsPartner)
          model.personalDetails.firstName mustBe (json \ "partnerDetails" \ "firstName").as[String]
          model.personalDetails.lastName mustBe (json \ "partnerDetails" \ "lastName").as[String]
          model.personalDetails.dateOfBirth mustBe (json \ "dateOfBirth").as[String]
      }
    }

    "must read previous address when address years is under a year" in {
      forAll(partnerGenerator(), "under_a_year"){
        (json, addressYears) =>
          val newJson  = json + ("partnerAddressYears" -> JsString(addressYears))
          val model = newJson.as[Individual](Individual.readsPartner)
          model.previousAddressDetails.value.isPreviousAddressLast12Month mustBe true
          model.previousAddressDetails.value.previousAddressDetails.value mustBe (json \ "partnerPreviousAddress").as[Address]
      }
    }

    "must not read previous address when address years is not under a year" in {
      forAll(partnerGenerator()){
        json =>
          val model = json.as[Individual](Individual.readsPartner)
          model.previousAddressDetails mustBe None
      }
    }

    "must read address" in {
      forAll(partnerGenerator()){
        json =>
          val model = json.as[Individual](Individual.readsPartner)
          model.correspondenceAddressDetails.addressDetails mustBe (json \ "partnerAddressId").as[Address]
      }
    }

    "must read contact details" in {
      forAll(partnerGenerator()){
        json =>
          val model = json.as[Individual](Individual.readsPartner)
          model.correspondenceContactDetails.contactDetails.email mustBe (json \ "partnerContactDetails" \ "emailAddress").as[String]
          model.correspondenceContactDetails.contactDetails.telephone mustBe (json \ "partnerContactDetails" \ "phoneNumber").as[String]
      }
    }

    "must read nino when it is present" in {
      forAll(partnerGenerator(), arbitrary[String]){
        (json, nino) =>
          val newJson  = json + ("partnerNino" -> Json.obj("value" -> nino))
          val model = newJson.as[Individual](Individual.readsPartner)
          model.referenceOrNino.value mustBe (newJson \ "partnerNino" \ "value").as[String]
      }
    }

    "must read no nino reason when it is present" in {
      forAll(partnerGenerator(), arbitrary[String]){
        (json, noNinoReason) =>
          val newJson  = json + ("noNinoReason" -> JsString(noNinoReason))
          val model = newJson.as[Individual](Individual.readsPartner)
          model.noNinoReason.value mustBe (newJson \ "noNinoReason").as[String]
      }
    }

    "must read utr when it is present" in {
      forAll(partnerGenerator(), arbitrary[String]){
        (json, utr) =>
          val newJson  = json + ("utr" -> Json.obj("value" -> utr))
          val model = newJson.as[Individual](Individual.readsPartner)
          model.utr.value mustBe (newJson \ "utr" \ "value").as[String]
      }
    }

    "must read no utr reason when it is present" in {
      forAll(partnerGenerator(), arbitrary[String]){
        (json, noUtrReason) =>
          val newJson  = json + ("noUtrReason" -> JsString(noUtrReason))
          val model = newJson.as[Individual](Individual.readsPartner)
          model.noUtrReason.value mustBe (newJson \ "noUtrReason").as[String]
      }
    }
  }
}
