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
import org.mongodb.scala.ObservableFuture
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

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

class SchemeDetailsCacheRepositorySpec extends AnyWordSpec with MockitoSugar with Matchers with Samples with BeforeAndAfter
  with BeforeAndAfterAll with ScalaFutures { // scalastyle:off magic.number

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(30, Seconds), Span(1, Millis))

  private val idField: String = "id"
  private val mongoUri: String = s"mongodb://$mongoHost:$mongoPort/pensions-scheme?heartbeatFrequencyMS=1000&rm.failover=default"

  private val mockAppConfig = mock[Configuration]
  private val mockConfig = mock[Config]

  when(mockAppConfig.underlying).thenReturn(mockConfig)
  when(mockConfig.getString("mongodb.pensions-scheme-cache.scheme-details.name")).thenReturn("pensions-scheme-scheme-details")
  when(mockConfig.getInt("mongodb.pensions-scheme-cache.scheme-details.timeToLiveInSeconds")).thenReturn(3600)
  when(mockConfig.getString("scheme.json.encryption.key")).thenReturn("gvBoGdgzqG1AarzF1LY0zQ==")

  Seq(true, false)
    .foreach { encrypted =>
      when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(encrypted)
      val schemeDetailsCacheRepository: SchemeDetailsCacheRepository = new SchemeDetailsCacheRepository(mockAppConfig, MongoComponent(mongoUri))

      "upsert" must {
        s"save a new scheme details cache as JsonDataEntry in Mongo collection when encrypted $encrypted and collection is empty" in {
          val record = ("id-1", Json.parse("""{"data":"1"}"""))
          val filters = Filters.eq(idField, record._1)

          val documentsInDB = for {
            _ <- schemeDetailsCacheRepository.collection.drop().toFuture()
            _ <- schemeDetailsCacheRepository.upsert(record._1, record._2)
            documentsInDB <- schemeDetailsCacheRepository.collection.find[JsonDataEntry](filters).toFuture()
          } yield documentsInDB

          whenReady(documentsInDB) {
            documentsInDB =>
              documentsInDB.size mustBe 1
              documentsInDB.map(_.id mustBe "id-1")
          }
        }

        s"update an existing scheme details cache as JsonDataEntry in Mongo collection when encrypted $encrypted" in {
          val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
          val record2 = ("id-1", Json.parse("""{"data":"2"}"""))
          val filters = Filters.eq(idField, "id-1")

          val documentsInDB = for {
            _ <- schemeDetailsCacheRepository.collection.drop().toFuture()
            _ <- schemeDetailsCacheRepository.upsert(record1._1, record1._2)
            _ <- schemeDetailsCacheRepository.upsert(record2._1, record2._2)
            documentsInDB <- schemeDetailsCacheRepository.collection.find[JsonDataEntry](filters).toFuture()
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

        s"insert a new scheme details cache as JsonDataEntry in Mongo collection when encrypted $encrypted and id is not same" in {
          val record1 = ("id-1", Json.parse("""{"data":"1"}"""))
          val record2 = ("id-2", Json.parse("""{"data":"2"}"""))

          val documentsInDB = for {
            _ <- schemeDetailsCacheRepository.collection.drop().toFuture()
            _ <- schemeDetailsCacheRepository.upsert(record1._1, record1._2)
            _ <- schemeDetailsCacheRepository.upsert(record2._1, record2._2)
            documentsInDB <- schemeDetailsCacheRepository.collection.find[JsonDataEntry]().toFuture()
          } yield documentsInDB

          whenReady(documentsInDB) {
            documentsInDB =>
              documentsInDB.size mustBe 2
          }
        }

        s"insert a new scheme details cache as JsonDataEntry in Mongo collection when encrypted $encrypted and collection is empty" in {
          val record = ("id-1", Json.parse("""{"data":"1"}"""))
          val filters = Filters.eq(idField, record._1)

          val documentsInDB = for {
            _ <- schemeDetailsCacheRepository.collection.drop().toFuture()
            _ <- schemeDetailsCacheRepository.upsert(record._1, record._2)
            documentsInDB <- schemeDetailsCacheRepository.collection.find[JsonDataEntry](filters).toFuture()
          } yield documentsInDB

          whenReady(documentsInDB) {
            documentsInDB =>
              documentsInDB.size mustBe 1
          }
        }

        s"save expireAt value as a date when encrypted is $encrypted" in {
          val ftr = schemeDetailsCacheRepository.collection.drop().toFuture().flatMap { _ =>
            schemeDetailsCacheRepository.upsert("id", Json.parse("{}")).flatMap { _ =>
              for {
                stringResults <- schemeDetailsCacheRepository.collection.find[JsonDataEntry](
                  BsonDocument("expireAt" -> BsonDocument("$type" -> BsonString("string")))
                ).toFuture()
                dateResults <- schemeDetailsCacheRepository.collection.find[JsonDataEntry](
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
        s"get a scheme details cache data record as ${if (encrypted) "DataEntry" else "JsonDataEntry"} by id in Mongo collection when encrypted $encrypted" in {
          val record = ("id-1", Json.parse("""{"data":"1"}"""))
          val documentsInDB = for {
            _ <- schemeDetailsCacheRepository.collection.drop().toFuture()
            _ <- schemeDetailsCacheRepository.upsert(record._1, record._2)
            documentsInDB <- schemeDetailsCacheRepository.get(record._1)
          } yield documentsInDB

          whenReady(documentsInDB) { documentsInDB =>
            documentsInDB.isDefined mustBe true
          }
        }
      }

      "getLastUpdated" must {
        s"get a scheme details cache's lastUpdated field by id in Mongo collection when encrypted $encrypted" in {
          //          when(mockAppConfig.get[Boolean](path = "encrypted")).thenReturn(false)
          //          val schemeDetailsCacheRepository: SchemeDetailsCacheRepository = buildRepository(mongoHost, mongoPort)

          val record = ("id-1", Json.parse("""{"data":"1"}"""))
          val documentsInDB = for {
            _ <- schemeDetailsCacheRepository.collection.drop().toFuture()
            _ <- schemeDetailsCacheRepository.upsert(record._1, record._2)
            documentsInDB <- schemeDetailsCacheRepository.getLastUpdated(record._1)
          } yield documentsInDB

          whenReady(documentsInDB) { documentsInDB =>
            documentsInDB.get.compareTo(Instant.now()) should be < 0
          }
        }
      }

      "remove" must {
        s"delete an existing JsonDataEntry scheme details cache record by id in Mongo collection when encrypted $encrypted" in {
          val record = ("id-1", Json.parse("""{"data":"1"}"""))
          val documentsInDB = for {
            _ <- schemeDetailsCacheRepository.collection.drop().toFuture()
            _ <- schemeDetailsCacheRepository.upsert(record._1, record._2)
            documentsInDB <- schemeDetailsCacheRepository.get(record._1)
          } yield documentsInDB

          whenReady(documentsInDB) { documentsInDB =>
            documentsInDB.isDefined mustBe true
          }

          val documentsInDB2 = for {
            _ <- schemeDetailsCacheRepository.remove(record._1)
            documentsInDB2 <- schemeDetailsCacheRepository.get(record._1)
          } yield documentsInDB2

          whenReady(documentsInDB2) { documentsInDB2 =>
            documentsInDB2.isDefined mustBe false
          }
        }

        s"not delete an existing JsonDataEntry scheme details cache record by id in Mongo collection when encrypted $encrypted" in {
          val record = ("id-1", Json.parse("""{"data":"1"}"""))
          val documentsInDB = for {
            _ <- schemeDetailsCacheRepository.collection.drop().toFuture()
            _ <- schemeDetailsCacheRepository.upsert(record._1, record._2)
            documentsInDB <- schemeDetailsCacheRepository.get(record._1)
          } yield documentsInDB

          whenReady(documentsInDB) { documentsInDB =>
            documentsInDB.isDefined mustBe true
          }

          val documentsInDB2 = for {
            _ <- schemeDetailsCacheRepository.remove("2")
            documentsInDB2 <- schemeDetailsCacheRepository.get(record._1)
          } yield documentsInDB2

          whenReady(documentsInDB2) { documentsInDB2 =>
            documentsInDB2.isDefined mustBe true
          }
        }
      }
    }
}
