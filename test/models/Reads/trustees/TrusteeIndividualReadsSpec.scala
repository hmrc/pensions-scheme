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

package models.Reads.trustees

import models.{Address, Individual}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Gen, Shrink}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import play.api.libs.json.{JsObject, JsString, Json}

class TrusteeIndividualReadsSpec extends FreeSpec with MustMatchers with GeneratorDrivenPropertyChecks with OptionValues {

  implicit def dontShrink[A]: Shrink[A] = Shrink.shrinkAny

  lazy val addressGen: Gen[JsObject] = Gen.oneOf(ukAddressGen, internationalAddressGen)

  lazy val ukAddressGen: Gen[JsObject] = for {
    line1 <- arbitrary[String]
    line2 <- arbitrary[String]
    line3 <- arbitrary[Option[String]]
    line4 <- arbitrary[Option[String]]
    postalCode <- arbitrary[String]
  } yield Json.obj("addressLine1" -> line1, "addressLine2" -> line2, "addressLine3" ->line3,
    "addressLine4" -> line4, "country" -> "GB", "postalCode" -> postalCode)

  lazy val internationalAddressGen: Gen[JsObject] = for {
    line1 <- arbitrary[String]
    line2 <- arbitrary[String]
    line3 <- arbitrary[Option[String]]
    line4 <- arbitrary[Option[String]]
    countryCode <- arbitrary[String]
  } yield Json.obj("addressLine1" -> line1, "addressLine2" -> line2, "addressLine3" ->line3,
    "addressLine4" -> line4, "country" -> countryCode)

  val individualGenerator: Gen[JsObject] =
    for {
      firstName <- arbitrary[String]
      lastName <- arbitrary[String]
      dateOfBirth <- arbitrary[String]
      correspondenceAddressDetails <- addressGen
      addressYears <- arbitrary[String]
      previousAddressDetails <- addressGen
      mobileNumber <- arbitrary[String]
      emailAddress <- arbitrary[String]
    } yield Json.obj(
      "trusteeDetails" -> Json.obj(
        "firstName" -> firstName,
        "lastName" -> lastName
      ),
      "dateOfBirth" -> dateOfBirth,
      "trusteeAddressId" -> correspondenceAddressDetails,
      "trusteeAddressYears" -> addressYears,
      "trusteePreviousAddress" -> previousAddressDetails,
      "trusteeContactDetails" -> Json.obj(
        "emailAddress" -> emailAddress,
        "phoneNumber" -> mobileNumber
      )
    )

  "An trustee individual" - {

    "must read individual details" in {
      forAll(individualGenerator){
        json =>
          val model = json.as[Individual](Individual.readsTrusteeIndividual)
          model.personalDetails.firstName mustBe (json \ "trusteeDetails" \ "firstName").as[String]
          model.personalDetails.lastName mustBe (json \ "trusteeDetails" \ "lastName").as[String]
          model.personalDetails.dateOfBirth mustBe (json \ "dateOfBirth").as[String]
      }
    }

    "must read previous address when address years is under a year" in {
      forAll(individualGenerator, "under_a_year"){
        (json, addressYears) =>
          val newJson  = json + ("trusteeAddressYears" -> JsString(addressYears))
          val model = newJson.as[Individual](Individual.readsTrusteeIndividual)
          model.previousAddressDetails.value.isPreviousAddressLast12Month mustBe true
          model.previousAddressDetails.value.previousAddressDetails.value mustBe (json \ "trusteePreviousAddress").as[Address]
      }
    }

    "must not read previous address when address years is not under a year" in {
      forAll(individualGenerator){
        json =>
          val model = json.as[Individual](Individual.readsTrusteeIndividual)
          model.previousAddressDetails mustBe None
      }
    }

    "must read address" in {
      forAll(individualGenerator){
        json =>
          val model = json.as[Individual](Individual.readsTrusteeIndividual)
          model.correspondenceAddressDetails.addressDetails mustBe (json \ "trusteeAddressId").as[Address]
      }
    }

    "must read contact details" in {
      forAll(individualGenerator){
        json =>
          val model = json.as[Individual](Individual.readsTrusteeIndividual)
          model.correspondenceContactDetails.contactDetails.email mustBe (json \ "trusteeContactDetails" \ "emailAddress").as[String]
          model.correspondenceContactDetails.contactDetails.telephone mustBe (json \ "trusteeContactDetails" \ "phoneNumber").as[String]
      }
    }

    "must read nino when it is present" in {
      forAll(individualGenerator, arbitrary[String]){
        (json, nino) =>
          val newJson  = json + ("trusteeNino" -> Json.obj("value" -> nino))
          val model = newJson.as[Individual](Individual.readsTrusteeIndividual)
          model.referenceOrNino.value mustBe (newJson \ "trusteeNino" \ "value").as[String]
      }
    }

    "must read no nino reason when it is present" in {
      forAll(individualGenerator, arbitrary[String]){
        (json, noNinoReason) =>
          val newJson  = json + ("noNinoReason" -> JsString(noNinoReason))
          val model = newJson.as[Individual](Individual.readsTrusteeIndividual)
          model.noNinoReason.value mustBe (newJson \ "noNinoReason").as[String]
      }
    }

    "must read utr when it is present" in {
      forAll(individualGenerator, arbitrary[String]){
        (json, utr) =>
          val newJson  = json + ("utr" -> Json.obj("value" -> utr))
          val model = newJson.as[Individual](Individual.readsTrusteeIndividual)
          model.utr.value mustBe (newJson \ "utr" \ "value").as[String]
      }
    }

    "must read no utr reason when it is present" in {
      forAll(individualGenerator, arbitrary[String]){
        (json, noUtrReason) =>
          val newJson  = json + ("noUtrReason" -> JsString(noUtrReason))
          val model = newJson.as[Individual](Individual.readsTrusteeIndividual)
          model.noUtrReason.value mustBe (newJson \ "noUtrReason").as[String]
      }
    }
  }
}
