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

import com.networknt.schema.{JsonSchema, JsonSchemaFactory, SpecVersion}
import org.scalactic.Equality
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.Logger
import play.api.libs.json.{JsNull, Json}

class InvalidPayloadHandlerSpec extends AnyFlatSpec with Matchers {

  import InvalidPayloadHandlerSpec._

  "loadSchema" should "load the Scheme Subscription schema" in {
    val logger = testFixture().handler
    val schema = logger.loadSchema("/resources/schemas/schemeSubscription.json")
    schema shouldBe a[JsonSchema]
    schema.toString should include("API#1359")

  }

  "log" should "handle enum failure" in {
    val json = Json.obj("test" -> "three")
    val expected = ValidationFailure("enum", "$.test: does not have a value in the enumeration [\"one\", \"two\"]", None)
    val logger = testFixture().handler
    val actual = logger.getFailures(enumSchema, json)
    actual.size shouldBe 1
    actual.head shouldEqual expected
  }

  it should "handle maximum failure" in {
    val json = Json.obj("test" -> 11)
    val expected = ValidationFailure("maximum", "$.test: must have a maximum value of 10.0", None)
    val logger = testFixture().handler
    val actual = logger.getFailures(maximumSchema, json)
    actual.size shouldBe 1
    actual.head shouldEqual expected
  }

  it should "handle maxLength failure" in {
    val json = Json.obj("test" -> "abc")
    val expected = ValidationFailure("maxLength", "$.test: must be at most 2 characters long", None)
    val logger = testFixture().handler
    val actual = logger.getFailures(maxLengthSchema, json)
    actual.size shouldBe 1
    actual.head shouldEqual expected
  }

  it should "handle minimum failure" in {
    val json = Json.obj("test" -> 0)
    val expected = ValidationFailure("minimum", "$.test: must have a minimum value of 1.0", None)
    val logger = testFixture().handler
    val actual = logger.getFailures(minimumSchema, json)
    actual.size shouldBe 1
    actual.head shouldEqual expected
  }

  it should "handle minLength failure" in {
    val json = Json.obj("test" -> "a")
    val expected = ValidationFailure("minLength", "$.test: must be at least 2 characters long", None)
    val logger = testFixture().handler
    val actual = logger.getFailures(minLengthSchema, json)
    actual.size shouldBe 1
    actual.head shouldEqual expected
  }

  it should "handle pattern failure" in {
    val json = Json.obj("test" -> "123")
    val expected = ValidationFailure("pattern", "$.test: does not match the regex pattern ^[A-Z]{2}$", None)
    val logger = testFixture().handler
    val actual = logger.getFailures(patternSchema, json)
    actual.size shouldBe 1
    actual.head shouldEqual expected
  }

  it should "handle required failure" in {
    val json = Json.obj()
    val expected = ValidationFailure("required", "$: required property 'test' not found", None)
    val logger = testFixture().handler
    val actual = logger.getFailures(requiredSchema, json)
    actual.size shouldBe 1
    actual.head shouldEqual expected
  }

  it should "handle type failure" in {
    val json = Json.obj("test" -> JsNull)
    val expected = ValidationFailure("type", "$.test: null found, string expected", None)
    val logger = testFixture().handler
    val actual = logger.getFailures(requiredSchema, json)
    actual.size shouldBe 1
    actual.head shouldEqual expected
  }

  it should "handle format failure" in {
    val json = Json.obj("test" -> "abc")
    val expected = ValidationFailure("format", "$.test: does not match the email pattern must be a valid RFC 5321 Mailbox", None)
    val logger = testFixture().handler
    val actual = logger.getFailures(formatSchema, json)
    actual.size shouldBe 1
    actual.head shouldEqual expected
  }

  it should "handle multiple failures" in {
    val json = Json.obj("test1" -> "abc", "test2" -> true)
    val maxLengthError = ValidationFailure("maxLength", "$.test1: must be at most 2 characters long", None)
    val typeError = ValidationFailure("type", "$.test2: boolean found, string expected", None)
    val logger = testFixture().handler
    val actual = logger.getFailures(multiSchema, json)
    actual.size shouldBe 2
    actual should contain.allOf(maxLengthError, typeError)
  }
}

object InvalidPayloadHandlerSpec {

  trait TestFixture {
    def logger: Logger

    def handler: InvalidPayloadHandlerImpl
  }

