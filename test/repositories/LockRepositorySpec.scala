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

import com.typesafe.config.Config
import config.AppConfig
import models.*
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import uk.gov.hmrc.mongo.MongoComponent
import org.mongodb.scala.ObservableFuture

import scala.compiletime.uninitialized
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*

class LockRepositorySpec extends AnyWordSpec with BeforeAndAfter with Matchers with BeforeAndAfterEach with Samples
  with MockitoSugar with ScalaFutures { // scalastyle:off magic.number

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(30, Seconds), Span(1, Millis))

  import repositories.LockRepositorySpec._

  var lockRepository: LockRepository = uninitialized

  override def beforeEach(): Unit = {
    when(mockConfiguration.underlying).thenReturn(mockConfig)
    when(mockConfig.getString("mongodb.pensions-scheme-cache.scheme-variation-lock.name")).thenReturn("scheme_variation_lock")
    lockRepository = buildRepository(mongoHost, mongoPort)
    super.beforeEach()
  }

  override def afterEach(): Unit = {
    Await.result(lockRepository.collection.drop().toFuture(), 5.seconds)
    super.afterEach()
  }

  "releaseLock" must {
    "Delete One" in {

      val documentsInDB = for {
        _ <- lockRepository.collection.insertOne(variance1).toFuture()
        _ <- lockRepository.collection.insertOne(variance2).toFuture()
        _ <- lockRepository.releaseLock(variance2)
        documentsInDB <- lockRepository.collection.countDocuments().toFuture()
      } yield documentsInDB
      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB.size mustBe 1
      }
    }
  }

  "Lock" must {
    "return locked if its new and unique combination for psaId and srn" in {
      val documentsInDB = for {
        res <- lockRepository.lock(variance1)
      } yield res

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB mustBe VarianceLock
      }
    }

    "return locked if existing lock" in {
      val documentsInDB = for {
        _ <- lockRepository.collection.insertOne(variance1).toFuture()
        documentsInDB <- lockRepository.lock(variance1)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB mustBe VarianceLock
      }
    }

    "return lockNotAvailableForPsa if its not a unique combination for psaId and srn, existing psaId has locked another scheme" in {
      val documentsInDB = lockRepository.collection.insertOne(variance1).toFuture().flatMap { _ =>
          lockRepository.lock(SchemeVariance(variance1.psaId, "srn2"))
        }


      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB mustBe PsaLock
      }
    }

    "return lockNotAvailableForSRN if its not unique combination for psaId and srn, existing srn" in {
      val documentsInDB = for {
        _ <- lockRepository.collection.insertOne(variance1).toFuture()
        documentsInDB <- lockRepository.lock(SchemeVariance("psa2", variance1.srn))
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB mustBe SchemeLock
      }
    }

    "return both locked if its not unique combination for existing psaId2 and srn1" in {
      val documentsInDB = for {
        _ <- lockRepository.collection.insertOne(variance1).toFuture()
        _ <- lockRepository.collection.insertOne(variance2).toFuture()
        documentsInDB <- lockRepository.lock(SchemeVariance(variance2.psaId, variance1.srn))
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB mustBe BothLock
      }
    }

    "return both locked if its not unique combination for existing psaId1 and srn2" in {
      val documentsInDB = for {
        _ <- lockRepository.collection.insertOne(variance1).toFuture()
        _ <- lockRepository.collection.insertOne(variance2).toFuture()
        documentsInDB <- lockRepository.lock(SchemeVariance(variance1.psaId, variance2.srn))
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB mustBe BothLock
      }
    }
  }

  "isLockByPsaIdOrSchemeId" should {
    "return locked if its new and unique combination for psaId and srn" in {
      val documentsInDB = for {
        res <- lockRepository.isLockByPsaIdOrSchemeId("psa1", "srn1")
      } yield res

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB mustBe None
      }
    }

    "return locked if existing lock" in {
      val documentsInDB = for {
        _ <- lockRepository.collection.insertOne(variance1).toFuture()
        documentsInDB <- lockRepository.isLockByPsaIdOrSchemeId(variance1.psaId, variance1.srn)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB mustBe Some(VarianceLock)
      }
    }
  }


  "return lockNotAvailableForPsa if its not unique combination for psaId and srn, existing psaId" in {
    val documentsInDB = for {
      _ <- lockRepository.collection.insertOne(variance1).toFuture()
      documentsInDB <- lockRepository.isLockByPsaIdOrSchemeId(variance1.psaId, "srn2")
    } yield documentsInDB

    whenReady(documentsInDB) { documentsInDB =>
      documentsInDB mustBe Some(PsaLock)
    }
  }
  "return lockNotAvailableForSRN if its not unique combination for psaId and srn, existing srn" in {
    val documentsInDB = for {
      _ <- lockRepository.collection.insertOne(variance1).toFuture()
      documentsInDB <- lockRepository.isLockByPsaIdOrSchemeId("psa2", variance1.srn)
    } yield documentsInDB

    whenReady(documentsInDB) { documentsInDB =>
      documentsInDB mustBe Some(SchemeLock)
    }
  }

  "return both locked if its not unique combination for existing psaId2 and srn1" in {
    val documentsInDB = for {
      _ <- lockRepository.collection.insertOne(variance1).toFuture()
      _ <- lockRepository.collection.insertOne(variance2).toFuture()
      documentsInDB <- lockRepository.isLockByPsaIdOrSchemeId(variance2.psaId, variance1.srn)
    } yield documentsInDB

    whenReady(documentsInDB) { documentsInDB =>
      documentsInDB mustBe Some(BothLock)
    }
  }

  "return both locked if its not unique combination for existing psaId1 and srn2" in {
    val documentsInDB = for {
      _ <- lockRepository.collection.insertOne(variance1).toFuture()
      _ <- lockRepository.collection.insertOne(variance2).toFuture()
      documentsInDB <- lockRepository.isLockByPsaIdOrSchemeId(variance1.psaId, variance2.srn)
    } yield documentsInDB

    whenReady(documentsInDB) { documentsInDB =>
      documentsInDB mustBe Some(BothLock)
    }
  }


  "getExistingLockByPSA" must {
    "Retrieve None" in {
      val documentsInDB = for {
        res <- lockRepository.getExistingLockByPSA("invalid-psa-id")
      } yield res

      whenReady(documentsInDB) { result =>
        result mustBe None
      }
    }

    "Retrieve One" in {
      val documentsInDB = for {
        _ <- lockRepository.collection.insertOne(variance1).toFuture()
        documentsInDB <- lockRepository.getExistingLockByPSA(variance1.psaId)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB mustBe Some(variance1)
      }
    }
  }

  "getExistingLockBySRN" must {
    "Retrieve None" in {
      val documentsInDB = for {
        res <- lockRepository.getExistingLockBySRN("invalid-srn-id")
      } yield res

      whenReady(documentsInDB) { result =>
        result mustBe None
      }
    }

    "Retrieve One" in {
      val documentsInDB = for {
        _ <- lockRepository.collection.insertOne(variance2).toFuture()
        documentsInDB <- lockRepository.getExistingLockBySRN(variance2.srn)
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB mustBe Some(variance2)
      }
    }
  }
}

object LockRepositorySpec extends MockitoSugar {

  private val mockConfiguration = mock[Configuration]
  private val mockConfig = mock[Config]
  private val mockAppConfig = mock[AppConfig]

  val variance1: SchemeVariance = SchemeVariance("psa1", "srn1")
  val variance2: SchemeVariance = SchemeVariance("psa2", "srn2")

  private def buildRepository(mongoHost: String, mongoPort: Int): LockRepository = {
    val databaseName = "pensions-scheme"
    val mongoUri = s"mongodb://$mongoHost:$mongoPort/$databaseName?heartbeatFrequencyMS=1000&rm.failover=default"
    new LockRepository(mockConfiguration, mockAppConfig, MongoComponent(mongoUri))
  }
}
