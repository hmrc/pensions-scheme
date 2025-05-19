/*
 * Copyright 2025 HM Revenue & Customs
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

import com.typesafe.config.Config
import models.Samples
import org.mockito.Mockito.when
import org.mongodb.scala.bson.{BsonDocument, BsonString}
import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.should
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.libs.json.Json
import repositories.SchemeDataEntry.JsonDataEntry
import uk.gov.hmrc.mongo.MongoComponent
import org.mongodb.scala.ObservableFuture

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

class SchemeSubscriptionCacheRepositorySpec extends AnyWordSpec with MockitoSugar with Matchers with Samples
  with BeforeAndAfter with BeforeAndAfterAll with ScalaFutures { // scalastyle:off magic.number

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(30, Seconds), Span(1, Millis))

  private val idField: String = "id"
  private val mongoUri = s"mongodb://$mongoHost:$mongoPort/pensions-scheme?heartbeatFrequencyMS=1000&rm.failover=default"

  private val mockAppConfig = mock[Configuration]
  private val mockConfig = mock[Config]

  when(mockAppConfig.underlying).thenReturn(mockConfig)
  when(mockConfig.getString("mongodb.pensions-scheme-cache.register-scheme.name")).thenReturn("pensions-scheme-register-scheme")
  when(mockConfig.getInt("mongodb.pensions-scheme-cache.register-scheme.timeToLiveInDays")).thenReturn(28)
  when(mockConfig.getString("scheme.json.encryption.key")).thenReturn("gvBoGdgzqG1AarzF1LY0zQ==")

  Seq(true, false)
    .foreach { encrypted =>
      when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(encrypted)
      val schemeSubscriptionCacheRepository: SchemeSubscriptionCacheRepository = new SchemeSubscriptionCacheRepository(mockAppConfig, MongoComponent(mongoUri))


      "upsert" must {
        s"save a new scheme subscription cache as JsonDataEntry in Mongo collection when encrypted $encrypted and collection is empty" in {
          val record = ("id-1", Json.parse("""{"data":"1"}"""))
          val filters = Filters.eq(idField, record._1)

          val documentsInDB = for {
            _ <- schemeSubscriptionCacheRepository.collection.drop().toFuture()
            _ <- schemeSubscriptionCacheRepository.upsert(record._1, record._2)
            documentsInDB <- schemeSubscriptionCacheRepository.collection.find[JsonDataEntry](filters).toFuture()
          } yield documentsInDB

          whenReady(documentsInDB) {
            documentsInDB =>
              documentsInDB.size mustBe 1
              documentsInDB.map(_.id mustBe "id-1")
          }
        }

        s"update an existing scheme subscription cache as JsonDataEntry in Mongo collection when $encrypted false" in {
          val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
          val record2 = ("id-1", Json.parse("""{"data":"2"}"""))
          val filters = Filters.eq(idField, "id-1")

          val documentsInDB = for {
            _ <- schemeSubscriptionCacheRepository.collection.drop().toFuture()
            _ <- schemeSubscriptionCacheRepository.upsert(record1._1, record1._2)
            _ <- schemeSubscriptionCacheRepository.upsert(record2._1, record2._2)
            documentsInDB <- schemeSubscriptionCacheRepository.collection.find[JsonDataEntry](filters).toFuture()
          } yield documentsInDB

          whenReady(documentsInDB) {
            documentsInDB =>
              documentsInDB.size mustBe 1
              if (!encrypted) {
                documentsInDB.head.data mustBe record2._2
                documentsInDB.head.data must not be record1._2
              }
          }
        }

        s"insert a new scheme subscription cache as JsonDataEntry in Mongo collection when encrypted $encrypted and id is not same" in {
          val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
          val record2 = ("id-2", Json.parse("""{"data":"2"}"""))

          val documentsInDB = for {
            _ <- schemeSubscriptionCacheRepository.collection.drop().toFuture()
            _ <- schemeSubscriptionCacheRepository.upsert(record1._1, record1._2)
            _ <- schemeSubscriptionCacheRepository.upsert(record2._1, record2._2)
            documentsInDB <- schemeSubscriptionCacheRepository.collection.find[JsonDataEntry]().toFuture()
          } yield documentsInDB

          whenReady(documentsInDB) {
            documentsInDB =>
              documentsInDB.size mustBe 2
          }
        }

        s"save expireAt value as a date when encrypted $encrypted" in {
          val ftr = schemeSubscriptionCacheRepository.collection.drop().toFuture().flatMap { _ =>
            schemeSubscriptionCacheRepository.upsert("id", Json.parse("{}")).flatMap { _ =>
              for {
                stringResults <- schemeSubscriptionCacheRepository.collection.find[JsonDataEntry](
                  BsonDocument("expireAt" -> BsonDocument("$type" -> BsonString("string")))
                ).toFuture()
                dateResults <- schemeSubscriptionCacheRepository.collection.find[JsonDataEntry](
                  BsonDocument("expireAt" -> BsonDocument("$type" -> BsonString("date")))
                ).toFuture()
              } yield stringResults -> dateResults
            }
          }

          whenReady(ftr) { case (stringResults, dateResults) =>
            stringResults.length mustBe 0
            dateResults.length mustBe 1
          }
        }
      }

      "get" must {
        s"get a scheme subscription cache data record as JsonDataEntry by id in Mongo collection when encrypted $encrypted" in {
          val record = ("id-1", Json.parse("""{"data":"1"}"""))

          val documentsInDB = for {
            _ <- schemeSubscriptionCacheRepository.collection.drop().toFuture()
            _ <- schemeSubscriptionCacheRepository.upsert(record._1, record._2)
            documentsInDB <- schemeSubscriptionCacheRepository.get(record._1)
          } yield documentsInDB

          whenReady(documentsInDB) { documentsInDB =>
            documentsInDB.isDefined mustBe true
          }
        }
      }

      "getLastUpdated" must {
        s"get a scheme subscription cache's lastUpdated field by id in Mongo collection when encrypted $encrypted" in {
          val record = ("id-1", Json.parse("""{"data":"1"}"""))
          val documentsInDB = for {
            _ <- schemeSubscriptionCacheRepository.collection.drop().toFuture()
            _ <- schemeSubscriptionCacheRepository.upsert(record._1, record._2)
            documentsInDB <- schemeSubscriptionCacheRepository.getLastUpdated(record._1)
          } yield documentsInDB

          whenReady(documentsInDB) { documentsInDB =>
            documentsInDB.get.compareTo(Instant.now()) should be < 0
          }
        }
      }

      "remove" must {
        s"delete an existing JsonDataEntry scheme subscription cache record by id in Mongo collection when encrypted $encrypted" in {
          val record = ("id-1", Json.parse("""{"data":"1"}"""))
          val documentsInDB = for {
            _ <- schemeSubscriptionCacheRepository.collection.drop().toFuture()
            _ <- schemeSubscriptionCacheRepository.upsert(record._1, record._2)
            documentsInDB <- schemeSubscriptionCacheRepository.get(record._1)
          } yield documentsInDB

          whenReady(documentsInDB) { documentsInDB =>
            documentsInDB.isDefined mustBe true
          }

          val documentsInDB2 = for {
            _ <- schemeSubscriptionCacheRepository.remove(record._1)
            documentsInDB2 <- schemeSubscriptionCacheRepository.get(record._1)
          } yield documentsInDB2

          whenReady(documentsInDB2) { documentsInDB2 =>
            documentsInDB2.isDefined mustBe false
          }
        }

        s"not delete an existing JsonDataEntry scheme subscription cache record by id in Mongo collection when encrypted $encrypted and id incorrect" in {
          val record = ("id-1", Json.parse("""{"data":"1"}"""))
          val documentsInDB = for {
            _ <- schemeSubscriptionCacheRepository.collection.drop().toFuture()
            _ <- schemeSubscriptionCacheRepository.upsert(record._1, record._2)
            documentsInDB <- schemeSubscriptionCacheRepository.get(record._1)
          } yield documentsInDB

          whenReady(documentsInDB) { documentsInDB =>
            documentsInDB.isDefined mustBe true
          }

          val documentsInDB2 = for {
            _ <- schemeSubscriptionCacheRepository.remove("2")
            documentsInDB2 <- schemeSubscriptionCacheRepository.get(record._1)
          } yield documentsInDB2

          whenReady(documentsInDB2) { documentsInDB2 =>
            documentsInDB2.isDefined mustBe true
          }
        }
      }
    }
}