  def testFixture(): TestFixture = new TestFixture {
    override val logger: Logger = Logger(classOf[InvalidPayloadHandler])
    override val handler: InvalidPayloadHandlerImpl = new InvalidPayloadHandlerImpl()
  }

  val enumSchemaString: String =
    """
      | {
      |   "$schema": "http://json-schema.org/draft-04/schema#",
      |   "type": "object",
      |   "properties": {
      |     "test": {
      |       "type": "string",
      |       "enum": [
      |         "one",
      |         "two"
      |       ]
      |     }
      |   }
      | }
    """.stripMargin

  lazy val enumSchema: JsonSchema = loadSchema(enumSchemaString)

  val maximumSchemaString: String =
    """
      | {
      |   "$schema": "http://json-schema.org/draft-04/schema#",
      |   "type": "object",
      |   "properties": {
      |     "test": {
      |       "type": "number",
      |       "maximum": 10.0
      |     }
      |   }
      | }
      |    """.stripMargin

  lazy val maximumSchema: JsonSchema = loadSchema(maximumSchemaString)

  val maxLengthSchemaString: String =
    """
      | {
      |   "$schema": "http://json-schema.org/draft-04/schema#",
      |   "type": "object",
      |   "properties": {
      |     "test": {
      |       "type": "string",
      |       "maxLength": 2
      |     }
      |   }
      | }
      |    """.stripMargin

  lazy val maxLengthSchema: JsonSchema = loadSchema(maxLengthSchemaString)

  val minimumSchemaString: String =
    """
      | {
      |   "$schema": "http://json-schema.org/draft-04/schema#",
      |   "type": "object",
      |   "properties": {
      |     "test": {
      |       "type": "number",
      |       "minimum": 1.0
      |     }
      |   }
      | }
      |    """.stripMargin

  lazy val minimumSchema: JsonSchema = loadSchema(minimumSchemaString)

  val minLengthSchemaString: String =
    """
      | {
      |   "$schema": "http://json-schema.org/draft-04/schema#",
      |   "type": "object",
      |   "properties": {
      |     "test": {
      |       "type": "string",
      |       "minLength": 2
      |     }
      |   }
      | }
      |    """.stripMargin

  lazy val minLengthSchema: JsonSchema = loadSchema(minLengthSchemaString)

  val patternSchemaString: String =
    """
      | {
      |   "$schema": "http://json-schema.org/draft-04/schema#",
      |   "type": "object",
      |   "properties": {
      |     "test": {
      |       "type": "string",
      |       "pattern": "^[A-Z]{2}$"
      |     }
      |   }
      | }
      |    """.stripMargin

  lazy val patternSchema: JsonSchema = loadSchema(patternSchemaString)

  val requiredSchemaString: String =
    """
      | {
      |   "$schema": "http://json-schema.org/draft-04/schema#",
      |   "type": "object",
      |   "properties": {
      |     "test": {
      |       "type": "string"
      |     }
      |   },
      |   "required": ["test"]
      | }
      |    """.stripMargin

  lazy val requiredSchema: JsonSchema = loadSchema(requiredSchemaString)

  val formatSchemaString: String =
    """
      | {
      |   "$schema": "http://json-schema.org/draft-04/schema#",
      |   "type": "object",
      |   "properties": {
      |     "test": {
      |       "type": "string",
      |       "format": "email"
      |     }
      |   }
      | }
      |    """.stripMargin

  lazy val formatSchema: JsonSchema = loadSchema(formatSchemaString)

  val multiSchemaString: String =
    """
      | {
      |   "$schema": "http://json-schema.org/draft-04/schema#",
      |   "type": "object",
      |   "properties": {
      |     "test1": {
      |       "type": "string",
      |       "maxLength": 2
      |     },
      |     "test2": {
      |       "type": "string"
      |     }
      |   }
      | }
      |    """.stripMargin

  lazy val multiSchema: JsonSchema = loadSchema(multiSchemaString)

  def loadSchema(schemaString: String): JsonSchema = {

    val factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4)
    factory.getSchema(schemaString)
  }

  implicit val validationFailureEquality: Equality[ValidationFailure] = (a: ValidationFailure, b: Any) => {
    b match {
      case failure: ValidationFailure =>
        a.failureType == failure.failureType && a.message.contains(failure.message) && a.value == failure.value
      case _ => false
    }
  }

}
