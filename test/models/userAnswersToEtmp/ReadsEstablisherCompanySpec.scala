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

import models.{Address, CompanyEstablisher, PreviousAddressDetails}
import models.userAnswersToEtmp.ReadsEstablisherCompany.readsEstablisherCompanies
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Gen, Shrink}
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsArray, JsObject, Json}

class ReadsEstablisherCompanySpec extends WordSpec with MustMatchers with OptionValues {

  implicit def dontShrink[A]: Shrink[A] = Shrink.shrinkAny

  def arbitraryString: Gen[String] =  Gen.alphaStr suchThat (_.nonEmpty)

  val companiesGen: Gen[JsArray] =
    for {
      company1 <- companyGenerator()
      company2 <- companyGenerator(isDeleted = true)
      company3 <- companyGenerator()
    } yield Json.arr(company1, company2, company3)

  private def companyGenerator(isDeleted: Boolean = false): Gen[JsObject] =
    for {
      companyName <- arbitraryString
      companyVat <-  arbitrary[String]
      companyPaye <-  arbitrary[String]
      utr <-  arbitrary[String]
      noUtrReason <- arbitraryString
      companyRegistrationNumber <-  arbitrary[String]
      noCrnReason <- arbitraryString
      correspondenceAddressDetails <- addressGen
      addressYears <- arbitraryString
      hasBeenTrading <- arbitrary[Boolean]
      previousAddressDetails <- Gen.option(addressGen)
      mobileNumber <- arbitraryString
      emailAddress <- arbitraryString
      otherDirectors <- arbitrary[Boolean]
    } yield Json.obj(
      "companyDetails" -> Json.obj(
        "companyName" -> companyName,
        "isDeleted" -> isDeleted
      ),
      "companyAddress" -> correspondenceAddressDetails,
      "companyAddressYears" -> addressYears,
      "hasBeenTrading" -> hasBeenTrading,
      "companyPreviousAddress" -> previousAddressDetails,
      "companyContactDetails" -> Json.obj(
        "emailAddress" -> emailAddress,
        "phoneNumber" -> mobileNumber
      ),
      "otherDirectors" -> otherDirectors
    ) ++
      (if(companyVat.nonEmpty) Json.obj("companyVat" -> Json.obj("value" -> companyVat))
      else Json.obj()

        ) ++
      (if(companyPaye.nonEmpty) Json.obj( "companyPaye" -> Json.obj("value" -> companyPaye))
      else Json.obj()

        ) ++
      (if(utr.nonEmpty) Json.obj("utr" -> Json.obj("value" -> utr))
      else Json.obj("noUtrReason" -> noUtrReason)

        ) ++
      (if(companyRegistrationNumber.nonEmpty)
        Json.obj("companyRegistrationNumber" -> Json.obj("value" -> companyRegistrationNumber))
      else
        Json.obj("noCrnReason" -> noCrnReason))

  lazy val addressGen: Gen[JsObject] = Gen.oneOf(ukAddressGen, internationalAddressGen)

  lazy val ukAddressGen: Gen[JsObject] = for {
    line1 <- arbitrary[String]
    line2 <- arbitrary[String]
    line3 <- arbitrary[Option[String]]
    line4 <- arbitrary[Option[String]]
    postalCode <- arbitrary[String]
  } yield Json.obj("addressLine1" -> line1, "addressLine2" -> line2, "addressLine3" -> line3,
    "addressLine4" -> line4, "country" -> "GB", "postalCode" -> postalCode)

  lazy val internationalAddressGen: Gen[JsObject] = for {
    line1 <- arbitrary[String]
    line2 <- arbitrary[String]
    line3 <- arbitrary[Option[String]]
    line4 <- arbitrary[Option[String]]
    countryCode <- arbitrary[String]
  } yield Json.obj("addressLine1" -> line1, "addressLine2" -> line2, "addressLine3" -> line3,
    "addressLine4" -> line4, "country" -> countryCode)


