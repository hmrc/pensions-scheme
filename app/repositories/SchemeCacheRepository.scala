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

import com.google.inject.Inject
import com.mongodb.client.model.FindOneAndUpdateOptions
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.bson.BsonBinary
import org.mongodb.scala.model._
import play.api.libs.json._
import play.api.{Configuration, Logging}
import repositories.SchemeDataEntry.SchemeDataEntryFormats.expireAtKey
import repositories.SchemeDataEntry.{DataEntry, JsonDataEntry, SchemeDataEntry, SchemeDataEntryFormats}
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter, PlainText, SymmetricCryptoFactory}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoBinaryFormats.{byteArrayReads, byteArrayWrites}
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

object SchemeDataEntry {

  sealed trait SchemeDataEntry

  case class DataEntry(id: String, data: BsonBinary, lastUpdated: DateTime, expireAt: DateTime) extends SchemeDataEntry

  case class JsonDataEntry(id: String, data: JsValue, lastUpdated: DateTime, expireAt: DateTime) extends SchemeDataEntry

  object DataEntry {
    def apply(id: String, data: Array[Byte], lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC), expireAt: DateTime): DataEntry =
      DataEntry(id, BsonBinary(data), lastUpdated, expireAt)

    final val bsonBinaryReads: Reads[BsonBinary] = byteArrayReads.map(t => BsonBinary(t))
    final val bsonBinaryWrites: Writes[BsonBinary] = byteArrayWrites.contramap(t => t.getData)
    implicit val bsonBinaryFormat: Format[BsonBinary] = Format(bsonBinaryReads, bsonBinaryWrites)

    implicit val dateFormat: Format[DateTime] = MongoJodaFormats.dateTimeFormat
    implicit val format: Format[DataEntry] = Json.format[DataEntry]
  }

  object JsonDataEntry {
    implicit val dateFormat: Format[DateTime] = MongoJodaFormats.dateTimeFormat
    implicit val format: Format[JsonDataEntry] = Json.format[JsonDataEntry]
  }

  object SchemeDataEntryFormats {
    implicit val dateFormat: Format[DateTime] = MongoJodaFormats.dateTimeFormat
    implicit val format: Format[SchemeDataEntry] = Json.format[SchemeDataEntry]

    val dataKey: String = "data"
    val idField: String = "id"
    val lastUpdatedKey: String = "lastUpdated"
    val expireAtKey: String = "expireAt"
  }
}

class SchemeCacheRepository @Inject()(
                                       collectionName: String,
                                       mongoComponent: MongoComponent,
                                       config: Configuration,
                                       encryptionKey: String,
                                       expireInSeconds: Option[Int],
                                       expireInDays: Option[Int]
                                     )(
                                       implicit ec: ExecutionContext
                                     )
  extends PlayMongoRepository[SchemeDataEntry](
    collectionName = collectionName,
    mongoComponent = mongoComponent,
    domainFormat = SchemeDataEntryFormats.format,
    extraCodecs = Seq(
      Codecs.playFormatCodec(JsonDataEntry.format),
      Codecs.playFormatCodec(DataEntry.format)
    ).toSeq,
    indexes = Seq(
      IndexModel(
        Indexes.ascending(expireAtKey),
        IndexOptions().name("dataExpiry")
          .expireAfter(0, TimeUnit.SECONDS).background(true)
      )
    ).toSeq
  ) with Logging {

  import SchemeDataEntryFormats._

  private val jsonCrypto: Encrypter with Decrypter = SymmetricCryptoFactory.aesCryptoFromConfig(baseConfigKey = encryptionKey, config.underlying)
  private val encrypted: Boolean = config.get[Boolean]("encrypted")

  private def getExpireAt: DateTime = if (expireInSeconds.isEmpty) {
    DateTime
      .now(DateTimeZone.UTC)
      .toLocalDate
      .plusDays(
        expireInDays.getOrElse(config.underlying.getInt("defaultDataExpireInDays")) + 1
      ).toDateTimeAtStartOfDay()
  } else {
    DateTime
      .now(DateTimeZone.UTC)
      .plusSeconds(
        expireInSeconds.getOrElse(config.underlying.getInt("defaultDataExpireInSeconds"))
      )
  }

  def upsert(id: String, data: JsValue)
            (implicit ec: ExecutionContext): Future[Unit] = {
    if (encrypted) {
      val unencrypted = PlainText(Json.stringify(data))
      val encryptedData = jsonCrypto.encrypt(unencrypted).value
      val dataAsByteArray: Array[Byte] = encryptedData.getBytes("UTF-8")
      val entry = DataEntry.apply(id, dataAsByteArray, expireAt = getExpireAt)
      val setOperation = Updates.combine(
        Updates.set(idField, entry.id),
        Updates.set(dataKey, entry.data),
        Updates.set(lastUpdatedKey, Codecs.toBson(entry.lastUpdated)),
        Updates.set(expireAtKey, Codecs.toBson(entry.expireAt))
      )
      collection.withDocumentClass[DataEntry]().findOneAndUpdate(
        filter = Filters.eq(idField, id),
        update = setOperation, new FindOneAndUpdateOptions().upsert(true)).toFuture().map(_ => ())
    } else {
      val record = JsonDataEntry(id, data, DateTime.now(DateTimeZone.UTC), getExpireAt)
      val setOperation = Updates.combine(
        Updates.set(idField, record.id),
        Updates.set(dataKey, Codecs.toBson(record.data)),
        Updates.set(lastUpdatedKey, Codecs.toBson(record.lastUpdated)),
        Updates.set(expireAtKey, Codecs.toBson(record.expireAt))
      )
      collection.withDocumentClass[JsonDataEntry]().findOneAndUpdate(
        filter = Filters.eq(idField, id),
        update = setOperation, new FindOneAndUpdateOptions().upsert(true)).toFuture().map(_ => ())
    }
  }

  def get(id: String)
         (implicit ec: ExecutionContext): Future[Option[JsValue]] = {
    if (encrypted) {
      collection.find[DataEntry](Filters.equal(idField, id)).headOption().map {
        _.map {
          dataEntry =>
            val dataAsString = new String(dataEntry.data.getData, StandardCharsets.UTF_8)
            val decrypted: PlainText = jsonCrypto.decrypt(Crypted(dataAsString))
            Json.parse(decrypted.value)
        }
      }
    } else {
      collection.find[JsonDataEntry](Filters.equal(idField, id)).headOption().map {
        _.map {
          dataEntry =>
            dataEntry.data
        }
      }
    }
  }

  def getLastUpdated(id: String)
                    (implicit ec: ExecutionContext): Future[Option[DateTime]] = {
    if (encrypted) {
      collection.find[DataEntry](Filters.equal(idField, id)).headOption().map {
        _.map {
          dataEntry =>
            dataEntry.lastUpdated
        }
      }
    } else {
      collection.find[JsonDataEntry](Filters.equal(idField, id)).headOption().map {
        _.map {
          dataEntry =>
            dataEntry.lastUpdated
        }
      }
    }
  }

  def remove(id: String)
            (implicit ec: ExecutionContext): Future[Boolean] = {
    collection.deleteOne(Filters.equal(idField, id)).toFuture().map { result =>
      logger.info(s"Removing row from collection ${collectionName} externalId:$id")
      result.wasAcknowledged
    }
  }
}
