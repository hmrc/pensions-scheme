package models.Writes

import models.{Address, Individual}
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.{JsArray, JsPath, JsValue, Json}
import utils.{PensionSchemeGenerators, SchemaValidatorForTests}
import wolfendale.scalacheck.regexp.RegexpGen



class IndividualWritesSpec extends WordSpec with MustMatchers with OptionValues with PensionSchemeGenerators {

  val schemaValidator = SchemaValidatorForTests()

  "An Individual object" should {
    "map correctly to an update payload for API 1468" when {
      "we have a list of individuals" in {
        forAll(individualGen) {
          director => {
            val mappedDirectors: JsValue = Json.toJson(director)(Individual.updateWrites)

            val validationErrors = schemaValidator.validateJson(mappedDirectors,"individualUpdate.json")

            validationErrors mustBe None
          }
        }
      }
    }
  }
}
