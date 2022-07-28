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
import models.SchemeWithId
import org.mockito.MockitoSugar
import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}
import play.api.Configuration
import play.api.libs.json.Json
import repositories.SchemeDetailsWithIdCacheRepository.DataCache
import uk.gov.hmrc.mongo.MongoComponent

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class SchemeDetailsWithIdCacheRepositorySpec extends AnyWordSpec with MockitoSugar with Matchers with MongoEmbedDatabase with BeforeAndAfter with
  BeforeAndAfterEach { // scalastyle:off magic.number

  private val idField: String = "id"

  import SchemeDetailsWithIdCacheRepositorySpec._

  override def beforeEach: Unit = {
    super.beforeEach
    when(mockAppConfig.get[String]("mongodb.pensions-scheme-cache.scheme-with-id.name")).thenReturn("pensions-scheme-scheme-with-id-cache")
    when(mockAppConfig.get[Int]("mongodb.pensions-scheme-cache.scheme-details.timeToLiveInSeconds")).thenReturn(3600)
  }

  withEmbedMongoFixture(port = 24680) { _ =>
    "upsert" must {
      "save a new scheme details with_id cache in Mongo collection when collection is empty" in {
        mongoCollectionDrop()

        val record = (SchemeWithId("SchemeId", "UserId"), Json.parse("""{"data":"1"}"""))
        val id: String = record._1.schemeId + record._1.userId
        val filters = Filters.eq(idField, id)

        val documentsInDB = for {
          _ <- schemeDetailsWithIdCacheRepository.upsert(record._1, record._2)
          documentsInDB <- schemeDetailsWithIdCacheRepository.collection.find[DataCache](filters).toFuture()
        } yield documentsInDB

        whenReady(documentsInDB) {
          documentsInDB =>
            documentsInDB.size mustBe 1
            documentsInDB.map(_.id mustBe record._1.schemeId + record._1.userId)
        }
      }

      "update an existing scheme details with_id cache in Mongo collection" in {
        mongoCollectionDrop()

        val record1 = (SchemeWithId("SchemeId", "UserId"), Json.parse("""{"data":"1"}"""))
        val record2 = (SchemeWithId("SchemeId", "UserId"), Json.parse("""{"data":"2"}"""))
        val id: String = record1._1.schemeId + record1._1.userId
        val filters = Filters.eq(idField, id)

        val documentsInDB = for {
          _ <- schemeDetailsWithIdCacheRepository.upsert(record1._1, record1._2)
          _ <- schemeDetailsWithIdCacheRepository.upsert(record2._1, record2._2)
          documentsInDB <- schemeDetailsWithIdCacheRepository.collection.find[DataCache](filters).toFuture()
        } yield documentsInDB

        whenReady(documentsInDB) {
          documentsInDB =>
            documentsInDB.size mustBe 1
            documentsInDB.head.data mustBe record2._2
            documentsInDB.head.data must not be record1._2
        }
      }

      "insert a new scheme details with_id cache in Mongo collection when id is not same" in {
        mongoCollectionDrop()

        val record1 = (SchemeWithId("SchemeId-1", "UserId-1"), Json.parse("""{"data":"1"}"""))
        val record2 = (SchemeWithId("SchemeId-2", "UserId-2"), Json.parse("""{"data":"2"}"""))

        val documentsInDB = for {
          _ <- schemeDetailsWithIdCacheRepository.upsert(record1._1, record1._2)
          _ <- schemeDetailsWithIdCacheRepository.upsert(record2._1, record2._2)
          documentsInDB <- schemeDetailsWithIdCacheRepository.collection.find[DataCache].toFuture()
        } yield documentsInDB

        whenReady(documentsInDB) {
          documentsInDB =>
            documentsInDB.size mustBe 2
        }
      }
    }

    "get" must {
      "get a scheme details with_id cache data record as JsonDataEntry by id in Mongo collection" in {
        mongoCollectionDrop()

        val record = (SchemeWithId("SchemeId", "UserId"), Json.parse("""{"data":"1"}"""))
        val id: String = record._1.schemeId + record._1.userId

        val documentsInDB = for {
          _ <- schemeDetailsWithIdCacheRepository.upsert(record._1, record._2)
          documentsInDB <- schemeDetailsWithIdCacheRepository.get(record._1)
        } yield documentsInDB

        whenReady(documentsInDB) { documentsInDB =>
          documentsInDB.isDefined mustBe true
        }
      }
    }

    "remove" must {
      "delete an existing scheme details with_id cache record by id in Mongo collection" in {
        mongoCollectionDrop()

        val record = (SchemeWithId("SchemeId", "UserId"), Json.parse("""{"data":"1"}"""))
        val id: String = record._1.schemeId + record._1.userId

        val documentsInDB = for {
          _ <- schemeDetailsWithIdCacheRepository.upsert(record._1, record._2)
          documentsInDB <- schemeDetailsWithIdCacheRepository.get(record._1)
        } yield documentsInDB

        whenReady(documentsInDB) { documentsInDB =>
          documentsInDB.isDefined mustBe true
        }

        val documentsInDB2 = for {
          _ <- schemeDetailsWithIdCacheRepository.remove(record._1)
          documentsInDB2 <- schemeDetailsWithIdCacheRepository.get(record._1)
        } yield documentsInDB2

        whenReady(documentsInDB2) { documentsInDB2 =>
          documentsInDB2.isDefined mustBe false
        }
      }

      "not delete an existing scheme details with_id cache record by id in Mongo collection when id incorrect" in {
        mongoCollectionDrop()

        val record = (SchemeWithId("SchemeId", "UserId"), Json.parse("""{"data":"1"}"""))
        val invalidRecord: SchemeWithId = SchemeWithId("Invalid-SchemeId", "Invalid-UserId")

        val documentsInDB = for {
          _ <- schemeDetailsWithIdCacheRepository.upsert(record._1, record._2)
          documentsInDB <- schemeDetailsWithIdCacheRepository.get(record._1)
        } yield documentsInDB

        whenReady(documentsInDB) { documentsInDB =>
          documentsInDB.isDefined mustBe true
        }

        val documentsInDB2 = for {
          _ <- schemeDetailsWithIdCacheRepository.remove(invalidRecord)
          documentsInDB2 <- schemeDetailsWithIdCacheRepository.get(record._1)
        } yield documentsInDB2

        whenReady(documentsInDB2) { documentsInDB2 =>
          documentsInDB2.isDefined mustBe true
        }
      }
    }
  }
}

object SchemeDetailsWithIdCacheRepositorySpec extends AnyWordSpec with MockitoSugar {

  import scala.concurrent.ExecutionContext.Implicits._

  private val mockAppConfig = mock[Configuration]
  private val databaseName = "pensions-scheme"
  private val mongoUri: String = s"mongodb://127.0.0.1:27017/$databaseName?heartbeatFrequencyMS=1000&rm.failover=default"
  private val mongoComponent = MongoComponent(mongoUri)

  private def mongoCollectionDrop(): Void = Await
    .result(schemeDetailsWithIdCacheRepository.collection.drop().toFuture(), Duration.Inf)

  def schemeDetailsWithIdCacheRepository: SchemeDetailsWithIdCacheRepository =
    new SchemeDetailsWithIdCacheRepository(mongoComponent, mockAppConfig)
}