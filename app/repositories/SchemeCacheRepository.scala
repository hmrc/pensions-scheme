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

package repositories

import com.google.inject.Inject
import com.mongodb.client.model.FindOneAndUpdateOptions
import crypto.DataEncryptor
import org.mongodb.scala.model._
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._
import play.api.{Configuration, Logging}
import repositories.SchemeDataEntry.SchemeDataEntryFormats.expireAtKey
import repositories.SchemeDataEntry.{JsonDataEntry, SchemeDataEntry, SchemeDataEntryFormats}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

object SchemeDataEntry {

  sealed trait SchemeDataEntry

  case class JsonDataEntry(id: String, data: JsValue, lastUpdated: Instant, expireAt: Instant) extends SchemeDataEntry

  object JsonDataEntry {
    implicit val format: Format[JsonDataEntry] = new Format[JsonDataEntry] {
      override def writes(o: JsonDataEntry): JsValue = Json.writes[JsonDataEntry].writes(o)

      private val instantReads = MongoJavatimeFormats.instantReads

      override def reads(json: JsValue): JsResult[JsonDataEntry] = (
        (JsPath \ "id").read[String] and
          (JsPath \ "data").read[JsValue] and
          (JsPath \ "lastUpdated").read(instantReads) and
          (JsPath \ "expireAt").read(instantReads)
        )((id, data, lastUpdated, expireAt) => JsonDataEntry(id, data, lastUpdated, expireAt))
        .reads(json)
    }
  }

  object SchemeDataEntryFormats {
    implicit val dateFormats: Format[Instant] = MongoJavatimeFormats.instantFormat
    implicit val format: Format[SchemeDataEntry] = Json.format[SchemeDataEntry]

    val dataKey: String = "data"
    val idField: String = "id"
    val lastUpdatedKey: String = "lastUpdated"
    val expireAtKey: String = "expireAt"
  }
}

@Singleton
class SchemeCacheRepository @Inject()(
                                       collectionName: String,
                                       mongoComponent: MongoComponent,
                                       config: Configuration,
                                       expireInSeconds: Option[Int],
                                       expireInDays: Option[Int],
                                       cipher: DataEncryptor
                                     )(
                                       implicit ec: ExecutionContext
                                     )
  extends PlayMongoRepository[SchemeDataEntry](
    collectionName = collectionName,
    mongoComponent = mongoComponent,
    domainFormat = SchemeDataEntryFormats.format,
    extraCodecs = Seq(
      Codecs.playFormatCodec(JsonDataEntry.format),
      Codecs.playFormatCodec(MongoJavatimeFormats.instantFormat)
    ),
    indexes = Seq(
      IndexModel(
        Indexes.ascending(expireAtKey),
        IndexOptions().name("dataExpiry")
          .expireAfter(0, TimeUnit.SECONDS).background(true)
      )
    )
  ) with Logging {

  import SchemeDataEntryFormats._

  private def getExpireAt: Instant = if (expireInSeconds.isEmpty) {
    val secondsInDay = 86400
    val expirySeconds = expireInDays match {
      case Some(days) => secondsInDay * days
      case _ => secondsInDay * (config.underlying.getInt("defaultDataExpireInDays") + 1)
    }
    Instant
      .now()
      .plusSeconds(expirySeconds)
  } else {
    val expirySeconds = expireInSeconds.getOrElse(config.underlying.getInt("defaultDataExpireInSeconds"))
    Instant
      .now()
      .plusSeconds(expirySeconds)
  }

  def upsert(id: String, data: JsValue)
            (implicit ec: ExecutionContext): Future[Unit] = {
      val setOperation = Updates.combine(
        Updates.set(idField, id),
        Updates.set(dataKey, Codecs.toBson(cipher.encrypt(id, data))),
        Updates.set(lastUpdatedKey, Instant.now()),
        Updates.set(expireAtKey, getExpireAt)
      )
      collection.withDocumentClass[JsonDataEntry]().findOneAndUpdate(
        filter = Filters.eq(idField, id),
        update = setOperation, new FindOneAndUpdateOptions().upsert(true)).toFuture().map(_ => ())
  }

  def get(id: String)
         (implicit ec: ExecutionContext): Future[Option[JsValue]] = {
      collection.find[JsonDataEntry](Filters.equal(idField, id)).headOption().map {
        _.map {
          dataEntry =>
            cipher.decrypt(id, dataEntry.data)
        }
      }
  }

  def getLastUpdated(id: String)
                    (implicit ec: ExecutionContext): Future[Option[Instant]] = {
      collection.find[JsonDataEntry](Filters.equal(idField, id)).headOption().map {
        _.map {
          dataEntry =>
            dataEntry.lastUpdated
        }
      }
  }

  def remove(id: String)
            (implicit ec: ExecutionContext): Future[Boolean] = {
    collection.deleteOne(Filters.equal(idField, id)).toFuture().map { result =>
      logger.info(s"Removing row from collection $collectionName externalId:$id")
      result.wasAcknowledged
    }
  }
}
