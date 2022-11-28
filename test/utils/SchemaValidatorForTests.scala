/*
 * Copyright 2022 HM Revenue & Customs
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

package utils

import com.eclipsesource.schema.drafts.Version4.schemaTypeReads
import com.eclipsesource.schema.drafts._
import com.eclipsesource.schema.{JsonSource, SchemaValidator}
import play.api.libs.json.{JsResult, JsValue}

// https://github.com/networknt/json-schema-validator/pull/625
// use SchemaValidatorForTests after fix
trait SchemaValidatorForTests {

  def validateJson(elementToValidate: JsValue, schemaFileName: String, schemaNodePath: String): JsResult[JsValue] = {

    val rootSchema = JsonSource.schemaFromStream(getClass.getResourceAsStream(s"/schemas/$schemaFileName")).get

    val schema = JsonSource.schemaFromString(
      s"""{
         |  "additionalProperties": { "$$ref": "/schemas/$schemaFileName$schemaNodePath" }
         |}""".stripMargin).get

    val validator = SchemaValidator(Some(Version4))
      .addSchema(s"/schemas/$schemaFileName", rootSchema)

    validator.validate(schema, elementToValidate)
  }

  def validateJson(elementToValidate: JsValue, schemaFileName: String): JsResult[JsValue] = {

    val rootSchema = JsonSource.schemaFromUrl(getClass.getResource(s"/schemas/$schemaFileName")).get
    val validator = SchemaValidator(Some(Version4))
    validator.validate(rootSchema, elementToValidate)
  }

}
