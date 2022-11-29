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

import models.SchemeWithId
import org.mockito.Mockito.when
import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.libs.json.Json
import repositories.SchemeDetailsWithIdCacheRepository.DataCache
import uk.gov.hmrc.mongo.MongoComponent

import scala.concurrent.ExecutionContext.Implicits.global

class SchemeDetailsWithIdCacheRepositorySpec extends AnyWordSpec with MockitoSugar with Matchers with EmbeddedMongoDBSupport with BeforeAndAfter
  with BeforeAndAfterAll with ScalaFutures { // scalastyle:off magic.number

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(30, Seconds), Span(1, Millis))

  private val idField: String = "id"

  import SchemeDetailsWithIdCacheRepositorySpec._

  var schemeDetailsWithIdCacheRepository: SchemeDetailsWithIdCacheRepository = _

  override def beforeAll(): Unit = {
    when(mockAppConfig.get[String]("mongodb.pensions-scheme-cache.scheme-with-id.name")).thenReturn("pensions-scheme-scheme-with-id-cache")
    when(mockAppConfig.get[Int]("mongodb.pensions-scheme-cache.scheme-details.timeToLiveInSeconds")).thenReturn(3600)
    initMongoDExecutable()
    startMongoD()
    schemeDetailsWithIdCacheRepository = buildRepository(mongoHost, mongoPort)
    super.beforeAll()
  }

  override def afterAll(): Unit =
    stopMongoD()

  "upsert" must {
    "save a new scheme details with_id cache in Mongo collection when collection is empty" in {

      val record = (SchemeWithId("SchemeId", "UserId"), Json.parse("""{"data":"1"}"""))
      val id: String = record._1.schemeId + record._1.userId
      val filters = Filters.eq(idField, id)

      val documentsInDB = for {
        _ <- schemeDetailsWithIdCacheRepository.collection.drop().toFuture()
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

      val record1 = (SchemeWithId("SchemeId", "UserId"), Json.parse("""{"data":"1"}"""))
      val record2 = (SchemeWithId("SchemeId", "UserId"), Json.parse("""{"data":"2"}"""))
      val id: String = record1._1.schemeId + record1._1.userId
      val filters = Filters.eq(idField, id)

      val documentsInDB = for {
        _ <- schemeDetailsWithIdCacheRepository.collection.drop().toFuture()
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

      val record1 = (SchemeWithId("SchemeId-1", "UserId-1"), Json.parse("""{"data":"1"}"""))
      val record2 = (SchemeWithId("SchemeId-2", "UserId-2"), Json.parse("""{"data":"2"}"""))

      val documentsInDB = for {
        _ <- schemeDetailsWithIdCacheRepository.collection.drop().toFuture()
        _ <- schemeDetailsWithIdCacheRepository.upsert(record1._1, record1._2)
        _ <- schemeDetailsWithIdCacheRepository.upsert(record2._1, record2._2)
        documentsInDB <- schemeDetailsWithIdCacheRepository.collection.find[DataCache]().toFuture()
      } yield documentsInDB

      whenReady(documentsInDB) {
        documentsInDB =>
          documentsInDB.size mustBe 2
      }
    }
  }

  "get" must {
    "get a scheme details with_id cache data record as JsonDataEntry by id in Mongo collection" in {

      val record = (SchemeWithId("SchemeId", "UserId"), Json.parse("""{"data":"1"}"""))

      val documentsInDB = for {
        _ <- schemeDetailsWithIdCacheRepository.collection.drop().toFuture()
        _ <- schemeDetailsWithIdCacheRepository.upsert(record._1, record._2)
        documentsInDB <- schemeDetailsWithIdCacheRepository.get(record._1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.isDefined mustBe true
      }
    }
  }
}

object SchemeDetailsWithIdCacheRepositorySpec extends MockitoSugar {

  private val mockAppConfig = mock[Configuration]

  private def buildRepository(mongoHost: String, mongoPort: Int): SchemeDetailsWithIdCacheRepository = {
    val databaseName = "pensions-scheme"
    val mongoUri = s"mongodb://$mongoHost:$mongoPort/$databaseName?heartbeatFrequencyMS=1000&rm.failover=default"
    new SchemeDetailsWithIdCacheRepository(MongoComponent(mongoUri), mockAppConfig)
  }
}