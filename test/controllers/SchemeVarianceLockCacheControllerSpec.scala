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

package controllers

import akka.stream.Materializer
import models.{SchemeVariance, SchemeVarianceLock}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.LockRepository
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.UnauthorizedException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SchemeVarianceLockCacheControllerSpec extends WordSpec with MustMatchers with MockitoSugar with GuiceOneAppPerSuite {

  implicit lazy val mat: Materializer = app.materializer

  private def configuration() = Configuration(
    "mongodb.pensions-scheme-cache.maxSize" -> 512000
  )

  val lockRepo = mock[LockRepository]
  val authConnector = mock[AuthConnector]
  val cc = app.injector.instanceOf[ControllerComponents]



  def controller(repo: LockRepository, authConnector: AuthConnector): SchemeVarianceLockCacheController = {
    new SchemeVarianceLockCacheController(configuration(), repo, authConnector, cc)
  }


  "SchemeCacheController" when {

    "lock" must {

      "return 200 when lock is obtained" in {

        when(lockRepo.lock(any())).thenReturn(Future.successful(SchemeVarianceLock(true, true)))
        when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))

        val result = call(controller(lockRepo, authConnector).lock("psaId", "srn"), FakeRequest("POST", "/"))

        status(result) mustEqual OK
        contentAsString(result) mustEqual Json.toJson(SchemeVarianceLock(true, true)).toString()
      }

      "return 200 when lock is not obtained" in {

        when(lockRepo.lock(any())).thenReturn(Future.successful(SchemeVarianceLock(true, false)))
        when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))

        val result = call(controller(lockRepo, authConnector).lock("psaId", "srn"), FakeRequest("POST", "/"))

        status(result) mustEqual OK
        contentAsString(result) mustEqual Json.toJson(SchemeVarianceLock(true, false)).toString()
      }

      "throw an exception when the repository call fails" in {
        when(lockRepo.lock(any())).thenReturn(
          Future.failed(new Exception()))
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller(lockRepo, authConnector).lock("psaId", "srn")(FakeRequest())

        an[Exception] must be thrownBy {
          status(result)
        }
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed {
          new UnauthorizedException("")
        }

        val result = call(controller(lockRepo, authConnector).lock("psaId", "srn"), FakeRequest("POST", "/"))

        an[UnauthorizedException] must be thrownBy {
          status(result)
        }
      }
    }

    "getLock" when {

      "return 200 and the relevant data when it exists" in {
        when(lockRepo.getExistingLock(eqTo(SchemeVariance("psaId", "srn")))).thenReturn(
          Future.successful {Some(SchemeVariance("psaId", "srn"))})
        when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))

        val result = controller(lockRepo, authConnector).getLock("psaId", "srn")(FakeRequest())

        status(result) mustEqual OK
        contentAsString(result) mustEqual Json.toJson(SchemeVariance("psaId", "srn")).toString()
      }

      "return 404 when the data doesn't exist" in {
        when(lockRepo.getExistingLock(eqTo(SchemeVariance("psaId", "srn")))).thenReturn(
          Future.successful(None))
        when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))

        val result = controller(lockRepo, authConnector).getLock("psaId", "srn")(FakeRequest())

        status(result) mustEqual NOT_FOUND
      }

      "throw an exception when the repository call fails" in {
        when(lockRepo.getExistingLock(eqTo(SchemeVariance("psaId", "srn")))).thenReturn(
          Future.failed(new Exception()))
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller(lockRepo, authConnector).getLock("psaId", "srn")(FakeRequest())

        an[Exception] must be thrownBy {
          status(result)
        }
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed {
          new UnauthorizedException("")
        }

        val result = controller(lockRepo, authConnector).getLock("psaId", "srn")(FakeRequest())

        an[UnauthorizedException] must be thrownBy {
          status(result)
        }
      }
    }

    "releaseLock" when {

      "return 200 and the relevant data when it exists" in {
        when(lockRepo.releaseLock(eqTo(SchemeVariance("psaId", "srn")))).thenReturn(
          Future.successful {{}})
        when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))

        val result = controller(lockRepo, authConnector).releaseLock("psaId", "srn")(FakeRequest())

        status(result) mustEqual OK
      }

      "throw an exception when the repository call fails" in {
        when(lockRepo.releaseLock(eqTo(SchemeVariance("psaId", "srn")))).thenReturn(
          Future.failed(new Exception()))
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller(lockRepo, authConnector).releaseLock("psaId", "srn")(FakeRequest())

        an[Exception] must be thrownBy {
          status(result)
        }
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed {
          new UnauthorizedException("")
        }

        val result = controller(lockRepo, authConnector).releaseLock("psaId", "srn")(FakeRequest())

        an[UnauthorizedException] must be thrownBy {
          status(result)
        }
      }
    }
  }
}
