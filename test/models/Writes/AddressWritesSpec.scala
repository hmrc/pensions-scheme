package models.Writes

import models.{InternationalAddress, UkAddress}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.Json

class AddressWritesSpec extends WordSpec with MustMatchers with OptionValues {

  "An address" should {
    "parse correctly to a valid DES format" when {
      "we have a UK address" when {
        val address = UkAddress("line1",Some("line2"),Some("line3"),Some("line4"),"GB","Test")
        "with address line 1" in {
          val result = Json.toJson(address)(UkAddress.writesToDes)

          result.toString() must include("line1")
        }

        "with address line 2" in {
          val result = Json.toJson(address)(UkAddress.writesToDes)

          result.toString() must include("line2")
        }

        "with address line 3" in {
          val result = Json.toJson(address)(UkAddress.writesToDes)

          result.toString() must include("line3")
        }

        "with address line 4" in {
          val result = Json.toJson(address)(UkAddress.writesToDes)

          result.toString() must include("line4")
        }

        "with countrycode" in {
          val result = Json.toJson(address)(UkAddress.writesToDes)

          result.toString() must include("countryCode")
        }

        "with postalcode" in {
          val result = Json.toJson(address)(UkAddress.writesToDes)

          result.toString() must include("postalCode")
        }
      }

      "we have an International address" when {
        val address = InternationalAddress("line1",Some("line2"),Some("line3"),Some("line4"),"IT",Some("test"))

        "with address line 1" in {
          val result = Json.toJson(address)(InternationalAddress.writesToDes)

          result.toString() must include("line1")
        }

        "with address line 2" in {
          val result = Json.toJson(address)(InternationalAddress.writesToDes)

          result.toString() must include("line2")
        }

        "with address line 3" in {
          val result = Json.toJson(address)(InternationalAddress.writesToDes)

          result.toString() must include("line3")
        }

        "with address line 4" in {
          val result = Json.toJson(address)(InternationalAddress.writesToDes)

          result.toString() must include("line4")
        }

        "with countrycode" in {
          val result = Json.toJson(address)(InternationalAddress.writesToDes)

          result.toString() must include("countryCode")
        }

        "with postalcode" in {
          val result = Json.toJson(address)(InternationalAddress.writesToDes)

          result.toString() must include("postalCode")
        }
      }
    }
  }
}
