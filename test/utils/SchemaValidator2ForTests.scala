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


import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.{JsonSchemaFactory, SpecVersion}
import play.api.libs.json._

import java.io.InputStream
import scala.jdk.CollectionConverters.CollectionHasAsScala

// https://github.com/networknt/json-schema-validator/pull/625
trait SchemaValidator2ForTests {

  def validateJson(elementToValidate: JsValue, schemaFileName: String, schemaNodePath: String): JsResult[JsValue] = {
    val mapper = new ObjectMapper()
    val jsonNode = mapper.readTree(elementToValidate.toString())

    val schemaUrl: InputStream = getClass.getResourceAsStream(schemaFileName)
    val factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4)
    val parentSchema = factory.getSchema(schemaUrl)

    val refNode =
      s"""{
         |  "additionalProperties": {
         |  "$$ref": "file:///schemas/$schemaFileName$schemaNodePath" }
         |}""".stripMargin

    val subSchema = parentSchema.getRefSchemaNode(refNode)
    val result = factory.getSchema(subSchema).validate(jsonNode).asScala.toSet

    if (result.isEmpty) {
      JsSuccess(elementToValidate)
    } else {
      JsError()
    }

    //    val deepValidationCheck = true
    //    val factory = JsonSchemaFactory.byDefault()
    //    val schemaPath = JsonLoader.fromResource(s"/schemas/$schemaFileName")
    //    val rootSchema = factory.getJsonSchema(schemaPath)
    //    val subSchema = factory.getJsonSchema(JsonLoader.fromString(
    //      s"""{
    //         |  "additionalProperties": { "$$ref": "/schemas/$schemaFileName$schemaNodePath" }
    //         |}""".stripMargin))
    //    val jsonDataAsString = JsonLoader.fromString(elementToValidate.toString())
    //    val result = rootSchema.validate(jsonDataAsString, deepValidationCheck)
    //    if(result.isSuccess)
    //      JsSuccess(elementToValidate)
    //    else
    //      JsError("")
  }
}