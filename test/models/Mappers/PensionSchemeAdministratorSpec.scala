package models.Mappers

import models._
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.Json
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

class PensionSchemeAdministratorSpec extends WordSpec with MustMatchers with OptionValues {
  "JSON Payload of a PSA" should {
    "Map to a valid PensionSchemeAdministrator object" when {
      val input = Json.obj(
        "legalStatus" -> "Individual",
        "sapNumber" -> "NumberTest",
        "noIdentifier" -> JsBoolean(true),
        "customerType" -> "TestCustomer"
      )

      "We have a valid legalStatus" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(apiReads).asOpt.value

        result.legalStatus mustEqual "Individual"
      }

      "We have a valid sapNumber" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(apiReads).asOpt.value

        result.sapNumber mustEqual "NumberTest"
      }

      "We have a valid noIdentifier" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(apiReads).asOpt.value

        result.noIdentifier mustEqual true
      }

      "We have valid customerType" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input)(apiReads).asOpt.value

        result.customerType mustEqual "TestCustomer"
      }

    }
  }


  val apiReads: Reads[PensionSchemeAdministrator] = (
    (JsPath \ "legalStatus").read[String] and
      (JsPath \ "sapNumber").read[String] and
      (JsPath \ "noIdentifier").read[Boolean] and
      (JsPath \ "customerType").read[String]
    ) ((legalStatus, sapNumber, noIdentifier, customerType) => PensionSchemeAdministrator(customerType = customerType,
    legalStatus = legalStatus,
    sapNumber = sapNumber,
    noIdentifier = noIdentifier,
    pensionSchemeAdministratoridentifierStatus = PensionSchemeAdministratorIdentifierStatusType(isExistingPensionSchemaAdministrator = false),
    correspondenceAddressDetail = UkAddressType(addressType = "", line1 = "", line2 = "", countryCode = "", postalCode = ""),
    correspondenceContactDetail = ContactDetails(telephone = "", email = ""),
    previousAddressDetail = PreviousAddressDetails(isPreviousAddressLast12Month = false)
  ))
}
