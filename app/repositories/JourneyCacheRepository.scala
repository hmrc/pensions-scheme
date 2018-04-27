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
import play.api.Logger
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.Subtype.GenericBinarySubtype
import reactivemongo.bson.{BSONBinary, BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.{ExecutionContext, Future}

abstract class JourneyCacheRepository(
                                       index: String,
                                       ttl: Int,
                                       component: ReactiveMongoComponent
                                     ) extends ReactiveRepository[JsValue, BSONObjectID](
    index,
    component.mongoConnector.db,
    implicitly
  ) {

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

  private val fieldName = "lastUpdated"
  private val createdIndexName = "userAnswersExpiry"
  private val expireAfterSeconds = "expireAfterSeconds"

  ensureIndex(fieldName, createdIndexName, ttl)

  private def ensureIndex(field: String, indexName: String, ttl: Int): Future[Boolean] = {

    import scala.concurrent.ExecutionContext.Implicits.global

    collection.indexesManager.ensure(Index(Seq((field, IndexType.Ascending)), Some(indexName),
      options = BSONDocument(expireAfterSeconds -> ttl))) map {
      result => {
        Logger.debug(s"set [$indexName] with value $ttl -> result : $result")
        result
      }
    } recover {
      case e => Logger.error("Failed to set TTL index", e)
        false
    }
  }

  def upsert(id: String, data: Array[Byte])(implicit ec: ExecutionContext): Future[Boolean] = {

    val document = Json.toJson(DataEntry(id, data))
    val selector = BSONDocument("id" -> id)
    val modifier = BSONDocument("$set" -> document)

    collection.update(selector, modifier, upsert = true)
      .map(_.ok)
  }

  def get(id: String)(implicit ec: ExecutionContext): Future[Option[Array[Byte]]] = {
    collection.find(BSONDocument("id" -> id)).one[DataEntry].map {
      _.map {
        dataEntry =>
          dataEntry.data.byteArray
      }
    }
  }
}