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

import com.google.inject.{Inject, Singleton}
import com.mongodb.client.model.FindOneAndUpdateOptions
import config.AppConfig
import models.*
import org.mongodb.scala.model.*
import org.mongodb.scala.{MongoCommandException, SingleObservableFuture}
import play.api.libs.json.*
import play.api.{Configuration, Logging}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}


object LockRepository {
  private[repositories] case class JsonDataEntry(psaId: String, srn: String, lastUpdated: Instant, expireAt: Instant)

  implicit val format: Format[JsonDataEntry] = Json.format[JsonDataEntry]
}

@Singleton
class LockRepository @Inject()(configuration: Configuration,
                               appConfig: AppConfig,
                               mongoComponent: MongoComponent)
                              (implicit ec: ExecutionContext)
  extends PlayMongoRepository[SchemeVariance](
    collectionName = configuration.underlying.getString("mongodb.pensions-scheme-cache.scheme-variation-lock.name"),
    mongoComponent = mongoComponent,
    domainFormat = SchemeVariance.format,
    extraCodecs = Seq(
      Codecs.playFormatCodec(MongoJavatimeFormats.instantFormat)
    ),
    indexes = Seq(
      IndexModel(
        Indexes.ascending("psaId"),
        IndexOptions().name("psaId_Index").unique(true)
      ),
      IndexModel(
        Indexes.ascending("srn"),
        IndexOptions().name("srn_Index").unique(true)
      ),
      IndexModel(
        Indexes.ascending("expireAt"),
        IndexOptions().name("dataExpiry").expireAfter(0, TimeUnit.SECONDS)
      )
    )
  ) with Logging {
  //scalastyle:off magic.number

  private lazy val documentExistsErrorCode = 11000
  private val srnKey = "srn"
  private val psaIdKey = "psaId"
  private val lastUpdated = "lastUpdated"
  private val expireAt = "expireAt"

  private def getExpireAt: Instant =
    Instant.now().plusSeconds(appConfig.lockTTLSeconds)


  private val filterPsa = Filters.eq("psaId", _: String)
  private val filterSrn = Filters.eq("srn", _: String)

  def releaseLock(lock: SchemeVariance): Future[Unit] = {
    collection.deleteOne(Filters.and(filterPsa(lock.psaId), filterSrn(lock.srn))).toFuture().map(_ => ())
  }

  def getExistingLock(lock: SchemeVariance): Future[Option[SchemeVariance]] =
    collection.find(Filters.and(filterPsa(lock.psaId), filterSrn(lock.srn))).headOption()

  def getExistingLockByPSA(psaId: String): Future[Option[SchemeVariance]] = {
    collection.find(filterPsa(psaId)).headOption()
  }

  def getExistingLockBySRN(srn: String): Future[Option[SchemeVariance]] =
    collection.find(filterSrn(srn)).headOption()

  def isLockByPsaIdOrSchemeId(psaId: String, srn: String): Future[Option[Lock]] = {
    collection.find(Filters.and(filterPsa(psaId), filterSrn(srn))).headOption().flatMap {
      case Some(_) => Future.successful(Some(VarianceLock))
      case None => findLock(psaId, srn).map(Some(_))
        .recoverWith {
          case _: Exception => Future(None)
        }
    }
  }

  def lock(newLock: SchemeVariance): Future[Lock] = {
    val modifier = Updates.combine(
      Updates.set(psaIdKey, newLock.psaId),
      Updates.set(srnKey, newLock.srn),
      Updates.set(lastUpdated, Instant.now()),
      Updates.set(expireAt, getExpireAt)
    )

    collection.findOneAndUpdate(
      Filters.and(filterPsa(newLock.psaId), filterSrn(newLock.srn)),
      modifier,
      new FindOneAndUpdateOptions().upsert(true)
    ).toFuture().map(_ => VarianceLock)
      .recoverWith {
        case e: MongoCommandException if e.getCode == documentExistsErrorCode =>
          findLock(newLock.psaId, newLock.srn)
      }
  }

  private def findLock(psaId: String, srn: String): Future[Lock] = {
    for {
      psaLock <- getExistingLockByPSA(psaId)
      srnLock <- getExistingLockBySRN(srn)
    } yield {
      (psaLock, srnLock) match {
        case (Some(_), None) => PsaLock
        case (None, Some(_)) => SchemeLock
        case (Some(SchemeVariance(_, _)), Some(SchemeVariance(_, _))) => BothLock
        case _ => throw new Exception(s"Expected SchemeVariance to be locked, but no lock was found with psaId: $psaId and srn: $srn")
      }
    }
  }
}
