/*
 * Copyright 2021 HM Revenue & Customs
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

import org.scalatest.AsyncWordSpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import reactivemongo.api.indexes.Index
import reactivemongo.play.json.collection.JSONCollection

trait MongoUnitSpec extends AsyncWordSpec {

  protected implicit val ordering: Ordering[Index] = Ordering.by { i: Index => i.name }

  protected def collection: JSONCollection

  protected def getIndexes: List[Index] = {
    await(collection.indexesManager.list())
  }

  protected def getIndex(name: String): Option[Index] = {
    getIndexes.find(_.name.contains(name))
  }

}
