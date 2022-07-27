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

import config.AppConfig
import models._
import org.mockito.MockitoSugar
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{Assertion, BeforeAndAfterAll, BeforeAndAfterEach}

/*
class LockMongoRepositoryTest
  extends AnyWordSpec
    with MongoUnitSpec
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with MongoSpecSupport
    with Eventually
    with MockitoSugar
    with Matchers {
*/

  class LockMongoRepositoryTest{
  self =>

  /*private val provider: MongoDbProvider = new MongoDbProvider {
    override val mongo: () => DB = self.mongo
  }

  private val appConfig = mock[AppConfig]
  when(appConfig.defaultDataExpireAfterDays).thenReturn(0)

  private def repository = new LockMongoRepository(appConfig, provider)

  override protected def collection: JSONCollection = repository.collection

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(repository.drop)
    await(repository.ensureIndexes)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(repository.drop)
  }

  "releaseLock" must {
    "Delete One" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      givenAnExistingDocument(SchemeVariance("psa2", "srn2"))

      await(repository.releaseLock(SchemeVariance("psa2", "srn2")))

      thenTheDocumentCountShouldBeOne
    }
  }

  "releaseLockByPSA by psaId" must {
    "Delete One" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      givenAnExistingDocument(SchemeVariance("psa2", "srn2"))

      await(repository.releaseLockByPSA("psa1"))

      thenTheDocumentCountShouldBeOne
    }
  }

  "releaseLockBySRN by srn" must {
    "Delete One" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      givenAnExistingDocument(SchemeVariance("psa2", "srn2"))

      await(repository.releaseLockBySRN("srn2"))

      thenTheDocumentCountShouldBeOne
    }
  }

  "getExistingLockByPSA" must {
    "Retrieve None" in {
      await(repository.getExistingLockByPSA("some id")) mustBe None
    }

    "Retrieve One" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      givenAnExistingDocument(SchemeVariance("psa2", "srn2"))

      await(repository.getExistingLockByPSA("psa1")) mustBe Some(SchemeVariance("psa1", "srn1"))
    }
  }

  "getExistingLockBySRN" must {
    "Retrieve None" in {
      await(repository.getExistingLockBySRN("some id")) mustBe None
    }

    "Retrieve One" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      givenAnExistingDocument(SchemeVariance("psa2", "srn2"))

      await(repository.getExistingLockBySRN("srn2")) mustBe Some(SchemeVariance("psa2", "srn2"))
    }
  }

  "list" must {
    "Retrieve None" in {
      await(repository.list) mustBe List()
    }

    "Retrieve all" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      givenAnExistingDocument(SchemeVariance("psa2", "srn2"))

      await(repository.list) mustBe Seq(SchemeVariance("psa1", "srn1"), SchemeVariance("psa2", "srn2"))
    }
  }

  "lock" should {

    "return locked if its new and unique combination for psaId and srn" in {
      await(repository.lock(SchemeVariance("psa1", "srn1"))) mustBe VarianceLock
    }

    "return locked if exiting lock" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      await(repository.lock(SchemeVariance("psa1", "srn1"))) mustBe VarianceLock
    }

    "return lockNotAvailableForPsa if its not unique combination for psaId and srn, existing psaId" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      await(repository.lock(SchemeVariance("psa1", "srn2"))) mustBe PsaLock
    }

    "return lockNotAvailableForSRN if its not unique combination for psaId and srn, existing srn" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      await(repository.lock(SchemeVariance("psa2", "srn1"))) mustBe SchemeLock
    }

    "return both locked if its not unique combination for existing psaId2 and srn1" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      givenAnExistingDocument(SchemeVariance("psa2", "srn2"))
      await(repository.lock(SchemeVariance("psa2", "srn1"))) mustBe BothLock
    }

    "return both locked if its not unique combination for existing psaId1 and srn2" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      givenAnExistingDocument(SchemeVariance("psa2", "srn2"))
      await(repository.lock(SchemeVariance("psa1", "srn2"))) mustBe BothLock
    }

    "create ttl on expireAt field" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      val index = getIndex("dataExpiry").get
      val expected = Index(key = Seq("expireAt" -> Ascending),
        name = Some("dataExpiry"),
        options = BSONDocument("expireAfterSeconds" -> 0)).copy(version = index.version)

      index mustBe expected
    }
  }

  "isLockByPsaIdOrSchemeId" should {

    "return locked if its new and unique combination for psaId and srn" in {
      await(repository.isLockByPsaIdOrSchemeId("psa1", "srn1")) mustBe None
    }

    "return locked if exiting lock" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      await(repository.isLockByPsaIdOrSchemeId("psa1", "srn1")) mustBe Some(VarianceLock)
    }

    "return lockNotAvailableForPsa if its not unique combination for psaId and srn, existing psaId" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      await(repository.isLockByPsaIdOrSchemeId("psa1", "srn2")) mustBe Some(PsaLock)
    }

    "return lockNotAvailableForSRN if its not unique combination for psaId and srn, existing srn" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      await(repository.isLockByPsaIdOrSchemeId("psa2", "srn1")) mustBe Some(SchemeLock)
    }

    "return both locked if its not unique combination for existing psaId2 and srn1" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      givenAnExistingDocument(SchemeVariance("psa2", "srn2"))
      await(repository.isLockByPsaIdOrSchemeId("psa2", "srn1")) mustBe Some(BothLock)
    }

    "return both locked if its not unique combination for existing psaId1 and srn2" in {
      givenAnExistingDocument(SchemeVariance("psa1", "srn1"))
      givenAnExistingDocument(SchemeVariance("psa2", "srn2"))
      await(repository.isLockByPsaIdOrSchemeId("psa1", "srn2")) mustBe Some(BothLock)
    }
  }

  private def givenAnExistingDocument(schemeVariance: SchemeVariance): Unit = {
    await(repository.collection.insert(ordered = false).one(schemeVariance))
  }

  private def thenTheDocumentCountShouldBeOne: Assertion = {
    await(repository.collection.count(None, None, skip = 0, None, ReadConcern.Local)) mustBe 1
  }*/

}