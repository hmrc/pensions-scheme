/*
 * Copyright 2021 HM Revenue & Customs
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
import models.SchemeWithId
import org.joda.time.{DateTime, DateTimeZone}
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.{ExecutionContext, Future}

class SchemeDetailsWithIdCacheRepository @Inject()(
                                     mongoComponent: ReactiveMongoComponent,
                                     configuration: Configuration
                                   )(implicit val ec: ExecutionContext)
  extends ReactiveRepository[JsValue, BSONObjectID](
    configuration.get[String](path = "mongodb.pensions-scheme-cache.scheme-with-id.name"),
    mongoComponent.mongoConnector.db,
    implicitly
  ) {

  override val logger: Logger = LoggerFactory.getLogger("SchemeDetailsWithIdCacheRepository")

  private def expireInSeconds: DateTime = DateTime.now(DateTimeZone.UTC).
    plusSeconds(configuration.get[Int](path = "mongodb.pensions-scheme-cache.scheme-with-id.timeToLiveInSeconds"))

  override lazy val indexes: Seq[Index] = Seq(
    Index(key = Seq("uniqueSchemeWithId" -> Ascending), name = Some("schemeId_userId_index"), unique = true),
    Index(key = Seq("expireAt" -> Ascending), name = Some("dataExpiry"), options = BSONDocument("expireAfterSeconds" -> 0))
  )

  (for { _ <- ensureIndexes } yield { () }) recoverWith {
    case t: Throwable => Future.successful(logger.error(s"Error creating indexes on collection ${collection.name}", t))
  } andThen {
    case _ => CollectionDiagnostics.logCollectionInfo(collection)
  }

  override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] =
    Future.sequence(indexes.map(collection.indexesManager.ensure(_)))

  private val selector: SchemeWithId => BSONDocument = schemeWithId =>
    BSONDocument("uniqueSchemeWithId" -> (schemeWithId.schemeId + schemeWithId.userId))

  private val modifier: JsValue => BSONDocument = document =>
    BSONDocument("$set" -> document)

  def save(schemeWithId: SchemeWithId, schemeDetails: JsValue): Future[Boolean] = {
    val id: String = schemeWithId.schemeId + schemeWithId.userId
    val document: JsValue = Json.toJson(DataCache.applyDataCache(id, schemeDetails, expireAt = expireInSeconds))
    collection.update(true).one(selector(schemeWithId), modifier(document), upsert = true).map(_.ok)
  }

  def get(schemeWithId: SchemeWithId): Future[Option[JsValue]] =
    collection.find(selector(schemeWithId), Option.empty[JsObject]).one[DataCache].map(_.map(_.data))

  def remove(schemeWithId: SchemeWithId): Future[Boolean] =
    collection.delete.one(selector(schemeWithId)).map(_.ok)

}

private case class DataCache(id: String, data: JsValue, lastUpdated: DateTime, expireAt: DateTime)

private object DataCache {
  implicit val dateFormat: Format[DateTime] = ReactiveMongoFormats.dateTimeFormats
  implicit val format: Format[DataCache] = Json.format[DataCache]

  def applyDataCache(id: String, data: JsValue,
            lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC),
            expireAt: DateTime): DataCache =
    DataCache(id, data, lastUpdated, expireAt)
}
