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

package service

import com.google.inject.Inject
import play.api.Logging
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

class DropLockEntriesService @Inject()(mongoLockRepository: MongoLockRepository,
                                       mongoComponent: MongoComponent)(implicit ec: ExecutionContext) extends Logging {
  private val lock = LockService(mongoLockRepository, "minimal_detail_data_expireAtLock", Duration(10, TimeUnit.MINUTES))

  private def dropLockEntries = {
    val collection = mongoComponent.database.getCollection("scheme_variation_lock")
    logger.info("[DDCNL-10877] Started drop of scheme_variation_lock collection")
    collection.drop().headOption()
  }

  lock.withLock {
    dropLockEntries.map {
      case Some(_) => logger.debug("[DDCNL-10877] drop of scheme_variation_lock collection complete")
      case None => logger.debug("[DDCNL-10877] drop of scheme_variation_lock locked by other instance")
    } recover {
      case e => logger.error("[DDCNL-10877] Locking finished with error", e)
    }
  }
}
