package models.Writes

import models.{PreviousAddressDetails, Samples}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.Json

class PreviousAddressDetailsWritesSpec extends WordSpec with MustMatchers with OptionValues with Samples{

  "A previous address details object" should {
    "Map previosaddressdetails inner object as `previousaddresdetail`" when {
      "required" in {
        val previousAddress = PreviousAddressDetails(true,Some(ukAddressSample))
        val result = Json.toJson(previousAddress)(PreviousAddressDetails.psaSubmissionWrites)

        result.toString() must include("\"previousAddressDetail\":")
      }
    }
  }
}
