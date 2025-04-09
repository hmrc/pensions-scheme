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

import com.google.inject.Inject
import com.mongodb.client.model.FindOneAndUpdateOptions
import models.SchemeWithId
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.*
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.*
import play.api.{Configuration, Logging}
import repositories.SchemeDetailsWithIdCacheRepository.*
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter, PlainText, SymmetricCryptoFactory}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.time.Instant
import java.util.concurrent.TimeUnit
import _root_.javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

object SchemeDetailsWithIdCacheRepository {

  private val dataKey: String = "data"
  private val idField: String = "id"
  private val uniqueSchemeWithId: String = "uniqueSchemeWithId"
  private val lastUpdatedKey: String = "lastUpdated"
  private val expireAtKey: String = "expireAt"

  case class DataCache(id: String, data: JsValue, lastUpdated: Instant, expireAt: Instant)

  object DataCache {
    implicit val format: Format[DataCache] = new Format[DataCache] {
      override def writes(o: DataCache): JsValue = Json.writes[DataCache].writes(o)

      private val instantReads = MongoJavatimeFormats.instantReads

      override def reads(json: JsValue): JsResult[DataCache] = (
        (JsPath \ "id").read[String] and
          (JsPath \ "data").read[JsValue] and
          (JsPath \ "lastUpdated").read(instantReads) and
          (JsPath \ "expireAt").read(instantReads)
      )((id, data, lastUpdated, expireAt) => DataCache(id, data, lastUpdated, expireAt))
        .reads(json)
    }
  }
}

@Singleton
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

  private val jsonCrypto: Encrypter & Decrypter = SymmetricCryptoFactory
    .aesCryptoFromConfig(baseConfigKey = "scheme.json.encryption", configuration.underlying)
  private val encrypted: Boolean = configuration.get[Boolean]("encrypted")
  private implicit val cryptoFormat: OFormat[Crypted] = Json.format[Crypted]

  private def expireInSeconds: Instant = Instant.now().
    plusSeconds(configuration.get[Int](path = "mongodb.pensions-scheme-cache.scheme-with-id.timeToLiveInSeconds"))

  def upsert(schemeWithId: SchemeWithId, schemeDetails: JsValue): Future[Boolean] = {
    val data = {
      if(encrypted) {
        val encryptedData = jsonCrypto.encrypt(PlainText(Json.stringify(schemeDetails)))
        Codecs.toBson(Json.toJson(encryptedData))
      } else {
        Codecs.toBson(schemeDetails)
      }
    }

    val id: String = schemeWithId.schemeId + schemeWithId.userId
    val modifier = Updates.combine(
      Updates.set(idField, id),
      Updates.set(dataKey, data),
      Updates.set(lastUpdatedKey, Instant.now()),
      Updates.set(expireAtKey, expireInSeconds)
    )

    collection.withDocumentClass[DataCache]().findOneAndUpdate(Filters.equal(uniqueSchemeWithId, id), modifier,
      new FindOneAndUpdateOptions().upsert(true)).toFuture().map(_ => true)
  }

  def get(schemeWithId: SchemeWithId): Future[Option[JsValue]] = {
    val id: String = schemeWithId.schemeId + schemeWithId.userId
    collection.find[DataCache](Filters.equal(uniqueSchemeWithId, id)).headOption().map {
      _.map { resp =>
        val data = resp.data
        data.validate[Crypted].map { encryptedData =>
          Json.parse(jsonCrypto.decrypt(encryptedData).value)
        }.getOrElse(data)
      }
    }
  }
}
