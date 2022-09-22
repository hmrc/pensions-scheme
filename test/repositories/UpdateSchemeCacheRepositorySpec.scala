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
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.MockitoSugar
import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}
import play.api.Configuration
import play.api.libs.json.Json
import repositories.SchemeDataEntry.{DataEntry, JsonDataEntry}
import uk.gov.hmrc.mongo.MongoComponent

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class UpdateSchemeCacheRepositorySpec extends AnyWordSpec with MockitoSugar with Matchers with MongoEmbedDatabase with BeforeAndAfter with
  BeforeAndAfterEach { // scalastyle:off magic.number

  private val idField: String = "id"

  import UpdateSchemeCacheRepositorySpec._

  override def beforeEach: Unit = {
    super.beforeEach
    when(mockAppConfig.underlying).thenReturn(mockConfig)
    when(mockConfig.getString("mongodb.pensions-scheme-cache.update-scheme.name")).thenReturn("pensions-scheme-update-scheme")
    when(mockConfig.getInt("mongodb.pensions-scheme-cache.update-scheme.timeToLiveInDays")).thenReturn(28)
    when(mockConfig.getString("scheme.json.encryption.key")).thenReturn("gvBoGdgzqG1AarzF1LY0zQ==")
  }

  withEmbedMongoFixture(port = 24680) { _ =>
    "upsert" must {
      "save a new update scheme cache as JsonDataEntry in Mongo collection when encrypted false and collection is empty" in {
        when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(false)
        mongoCollectionDrop()

        val record = ("id-1", Json.parse("""{"data":"1"}"""))
        val filters = Filters.eq(idField, record._1)

        val documentsInDB = for {
          _ <- updateSchemeCacheRepository.upsert(record._1, record._2)
          documentsInDB <- updateSchemeCacheRepository.collection.find[JsonDataEntry](filters).toFuture()
        } yield documentsInDB

        whenReady(documentsInDB) {
          documentsInDB =>
            documentsInDB.size mustBe 1
            documentsInDB.map(_.id mustBe "id-1")
        }
      }

      "update an existing update scheme cache as JsonDataEntry in Mongo collection when encrypted false" in {
        when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(false)
        mongoCollectionDrop()

        val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
        val record2 = ("id-1", Json.parse("""{"data":"2"}"""))
        val filters = Filters.eq(idField, "id-1")

        val documentsInDB = for {
          _ <- updateSchemeCacheRepository.upsert(record1._1, record1._2)
          _ <- updateSchemeCacheRepository.upsert(record2._1, record2._2)
          documentsInDB <- updateSchemeCacheRepository.collection.find[JsonDataEntry](filters).toFuture()
        } yield documentsInDB

        whenReady(documentsInDB) {
          documentsInDB =>
            documentsInDB.size mustBe 1
            documentsInDB.head.data mustBe record2._2
            documentsInDB.head.data must not be record1._2
        }
      }

      "insert a new update scheme cache as JsonDataEntry in Mongo collection when encrypted false and id is not same" in {
        when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(false)
        mongoCollectionDrop()

        val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
        val record2 = ("id-2", Json.parse("""{"data":"2"}"""))

        val documentsInDB = for {
          _ <- updateSchemeCacheRepository.upsert(record1._1, record1._2)
          _ <- updateSchemeCacheRepository.upsert(record2._1, record2._2)
          documentsInDB <- updateSchemeCacheRepository.collection.find[JsonDataEntry].toFuture()
        } yield documentsInDB

        whenReady(documentsInDB) {
          documentsInDB =>
            documentsInDB.size mustBe 2
        }
      }

      "insert a new update scheme cache as DataEntry in Mongo collection when encrypted true and collection is empty" in {
        when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(true)
        mongoCollectionDrop()

        val record = ("id-1", Json.parse("""{"data":"1"}"""))
        val filters = Filters.eq(idField, record._1)

        val documentsInDB = for {
          _ <- updateSchemeCacheRepository.upsert(record._1, record._2)
          documentsInDB <- updateSchemeCacheRepository.collection.find[DataEntry](filters).toFuture()
        } yield documentsInDB

        whenReady(documentsInDB) {
          documentsInDB =>
            documentsInDB.size mustBe 1
        }
      }

      "update an existing update scheme cache as DataEntry in Mongo collection when encrypted true" in {
        when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(true)
        mongoCollectionDrop()

        val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
        val record2 = ("id-1", Json.parse("""{"data":"2"}"""))
        val filters = Filters.eq(idField, "id-1")

        val documentsInDB = for {
          _ <- updateSchemeCacheRepository.upsert(record1._1, record1._2)
          _ <- updateSchemeCacheRepository.upsert(record2._1, record2._2)
          documentsInDB <- updateSchemeCacheRepository.collection.find[DataEntry](filters).toFuture()
        } yield documentsInDB

        whenReady(documentsInDB) {
          documentsInDB =>
            documentsInDB.size mustBe 1
        }
      }

      "insert a new update scheme cache as DataEntry in Mongo collection when encrypted true and id is not same" in {
        when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(true)
        mongoCollectionDrop()

        val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
        val record2 = ("id-2", Json.parse("""{"data":"2"}"""))

        val documentsInDB = for {
          _ <- updateSchemeCacheRepository.upsert(record1._1, record1._2)
          _ <- updateSchemeCacheRepository.upsert(record2._1, record2._2)
          documentsInDB <- updateSchemeCacheRepository.collection.find[DataEntry].toFuture()
        } yield documentsInDB

        whenReady(documentsInDB) {
          documentsInDB =>
            documentsInDB.size mustBe 2
        }
      }
    }

    "get" must {
      "get an update scheme cache data record as JsonDataEntry by id in Mongo collection when encrypted false" in {

        when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(false)
        mongoCollectionDrop()

        val record = ("id-1", Json.parse("""{"data":"1"}"""))

        val documentsInDB = for {
          _ <- updateSchemeCacheRepository.upsert(record._1, record._2)
          documentsInDB <- updateSchemeCacheRepository.get(record._1)
        } yield documentsInDB

        whenReady(documentsInDB) { documentsInDB =>
          documentsInDB.isDefined mustBe true
        }
      }

      "get an update scheme cache data record as DataEntry by id in Mongo collection when encrypted true" in {

        when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(true)
        mongoCollectionDrop()

        val record = ("id-1", Json.parse("""{"data":"1"}"""))

        val documentsInDB = for {
          _ <- updateSchemeCacheRepository.upsert(record._1, record._2)
          documentsInDB <- updateSchemeCacheRepository.get(record._1)
        } yield documentsInDB

        whenReady(documentsInDB) { documentsInDB =>
          documentsInDB.isDefined mustBe true
        }
      }
    }

    "getLastUpdated" must {
      "get an update scheme cache's lastUpdated field by id in Mongo collection when encrypted false" in {

        when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(false)
        mongoCollectionDrop()

        val record = ("id-1", Json.parse("""{"data":"1"}"""))
        val documentsInDB = for {
          _ <- updateSchemeCacheRepository.upsert(record._1, record._2)
          documentsInDB <- updateSchemeCacheRepository.getLastUpdated(record._1)
        } yield documentsInDB

        whenReady(documentsInDB) { documentsInDB =>
          documentsInDB.get.compareTo(DateTime.now(DateTimeZone.UTC)) mustBe -1
        }
      }

      "get an update scheme cache data's lastUpdated field by id in Mongo collection when encrypted true" in {

        when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(true)
        mongoCollectionDrop()

        val record = ("id-1", Json.parse("""{"data":"1"}"""))
        val documentsInDB = for {
          _ <- updateSchemeCacheRepository.upsert(record._1, record._2)
          documentsInDB <- updateSchemeCacheRepository.getLastUpdated(record._1)
        } yield documentsInDB

        whenReady(documentsInDB) { documentsInDB =>
          documentsInDB.get.compareTo(DateTime.now(DateTimeZone.UTC)) mustBe -1

        }
      }
    }

    "remove" must {
      "delete an existing JsonDataEntry update scheme cache record by id in Mongo collection when encrypted false" in {

        when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(false)
        mongoCollectionDrop()

        val record = ("id-1", Json.parse("""{"data":"1"}"""))
        val documentsInDB = for {
          _ <- updateSchemeCacheRepository.upsert(record._1, record._2)
          documentsInDB <- updateSchemeCacheRepository.get(record._1)
        } yield documentsInDB

        whenReady(documentsInDB) { documentsInDB =>
          documentsInDB.isDefined mustBe true
        }

        val documentsInDB2 = for {
          _ <- updateSchemeCacheRepository.remove(record._1)
          documentsInDB2 <- updateSchemeCacheRepository.get(record._1)
        } yield documentsInDB2

        whenReady(documentsInDB2) { documentsInDB2 =>
          documentsInDB2.isDefined mustBe false
        }
      }

      "not delete an existing JsonDataEntry update scheme cache record by id in Mongo collection when encrypted false and id incorrect" in {

        when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(false)
        mongoCollectionDrop()

        val record = ("id-1", Json.parse("""{"data":"1"}"""))
        val documentsInDB = for {
          _ <- updateSchemeCacheRepository.upsert(record._1, record._2)
          documentsInDB <- updateSchemeCacheRepository.get(record._1)
        } yield documentsInDB

        whenReady(documentsInDB) { documentsInDB =>
          documentsInDB.isDefined mustBe true
        }

        val documentsInDB2 = for {
          _ <- updateSchemeCacheRepository.remove("2")
          documentsInDB2 <- updateSchemeCacheRepository.get(record._1)
        } yield documentsInDB2

        whenReady(documentsInDB2) { documentsInDB2 =>
          documentsInDB2.isDefined mustBe true
        }
      }

      "delete an existing DataEntry update scheme cache record by id in Mongo collection when encrypted true" in {

        when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(true)
        mongoCollectionDrop()

        val record = ("id-1", Json.parse("""{"data":"1"}"""))
        val documentsInDB = for {
          _ <- updateSchemeCacheRepository.upsert(record._1, record._2)
          documentsInDB <- updateSchemeCacheRepository.get(record._1)
        } yield documentsInDB

        whenReady(documentsInDB) { documentsInDB =>
          documentsInDB.isDefined mustBe true
        }

        val documentsInDB2 = for {
          _ <- updateSchemeCacheRepository.remove(record._1)
          documentsInDB2 <- updateSchemeCacheRepository.get(record._1)
        } yield documentsInDB2

        whenReady(documentsInDB2) { documentsInDB2 =>
          documentsInDB2.isDefined mustBe false
        }
      }

      "not delete an existing DataEntry update scheme cache record by id in Mongo collection when encrypted true" in {

        when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(true)
        mongoCollectionDrop()

        val record = ("id-1", Json.parse("""{"data":"1"}"""))
        val documentsInDB = for {
          _ <- updateSchemeCacheRepository.upsert(record._1, record._2)
          documentsInDB <- updateSchemeCacheRepository.get(record._1)
        } yield documentsInDB

        whenReady(documentsInDB) { documentsInDB =>
          documentsInDB.isDefined mustBe true
        }

        val documentsInDB2 = for {
          _ <- updateSchemeCacheRepository.remove("2")
          documentsInDB2 <- updateSchemeCacheRepository.get(record._1)
        } yield documentsInDB2

        whenReady(documentsInDB2) { documentsInDB2 =>
          documentsInDB2.isDefined mustBe true
        }
      }
    }
  }
}

object UpdateSchemeCacheRepositorySpec extends AnyWordSpec with MockitoSugar {

  import scala.concurrent.ExecutionContext.Implicits._

  private val mockAppConfig = mock[Configuration]
  private val mockConfig = mock[Config]
  private val databaseName = "pensions-scheme"
  private val mongoUri: String = s"mongodb://127.0.0.1:27017/$databaseName?heartbeatFrequencyMS=1000&rm.failover=default"
  private val mongoComponent = MongoComponent(mongoUri)

  private def mongoCollectionDrop(): Void = Await
    .result(updateSchemeCacheRepository.collection.drop().toFuture(), Duration.Inf)

  def updateSchemeCacheRepository: UpdateSchemeCacheRepository =
    new UpdateSchemeCacheRepository(mockAppConfig, mongoComponent)
}