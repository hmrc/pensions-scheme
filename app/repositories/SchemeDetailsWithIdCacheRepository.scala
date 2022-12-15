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

import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

@Singleton
object SchemeDetailsWithIdCacheRepository {

  private val dataKey: String = "data"
  private val idField: String = "id"
  private val uniqueSchemeWithId: String = "uniqueSchemeWithId"
  private val lastUpdatedKey: String = "lastUpdated"
  private val expireAtKey: String = "expireAt"

  case class DataCache(id: String, data: JsValue, lastUpdated: DateTime, expireAt: DateTime)

  object DataCache {
    implicit val dateFormat: Format[DateTime] = MongoJodaFormats.dateTimeFormat
    implicit val format: Format[DataCache] = Json.format[DataCache]
  }
}

class SchemeDetailsWithIdCacheRepository @Inject()(
                                                    mongoComponent: MongoComponent,
                                                    configuration: Configuration
                                                  )(implicit val ec: ExecutionContext)
  extends PlayMongoRepository[DataCache](
    mongoComponent = mongoComponent,
    collectionName = configuration.get[String](path = "mongodb.pensions-scheme-cache.scheme-with-id.name"),
    domainFormat = DataCache.format,
    extraCodecs = Seq(
      Codecs.playFormatCodec(DataCache.format)
    ),
    indexes = Seq(
      IndexModel(
        Indexes.ascending(uniqueSchemeWithId),
        IndexOptions().name("schemeId_userId_index").unique(true)
      ),
      IndexModel(
        Indexes.ascending(expireAtKey),
        IndexOptions().name("dataExpiry").expireAfter(0, TimeUnit.SECONDS)
      )
    )
  ) with Logging {

  import DataCache._

  private def expireInSeconds: DateTime = DateTime.now(DateTimeZone.UTC).
    plusSeconds(configuration.get[Int](path = "mongodb.pensions-scheme-cache.scheme-with-id.timeToLiveInSeconds"))

  def upsert(schemeWithId: SchemeWithId, schemeDetails: JsValue): Future[Boolean] = {
    val id: String = schemeWithId.schemeId + schemeWithId.userId
    val modifier = Updates.combine(
      Updates.set(idField, id),
      Updates.set(dataKey, Codecs.toBson(schemeDetails)),
      Updates.set(lastUpdatedKey, Codecs.toBson(DateTime.now(DateTimeZone.UTC))),
      Updates.set(expireAtKey, Codecs.toBson(expireInSeconds))
    )

    collection.withDocumentClass[DataCache]().findOneAndUpdate(Filters.equal(uniqueSchemeWithId, id), modifier,
      new FindOneAndUpdateOptions().upsert(true)).toFuture().map(_ => true)
  }

  def get(schemeWithId: SchemeWithId): Future[Option[JsValue]] = {
    val id: String = schemeWithId.schemeId + schemeWithId.userId
    collection.find[DataCache](Filters.equal(uniqueSchemeWithId, id)).headOption().map {
      _.map(_.data)
    }
  }
}
