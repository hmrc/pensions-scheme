/*
 * Copyright 2018 HM Revenue & Customs
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

import org.joda.time.{DateTime, DateTimeZone}
import play.api.{Configuration, Logger}
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.Subtype.GenericBinarySubtype
import reactivemongo.bson.{BSONBinary, BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.{ExecutionContext, Future}

abstract class PensionsSchemeCacheRepository(
                                              index: String,
                                              ttl: Option[Int],
                                              component: ReactiveMongoComponent,
                                              crypto: ApplicationCrypto,
                                              config: Configuration
                                            ) extends ReactiveRepository[JsValue, BSONObjectID](
  index,
  component.mongoConnector.db,
  implicitly
) {

  private val encrypted: Boolean = config.underlying.getBoolean("encrypted")

  private case class DataEntry(
                                id: String,
                                data: BSONBinary,
                                lastUpdated: DateTime
                              )

  private object DataEntry {

    def apply(id: String, data: Array[Byte], lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC)): DataEntry =
      DataEntry(id, BSONBinary(data, GenericBinarySubtype), lastUpdated)

    private implicit val dateFormat: Format[DateTime] = ReactiveMongoFormats.dateTimeFormats
    implicit val format: Format[DataEntry] = Json.format[DataEntry]
  }

  private case class JsonDataEntry(
                                    id: String,
                                    data: JsValue,
                                    lastUpdated: DateTime
                                  )

  private object JsonDataEntry {
//        def apply(id: String, data: JsValue, lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC)): JsonDataEntry =
//          JsonDataEntry(id, data, lastUpdated)
    private implicit val dateFormat: Format[DateTime] = ReactiveMongoFormats.dateTimeFormats
    implicit val format: Format[JsonDataEntry] = Json.format[JsonDataEntry]
  }

  private val fieldName = "lastUpdated"
  private val createdIndexName = "userAnswersExpiry"
  private val expireAfterSeconds = "expireAfterSeconds"

  ensureIndex(fieldName, createdIndexName, ttl)

  private def ensureIndex(field: String, indexName: String, ttl: Option[Int]): Future[Boolean] = {

    import scala.concurrent.ExecutionContext.Implicits.global

    val defaultIndex: Index = Index(Seq((field, IndexType.Ascending)), Some(indexName))

    val index: Index = ttl.fold(defaultIndex) { ttl =>
      Index(
        Seq((field, IndexType.Ascending)),
        Some(indexName),
        options = BSONDocument(expireAfterSeconds -> ttl)
      )
    }

    collection.indexesManager.ensure(index) map {
      result => {
        Logger.debug(s"set [$indexName] with value $ttl -> result : $result")
        result
      }
    } recover {
      case e => Logger.error("Failed to set TTL index", e)
        false
    }
  }

//  def upsert(id: String, data: Array[Byte])(implicit ec: ExecutionContext): Future[Boolean] = {
//
//    val document = Json.toJson(DataEntry(id, data))
//    val selector = BSONDocument("id" -> id)
//    val modifier = BSONDocument("$set" -> document)
//
//    collection.update(selector, modifier, upsert = true)
//      .map(_.ok)
//  }

  def upsert(id: String, data: JsValue)(implicit ec: ExecutionContext): Future[Boolean] = {

    val unencrypted = PlainText(Json.stringify(data))
    val encryptedData = Json.toJson(crypto.JsonCrypto.encrypt(unencrypted).value)

    val document: JsValue = {
      if(encrypted)
        Json.toJson(JsonDataEntry(id, encryptedData, DateTime.now(DateTimeZone.UTC)))
      else
        Json.toJson(JsonDataEntry(id, data, DateTime.now(DateTimeZone.UTC)))
    }
    val selector = BSONDocument("id" -> id)
    val modifier = BSONDocument("$set" -> document)
    collection.update(selector, modifier, upsert = true)
      .map(_.ok)
  }

//  def get(id: String)(implicit ec: ExecutionContext): Future[Option[Array[Byte]]] = {
//    collection.find(BSONDocument("id" -> id)).one[DataEntry].map {
//      _.map {
//        dataEntry =>
//          dataEntry.data.byteArray
//      }
//    }
//  }

  def get(id: String)(implicit ec: ExecutionContext): Future[Option[JsValue]] = {
    if(encrypted) {
      collection.find(BSONDocument("id" -> id)).one[DataEntry].map {
        _.map {
          dataEntry =>
            Json.toJson(dataEntry.data)
        }
      }
    } else {
      collection.find(BSONDocument("id" -> id)).one[JsonDataEntry].map {
        _.map {
          dataEntry =>
            dataEntry.data
        }
      }
    }
  }

  def getLastUpdated(id: String)(implicit ec: ExecutionContext): Future[Option[DateTime]] = {
    collection.find(BSONDocument("id" -> id)).one[DataEntry].map {
      _.map {
        dataEntry =>
          dataEntry.lastUpdated
      }
    }
  }

  def remove(id: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val selector = BSONDocument("id" -> id)
    collection.remove(selector).map(_.ok)
  }

  def dropCollection()(implicit ec: ExecutionContext): Future[Unit] = {
    collection.drop()
  }
}
