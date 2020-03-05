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
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import play.api.libs.json._

class ReadsTrusteePartnershipSpec extends FreeSpec with MustMatchers with GeneratorDrivenPropertyChecks with OptionValues {
  private def nullToNone(o: Option[JsValue]): Option[JsValue] = o.flatMap(jsValue => if (jsValue == JsNull) None else Some(jsValue))

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

  private def codeJson(code: String, hasCode: Boolean): JsValue =
    (code, hasCode) match {
      case (_, true) => Json.obj("value" -> code)
      case _ => JsNull
    }

  private def noCodeReasonJson(reason: String, hasCode: Boolean): JsValue =
    (reason, hasCode) match {
      case (_, false) => JsString(reason)
      case _ => JsNull
    }

  private implicit def dontShrink[A]: Shrink[A] = Shrink.shrinkAny

  private val partnershipGenerator: Gen[JsObject] =
    for {
      hasVat <- arbitrary[Boolean]
      vat <- nonEmptyString
      hasUtr <- arbitrary[Boolean]
      utr <- nonEmptyString
      noUtrReason <- nonEmptyString
      hasPaye <- arbitrary[Boolean]
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
        "isTrusteeNew" -> true,
        "hasVat" -> hasVat,
        "partnershipVat" -> codeJson(vat, hasVat),
        "hasUtr" -> hasUtr,
        "utr" -> codeJson(utr, hasUtr),
        "noUtrReason" -> noCodeReasonJson(noUtrReason, hasUtr),
        "hasPaye" -> hasPaye,
        "partnershipPaye" -> codeJson(paye, hasPaye),
        "partnershipContactDetails" -> Json.obj(
          "emailAddress" -> emailAddress,
          "phoneNumber" -> phoneNumber
        ),
        "hasBeenTrading" -> hasBeenTrading,
        "partnershipPreviousAddress" -> previousAddressDetails,
        "partnershipAddress" -> addressDetails,
        "trusteeKind" -> "partnership",
        "partnershipDetails" -> Json.obj(
          "name" -> name,
          "isDeleted" -> false
        ),
        "partnershipAddressYears" -> addressYears
      )
    }

  "A trustee partnership" - {
    "must be read from valid data" in {
      forAll(partnershipGenerator) { json =>
        val transformedTrustee = JsArray(Seq(json)).as[Seq[PartnershipTrustee]](ReadsTrusteePartnership.readsTrusteePartnerships).head

        transformedTrustee.organizationName mustBe (json \ "partnershipDetails" \ "name").as[String]

        if ((json \ "hasUtr").as[Boolean]) {
          transformedTrustee.utr mustBe Option((json \ "utr" \ "value").as[String])
          transformedTrustee.noUtrReason mustBe None
        } else {
          transformedTrustee.utr mustBe None
          transformedTrustee.noUtrReason mustBe Option((json \ "noUtrReason").as[String])
        }

        if ((json \ "hasVat").as[Boolean]) {
          transformedTrustee.vatRegistrationNumber mustBe Option((json \ "partnershipVat" \ "value").as[String])
        } else {
          transformedTrustee.vatRegistrationNumber mustBe None
        }

        if ((json \ "hasPaye").as[Boolean]) {
          transformedTrustee.payeReference mustBe Option((json \ "partnershipPaye" \ "value").as[String])
        } else {
          transformedTrustee.payeReference mustBe None
        }

        if ((json \ "partnershipAddress" \ "country").as[String] == "GB") {
          transformedTrustee.correspondenceAddressDetails.addressDetails mustBe UkAddress(
            addressLine1 = (json \ "partnershipAddress" \ "addressLine1").as[String],
            addressLine2 = nullToNone((json \ "partnershipAddress" \ "addressLine2").toOption).map(_.as[String]),
            addressLine3 = nullToNone((json \ "partnershipAddress" \ "addressLine3").toOption).map(_.as[String]),
            addressLine4 = nullToNone((json \ "partnershipAddress" \ "addressLine4").toOption).map(_.as[String]),
            countryCode = "GB",
            postalCode = (json \ "partnershipAddress" \ "postalCode").as[String]
          )
        } else {
          transformedTrustee.correspondenceAddressDetails.addressDetails mustBe InternationalAddress(
            addressLine1 = (json \ "partnershipAddress" \ "addressLine1").as[String],
            addressLine2 = nullToNone((json \ "partnershipAddress" \ "addressLine2").toOption).map(_.as[String]),
            addressLine3 = nullToNone((json \ "partnershipAddress" \ "addressLine3").toOption).map(_.as[String]),
            addressLine4 = nullToNone((json \ "partnershipAddress" \ "addressLine4").toOption).map(_.as[String]),
            countryCode = (json \ "partnershipAddress" \ "country").as[String],
            postalCode = None
          )
        }

        transformedTrustee.correspondenceContactDetails mustBe CorrespondenceContactDetails(ContactDetails(
          telephone = (json \ "partnershipContactDetails" \ "phoneNumber").as[String],
          email = (json \ "partnershipContactDetails" \ "emailAddress").as[String]
        ))

        if ((json \ "hasBeenTrading").as[Boolean] && (json \ "partnershipAddressYears").as[String] == "under_a_year") {
          if ((json \ "partnershipPreviousAddress" \ "country").as[String] == "GB") {
            transformedTrustee.previousAddressDetails.flatMap(_.previousAddressDetails) mustBe Some(UkAddress(
              addressLine1 = (json \ "partnershipPreviousAddress" \ "addressLine1").as[String],
              addressLine2 = nullToNone((json \ "partnershipPreviousAddress" \ "addressLine2").toOption).map(_.as[String]),
              addressLine3 = nullToNone((json \ "partnershipPreviousAddress" \ "addressLine3").toOption).map(_.as[String]),
              addressLine4 = nullToNone((json \ "partnershipPreviousAddress" \ "addressLine4").toOption).map(_.as[String]),
              countryCode = "GB",
              postalCode = (json \ "partnershipPreviousAddress" \ "postalCode").as[String]
            ))
          } else {
            transformedTrustee.previousAddressDetails.flatMap(_.previousAddressDetails) mustBe Some(InternationalAddress(
              addressLine1 = (json \ "partnershipPreviousAddress" \ "addressLine1").as[String],
              addressLine2 = nullToNone((json \ "partnershipPreviousAddress" \ "addressLine2").toOption).map(_.as[String]),
              addressLine3 = nullToNone((json \ "partnershipPreviousAddress" \ "addressLine3").toOption).map(_.as[String]),
              addressLine4 = nullToNone((json \ "partnershipPreviousAddress" \ "addressLine4").toOption).map(_.as[String]),
              countryCode = (json \ "partnershipPreviousAddress" \ "country").as[String],
              postalCode = None
            ))
          }
        } else {
          transformedTrustee.previousAddressDetails mustBe None
        }
      }
    }
  }
}
