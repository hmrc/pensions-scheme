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

package models

import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, JsString, Json}

class LockSpec extends AnyWordSpec with OptionValues with Matchers {

  "Lock" should {
    "create a Lock from correctly formated json" when {
      "Lock type is VarianceLock" in {
        Json.fromJson[Lock](JsString("SuccessfulVarianceLock")).asOpt mustBe Some(VarianceLock)
      }

      "Lock type is PsaLock" in {
        Json.fromJson[Lock](JsString("PsaHasLockedAnotherScheme")).asOpt mustBe Some(PsaLock)
      }

      "Lock type is SchemeLock" in {
        Json.fromJson[Lock](JsString("AnotherPsaHasLockedScheme")).asOpt mustBe Some(SchemeLock)
      }

      "Lock type is BothLock" in {
        Json.fromJson[Lock](JsString("PsaAndSchemeHasAlreadyLocked")).asOpt mustBe Some(BothLock)
      }
    }

    "raise an error if Json incorrect" in {
      Json.fromJson[Lock](JsString("RubbishLock")) mustBe JsError("cannot parse it")
    }

    "create create correctly formated json" when {
      "Lock type is VarianceLock" in {
        Json.toJson[Lock](VarianceLock) mustBe JsString("SuccessfulVarianceLock")
      }

      "Lock type is PsaLock" in {
        Json.toJson[Lock](PsaLock) mustBe JsString("PsaHasLockedAnotherScheme")
      }

      "Lock type is SchemeLock" in {
        Json.toJson[Lock](SchemeLock) mustBe JsString("AnotherPsaHasLockedScheme")
      }

      "Lock type is BothLock" in {
        Json.toJson[Lock](BothLock) mustBe JsString("PsaAndSchemeHasAlreadyLocked")
      }
    }
  }

}
