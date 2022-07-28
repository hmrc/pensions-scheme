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

package repositories

import com.github.simplyscala.MongoEmbedDatabase
import com.typesafe.config.Config
import config.AppConfig
import models.SchemeVariance
import org.mockito.MockitoSugar
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}
import play.api.Configuration
import uk.gov.hmrc.mongo.MongoComponent

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration


class LockRepositorySpec extends AnyWordSpec with BeforeAndAfter with Matchers with BeforeAndAfterEach with MongoEmbedDatabase with
  MockitoSugar { // scalastyle:off magic.number

  import repositories.LockRepositorySpec._

  override def beforeEach: Unit = {
    super.beforeEach
    when(mockConfiguration.underlying).thenReturn(mockConfig)
    when(mockConfig.getString("mongodb.pensions-scheme-cache.scheme-variation-lock.name")).thenReturn("scheme_variation_lock")
  }

  withEmbedMongoFixture(port = 24680) { _ =>


    "Lock" must {
      "lock scheme for relevant psaId AND srn from MongoCollection" in {
        mongoCollectionDrop()

        lockRepository.lock(testSchemeVariance).map { result =>
          result mustBe testSchemeVariance
        }
      }
    }

  }
}

object LockRepositorySpec extends AnyWordSpec with MockitoSugar {
  private val mockConfiguration = mock[Configuration]
  private val mockConfig = mock[Config]
  private val mockAppConfig = mock[AppConfig]

  private val databaseName = "pensions-scheme"
  private val mongoUri: String = s"mongodb://127.0.0.1:27017/$databaseName?heartbeatFrequencyMS=1000&rm.failover=default"
  private val mongoComponent = MongoComponent(mongoUri)
  private val testSchemeVariance = SchemeVariance("psa1", "srn1")


  private def mongoCollectionDrop(): Void = Await
    .result(lockRepository.collection.drop().toFuture(), Duration.Inf)

  def lockRepository: LockRepository = new LockRepository(mockConfiguration, mockAppConfig, mongoComponent)


}
