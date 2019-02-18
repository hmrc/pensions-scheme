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

import models.PensionSchemeUpdateDeclaration
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.Json
import utils.{PensionSchemeGenerators, SchemaValidatorForTests}

class PensionSchemeDeclarationWriteSpec extends WordSpec with MustMatchers with OptionValues with PensionSchemeGenerators with SchemaValidatorForTests {

  "PensionSchemeDeclaration" must{

    "write correct for valid scheme declaration" in {

      val declaration = PensionSchemeUpdateDeclaration(true)

      val valid = Json.toJson("pensionSchemeDeclaration" -> Json.toJson(declaration))

      val result = validateJson(elementToValidate = valid,
        schemaFileName = "api1468_schema.json",
        schemaNodePath = "#/properties/pensionSchemeDeclaration")

      result.isSuccess mustBe true
    }

    "invalid scheme declaration with incorrect format" in {

      val valid = Json.obj("pensionSchemeDeclaration" -> Json.obj("declaration1" -> "INVALID"))

      val result = validateJson(elementToValidate = valid,
        schemaFileName = "api1468_schema.json",
        schemaNodePath = "#/properties/pensionSchemeDeclaration")

      result.isError mustBe true
    }
  }

}
