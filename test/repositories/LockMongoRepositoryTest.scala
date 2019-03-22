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
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import reactivemongo.api.DB
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

  private val config = mock[AppConfig]

  private def repository = new LockMongoRepository(config, provider)

  override protected def collection: JSONCollection = repository.collection

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(repository.drop)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(repository.drop)
  }

  "removeLock" should {
    "Delete One" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      givenAnExistingDocument(SchemeVariance("psa2", "srn2"))

      await(repository.removeLock(SchemeVariance("psa2", "srn2")))

      thenTheDocumentCountShouldBe(1)
    }
  }

  "removeByPSA by psaId" should {
    "Delete One" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      givenAnExistingDocument(SchemeVariance("psa2", "srn2"))

      await(repository.removeByPSA("psa1"))

      thenTheDocumentCountShouldBe(1)
    }
  }

  "removeBySRN by srn" should {
    "Delete One" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      givenAnExistingDocument(SchemeVariance("psa2", "srn2"))

      await(repository.removeBySRN("srn2"))

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

    "return true if its new and unique combination for psaId and srn"in {
      await(repository.lock(SchemeVariance("psa1", "srn1"))) shouldBe (true, true)
    }

    "return true if exiting lock"in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      await(repository.lock(SchemeVariance("psa1", "srn1"))) shouldBe (true, true)
    }

    "return false if its not unique combination for psaId and srn, existing psaId"in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      await(repository.lock(SchemeVariance("psa1", "srn2"))) shouldBe (lockNotAvailableForPsa, true)
    }

    "return false if its not unique combination for psaId and srn, existing srn"in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      await(repository.lock(SchemeVariance("psa2", "srn1"))) shouldBe (true, lockNotAvailableForSRN)
    }
  }

  private def givenAnExistingDocument(schemeVariance: SchemeVariance): Unit = {
    await(repository.collection.insert(schemeVariance))
  }

  private def thenTheDocumentCountShouldBe(count: Int): Unit = {
    await(repository.collection.count()) shouldBe count
  }

}