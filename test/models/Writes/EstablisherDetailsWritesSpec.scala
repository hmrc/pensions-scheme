package models.Writes

import models.EstablisherDetails
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsValue, Json}
import utils.{PensionSchemeGenerators, SchemaValidatorForTests}

class EstablisherDetailsWritesSpec extends WordSpec with MustMatchers with OptionValues with PensionSchemeGenerators {
  val schemaValidator = SchemaValidatorForTests()

  "An establisher details object" should {
    "map correctly to an update payload for API 1468" when {
      "we have directors, companies and partnerhips" in {
        forAll(establisherDetailsGen) {
          establishers => {
            val mappedEstablishers: JsValue = Json.toJson(establishers)(EstablisherDetails.updateWrites)

            val validationErrors = schemaValidator.validateJson(mappedEstablishers,"establisherDetailsUpdate.json")

            validationErrors mustBe None
          }
        }
      }
    }
  }
}
