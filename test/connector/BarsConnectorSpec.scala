/*
 * Copyright 2018 HM Revenue & Customs
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

package connector

import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec

class BarsConnectorSpec extends UnitSpec with MockitoSugar {

  "BarsConnector" should {

    "return invalid account when sort code is invalid" in {

    }

    "return invalid account when sort code is valid but not found on EISCD" in {

    }

    "return invalid account when sort code is valid but check on EISCD errors" in {

    }

    "return valid account when sort code is valid and found on EISCD" in {

    }
  }
}
