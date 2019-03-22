/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.LastError
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.api.{Cursor, DB}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.mongo.ReactiveRepository

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


case class SchemeVariance(psaId: String, srn: String)

object SchemeVariance {
  implicit val format: OFormat[SchemeVariance] = Json.format[SchemeVariance]
}

@ImplementedBy(classOf[LockMongoRepository])
trait LockRepository {

  def removeLock(lock: SchemeVariance): Future[Unit]

  def removeByPSA(psaId: String): Future[Unit]

  def removeBySRN(id: String): Future[Unit]

  def getExistingLock(lock: SchemeVariance): Future[Option[SchemeVariance]]

  def getExistingLockByPSA(psaId: String): Future[Option[SchemeVariance]]

  def getExistingLockBySRN(srn: String): Future[Option[SchemeVariance]]

  def list: Future[List[SchemeVariance]]

  def replaceLock(newLock: SchemeVariance): Future[Boolean]

  def lock(newLock: SchemeVariance): Future[(Boolean, Boolean)]
}

@Singleton
class LockMongoRepository @Inject()(config: AppConfig,
                                    mongoDbProvider: MongoDbProvider)
  extends ReactiveRepository[SchemeVariance, BSONObjectID](
    collectionName = "scheme_variation_lock",
    mongo = mongoDbProvider.mongo,
    domainFormat = SchemeVariance.format) with LockRepository {

  private lazy val documentExistsErrorCode = Some(11000)

  override lazy val indexes: Seq[Index] = Seq(
    Index(key = Seq("psaId" -> Ascending), name = Some("psaId_Index"), unique = true),
    Index(key = Seq("srn" -> Ascending), name = Some("srn_Index"), unique = true)
  )

  override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] = {
    Future.sequence(indexes.map(collection.indexesManager.ensure(_)))
  }

  override def removeLock(lock: SchemeVariance): Future[Unit] = collection.findAndRemove(byLock(lock.psaId, lock.srn)).map(_ => ())

  override def removeByPSA(psaId: String): Future[Unit] = collection.findAndRemove(byPsaId(psaId)).map(_ => ())

  override def removeBySRN(srn: String): Future[Unit] = collection.findAndRemove(bySrn(srn)).map(_ => ())

  override def getExistingLock(lock: SchemeVariance): Future[Option[SchemeVariance]] = collection.find(byLock(lock.psaId, lock.srn)).one[SchemeVariance]

  override def getExistingLockByPSA(psaId: String): Future[Option[SchemeVariance]] = collection.find(byPsaId(psaId)).one[SchemeVariance]

  override def getExistingLockBySRN(srn: String): Future[Option[SchemeVariance]] = collection.find(bySrn(srn)).one[SchemeVariance]

  override def list: Future[List[SchemeVariance]] = {
    val arbitraryLimit = 10000
    collection.find(Json.obj())
      .cursor[SchemeVariance]()
      .collect[List](arbitraryLimit, Cursor.FailOnError())
  }

  override def replaceLock(newLock: SchemeVariance): Future[Boolean] = {
    collection.update(
      byLock(newLock.psaId,newLock.srn),
      Json.obj("$set" -> newLock), upsert = true).map {
      lastError =>
        lastError.writeErrors.isEmpty
    } recoverWith {
      case e: LastError if e.code == documentExistsErrorCode => {
        getExistingLock(newLock).map {
          _.getOrElse(throw new Exception(s"Expected SchemeVariance to be locked, but no lock was found with psaId: ${newLock.psaId} and srn: ${newLock.srn}"))
        }.map{existingLock=> existingLock.psaId==newLock.psaId && existingLock.srn==newLock.srn }
      }
    }
  }

  override def lock(newLock: SchemeVariance): Future[(Boolean, Boolean)] = {
    collection.insert(newLock)
      .map(_ => (true, true)) recoverWith {
      case e: LastError if e.code == documentExistsErrorCode => {
        for{
          psaLock <- getExistingLockByPSA(newLock.psaId)
          srnLock <- getExistingLockBySRN(newLock.srn)
        } yield {
          (psaLock, srnLock) match {
            case (Some(existingPsaLock), None) => (existingPsaLock.psaId!=newLock.psaId, existingPsaLock.srn!=newLock.srn)
            case (None, Some(existingSrnLock)) => (existingSrnLock.psaId!=newLock.psaId, existingSrnLock.srn!=newLock.srn)
            case (Some(existingPsaLock), Some(existingSrnLock)) => (existingSrnLock.psaId==newLock.psaId, existingSrnLock.srn==newLock.srn)
            case _ => throw new Exception(s"Expected SchemeVariance to be locked, but no lock was found with psaId: ${newLock.psaId} and srn: ${newLock.srn}")
          }
        }
      }
    }
  }

  private def byPsaId(psaId: String): JsObject = {
    Json.obj("psaId" -> psaId)
  }

  private def bySrn(srn: String): JsObject = {
    Json.obj("srn" -> srn)
  }

  private def byLock(psaId: String, srn: String): JsObject = {
    Json.obj("psaId" -> psaId, "srn" -> srn)
  }
}
