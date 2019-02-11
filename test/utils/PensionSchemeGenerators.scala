/*
 * Copyright 2019 HM Revenue & Customs
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

import models.{Address, InternationalAddress, UkAddress}
import org.scalacheck.Gen

trait PensionSchemeGenerators {
  val schemaValidator = SchemaValidatorForTests()
  val addressLineGen : Gen[String] = Gen.listOfN[Char] (35, Gen.alphaChar).map (_.mkString)
  val addressLineOptional: Gen[Option[String]] = Gen.option(addressLineGen)
  val postalCodeGem: Gen[String] = Gen.listOfN[Char] (10, Gen.alphaChar).map (_.mkString)
  val countryCode: Gen[String] = Gen.oneOf(Seq("ES","IT"))

  val ukAddressGen: Gen[Address] = for {
    line1 <- addressLineGen
    line2 <- addressLineGen
    line3 <- addressLineOptional
    line4 <- addressLineOptional
    postalCode <- postalCodeGem
  } yield UkAddress(line1,Some(line2),line3,line4,"GB",postalCode)

  val internationalAddressGen: Gen[Address] = for {
    line1 <- addressLineGen
    line2 <- addressLineGen
    line3 <- addressLineOptional
    line4 <- addressLineOptional
    countryCode <- countryCode
  } yield InternationalAddress(line1,Some(line2),line3,line4,countryCode)
}
