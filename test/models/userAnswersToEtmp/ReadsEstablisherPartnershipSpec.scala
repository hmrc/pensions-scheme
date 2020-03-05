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

package utils

import models.userAnswersToEtmp.ReadsEstablisherPartnership
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Gen, Shrink}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import play.api.libs.json.{JsObject, Json}

class ReadsEstablisherPartnershipSpec extends FreeSpec with MustMatchers with GeneratorDrivenPropertyChecks with OptionValues {

  implicit def dontShrink[A]: Shrink[A] = Shrink.shrinkAny

  val partnershipGenerator: Gen[JsObject] =
    for {
      firstName <- arbitrary[String]
      lastName <- arbitrary[String]
      dateOfBirth <- arbitrary[String]
      referenceOrNino <- arbitrary[Option[String]]
      noNinoReason <- arbitrary[Option[String]]
      utr <- arbitrary[Option[String]]
      noUtrReason <- arbitrary[Option[String]]
      correspondenceAddressDetails <- addressGen
      addressYears <- arbitrary[String]
      previousAddressDetails <- Gen.option(addressGen)
      mobileNumber <- arbitrary[String]
      emailAddress <- arbitrary[String]
    } yield Json.obj(
      "trusteeDetails" -> Json.obj(
        "firstName" -> firstName,
        "lastName" -> lastName
      ),
      "dateOfBirth" -> dateOfBirth,
      "trusteeNino" -> Json.obj(
        "value" -> referenceOrNino
      ),
      "noNinoReason" -> noNinoReason,
      "utr" ->  Json.obj(
        "value" -> utr
      ),
      "noUtrReason" -> noUtrReason,
      "trusteeAddressId" -> correspondenceAddressDetails,
      "trusteeAddressYears" -> addressYears,
      "trusteePreviousAddress" -> previousAddressDetails,
      "trusteeContactDetails" -> Json.obj(
        "emailAddress" -> emailAddress,
        "phoneNumber" -> mobileNumber
      )
    )


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

  /** Going from UA to ETMP **/

  "An establisher partnership" - {
    "must be read from valid data" in {
      forAll(partnershipGenerator){
        json =>
          val model = json.as[Seq[Partnership]](ReadsEstablisherPartnership.readsEstablisherPartnerships)

//          model.personalDetails.firstName mustBe (json \ "trusteeDetails" \ "firstName").as[String]
//          model.personalDetails.lastName mustBe (json \ "trusteeDetails" \ "lastName").as[String]
//          model.personalDetails.dateOfBirth mustBe (json \ "dateOfBirth").as[String]
      }
    }

//    "must read nino when it is present" in {
//      forAll(partnershipGenerator, arbitrary[String]){
//        (json, nino) =>
//          val newJson  = json + ("trusteeNino" -> Json.obj("value" -> nino))
//          val model = newJson.as[Individual](ReadsEstablisherDetails.readsTrusteeIndividual)
//          model.referenceOrNino.value mustBe (newJson \ "trusteeNino" \ "value").as[String]
//      }
//    }
//
//    "must read utr when it is present" in {
//      forAll(partnershipGenerator, arbitrary[String]){
//        (json, utr) =>
//          val newJson  = json + ("utr" -> Json.obj("value" -> utr))
//          val model = newJson.as[Individual](ReadsEstablisherDetails.readsTrusteeIndividual)
//          model.utr.value mustBe (newJson \ "utr" \ "value").as[String]
//      }
//    }
  }


}