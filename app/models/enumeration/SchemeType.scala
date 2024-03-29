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

package models.enumeration

object SchemeType extends Enumeration {

  sealed case class TypeValue(name: String, value: String) extends Val(name)

  val single = TypeValue("single", "A single trust under which all of the assets are held for the benefit of all members of the scheme")
  val group = TypeValue("group", "A group life/death in service scheme")
  val corp = TypeValue("corp", "A body corporate")
  val other = TypeValue("other", "Other")

  def valueWithName(name: String): String = {
    super.withName(name).asInstanceOf[TypeValue].value
  }

  def nameWithValue(value: String): String =
    Seq(single, group, corp, other).find(_.value == value).getOrElse(throw new IllegalArgumentException("Unknown value")).name
}
