/*
 * Copyright 2018 HM Revenue & Customs
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

package models.Reads.schemes

import org.joda.time.LocalDate
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import play.api.libs.json.{JsArray, JsObject, Json}

//scalastyle:off magic.number
trait PSASchemeDetailsGenerator {

  val dateGenerator: Gen[LocalDate] = for {
    day <- Gen.choose(1,28)
    month <-Gen.choose(1,12)
    year <-Gen.choose(1990,2000)
  } yield new LocalDate(year,month,day)


  val addressGenerator : Gen[JsObject] = for {
    nonUkAddress <- arbitrary[Boolean]
    line1 <- Gen.alphaStr
    line2 <- Gen.alphaStr
    line3 <- Gen.option(Gen.alphaStr)
    line4 <- Gen.option(Gen.alphaStr)
    postalCode <- Gen.option(Gen.alphaStr)
    countryCode <- Gen.listOfN(2, Gen.alphaChar).map(_.mkString)
  } yield {
    Json.obj(
      "nonUKAddress" -> nonUkAddress,
      "line1" -> line1,
      "line2" -> line2,
      "line3" -> line3,
      "line4" -> line4,
      "postalCode" -> postalCode,
      "countryCode" -> countryCode
    )
  }

  val psaContactDetailsGenerator : Gen[JsObject] = for {
    telephone <- Gen.numStr
    email <- Gen.option(Gen.alphaStr)
  } yield {
    Json.obj(
      "telephone" -> telephone,
      "email" -> email
    )
  }

  val previousAddressGenerator : Gen[JsObject] = for {
    isPreviousAddressLast12Month <- arbitrary[Boolean]
    previousAddress <- Gen.option(addressGenerator)
  } yield {
    Json.obj("isPreviousAddressLast12Month" -> isPreviousAddressLast12Month,
      "previousAddress" -> previousAddress)
  }

  val personalDetailsGenerator : Gen[JsObject] = for {
    firstName <- Gen.alphaStr
    middleName <- Gen.option(Gen.alphaStr)
    lastName <- Gen.alphaStr
    dateOfBirth <- dateGenerator
  } yield {
    Json.obj(
      "firstName" -> firstName,
      "middleName" -> middleName,
      "lastName" -> lastName,
      "dateOfBirth" -> dateOfBirth
    )
  }

}
