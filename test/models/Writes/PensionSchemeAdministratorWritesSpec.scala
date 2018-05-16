package models.Writes

import models.{PensionSchemeAdministrator, PreviousAddressDetails, Samples}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.Json

class PensionSchemeAdministratorWritesSpec extends WordSpec with MustMatchers with OptionValues with Samples {
  "An object of a Pension Scheme Administrator" should {
    "Map all previousdetailsaddressess to `previousDetail`" when {
      "We are doing a PSA submission containing previous address at rool level" in {
        val result = Json.toJson(pensionSchemeAdministratorSample.copy(previousAddressDetail = PreviousAddressDetails(true,Some(ukAddressSample))))(PensionSchemeAdministrator.psaSubmissionWrites)

        result.toString() must include("true,\"previousAddressDetail\":")
      }

      "We are doing a PSA submission with directors that have previous address" in {
        val directorWithPreviousAddress = directorSample.copy(previousAddressDetail = PreviousAddressDetails(true,Some(ukAddressSample)))
        val result = Json.toJson(pensionSchemeAdministratorSample.copy(directorOrPartnerDetail = Some(List(directorWithPreviousAddress))))(PensionSchemeAdministrator.psaSubmissionWrites)

        println(result)
        result.toString() must include("true,\"previousAddressDetail\":")
      }
    }
  }
}
