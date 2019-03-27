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

import config.AppConfig
import models.{SchemeVariance, SchemeVarianceLock}
import org.mockito.Mockito._
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import reactivemongo.api.DB
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.mongo.MongoSpecSupport

import scala.concurrent.ExecutionContext.Implicits.global

class LockMongoRepositoryTest extends MongoUnitSpec
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with MongoSpecSupport
  with Eventually
  with MockitoSugar {
  self =>

  private val provider: MongoDbProvider = new MongoDbProvider {
    override val mongo: () => DB = self.mongo
  }

  private val appConfig = mock[AppConfig]
  when(appConfig.defaultDataExpireAfterDays).thenReturn(0)

  private def repository = new LockMongoRepository(appConfig, provider)

  override protected def collection: JSONCollection = repository.collection

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(repository.drop)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(repository.drop)
  }

  "releaseLock" should {
    "Delete One" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      givenAnExistingDocument(SchemeVariance("psa2", "srn2"))

      await(repository.releaseLock(SchemeVariance("psa2", "srn2")))

      thenTheDocumentCountShouldBe(1)
    }
  }

  "releaseLockByPSA by psaId" should {
    "Delete One" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      givenAnExistingDocument(SchemeVariance("psa2", "srn2"))

      await(repository.releaseLockByPSA("psa1"))

      thenTheDocumentCountShouldBe(1)
    }
  }

  "releaseLockBySRN by srn" should {
    "Delete One" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      givenAnExistingDocument(SchemeVariance("psa2", "srn2"))

      await(repository.releaseLockBySRN("srn2"))

      thenTheDocumentCountShouldBe(1)
    }
  }

  "getExistingLockByPSA" should {
    "Retrieve None" in {
      await(repository.getExistingLockByPSA("some id")) shouldBe None
    }

    "Retrieve One" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      givenAnExistingDocument(SchemeVariance("psa2", "srn2"))

      await(repository.getExistingLockByPSA("psa1")) shouldBe Some(SchemeVariance("psa1", "srn1"))
    }
  }

  "getExistingLockBySRN" should {
    "Retrieve None" in {
      await(repository.getExistingLockBySRN("some id")) shouldBe None
    }

    "Retrieve One" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      givenAnExistingDocument(SchemeVariance("psa2", "srn2"))

      await(repository.getExistingLockBySRN("srn2")) shouldBe Some(SchemeVariance("psa2", "srn2"))
    }
  }

  "list" should {
    "Retrieve None" in {
      await(repository.list) shouldBe List()
    }

    "Retrieve all" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      givenAnExistingDocument(SchemeVariance("psa2", "srn2"))

      await(repository.list) shouldBe Seq(SchemeVariance("psa1", "srn1"),SchemeVariance("psa2", "srn2"))
    }
  }

  "replaceLock" should {
    "return true for existing lock" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      givenAnExistingDocument(SchemeVariance("psa2", "srn2"))

      await(repository.replaceLock(SchemeVariance("psa2", "srn2"))) shouldBe true
    }

    "return true and new lock if its not exist lock" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      givenAnExistingDocument(SchemeVariance("psa2", "srn2"))

      await(repository.replaceLock(SchemeVariance("psa3", "srn3"))) shouldBe true
    }

    "throw exception if failed to find the lock which doesn't allow" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      givenAnExistingDocument(SchemeVariance("psa2", "srn2"))

      val result = repository.replaceLock(SchemeVariance("psa2", "srn1"))

      whenReady(result.failed) { e =>
        e shouldBe a[Exception]
        e.getMessage shouldBe "Expected SchemeVariance to be locked, but no lock was found with psaId: psa2 and srn: srn1"
      }
    }
  }

  "lock" should{

    val lockNotAvailableForPsa : Boolean = false
    val lockNotAvailableForSRN : Boolean = false
    val locked : Boolean = true

    "return locked if its new and unique combination for psaId and srn"in {
      await(repository.lock(SchemeVariance("psa1", "srn1"))) shouldBe SchemeVarianceLock(locked, locked)
    }

    "return locked if exiting lock"in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      await(repository.lock(SchemeVariance("psa1", "srn1"))) shouldBe SchemeVarianceLock(locked, locked)
    }

    "return lockNotAvailableForPsa if its not unique combination for psaId and srn, existing psaId"in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      await(repository.lock(SchemeVariance("psa1", "srn2"))) shouldBe SchemeVarianceLock(lockNotAvailableForPsa, locked)
    }

    "return lockNotAvailableForSRN if its not unique combination for psaId and srn, existing srn"in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      await(repository.lock(SchemeVariance("psa2", "srn1"))) shouldBe SchemeVarianceLock(locked, lockNotAvailableForSRN)
    }

    "create ttl on expireAt field" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      val index = getIndex("expireAt").get
      val expected = Index(key = Seq("expireAt" -> Ascending),
        name = Some("dataExpiry"),
        options = BSONDocument("expireAfterSeconds" -> 0)).copy(version = index.version)

      index shouldBe expected
    }
  }

  private def givenAnExistingDocument(schemeVariance: SchemeVariance): Unit = {
    await(repository.collection.insert(schemeVariance))
  }

  private def thenTheDocumentCountShouldBe(count: Int): Unit = {
    await(repository.collection.count()) shouldBe count
  }

}