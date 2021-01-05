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

import models.userAnswersToEtmp.reads.CommonGenerator.directorGenerator
import models.userAnswersToEtmp.{Address, Individual}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Shrink
import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.{JsString, Json}

class DirectorReadsSpec extends FreeSpec with MustMatchers with ScalaCheckDrivenPropertyChecks with OptionValues {

  implicit def dontShrink[A]: Shrink[A] = Shrink.shrinkAny

  "A company director" - {

    "must read director details" in {
      forAll(directorGenerator()){
        json =>
          val model = json.as[Individual](Individual.readsCompanyDirector)
          model.personalDetails.firstName mustBe (json \ "directorDetails" \ "firstName").as[String]
          model.personalDetails.lastName mustBe (json \ "directorDetails" \ "lastName").as[String]
          model.personalDetails.dateOfBirth mustBe (json \ "dateOfBirth").as[String]
      }
    }

    "must read previous address when address years is under a year" in {
      forAll(directorGenerator(), "under_a_year"){
        (json, addressYears) =>
          val newJson  = json + ("companyDirectorAddressYears" -> JsString(addressYears))
          val model = newJson.as[Individual](Individual.readsCompanyDirector)
          model.previousAddressDetails.value.isPreviousAddressLast12Month mustBe true
          model.previousAddressDetails.value.previousAddressDetails.value mustBe (json \ "previousAddress").as[Address]
      }
    }

    "must not read previous address when address years is not under a year" in {
      forAll(directorGenerator()){
        json =>
          val model = json.as[Individual](Individual.readsCompanyDirector)
          model.previousAddressDetails mustBe None
      }
    }

    "must read address" in {
      forAll(directorGenerator()){
        json =>
          val model = json.as[Individual](Individual.readsCompanyDirector)
          model.correspondenceAddressDetails.addressDetails mustBe (json \ "directorAddressId").as[Address]
      }
    }

    "must read contact details" in {
      forAll(directorGenerator()){
        json =>
          val model = json.as[Individual](Individual.readsCompanyDirector)
          model.correspondenceContactDetails.contactDetails.email mustBe (json \ "directorContactDetails" \ "emailAddress").as[String]
          model.correspondenceContactDetails.contactDetails.telephone mustBe (json \ "directorContactDetails" \ "phoneNumber").as[String]
      }
    }

    "must read nino when it is present" in {
      forAll(directorGenerator(), arbitrary[String]){
        (json, nino) =>
          val newJson  = json + ("directorNino" -> Json.obj("value" -> nino))
          val model = newJson.as[Individual](Individual.readsCompanyDirector)
          model.referenceOrNino.value mustBe (newJson \ "directorNino" \ "value").as[String]
      }
    }

    "must read no nino reason when it is present" in {
      forAll(directorGenerator(), arbitrary[String]){
        (json, noNinoReason) =>
          val newJson  = json + ("noNinoReason" -> JsString(noNinoReason))
          val model = newJson.as[Individual](Individual.readsCompanyDirector)
          model.noNinoReason.value mustBe (newJson \ "noNinoReason").as[String]
      }
    }

    "must read utr when it is present" in {
      forAll(directorGenerator(), arbitrary[String]){
        (json, utr) =>
          val newJson  = json + ("utr" -> Json.obj("value" -> utr))
          val model = newJson.as[Individual](Individual.readsCompanyDirector)
          model.utr.value mustBe (newJson \ "utr" \ "value").as[String]
      }
    }

    "must read no utr reason when it is present" in {
      forAll(directorGenerator(), arbitrary[String]){
        (json, noUtrReason) =>
          val newJson  = json + ("noUtrReason" -> JsString(noUtrReason))
          val model = newJson.as[Individual](Individual.readsCompanyDirector)
          model.noUtrReason.value mustBe (newJson \ "noUtrReason").as[String]
      }
    }
  }
}
