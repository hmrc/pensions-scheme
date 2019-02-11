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

package models.Writes

import java.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.{JsonSchemaFactory, ValidationMessage}
import models.{Address, InternationalAddress, UkAddress}
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsValue, Json}



class AddressWritesSpec extends WordSpec with MustMatchers with OptionValues {

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


  case class SchemaValidatorForTests() {
    def validateJson(elementToValidate: JsValue, schemaFileName: String): Array[AnyRef] = {
      val schemaUrl = getClass.getResource(s"/resources/schemas/$schemaFileName")
      val factory = JsonSchemaFactory.getInstance()
      val schema = factory.getSchema(schemaUrl)


      val mapper = new ObjectMapper()
      val jsonToValidate  = mapper.readTree(elementToValidate.toString())

      schema.validate(jsonToValidate).toArray()
    }
  }


  "An updated address" should {
    "parse correctly to a valid DES format" when {
      "we have a UK address" in {
        forAll(ukAddressGen) {
          address => {
            val mappedAddress: JsValue = Json.toJson(address)(Address.updateWrites)

            val validationErrors = SchemaValidatorForTests().validateJson(mappedAddress,"address.json")

            validationErrors mustBe Array.empty
          }
        }
      }

      "we have an international address" in {
        forAll(internationalAddressGen) {
          address => {
            val mappedAddress: JsValue = Json.toJson(address)(Address.updateWrites)

            val validationErrors = SchemaValidatorForTests().validateJson(mappedAddress,"address.json")

            validationErrors mustBe Array.empty
          }
        }
      }
    }
  }


  "An address" should {
    "parse correctly to a valid DES format" when {
      "we have a UK address" when {
        val address = UkAddress("line1", Some("line2"), Some("line3"), Some("line4"), "GB", "Test")
        val result = Json.toJson(address.asInstanceOf[Address])

        "with address line 1" in {
          result.toString() must include("line1")
        }

        "with address line 2" in {
          result.toString() must include("line2")
        }

        "with address line 3" in {
          result.toString() must include("line3")
        }

        "with address line 4" in {
          result.toString() must include("line4")
        }

        "with countrycode" in {
          result.toString() must include("countryCode")
        }

        "with postalcode" in {
          result.toString() must include("postalCode")
        }

        "with an address type of UK" in {
          result.toString() must include("\"addressType\":\"UK\"")
        }
      }

      "we have an International address" when {
        val address = InternationalAddress("line1", Some("line2"), Some("line3"), Some("line4"), "IT", Some("test"))
        val result = Json.toJson(address.asInstanceOf[Address])

        "with address line 1" in {
          result.toString() must include("line1")
        }

        "with address line 2" in {
          result.toString() must include("line2")
        }

        "with address line 3" in {
          result.toString() must include("line3")
        }

        "with address line 4" in {
          result.toString() must include("line4")
        }

        "with countrycode" in {
          result.toString() must include("countryCode")
        }

        "with postalcode" in {
          result.toString() must include("postalCode")
        }

        "with an address type of Non-UK" in {
          result.toString() must include("\"addressType\":\"NON-UK\"")
        }
      }
    }
  }
}
