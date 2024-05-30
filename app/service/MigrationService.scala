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

package service

import com.google.inject.Inject
import org.mongodb.scala.bson.BsonDateTime
import org.mongodb.scala.model.{Filters, Updates}
import play.api.Logging
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}

import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

class MigrationService @Inject()(mongoLockRepository: MongoLockRepository,
                                 mongoComponent: MongoComponent)(implicit ec: ExecutionContext) extends Logging {
  /*
  private val lock = LockService(mongoLockRepository, "event_reporting_data_expireAtLock", Duration(10, TimeUnit.MINUTES))

  private def fixExpireAt(collectionName: String) = {
    val collection = mongoComponent.database.getCollection(collectionName)
    val date = Instant.now()
    logger.warn("[PODS-9319] Started event reporting data migration for " + collectionName + s" collection setting date to $date")
    collection.updateMany(filter = Filters.empty(), update = Updates.set("expireAt", BsonDateTime(java.util.Date.from(date))))
      .toFuture().map { update =>
        update.getMatchedCount -> update.getModifiedCount
      }
  }

  logger.warn("[PODS-9319] Migration started")
  lock withLock {
    for {
      res <- fixExpireAt("pensions-scheme-scheme-details")
    } yield res
  } map {
    case Some((matches, modified)) =>
      logger.warn(s"[PODS-9319] data migration completed, $matches rows were matched. $modified rows were modified")
    case None => logger.warn(s"[PODS-9319] data migration locked by other instance")
  } recover {
    case e => logger.error("Locking finished with error", e)
  }
  */
}
