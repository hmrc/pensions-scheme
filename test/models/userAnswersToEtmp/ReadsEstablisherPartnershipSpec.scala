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

package models.userAnswersToEtmp

import models._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Gen, Shrink}
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json._

class ReadsEstablisherPartnershipSpec extends WordSpec with MustMatchers with OptionValues {
  private def jsValueOrNone(o: Option[JsValue]): Option[JsValue] = o.flatMap(jsValue => if (jsValue == JsNull) None else Some(jsValue))

  private def nonEmptyString: Gen[String] = Gen.alphaStr.suchThat(!_.isEmpty)

  private val ukAddressGen: Gen[JsObject] = for {
    line1 <- nonEmptyString
    line2 <- nonEmptyString
    line3 <- Gen.option(nonEmptyString)
    line4 <- Gen.option(nonEmptyString)
    postalCode <- nonEmptyString
  } yield Json.obj("addressLine1" -> line1, "addressLine2" -> line2, "addressLine3" -> line3,
    "addressLine4" -> line4, "country" -> "GB", "postalCode" -> postalCode)

  private val internationalAddressGen: Gen[JsObject] = for {
    line1 <- nonEmptyString
    line2 <- nonEmptyString
    line3 <- Gen.option(nonEmptyString)
    line4 <- Gen.option(nonEmptyString)
    countryCode <- nonEmptyString
  } yield Json.obj("addressLine1" -> line1, "addressLine2" -> line2, "addressLine3" -> line3,
    "addressLine4" -> line4, "country" -> countryCode)

  private val addressGen: Gen[JsObject] = Gen.oneOf(ukAddressGen, internationalAddressGen)
  private val addressYearsGen: Gen[String] = Gen.oneOf("over_a_year", "under_a_year")

  private implicit def dontShrink[A]: Shrink[A] = Shrink.shrinkAny

  private val partnershipGenerator: Gen[JsObject] =
    for {
      vat <- nonEmptyString
      utr <- nonEmptyString
      noUtrReason <- nonEmptyString
      paye <- nonEmptyString
      emailAddress <- nonEmptyString
      phoneNumber <- nonEmptyString
      hasBeenTrading <- arbitrary[Boolean]
      addressDetails <- addressGen
      previousAddressDetails <- addressGen
      name <- nonEmptyString
      addressYears <- addressYearsGen
    } yield {
      Json.obj(
        "isEstablisherNew" -> true,
        "partnershipVat" -> Json.obj("value" -> vat),
        "utr" -> Json.obj("value" -> utr),
        "noUtrReason" -> JsString(noUtrReason),
        "partnershipPaye" -> Json.obj("value" -> paye),
        "partnershipContactDetails" -> Json.obj(
          "emailAddress" -> emailAddress,
          "phoneNumber" -> phoneNumber
        ),
        "hasBeenTrading" -> hasBeenTrading,
        "partnershipPreviousAddress" -> previousAddressDetails,
        "partnershipAddress" -> addressDetails,
        "establisherKind" -> "partnership",
        "partnershipDetails" -> Json.obj(
          "name" -> name,
          "isDeleted" -> false
        ),
        "partnershipAddressYears" -> addressYears
      )
    }

