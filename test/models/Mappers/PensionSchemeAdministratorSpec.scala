package models.Mappers

import models.{NumberOfDirectorOrPartnersType, _}
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

      "We have a valid idType" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input + ("idType" -> JsString("TestId")))(apiReads).asOpt.value

        result.idType mustEqual Some("TestId")
      }

      "We have a valid idNumber" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input + ("idNumber" -> JsString("TestIdNumber")))(apiReads).asOpt.value

        result.idNumber mustEqual Some("TestIdNumber")
      }

      "We have a moreThanTenDirectors flag" in {
        val result = Json.fromJson[PensionSchemeAdministrator](input + ("moreThanTenDirectors" -> JsBoolean(true)))(apiReads).asOpt.value

        result.numberOfDirectorOrPartners.value.isMorethanTenDirectors.value mustEqual true
      }
    }

    "Map NumberOfDirectorOrPartnersType correctly" when {
      "we have moreThanTenDirectors flag" in {
        val input = Json.obj("moreThanTenDirectors" -> JsBoolean(true))
        val result = Json.fromJson[NumberOfDirectorOrPartnersType](input)(numberOfDirectorOrPartnersTypeReads).asOpt.value

        result.isMorethanTenDirectors mustEqual Some(true)
      }
    }
  }

  val numberOfDirectorOrPartnersTypeReads : Reads[NumberOfDirectorOrPartnersType] = {
    (
      (JsPath \ "moreThanTenDirectors").readNullable[Boolean] and
      (JsPath \ "moreThanTenPartners").readNullable[Boolean]
    )(NumberOfDirectorOrPartnersType.apply _)
  }

  val apiReads: Reads[PensionSchemeAdministrator] = (
    (JsPath \ "legalStatus").read[String] and
      (JsPath \ "sapNumber").read[String] and
      (JsPath \ "noIdentifier").read[Boolean] and
      (JsPath \ "customerType").read[String] and
      (JsPath \ "idType").readNullable[String] and
      (JsPath \ "idNumber").readNullable[String] and
      JsPath.read(Reads.optionWithNull(numberOfDirectorOrPartnersTypeReads))
    ) ((legalStatus, sapNumber, noIdentifier, customerType, idType, idNumber, numberOfDirectorOrPartners) => PensionSchemeAdministrator(
    customerType = customerType,
    legalStatus = legalStatus,
    sapNumber = sapNumber,
    noIdentifier = noIdentifier,
    idType = idType,
    idNumber = idNumber,
    numberOfDirectorOrPartners = numberOfDirectorOrPartners,
    pensionSchemeAdministratoridentifierStatus = PensionSchemeAdministratorIdentifierStatusType(isExistingPensionSchemaAdministrator = false),
    correspondenceAddressDetail = UkAddressType(addressType = "", line1 = "", line2 = "", countryCode = "", postalCode = ""),
    correspondenceContactDetail = ContactDetails(telephone = "", email = ""),
    previousAddressDetail = PreviousAddressDetails(isPreviousAddressLast12Month = false)
  ))
}
