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
import models.SchemeWithId
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.model._
import play.api.libs.json._
import play.api.{Configuration, Logging}
import repositories.SchemeDetailsWithIdCacheRepository._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import scala.concurrent.{ExecutionContext, Future}

object SchemeDetailsWithIdCacheRepository {

  private val dataKey: String = "data"
  private val idField: String = "uniqueSchemeWithId"
  private val lastUpdatedKey: String = "lastUpdated"
  private val expireAtKey: String = "expireAt"

  case class JsonDataEntry(uniqueSchemeWithId: String, data: JsValue, lastUpdated: DateTime, expireAt: DateTime)

  object JsonDataEntry {
    implicit val dateFormat: Format[DateTime] = MongoJodaFormats.dateTimeFormat
    implicit val format: Format[JsonDataEntry] = Json.format[JsonDataEntry]

  }
}

class SchemeDetailsWithIdCacheRepository @Inject()(
                                                    mongoComponent: MongoComponent,
                                                    configuration: Configuration
                                                  )(implicit val ec: ExecutionContext)
  extends PlayMongoRepository[JsonDataEntry](
    mongoComponent = mongoComponent,
    collectionName = configuration.get[String](path = "mongodb.pensions-scheme-cache.scheme-with-id.name"),
    domainFormat = JsonDataEntry.format,
    extraCodecs = Seq(
      Codecs.playFormatCodec(JsonDataEntry.format)
    ),
    indexes = Seq(
      IndexModel(
        Indexes.ascending(lastUpdatedKey),
        IndexOptions().name(expireAtKey).background(true)
      )
    )
  ) with Logging {

  import JsonDataEntry._

  private val filterScheme = Filters.equal("uniqueSchemeWithId", _: String)

  private def expireInSeconds: DateTime = DateTime.now(DateTimeZone.UTC).
    plusSeconds(configuration.get[Int](path = "mongodb.pensions-scheme-cache.scheme-with-id.timeToLiveInSeconds"))

  def save(schemeWithId: SchemeWithId, schemeDetails: JsValue): Future[Boolean] = {
    val id: String = schemeWithId.schemeId + schemeWithId.userId
    val modifier = Updates.combine(
      Updates.set(idField, id),
      Updates.set(dataKey, Codecs.toBson(schemeDetails)),
      Updates.set(lastUpdatedKey, Codecs.toBson(DateTime.now(DateTimeZone.UTC))),
      Updates.set(expireAtKey, Codecs.toBson(expireInSeconds))
    )
//
//    collection.withDocumentClass[JsonDataEntry]().findOneAndUpdate(
//      filter = Filters.eq(idField, id),
//      update = setOperation, new FindOneAndUpdateOptions().upsert(true)).toFuture().map(_ => ())

    collection.withDocumentClass[JsonDataEntry]().findOneAndUpdate(filterScheme(id), modifier,
      new FindOneAndUpdateOptions().upsert(true)).toFuture().map(_ => true)

  }

  /*
  {
   "_id":{
      "$oid":"62da9dfc70fa3d765b416a7d"
   },
   "uniqueSchemeWithId":"S2400000001A2100005",
   "data":{
      "benefits":"opt1",
      "pspDetails":[

...

   },
   "expireAt":{
      "$date":{
         "$numberLong":"1658498060670"
      }
   },
   "lastUpdated":{
      "$date":{
         "$numberLong":"1658494460670"
      }
   }
}
   */

  def get(schemeWithId: SchemeWithId): Future[Option[JsValue]] = {
    collection.find[JsonDataEntry](Filters.equal(idField, schemeWithId.schemeId + schemeWithId.userId)).headOption().map {
      _.map(_.data)
    }
  }

  def remove(schemeWithId: SchemeWithId): Future[Boolean] = {
    collection.deleteOne(Filters.equal(idField, schemeWithId.schemeId + schemeWithId.userId)).toFuture().map { result =>
      logger.info(s"Removing row from collection $collectionName externalId:${schemeWithId.schemeId}")
      result.wasAcknowledged
    }
  }

}
