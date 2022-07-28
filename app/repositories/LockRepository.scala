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

import com.google.inject.{Inject, Singleton}
import com.mongodb.client.model.FindOneAndUpdateOptions
import config.AppConfig
import models._
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.MongoException
import org.mongodb.scala.model._
import play.api.libs.json._
import play.api.{Configuration, Logging}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LockRepository @Inject()(config: Configuration,
                               appConfig: AppConfig,
                               mongoComponent: MongoComponent)
                              (implicit ec: ExecutionContext)
  extends PlayMongoRepository[SchemeVariance](
    collectionName = config.underlying.getString("mongodb.pensions-scheme-cache.scheme-variation-lock.name"),
    mongoComponent = mongoComponent,
    domainFormat = SchemeVariance.format,
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
        IndexOptions().name("dataExpiry2").expireAfter(0, TimeUnit.SECONDS).background(true)
      )
    )
  ) with Logging {
  //scalastyle:off magic.number
  private lazy val documentExistsErrorCode = 11000
  val srnKey = "srn"
  val psaIdKey = "psaId"

  private def getExpireAt: DateTime =
    DateTime.now(DateTimeZone.UTC).toLocalDate.plusDays(appConfig.defaultDataExpireAfterDays + 1).toDateTimeAtStartOfDay()

  private case class JsonDataEntry(psaId: String, srn: String, data: JsValue, lastUpdated: DateTime, expireAt: DateTime)

  private object JsonDataEntry {
    implicit val dateFormat: Format[DateTime] = MongoJodaFormats.dateTimeFormat
    implicit val format: Format[JsonDataEntry] = Json.format[JsonDataEntry]
  }

  private val filterPsa = Filters.eq("psaId", _: String)
  private val filterSrn = Filters.eq("srn", _: String)

  def releaseLock(lock: SchemeVariance): Future[Unit] = {
    collection.deleteOne(Filters.and(filterPsa(lock.psaId), filterSrn(lock.srn))).toFuture().map(_ => ())
  }

  def releaseLockByPSA(psaId: String): Future[Unit] =
    collection.deleteOne(filterPsa(psaId)).toFuture().map(_ => ())

  def releaseLockBySRN(srn: String): Future[Unit] =
    collection.deleteOne(filterSrn(srn)).toFuture().map(_ => ())

  def getExistingLock(lock: SchemeVariance): Future[Option[SchemeVariance]] =
    collection.find(Filters.and(filterPsa(lock.psaId), filterSrn(lock.srn))).toFuture().map(_.headOption)

  def getExistingLockByPSA(psaId: String): Future[Option[SchemeVariance]] = {
    collection.find(filterPsa(psaId)).toFuture().map(_.headOption)
  }

  def getExistingLockBySRN(srn: String): Future[Option[SchemeVariance]] =
    collection.find(filterSrn(srn)).toFuture().map(_.headOption)

  def isLockByPsaIdOrSchemeId(psaId: String, srn: String): Future[Option[Lock]] = {
    collection.find(Filters.and(filterPsa(psaId), filterSrn(srn))).toFuture().map(_.headOption).flatMap {
      case Some(_) => Future.successful(Some(VarianceLock))
      case None => for {
        psaLock <- getExistingLockByPSA(psaId)
        srnLock <- getExistingLockBySRN(srn)
      } yield {
        (psaLock, srnLock) match {
          case (Some(_), None) => Some(PsaLock)
          case (None, Some(_)) => Some(SchemeLock)
          case (Some(SchemeVariance(_, _)), Some(SchemeVariance(_, _))) => Some(BothLock)
          case (Some(_), Some(_)) => Some(VarianceLock)
          case _ => None
        }
      }
    }
  }

  def lock(newLock: SchemeVariance): Future[Lock] = {
    val dataKey = "data"
    val data: JsValue = Json.toJson(JsonDataEntry(newLock.psaId, newLock.srn, Json.toJson(newLock), DateTime.now(DateTimeZone.UTC), getExpireAt))
    val modifier = Updates.combine(
      Updates.set(psaIdKey, newLock.psaId),
      Updates.set(srnKey, newLock.srn),
      Updates.set(dataKey, Codecs.toBson(data))
    )

    collection.findOneAndUpdate(
      Filters.and(filterPsa(newLock.psaId), filterSrn(newLock.srn)),
      modifier,
      new FindOneAndUpdateOptions().upsert(true)
    ).toFuture().map(_ => VarianceLock)
      .recoverWith {
        case e: MongoException if e.getCode == documentExistsErrorCode =>
          //          case e: LastError if e.code == documentExistsErrorCode =>
          findLock(newLock.psaId, newLock.srn)
      }
  }

  private def findLock(psaId: String, srn: String): Future[Lock] = {
    for {
      psaLock <- getExistingLockByPSA(psaId) // has this psa got any scheme locked
      srnLock <- getExistingLockBySRN(srn) // is this scheme locked to any psa
    } yield {
      (psaLock, srnLock) match {
        case (Some(_), None) => PsaLock // this psa has locked a scheme
        case (None, Some(_)) => SchemeLock // this scheme is locked to a psa
        case (Some(SchemeVariance(_, _)), Some(SchemeVariance(_, _))) => BothLock // this psa has locked a scheme and this scheme is locked
        case (Some(_), Some(_)) => VarianceLock // <- Impossible ??
        case _ => throw new Exception(s"Expected SchemeVariance to be locked, but no lock was found with psaId: $psaId and srn: $srn")
      }
    }
  }
}
