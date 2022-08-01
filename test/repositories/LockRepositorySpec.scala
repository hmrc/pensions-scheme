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

import com.github.simplyscala.MongoEmbedDatabase
import com.typesafe.config.Config
import config.AppConfig
import models._
import org.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}
import play.api.Configuration
import uk.gov.hmrc.mongo.MongoComponent

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration


class LockRepositorySpec extends AnyWordSpec with BeforeAndAfter with Matchers with BeforeAndAfterEach with MongoEmbedDatabase with
  MockitoSugar { // scalastyle:off magic.number

  import repositories.LockRepositorySpec._

  override def beforeEach: Unit = {
    super.beforeEach
    when(mockConfiguration.underlying).thenReturn(mockConfig)
    when(mockConfig.getString("mongodb.pensions-scheme-cache.scheme-variation-lock.name")).thenReturn("scheme_variation_lock")
  }

  withEmbedMongoFixture(port = 24680) { _ =>

    "releaseLock" must {
      "Delete One" in {
        mongoCollectionDrop()

        val documentsInDB = for {
          _ <- lockRepository.collection.insertOne(variance1).toFuture()
          _ <- lockRepository.collection.insertOne(variance2).toFuture()
          _ <- lockRepository.releaseLock(variance2)
          documentsInDB <- lockRepository.collection.countDocuments().toFuture()
        } yield documentsInDB

        whenReady(documentsInDB) { documentsInDB =>
          documentsInDB mustBe 1
        }
      }
    }

    "getExistingLockByPSA" must {
      "Retrieve None" in {
        mongoCollectionDrop()

        val result = lockRepository.getExistingLockByPSA("psa1")

        whenReady(result) { result =>
          result mustBe None
        }
      }

      "Retrieve One" in {
        mongoCollectionDrop()
        val documentsInDB = for {
          _ <- lockRepository.collection.insertOne(variance1).toFuture()
          _ <- lockRepository.collection.insertOne(variance2).toFuture()
          documentsInDB <- lockRepository.getExistingLockByPSA(variance1.psaId)
        } yield documentsInDB

        whenReady(documentsInDB) { documentsInDB =>
          documentsInDB mustBe Some(SchemeVariance("psa1", "srn1"))
        }
      }
    }

    "getExistingLockBySRN" must {
      "Retrieve None" in {
        mongoCollectionDrop()
        val result = lockRepository.getExistingLockBySRN("srn1")

        whenReady(result) { result =>
          result mustBe None
        }
      }
      // works sometimes!!
      "Retrieve One" in {
        mongoCollectionDrop()
        val documentsInDB = for {
          _ <- lockRepository.collection.insertOne(variance1).toFuture()
          _ <- lockRepository.collection.insertOne(variance2).toFuture()
          documentsInDB <- lockRepository.getExistingLockBySRN(variance2.srn)
        } yield documentsInDB

        whenReady(documentsInDB) { documentsInDB =>
          documentsInDB mustBe Some(SchemeVariance("psa2", "srn2"))
        }
      }
    }

    "Lock" must {
      "return locked if its new and unique combination for psaId and srn" in {
        mongoCollectionDrop()
        val documentsInDB = lockRepository.lock(variance1)

        whenReady(documentsInDB) { documentsInDB =>
          documentsInDB mustBe VarianceLock
        }
      }

      "return locked if existing lock" in {
        mongoCollectionDrop()

        val documentsInDB = for {
          _ <- lockRepository.collection.insertOne(variance1).toFuture()
          documentsInDB <- lockRepository.lock(variance1)
        } yield documentsInDB

        whenReady(documentsInDB) { documentsInDB =>
          documentsInDB mustBe VarianceLock
        }
      }

      "return lockNotAvailableForPsa if its not a unique combination for psaId and srn, existing psaId has locked another scheme" in {
        mongoCollectionDrop()
        val documentsInDB = for {
          _ <- lockRepository.collection.insertOne(variance1).toFuture()
          documentsInDB <- lockRepository.lock(SchemeVariance("psa1", "srn2"))
        } yield documentsInDB

        whenReady(documentsInDB) { documentsInDB =>
          documentsInDB mustBe PsaLock
        }
      }

      "return lockNotAvailableForSRN if its not unique combination for psaId and srn, existing srn" in {
        mongoCollectionDrop()
        val documentsInDB = for {
          _ <- lockRepository.collection.insertOne(variance1).toFuture()
          documentsInDB <- lockRepository.lock(SchemeVariance("psa2", "srn1"))
        } yield documentsInDB

        whenReady(documentsInDB) { documentsInDB =>
          documentsInDB mustBe SchemeLock
        }
      }

      "return both locked if its not unique combination for existing psaId2 and srn1" in {
        mongoCollectionDrop()
        val documentsInDB = for {
          _ <- lockRepository.collection.insertOne(variance1).toFuture()
          _ <- lockRepository.collection.insertOne(variance2).toFuture()
          documentsInDB <- lockRepository.lock(SchemeVariance("psa2", "srn1"))
        } yield documentsInDB

        whenReady(documentsInDB) { documentsInDB =>
          documentsInDB mustBe BothLock
        }
      }

      "return both locked if its not unique combination for existing psaId1 and srn2" in {
        mongoCollectionDrop()
        val documentsInDB = for {
          _ <- lockRepository.collection.insertOne(variance1).toFuture()
          _ <- lockRepository.collection.insertOne(variance2).toFuture()
          documentsInDB <- lockRepository.lock(SchemeVariance("psa1", "srn2"))
        } yield documentsInDB

        whenReady(documentsInDB) { documentsInDB =>
          documentsInDB mustBe BothLock
        }
      }
    }

    "isLockByPsaIdOrSchemeId" should {
      "return locked if its new and unique combination for psaId and srn" in {
        mongoCollectionDrop()
        val documentsInDB = lockRepository.isLockByPsaIdOrSchemeId("psa1", "srn1")

        whenReady(documentsInDB) { documentsInDB =>
          documentsInDB mustBe None
        }
      }

      "return locked if exiting lock" in {
        mongoCollectionDrop()

        val documentsInDB = for {
          _ <- lockRepository.collection.insertOne(variance1).toFuture()
          documentsInDB <- lockRepository.isLockByPsaIdOrSchemeId("psa1", "srn1")
        } yield documentsInDB

        whenReady(documentsInDB) { documentsInDB =>
          documentsInDB mustBe Some(VarianceLock)
        }
      }
    }


    "return lockNotAvailableForPsa if its not unique combination for psaId and srn, existing psaId" in {
      mongoCollectionDrop()
      val documentsInDB = for {
        _ <- lockRepository.collection.insertOne(variance1).toFuture()
        documentsInDB <- lockRepository.isLockByPsaIdOrSchemeId("psa1", "srn2")
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB mustBe Some(PsaLock)
      }
    }
    "return lockNotAvailableForSRN if its not unique combination for psaId and srn, existing srn" in {
      mongoCollectionDrop()
      val documentsInDB = for {
        _ <- lockRepository.collection.insertOne(variance1).toFuture()
        documentsInDB <- lockRepository.isLockByPsaIdOrSchemeId("psa2", "srn1")
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB mustBe Some(SchemeLock)
      }
    }

    "return both locked if its not unique combination for existing psaId2 and srn1" in {
      mongoCollectionDrop()
      val documentsInDB = for {
        _ <- lockRepository.collection.insertOne(variance1).toFuture()
        _ <- lockRepository.collection.insertOne(variance2).toFuture()
        documentsInDB <- lockRepository.isLockByPsaIdOrSchemeId("psa2", "srn1")
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB mustBe Some(BothLock)
      }
    }

    "return both locked if its not unique combination for existing psaId1 and srn2" in {
      mongoCollectionDrop()
      val documentsInDB = for {
        _ <- lockRepository.collection.insertOne(variance1).toFuture()
        _ <- lockRepository.collection.insertOne(variance2).toFuture()
        documentsInDB <- lockRepository.isLockByPsaIdOrSchemeId("psa1", "srn2")
      } yield documentsInDB

      whenReady(documentsInDB) { documentsInDB =>
        documentsInDB mustBe Some(BothLock)
      }
    }
  }
}

object LockRepositorySpec extends AnyWordSpec with MockitoSugar {
  private val mockConfiguration = mock[Configuration]
  private val mockConfig = mock[Config]
  private val mockAppConfig = mock[AppConfig]

  private val databaseName = "pensions-scheme"
  private val mongoUri: String = s"mongodb://127.0.0.1:27017/$databaseName?heartbeatFrequencyMS=1000&rm.failover=default"
  private val mongoComponent = MongoComponent(mongoUri)
  val variance1: SchemeVariance = SchemeVariance("psa1", "srn1")
  val variance2: SchemeVariance = SchemeVariance("psa2", "srn2")


  private def mongoCollectionDrop(): Void = Await
    .result(lockRepository.collection.drop().toFuture(), Duration.Inf)

  def lockRepository: LockRepository = new LockRepository(mockConfiguration, mockAppConfig, mongoComponent)


}
