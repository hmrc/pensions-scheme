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
import com.google.inject.ImplementedBy
import com.networknt.schema.{JsonSchema, JsonSchemaFactory, SpecVersion, ValidationMessage}
import play.api.Logger
import play.api.libs.json._

import java.io.InputStream
import javax.inject.Inject
import scala.collection.mutable
import scala.jdk.CollectionConverters._

@ImplementedBy(classOf[InvalidPayloadHandlerImpl])
trait InvalidPayloadHandler {

  def getFailures(schemaFileName: String, json: JsValue): Set[ValidationFailure]

  def logFailures(schemaFileName: String, json: JsValue, args: String*): Unit

}

class InvalidPayloadHandlerImpl @Inject() extends InvalidPayloadHandler {
  private val logger = Logger(classOf[InvalidPayloadHandler])

  private[utils] def loadSchema(schemaFileName: String): JsonSchema = {
    val schemaUrl: InputStream = getClass.getResourceAsStream(schemaFileName)
    val factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4)
    factory.getSchema(schemaUrl)
  }

  override def getFailures(schemaFileName: String, json: JsValue): Set[ValidationFailure] = {

    val schema = loadSchema(schemaFileName)
    getFailures(schema, json)

  }

  private[utils] def getFailures(schema: JsonSchema, json: JsValue): Set[ValidationFailure] = {

    val mapper = new ObjectMapper()
    val jsonNode = mapper.readTree(json.toString())

    val set: Set[ValidationMessage] = schema.validate(jsonNode).asScala.toSet

    set.map {
      message =>
        val value = valueFromJson(message, json)
        ValidationFailure(message.getType, message.getMessage, value)
    }

  }

  override def logFailures(schemaFileName: String, json: JsValue, args: String*): Unit = {

    val schema = loadSchema(schemaFileName)
    logFailure(schema, json, args)

  }

  private[utils] def logFailure(schema: JsonSchema, json: JsValue, args: Seq[String]): Unit = {

    val failures = getFailures(schema, json)
    val msg = new mutable.StringBuilder()

    msg.append(s"Invalid Payload JSON Failures${if (args.nonEmpty) s" for url: ${args.head}"}\n")
    msg.append(s"Failures: ${failures.size}\n")
    msg.append("\n")

    failures.foreach {
      failure =>
        msg.append(s"${failure.message}\n")
        msg.append(s"Type: ${failure.failureType}\n")
        msg.append(s"Value: ${failure.value.getOrElse("[none]")}\n")
        msg.append("\n")
    }

    logger.warn(msg.toString())

  }

  private def valueFromJson(message: ValidationMessage, json: JsValue): Option[String] = {
    message.getType match {
      case "enum" | "format" | "maximum" | "maxLength" | "minimum" | "minLength" | "pattern" | "type" =>
        (json \ message.getMessageKey.drop(2)).asOpt[JsValue] match {
          case Some(JsBoolean(bool)) => Some(bool.toString)
          case Some(JsNull) => Some("null")
          case Some(JsNumber(n)) => Some(depersonalise(n.toString))
          case Some(JsString(s)) => Some(depersonalise(s))
          case _ => None
        }
      case _ => None
    }
  }

  private def depersonalise(value: String): String = {
    value
      .replaceAll("[a-zA-Z]", "x")
      .replaceAll("[0-9]", "9")
  }

}

case class ValidationFailure(failureType: String, message: String, value: Option[String])
