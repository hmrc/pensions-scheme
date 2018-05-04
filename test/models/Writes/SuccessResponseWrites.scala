package models.Writes

import models._
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.Json

class SuccessResponseWrites extends WordSpec with MustMatchers with OptionValues with Samples {
  "A success response object" should {
    "Map an address as default type" when {
      val successRespone = SuccessResponse("test", "test", true,Some(IndividualType("Test",None,"Test")),Some(OrganisationType("Test")), ukAddressSample, ContactCommDetailsType())
      val response = Json.toJson(successRespone)

      "We have an address" in {
        response.toString() must include("addressLine1")
      }

      "We have a safeId" in {
        response.toString() must include("safeId")
      }

      "We have a sapNumber" in {
        response.toString() must include("sapNumber")
      }

      "We have a isAnIndividual" in {
        response.toString() must include("isAnIndividual")
      }

      "We have a individual" in {
        response.toString() must include("individual")
      }

      "We have a organisation" in {
        response.toString() must include("organisation")
      }

      "We have contactDetails" in {
        response.toString() must include("contactDetails")
      }
    }
  }
}
