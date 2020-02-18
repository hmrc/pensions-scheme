/*
 * Copyright 2020 HM Revenue & Customs
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

import com.google.inject.{ImplementedBy, Inject, Singleton}
import config.AppConfig
import models._
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.LastError
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.api.{Cursor, DB}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[MongoDb])
trait MongoDbProvider {
  def mongo: () => DB
}

@Singleton
class MongoDb @Inject()(component: ReactiveMongoComponent) extends MongoDbProvider {
  override val mongo: () => DB = component.mongoConnector.db
}


@ImplementedBy(classOf[LockMongoRepository])
trait LockRepository {

  def releaseLock(lock: SchemeVariance): Future[Unit]

  def releaseLockByPSA(psaId: String): Future[Unit]

  def releaseLockBySRN(id: String): Future[Unit]

  def getExistingLock(lock: SchemeVariance): Future[Option[SchemeVariance]]

  def getExistingLockByPSA(psaId: String): Future[Option[SchemeVariance]]

  def getExistingLockBySRN(srn: String): Future[Option[SchemeVariance]]

  def isLockByPsaIdOrSchemeId(psaId: String, srn: String): Future[Option[Lock]]

  def list: Future[List[SchemeVariance]]

  def replaceLock(newLock: SchemeVariance): Future[Boolean]

  def lock(newLock: SchemeVariance): Future[Lock]
}

@Singleton
class LockMongoRepository @Inject()(config: AppConfig,
                                    mongoDbProvider: MongoDbProvider)
  extends ReactiveRepository[SchemeVariance, BSONObjectID](
    collectionName = "scheme_variation_lock",
    mongo = mongoDbProvider.mongo,
    domainFormat = SchemeVariance.format) with LockRepository {
  //scalastyle:off magic.number
  private lazy val documentExistsErrorCode = Some(11000)

  private def getExpireAt: DateTime =
    DateTime.now(DateTimeZone.UTC).toLocalDate.plusDays(config.defaultDataExpireAfterDays + 1).toDateTimeAtStartOfDay()

  private case class JsonDataEntry(psaId: String, srn: String, data: JsValue, lastUpdated: DateTime, expireAt: DateTime)

  private object JsonDataEntry {
    implicit val dateFormat: Format[DateTime] = ReactiveMongoFormats.dateTimeFormats
    implicit val format: OFormat[JsonDataEntry] = Json.format[JsonDataEntry]
  }


  override lazy val indexes: Seq[Index] = Seq(
    Index(key = Seq("psaId" -> Ascending), name = Some("psaId_Index"), unique = true),
    Index(key = Seq("srn" -> Ascending), name = Some("srn_Index"), unique = true),
    Index(key = Seq("expireAt" -> Ascending), name = Some("dataExpiry"), options = BSONDocument("expireAfterSeconds" -> 0))
  )

  override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] = {
    Future.sequence(indexes.map(collection.indexesManager.ensure(_)))
  }

  override def releaseLock(lock: SchemeVariance): Future[Unit] = collection.findAndRemove(byLock(lock.psaId, lock.srn)).map(_ => ())

  override def releaseLockByPSA(psaId: String): Future[Unit] = collection.findAndRemove(byPsaId(psaId)).map(_ => ())

  override def releaseLockBySRN(srn: String): Future[Unit] = collection.findAndRemove(bySrn(srn)).map(_ => ())

  override def getExistingLock(lock: SchemeVariance): Future[Option[SchemeVariance]] = collection.find(byLock(lock.psaId, lock.srn)).one[SchemeVariance]

  override def getExistingLockByPSA(psaId: String): Future[Option[SchemeVariance]] = collection.find(byPsaId(psaId)).one[SchemeVariance]

  override def getExistingLockBySRN(srn: String): Future[Option[SchemeVariance]] = collection.find(bySrn(srn)).one[SchemeVariance]

  override def isLockByPsaIdOrSchemeId(psaId: String, srn: String): Future[Option[Lock]] = collection.find(
    byLock(psaId, srn)).one[SchemeVariance].flatMap[Option[Lock]] {
    case Some(x) => Future.successful(Some(VarianceLock))
    case None => for {
      psaLock <- getExistingLockByPSA(psaId)
      srnLock <- getExistingLockBySRN(srn)
    } yield {
      (psaLock, srnLock) match {
        case (Some(_), None) => Some(PsaLock)
        case (None, Some(_)) => Some(SchemeLock)
        case (Some(SchemeVariance(psaId, _)), Some(SchemeVariance(_, srn))) => Some(BothLock)
        case (Some(SchemeVariance(_, srn)), Some(SchemeVariance(psaId, _))) => Some(BothLock)
        case (Some(_), Some(_)) => Some(VarianceLock)
        case _ => None
      }
    }
  }

  override def list: Future[List[SchemeVariance]] = {
    //scalastyle:off magic.number
    val arbitraryLimit = 10000
    collection.find(Json.obj())
      .cursor[SchemeVariance]()
      .collect[List](arbitraryLimit, Cursor.FailOnError())
  }

  override def replaceLock(newLock: SchemeVariance): Future[Boolean] = {
    collection.update(
      byLock(newLock.psaId, newLock.srn), modifier(newLock), upsert = true).map {
      lastError =>
        lastError.writeErrors.isEmpty
    } recoverWith {
      case e: LastError if e.code == documentExistsErrorCode => {
        getExistingLock(newLock).map {
          case Some(existingLock) => existingLock.psaId == newLock.psaId && existingLock.srn == newLock.srn
          case None => throw new Exception(s"Expected SchemeVariance to be locked, but no lock was found with psaId: ${newLock.psaId} and srn: ${newLock.srn}")
        }
      }
    }
  }

  override def lock(newLock: SchemeVariance): Future[Lock] = {

    collection.update(byLock(newLock.psaId, newLock.srn), modifier(newLock), upsert = true)
      .map[Lock](_ => VarianceLock) recoverWith {
      case e: LastError if e.code == documentExistsErrorCode => {
        findLock(newLock.psaId, newLock.srn)
      }
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
        case (Some(SchemeVariance(psaId, _)), Some(SchemeVariance(_, srn))) => BothLock
        case (Some(SchemeVariance(_, srn)), Some(SchemeVariance(psaId, _))) => BothLock
        case (Some(_), Some(_)) => VarianceLock
        case _ => throw new Exception(s"Expected SchemeVariance to be locked, but no lock was found with psaId: ${psaId} and srn: ${srn}")
      }
    }
  }

  private def byPsaId(psaId: String): BSONDocument = BSONDocument("psaId" -> psaId)

  private def bySrn(srn: String): BSONDocument = BSONDocument("srn" -> srn)

  private def byLock(psaId: String, srn: String): BSONDocument = BSONDocument("psaId" -> psaId, "srn" -> srn)

  private def modifier(newLock: SchemeVariance): BSONDocument = BSONDocument("$set" -> Json.toJson(
    JsonDataEntry(newLock.psaId, newLock.srn, Json.toJson(newLock), DateTime.now(DateTimeZone.UTC), getExpireAt)))
}
