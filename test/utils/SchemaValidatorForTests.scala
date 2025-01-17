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
import scala.jdk.CollectionConverters.CollectionHasAsScala

// scalastyle:off
trait SchemaValidatorForTests {

  def validateJson(elementToValidate: JsValue,
                   schemaFileName: String,
                   relevantProperties: Array[String],
                   relevantDefinitions: Option[Array[Array[String]]]): Set[ValidationMessage] = {

    val schemaUrl: InputStream = getClass.getResourceAsStream(s"/schemas/$schemaFileName")

    def removeIrrelevantNodes(json: JsObject, nodes:Array[String]): JsValue = {
      val head = nodes.head
      val cleanedUpJson = JsObject(Seq(head -> (json \ head).get))

      val isArray = (json \ head \ "type").asOpt[String] match {
        case Some("array") => true
        case _ => false
      }

      if(nodes.length > 1) {
        def getObj(headJson: JsObject) = {
          val tailNode = nodes.tail.head
          val cleanedUpHeadJson = (headJson - "properties") +
            ("properties" -> removeIrrelevantNodes((headJson \ "properties").get.as[JsObject], nodes.tail)) -
            "required" +
            ("required" -> JsArray(Seq(JsString(tailNode))))
          cleanedUpJson - head + (head -> cleanedUpHeadJson)
        }
        if(isArray) {
          val headNode = (cleanedUpJson \ head).as[JsObject]
          val itemsNode = (headNode \ "items").as[JsObject]
          val propertiesNode = (itemsNode \ "properties").as[JsObject]
          val cleanedUpPropertiesNode = removeIrrelevantNodes(propertiesNode, nodes.tail)
          val cleanedUpItemsNode = itemsNode - "properties" + ("properties" -> cleanedUpPropertiesNode) -
            "required" + ("required" -> JsArray(Seq(JsString(nodes.tail.head))))
          val cleandUpArrayNode = headNode - "items" + ("items" -> cleanedUpItemsNode)
          cleanedUpJson - head + (head -> cleandUpArrayNode)

        } else {
          getObj((cleanedUpJson \ head).as[JsObject])
        }
      } else {
        cleanedUpJson
      }
    }

    def removeIrrelevantDefinitions(json: JsObject, nodes:Array[Array[String]]): JsValue = {
      nodes.foldLeft(Json.obj())({ case (acc, nodes) =>
        acc.deepMerge(removeIrrelevantNodes(json, nodes).as[JsObject])
      })
    }


    val schemaJson = Json.parse(schemaUrl).as[JsObject]
    val schemaJsonNoIrrelevantNodes = (schemaJson - "properties") +
      ("properties" -> removeIrrelevantNodes((schemaJson \ "properties").get.as[JsObject], relevantProperties)) -
      "required" +
      ("required" -> JsArray(Seq(JsString(relevantProperties.head))))

    val schemaJsonWithNoIrrelevantNodesAndDefinitions = {
      relevantDefinitions.map { relevantDefinitions =>
        schemaJsonNoIrrelevantNodes - "definitions" +
          ("definitions" -> removeIrrelevantDefinitions((schemaJson \ "definitions").get.as[JsObject], relevantDefinitions))
      }.getOrElse(schemaJsonNoIrrelevantNodes)
    }

    //println(Json.prettyPrint(schemaJsonWithNoIrrelevantNodesAndDefinitions))

    val factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4)
    val schema = factory.getSchema(schemaJsonWithNoIrrelevantNodesAndDefinitions.toString())

    val mapper = new ObjectMapper()
    val jsonNode = mapper.readTree(elementToValidate.toString())

    schema.validate(jsonNode).asScala.toSet
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
