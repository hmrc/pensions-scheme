/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.cache

import akka.stream.Materializer
import models.{SchemeLock, SchemeVariance, VarianceLock}
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.MockitoSugar
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
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

class SchemeVarianceLockCacheControllerSpec extends AnyWordSpec with Matchers with MockitoSugar with GuiceOneAppPerSuite {

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

  def getRequest(request : FakeRequest[_]): FakeRequest[_] =
    request.withHeaders(("psaId", "A2100005"), ("srn", "SR0000001"))


  "SchemeCacheController" when {

    "lock" must {

      "return 200 when lock is obtained" in {

        when(lockRepo.lock(any())).thenReturn(Future.successful(VarianceLock))
        when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))

        val result = controller(lockRepo, authConnector).lock()(getRequest(FakeRequest("POST", "/")))

        status(result) mustEqual OK
        contentAsString(result) mustEqual Json.toJson("SuccessfulVarianceLock").toString()
      }

      "return 200 when lock is not obtained" in {

        when(lockRepo.lock(any())).thenReturn(Future.successful(SchemeLock))
        when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))

        val result = controller(lockRepo, authConnector).lock()(getRequest(FakeRequest("POST", "/")))

        status(result) mustEqual OK
        contentAsString(result) mustEqual Json.toJson("AnotherPsaHasLockedScheme").toString()
      }

      "throw an exception when the repository call fails" in {
        when(lockRepo.lock(any())).thenReturn(
          Future.failed(new Exception()))
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller(lockRepo, authConnector).lock()(FakeRequest())

        an[Exception] must be thrownBy {
          status(result)
        }
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed {
          new UnauthorizedException("")
        }

        val result = controller(lockRepo, authConnector).lock()(getRequest(FakeRequest("POST", "/")))

        an[UnauthorizedException] must be thrownBy {
          status(result)
        }
      }
    }

    "isLockByPsaIdOrSchemeId" must {

      "return 200 when lock is found" in {

        when(lockRepo.isLockByPsaIdOrSchemeId(any(), any())).thenReturn(Future.successful(Some(VarianceLock)))
        when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))

        val result = controller(lockRepo, authConnector).isLockByPsaIdOrSchemeId()(getRequest(FakeRequest("POST", "/")))

        status(result) mustEqual OK
        contentAsString(result) mustEqual Json.toJson("SuccessfulVarianceLock").toString()
      }

      "return 200 when lock is not found" in {

        when(lockRepo.isLockByPsaIdOrSchemeId(any(), any())).thenReturn(Future.successful(None))
        when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))

        val result = controller(lockRepo, authConnector).isLockByPsaIdOrSchemeId()(getRequest(FakeRequest("POST", "/")))

        status(result) mustEqual NOT_FOUND
      }

      "throw an exception when the repository call fails" in {
        when(lockRepo.isLockByPsaIdOrSchemeId(any(), any())).thenReturn(
          Future.failed(new Exception()))
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller(lockRepo, authConnector).isLockByPsaIdOrSchemeId()(FakeRequest())

        an[Exception] must be thrownBy {
          status(result)
        }
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed {
          new UnauthorizedException("")
        }

        val result = controller(lockRepo, authConnector).isLockByPsaIdOrSchemeId()(getRequest(FakeRequest("POST", "/")))

        an[UnauthorizedException] must be thrownBy {
          status(result)
        }
      }
    }

    "getLock" when {

      "return 200 and the relevant data when it exists" in {
        when(lockRepo.getExistingLock(eqTo(SchemeVariance("A2100005", "SR0000001")))).thenReturn(
          Future.successful {Some(SchemeVariance("A2100005", "SR0000001"))})
        when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))

        val result = controller(lockRepo, authConnector).getLock()(getRequest(FakeRequest("GET", "/")))

        status(result) mustEqual OK
        contentAsString(result) mustEqual Json.toJson(SchemeVariance("A2100005", "SR0000001")).toString()
      }

      "return 404 when the data doesn't exist" in {
        when(lockRepo.getExistingLock(eqTo(SchemeVariance("A2100005", "SR0000001")))).thenReturn(
          Future.successful(None))
        when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))

        val result = controller(lockRepo, authConnector).getLock()(getRequest(FakeRequest("GET", "/")))

        status(result) mustEqual NOT_FOUND
      }

      "throw an exception when the repository call fails" in {
        when(lockRepo.getExistingLock(eqTo(SchemeVariance("A2100005", "SR0000001")))).thenReturn(
          Future.failed(new Exception()))
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller(lockRepo, authConnector).getLock()(getRequest(FakeRequest()))

        an[Exception] must be thrownBy {
          status(result)
        }
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed {
          new UnauthorizedException("")
        }

        val result = controller(lockRepo, authConnector).getLock()(getRequest(FakeRequest()))

        an[UnauthorizedException] must be thrownBy {
          status(result)
        }
      }
    }

    "getLockByPsa" when {

      "return 200 and the relevant data when it exists" in {
        when(lockRepo.getExistingLockByPSA(eqTo("A2100005"))).thenReturn(
          Future.successful {Some(SchemeVariance("A2100005", "SR0000001"))})
        when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))

        val result = controller(lockRepo, authConnector).getLockByPsa()(getRequest(FakeRequest("GET", "/")))

        status(result) mustEqual OK
        contentAsString(result) mustEqual Json.toJson(SchemeVariance("A2100005", "SR0000001")).toString()
      }

      "return 404 when the data doesn't exist" in {
        when(lockRepo.getExistingLockByPSA(eqTo("A2100005"))).thenReturn(
          Future.successful(None))
        when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))

        val result = controller(lockRepo, authConnector).getLockByPsa()(getRequest(FakeRequest("GET", "/")))

        status(result) mustEqual NOT_FOUND
      }

      "throw an exception when the repository call fails" in {
        when(lockRepo.getExistingLockByPSA(eqTo("A2100005"))).thenReturn(
          Future.failed(new Exception()))
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller(lockRepo, authConnector).getLockByPsa()(getRequest(FakeRequest()))

        an[Exception] must be thrownBy {
          status(result)
        }
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed {
          new UnauthorizedException("")
        }

        val result = controller(lockRepo, authConnector).getLockByPsa()(getRequest(FakeRequest()))

        an[UnauthorizedException] must be thrownBy {
          status(result)
        }
      }
    }

    "getLockByScheme" when {

      "return 200 and the relevant data when it exists" in {
        when(lockRepo.getExistingLockBySRN(eqTo("SR0000001"))).thenReturn(
          Future.successful {Some(SchemeVariance("A2100005", "SR0000001"))})
        when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))

        val result = controller(lockRepo, authConnector).getLockByScheme()(getRequest(FakeRequest("GET", "/")))

        status(result) mustEqual OK
        contentAsString(result) mustEqual Json.toJson(SchemeVariance("A2100005", "SR0000001")).toString()
      }

      "return 404 when the data doesn't exist" in {
        when(lockRepo.getExistingLockBySRN(eqTo("SR0000001"))).thenReturn(
          Future.successful(None))
        when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))

        val result = controller(lockRepo, authConnector).getLockByScheme()(getRequest(FakeRequest("GET", "/")))

        status(result) mustEqual NOT_FOUND
      }

      "throw an exception when the repository call fails" in {
        when(lockRepo.getExistingLockBySRN(eqTo("SR0000001"))).thenReturn(
          Future.failed(new Exception()))
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller(lockRepo, authConnector).getLockByScheme()(getRequest(FakeRequest()))

        an[Exception] must be thrownBy {
          status(result)
        }
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed {
          new UnauthorizedException("")
        }

        val result = controller(lockRepo, authConnector).getLockByScheme()(getRequest(FakeRequest()))

        an[UnauthorizedException] must be thrownBy {
          status(result)
        }
      }
    }

    "releaseLock" when {

      "return 200 and the relevant data when it exists" in {
        when(lockRepo.releaseLock(eqTo(SchemeVariance("A2100005", "SR0000001")))).thenReturn(
          Future.successful {{}})
        when(authConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.successful(()))

        val result = controller(lockRepo, authConnector).releaseLock()(getRequest(FakeRequest("DELETE", "/")))

        status(result) mustEqual OK
      }

      "throw an exception when the repository call fails" in {
        when(lockRepo.releaseLock(eqTo(SchemeVariance("A2100005", "SR0000001")))).thenReturn(
          Future.failed(new Exception()))
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller(lockRepo, authConnector).releaseLock()(getRequest(FakeRequest()))

        an[Exception] must be thrownBy {
          status(result)
        }
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed {
          new UnauthorizedException("")
        }

        val result = controller(lockRepo, authConnector).releaseLock()(getRequest(FakeRequest()))

        an[UnauthorizedException] must be thrownBy {
          status(result)
        }
      }
    }
  }
}
