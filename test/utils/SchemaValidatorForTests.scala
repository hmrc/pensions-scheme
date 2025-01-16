/*
 * Copyright 2024 HM Revenue & Customs
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
import com.networknt.schema.{JsonSchemaFactory, SpecVersion, ValidationMessage}
import play.api.libs.json._

import java.io.InputStream
import scala.annotation.tailrec
import scala.jdk.CollectionConverters.CollectionHasAsScala

trait SchemaValidatorForTests {

  def validateJson(elementToValidate: JsValue, schemaFileName: String, schemaNodePath: String): JsResult[JsValue] = {

    val relevantNodes: Array[String] = schemaNodePath.split("/").drop(2)

    val schemaUrl: InputStream = getClass.getResourceAsStream(s"/schemas/$schemaFileName")

    @tailrec
    def removeIrrelevantNodes(json: JsObject, nodes:Array[String]): JsValue = {
      val head = nodes.head
      val cleanedUpJson = JsObject(Seq(head -> (json \ head).get))
      if(nodes.length > 1) {
        removeIrrelevantNodes(cleanedUpJson, nodes.tail)
      } else {
        cleanedUpJson
      }
    }


    val schemaJson = Json.parse(schemaUrl).as[JsObject]
    val schemaJsonNoIrrelevantNodes = (schemaJson - "properties") +
      ("properties" -> removeIrrelevantNodes((schemaJson \ "properties").get.as[JsObject], relevantNodes)) -
      "required" +
      ("required" -> JsArray(Seq(JsString(relevantNodes(0)))))


    val factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4)
    val schema = factory.getSchema(schemaJsonNoIrrelevantNodes.toString())

    val mapper = new ObjectMapper()
    val jsonNode = mapper.readTree(elementToValidate.toString())

    val set = schema.validate(jsonNode).asScala.toSet
    if(set.isEmpty) JsSuccess(JsObject(Seq()))
    else JsError(set.toString)
  }

  def validateJson(elementToValidate: JsValue, schemaFileName: String): Set[ValidationMessage] = {

    val schemaUrl: InputStream = getClass.getResourceAsStream(s"/schemas/$schemaFileName")
    val factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4)
    val schema = factory.getSchema(schemaUrl)
    val mapper = new ObjectMapper()
    val jsonNode = mapper.readTree(elementToValidate.toString())

    schema.validate(jsonNode).asScala.toSet
  }

}
