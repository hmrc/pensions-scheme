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

package utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchemaFactory
import play.api.libs.json.JsValue

case class SchemaValidatorForTests() {
  def validateJson(elementToValidate: JsValue, schemaFileName: String): Option[Array[AnyRef]] = {
    val schemaUrl = getClass.getResource(s"/schemas/$schemaFileName")
    val jsonSchemaFactory = JsonSchemaFactory.getInstance()
    val schema = jsonSchemaFactory.getSchema(schemaUrl)

    val jsonToValidate  = new ObjectMapper().readTree(elementToValidate.toString())

    val result = schema.validate(jsonToValidate).toArray()

    if (result.nonEmpty) Some(result) else None
  }

  def validateJson(elementToValidate: JsValue, schemaFileName: String, nodeName: String): Option[Array[AnyRef]] = {
    val schemaUrl = getClass.getResource(s"/schemas/$schemaFileName")
    val jsonSchemaFactory = JsonSchemaFactory.getInstance()
    val schemaNode = jsonSchemaFactory.getSchema(schemaUrl).getRefSchemaNode(nodeName)
    val schema = jsonSchemaFactory.getSchema(schemaNode)
    val schemaRoot = jsonSchemaFactory.getSchema(schemaUrl).getSchemaNode

    val jsonToValidate  = new ObjectMapper().readTree(elementToValidate.toString())

    val result = schema.validate(schemaNode, schemaRoot, nodeName).toArray()

    if (result.nonEmpty) Some(result) else None
  }
}