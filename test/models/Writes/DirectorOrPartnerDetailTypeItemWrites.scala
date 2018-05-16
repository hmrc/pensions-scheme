package models.Writes

import models.{DirectorOrPartnerDetailTypeItem, PreviousAddressDetails, Samples}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.Json

class DirectorOrPartnerDetailTypeItemWrites extends WordSpec with MustMatchers with OptionValues with Samples {
  "An object of a directorOrPartner detail type item" should {
    "Map previosaddressdetails inner object as `previousaddresdetail`" when {
      "required" in {
        val result = Json.toJson(directorSample.copy(previousAddressDetail = PreviousAddressDetails(true,Some(ukAddressSample))))(DirectorOrPartnerDetailTypeItem.psaSubmissionWrites)

        result.toString() must include("true,\"previousAddressDetail\":")
      }
    }
  }
}