  "A Json payload containing trustee partnership" should {
    "have partnership name read correctly" in {
      forAll(partnershipGenerator) { json =>
        val transformedEstablisher = JsArray(Seq(json)).as[Seq[Partnership]](ReadsEstablisherPartnership.readsEstablisherPartnerships).head
        transformedEstablisher.organizationName mustBe (json \ "partnershipDetails" \ "name").as[String]
      }
    }

    "have partnership utr read correctly" in {
      forAll(partnershipGenerator) { json =>
        val transformedEstablisher = JsArray(Seq(json)).as[Seq[Partnership]](ReadsEstablisherPartnership.readsEstablisherPartnerships).head
        transformedEstablisher.utr mustBe Option((json \ "utr" \ "value").as[String])
      }
    }

    "have partnership no utr reason read correctly" in {
      forAll(partnershipGenerator) { json =>
        val transformedEstablisher = JsArray(Seq(json)).as[Seq[Partnership]](ReadsEstablisherPartnership.readsEstablisherPartnerships).head
        transformedEstablisher.noUtrReason mustBe Option((json \ "noUtrReason").as[String])
      }
    }

    "have partnership vat read correctly" in {
      forAll(partnershipGenerator) { json =>
        val transformedEstablisher = JsArray(Seq(json)).as[Seq[Partnership]](ReadsEstablisherPartnership.readsEstablisherPartnerships).head
        transformedEstablisher.vatRegistrationNumber mustBe Option((json \ "partnershipVat" \ "value").as[String])
      }
    }

    "have partnership paye read correctly" in {
      forAll(partnershipGenerator) { json =>
        val transformedEstablisher = JsArray(Seq(json)).as[Seq[Partnership]](ReadsEstablisherPartnership.readsEstablisherPartnerships).head
        transformedEstablisher.payeReference mustBe Option((json \ "partnershipPaye" \ "value").as[String])
      }
    }

    "have partnership address read correctly" in {
      forAll(partnershipGenerator) { json =>
        val transformedEstablisher = JsArray(Seq(json)).as[Seq[Partnership]](ReadsEstablisherPartnership.readsEstablisherPartnerships).head
        if ((json \ "partnershipAddress" \ "country").as[String] == "GB") {
          transformedEstablisher.correspondenceAddressDetails.addressDetails mustBe UkAddress(
            addressLine1 = (json \ "partnershipAddress" \ "addressLine1").as[String],
            addressLine2 = jsValueOrNone((json \ "partnershipAddress" \ "addressLine2").toOption).map(_.as[String]),
            addressLine3 = jsValueOrNone((json \ "partnershipAddress" \ "addressLine3").toOption).map(_.as[String]),
            addressLine4 = jsValueOrNone((json \ "partnershipAddress" \ "addressLine4").toOption).map(_.as[String]),
            countryCode = "GB",
            postalCode = (json \ "partnershipAddress" \ "postalCode").as[String]
          )
        } else {
          transformedEstablisher.correspondenceAddressDetails.addressDetails mustBe InternationalAddress(
            addressLine1 = (json \ "partnershipAddress" \ "addressLine1").as[String],
            addressLine2 = jsValueOrNone((json \ "partnershipAddress" \ "addressLine2").toOption).map(_.as[String]),
            addressLine3 = jsValueOrNone((json \ "partnershipAddress" \ "addressLine3").toOption).map(_.as[String]),
            addressLine4 = jsValueOrNone((json \ "partnershipAddress" \ "addressLine4").toOption).map(_.as[String]),
            countryCode = (json \ "partnershipAddress" \ "country").as[String],
            postalCode = None
          )
        }
      }
    }

    "have partnership contact details read correctly" in {
      forAll(partnershipGenerator) { json =>
        val transformedEstablisher = JsArray(Seq(json)).as[Seq[Partnership]](ReadsEstablisherPartnership.readsEstablisherPartnerships).head
        transformedEstablisher.correspondenceContactDetails mustBe CorrespondenceContactDetails(ContactDetails(
          telephone = (json \ "partnershipContactDetails" \ "phoneNumber").as[String],
          email = (json \ "partnershipContactDetails" \ "emailAddress").as[String]
        ))
      }
    }

    "have partnership previous address read correctly" in {
      forAll(partnershipGenerator) { json =>
        val transformedEstablisher = JsArray(Seq(json)).as[Seq[Partnership]](ReadsEstablisherPartnership.readsEstablisherPartnerships).head
        if ((json \ "hasBeenTrading").as[Boolean] && (json \ "partnershipAddressYears").as[String] == "under_a_year") {
          if ((json \ "partnershipPreviousAddress" \ "country").as[String] == "GB") {
            transformedEstablisher.previousAddressDetails.flatMap(_.previousAddressDetails) mustBe Some(UkAddress(
              addressLine1 = (json \ "partnershipPreviousAddress" \ "addressLine1").as[String],
              addressLine2 = jsValueOrNone((json \ "partnershipPreviousAddress" \ "addressLine2").toOption).map(_.as[String]),
              addressLine3 = jsValueOrNone((json \ "partnershipPreviousAddress" \ "addressLine3").toOption).map(_.as[String]),
              addressLine4 = jsValueOrNone((json \ "partnershipPreviousAddress" \ "addressLine4").toOption).map(_.as[String]),
              countryCode = "GB",
              postalCode = (json \ "partnershipPreviousAddress" \ "postalCode").as[String]
            ))
          } else {
            transformedEstablisher.previousAddressDetails.flatMap(_.previousAddressDetails) mustBe Some(InternationalAddress(
              addressLine1 = (json \ "partnershipPreviousAddress" \ "addressLine1").as[String],
              addressLine2 = jsValueOrNone((json \ "partnershipPreviousAddress" \ "addressLine2").toOption).map(_.as[String]),
              addressLine3 = jsValueOrNone((json \ "partnershipPreviousAddress" \ "addressLine3").toOption).map(_.as[String]),
              addressLine4 = jsValueOrNone((json \ "partnershipPreviousAddress" \ "addressLine4").toOption).map(_.as[String]),
              countryCode = (json \ "partnershipPreviousAddress" \ "country").as[String],
              postalCode = None
            ))
          }
        } else {
          transformedEstablisher.previousAddressDetails mustBe None
        }
      }
    }
  }
}
