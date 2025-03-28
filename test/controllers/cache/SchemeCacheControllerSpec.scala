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

package controllers.cache

import controllers.actions.PsaPspEnrolmentAuthAction
import org.apache.pekko.stream.Materializer
import org.apache.pekko.util.ByteString
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.UnauthorizedException
import utils.AuthUtils

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SchemeCacheControllerSpec
  extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with GuiceOneAppPerSuite {

  private implicit lazy val mat: Materializer = app.materializer

  private val repo: SchemeCacheRepository = mock[SchemeCacheRepository]
  private val authConnector: AuthConnector = mock[AuthConnector]
  private val cc: ControllerComponents = app.injector.instanceOf[ControllerComponents]


  private def modules: Seq[GuiceableModule] =
    Seq(
      bind[AuthConnector].toInstance(authConnector),
      bind[LockRepository].toInstance(mock[LockRepository]),
      bind[RacdacSchemeSubscriptionCacheRepository].toInstance(mock[RacdacSchemeSubscriptionCacheRepository]),
      bind[SchemeCacheRepository].toInstance(repo),
      bind[SchemeDetailsCacheRepository].toInstance(mock[SchemeDetailsCacheRepository]),
      bind[SchemeDetailsWithIdCacheRepository].toInstance(mock[SchemeDetailsWithIdCacheRepository]),
      bind[SchemeSubscriptionCacheRepository].toInstance(mock[SchemeSubscriptionCacheRepository]),
      bind[UpdateSchemeCacheRepository].toInstance(mock[UpdateSchemeCacheRepository])
    )

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      //turn off metrics
      "metrics.jvm" -> false,
      "metrics.enabled" -> false
    )
    .overrides(modules: _*)
    .build()

  private class SchemeCacheControllerImpl(
                                           repo: SchemeCacheRepository,
                                           authConnector: AuthConnector
                                         ) extends SchemeCacheController(repo, authConnector, cc, app.injector.instanceOf[PsaPspEnrolmentAuthAction])

  def controller(repo: SchemeCacheRepository, authConnector: AuthConnector): SchemeCacheController = {
    new SchemeCacheControllerImpl(repo, authConnector)
  }

  // scalastyle:off method.length
  "SchemeCacheController" must {

    s".get" must {
      "return 200 and the relevant data when it exists" in {
        when(repo.get(eqTo("foo"))(any())) thenReturn Future.successful {
          Some(Json.obj())
        }
        AuthUtils.authStub(authConnector)

        val result = controller(repo, authConnector).get("foo")(FakeRequest())

        status(result) mustEqual OK
        contentAsString(result) mustEqual "{}"
      }

      "return 404 when the data doesn't exist" in {
        when(repo.get(eqTo("foo"))(any())) thenReturn Future.successful {
          None
        }
        AuthUtils.authStub(authConnector)

        val result = controller(repo, authConnector).get("foo")(FakeRequest())

        status(result) mustEqual NOT_FOUND
      }

      "throw an exception when the repository call fails" in {
        when(repo.get(eqTo("foo"))(any())) thenReturn Future.failed {
          new Exception()
        }
        AuthUtils.authStub(authConnector)

        val result = controller(repo, authConnector).get("foo")(FakeRequest())

        an[Exception] must be thrownBy {
          status(result)
        }
      }
    }

    s".save" must {

      "return 200 when the request body can be parsed and passed to the repository successfully" in {

        when(repo.upsert(any(), any())(any())) thenReturn Future.successful((): Unit)
        AuthUtils.authStub(authConnector)

        val result = call(controller(repo, authConnector).save("foo"), FakeRequest("POST", "/").withJsonBody(Json.obj("abc" -> "def")))

        status(result) mustEqual OK
      }

      "return 400 when the request body cannot be parsed" in {
        when(repo.upsert(any(), any())(any())) thenReturn Future.successful((): Unit)
        AuthUtils.authStub(authConnector)

        val result = call(controller(repo, authConnector).save("foo"), FakeRequest().withRawBody(ByteString("foo")))

        status(result) mustEqual BAD_REQUEST
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed(new UnauthorizedException(""))

        val result = call(controller(repo, authConnector).save("foo"), FakeRequest().withRawBody(ByteString("foo")))

        an[UnauthorizedException] must be thrownBy {
          status(result)
        }
      }
    }

    s".remove" must {
      "return 200 when the data is removed successfully" in {
        when(repo.remove(eqTo("foo"))(any())) thenReturn Future.successful(true)
        AuthUtils.authStub(authConnector)

        val result = controller(repo, authConnector).remove("foo")(FakeRequest())

        status(result) mustEqual OK
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed(new UnauthorizedException(""))

        val result = controller(repo, authConnector).remove("foo")(FakeRequest())

        an[UnauthorizedException] must be thrownBy {
          status(result)
        }
      }
    }

    s".lastUpdated" must {
      "return 200 and the relevant data when it exists" in {
        val date = Instant.now()
        when(repo.getLastUpdated(eqTo("foo"))(any())) thenReturn Future.successful {
          Some(date)
        }
        AuthUtils.authStub(authConnector)

        val result = controller(repo, authConnector).lastUpdated("foo")(FakeRequest())

        status(result) mustEqual OK
        contentAsJson(result) mustEqual Json.toJson(date.toEpochMilli)
      }

      "return 404 when the data doesn't exist" in {
        when(repo.getLastUpdated(eqTo("foo"))(any())) thenReturn Future.successful {
          None
        }
        AuthUtils.authStub(authConnector)

        val result = controller(repo, authConnector).lastUpdated("foo")(FakeRequest())

        status(result) mustEqual NOT_FOUND
      }

      "throw an exception when the repository call fails" in {
        when(repo.getLastUpdated(eqTo("foo"))(any())) thenReturn Future.failed {
          new Exception()
        }
        AuthUtils.authStub(authConnector)

        val result = controller(repo, authConnector).lastUpdated("foo")(FakeRequest())

        an[Exception] must be thrownBy {
          status(result)
        }
      }

      "throw an exception when the call is not authorised" in {
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed(new UnauthorizedException(""))

        val result = controller(repo, authConnector).lastUpdated("foo")(FakeRequest())

        an[UnauthorizedException] must be thrownBy {
          status(result)
        }
      }
    }
  }
}
