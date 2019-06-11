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

import akka.util.ByteString
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import play.api.http.HttpEntity
import play.api.mvc.{ResponseHeader, Result}
import uk.gov.hmrc.http.{HttpException, NotFoundException, Upstream4xxResponse}

class ErrorHandlerSpec extends WordSpec with MustMatchers {
  private val eh = new ErrorHandler {
    def testResult(ex: HttpException) = {
      result(ex)
    }
  }

  "recoverFromError" must {
    "return a not found exception when such is passed into" in {
      val testMessage = "a message"
      val exception = new NotFoundException(testMessage)

      val result = eh.recoverFromError(exception)
      ScalaFutures.whenReady(result.failed) {
        _.getMessage mustBe testMessage
      }
    }

    "return a 4xx exception when such is passed into" in {
      val testMessage = "INVALID_BUSINESS_PARTNER"
      val exception = Upstream4xxResponse(testMessage, 403, 403)

      val result = eh.recoverFromError(exception)
      ScalaFutures.whenReady(result.failed) {
        _.getMessage mustBe testMessage
      }
    }
  }

  "result" must {
    "return correct result for json" in {
      val testJson = "some json"
      val testMessage = s"Response body: '$testJson'"
      val ex = new HttpException(testMessage, 433)
      val expectedResult = Result(ResponseHeader(ex.responseCode), HttpEntity.Strict(ByteString(testJson), Some("application/json")))
      val result = eh.testResult(ex)
      result.header mustBe expectedResult.header
      result.body mustBe expectedResult.body
    }

    "return correct result for plain text" in {
      val testJson = "some text"
      val ex = new HttpException(testJson, 433)
      val expectedResult = Result(ResponseHeader(ex.responseCode), HttpEntity.Strict(ByteString(testJson), Some("text/plain")))
      val result = eh.testResult(ex)
      result.header mustBe expectedResult.header
      result.body mustBe expectedResult.body
    }
  }
}