  "A Json payload containing establisher company" should {

    "read company name correctly" in {
      forAll(companiesGen) { json =>
        val model = json.as[Seq[CompanyEstablisher]](readsEstablisherCompanies)

        model.head.organizationName mustBe (json \ 0 \ "companyDetails" \ "companyName").as[String]
        model(1).organizationName mustBe (json \ 2 \ "companyDetails" \ "companyName").as[String]
      }
    }

    "read company vat correctly" in {
      forAll(companiesGen) { json =>
        val model = json.as[Seq[CompanyEstablisher]](readsEstablisherCompanies)

        model.head.vatRegistrationNumber mustBe (json \ 0 \ "companyVat" \ "value").asOpt[String]
        model(1).vatRegistrationNumber mustBe (json \ 2 \ "companyVat" \ "value").asOpt[String]
      }
    }

    "read company paye correctly" in {
      forAll(companiesGen) { json =>
        val model = json.as[Seq[CompanyEstablisher]](readsEstablisherCompanies)

        model.head.payeReference mustBe (json \ 0 \ "companyPaye" \ "value").asOpt[String]
        model(1).payeReference mustBe (json \ 2 \ "companyPaye" \ "value").asOpt[String]
      }
    }

    "read company utr correctly" in {
      forAll(companiesGen) { json =>
        val model = json.as[Seq[CompanyEstablisher]](readsEstablisherCompanies)

        model.head.utr mustBe (json \ 0 \ "utr" \ "value").asOpt[String]
        model.head.noUtrReason mustBe (json \ 0 \ "noUtrReason").asOpt[String]
        model(1).utr mustBe (json \ 2 \ "utr" \ "value").asOpt[String]
        model(1).noUtrReason mustBe (json \ 2 \ "noUtrReason").asOpt[String]
      }
    }

    "read company crn correctly" in {
      forAll(companiesGen) { json =>
        val model = json.as[Seq[CompanyEstablisher]](readsEstablisherCompanies)

        model.head.crnNumber mustBe (json \ 0 \ "companyRegistrationNumber" \ "value").asOpt[String]
        model.head.noCrnReason mustBe (json \ 0 \ "noCrnReason").asOpt[String]
        model(1).crnNumber mustBe (json \ 2 \ "companyRegistrationNumber" \ "value").asOpt[String]
        model(1).noCrnReason mustBe (json \ 2 \ "noCrnReason").asOpt[String]
      }
    }

    "read company address correctly" in {
      forAll(companiesGen) { json =>
        val model = json.as[Seq[CompanyEstablisher]](readsEstablisherCompanies)

        model.head.correspondenceAddressDetails.addressDetails mustBe
          (json \ 0 \ "companyAddress").as[Address]
      }
    }

    "read company previous address correctly" in {
      forAll(companiesGen) { json =>
        val model = json.as[Seq[CompanyEstablisher]](readsEstablisherCompanies)

        model.head.previousAddressDetails mustBe
          (json \ 0 \ "companyPreviousAddress").asOpt[PreviousAddressDetails]
      }
    }

    "read company contact details correctly" in {
      forAll(companiesGen) { json =>
        val model = json.as[Seq[CompanyEstablisher]](readsEstablisherCompanies)

        model.head.correspondenceContactDetails.contactDetails.email mustBe
          (json \ 0 \ "companyContactDetails" \ "emailAddress").as[String]

        model.head.correspondenceContactDetails.contactDetails.telephone mustBe
          (json \ 0 \ "companyContactDetails" \ "phoneNumber").as[String]
      }
    }

    "read other directors flag correctly" in {
      forAll(companiesGen) { json =>
        val model = json.as[Seq[CompanyEstablisher]](readsEstablisherCompanies)

        model.head.haveMoreThanTenDirectorOrPartner mustBe
          (json \ 0 \ "otherDirectors").as[Boolean]

        model(1).haveMoreThanTenDirectorOrPartner mustBe
          (json \ 2 \ "otherDirectors").as[Boolean]
      }
    }

  }
}
