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

package models

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.PathBindable

sealed trait FeatureToggle {
  def name: FeatureToggleName

  def isEnabled: Boolean

  def isDisabled: Boolean = !isEnabled
}

sealed trait FeatureToggleName {
  def asString: String
}

object FeatureToggleName {


  case object DummyToggle extends FeatureToggleName {
    val asString = "dummy"
  }

  case object SchemeRegistration extends FeatureToggleName {
    val asString = "scheme-registration"
  }

  val toggles = Seq(DummyToggle, SchemeRegistration)

  implicit val reads: Reads[Option[FeatureToggleName]] = Reads {
    case JsString(SchemeRegistration.asString) => JsSuccess(Some(SchemeRegistration))
    case JsString(DummyToggle.asString) => JsSuccess(Some(DummyToggle))
    case _ => JsSuccess(None)
  }

  implicit val writes: Writes[FeatureToggleName] =
    Writes(value => JsString(value.asString))

  implicit def pathBindable: PathBindable[FeatureToggleName] =
    new PathBindable[FeatureToggleName] {

      override def bind(key: String, value: String): Either[String, FeatureToggleName] = {
        JsString(value).validate[Option[FeatureToggleName]] match {
          case JsSuccess(Some(name), _) => Right(name)
          case _ => Left("invalid feature toggle name")
        }
      }

      override def unbind(key: String, value: FeatureToggleName): String =
        value.asString
    }

}

object FeatureToggle {
  val InvalidToggleName: String = "Invalid-Toggle"

  case class Enabled(name: FeatureToggleName) extends FeatureToggle {
    val isEnabled = true
  }

  case class Disabled(name: FeatureToggleName) extends FeatureToggle {
    val isEnabled = false
  }

  def apply(name: FeatureToggleName, enabled: Boolean): FeatureToggle =
    if (enabled) Enabled(name) else Disabled(name)

  implicit val reads: Reads[FeatureToggle] = {
    case object InvalidToggle extends FeatureToggleName {
      val asString: String = InvalidToggleName
    }

    (__ \ "isEnabled").read[Boolean].flatMap {
      case true =>
        (__ \ "name").read[Option[FeatureToggleName]].map {
          case None => Disabled(InvalidToggle)
          case Some(name) => Enabled(name)
        }
      case false =>
        (__ \ "name").read[Option[FeatureToggleName]].map {
          case None => Disabled(InvalidToggle)
          case Some(name) => Disabled(name)
        }
    }
  }

  implicit val writes: Writes[FeatureToggle] =
    ((__ \ "name").write[FeatureToggleName] and
      (__ \ "isEnabled").write[Boolean]).apply(ft => (ft.name, ft.isEnabled))
}

object FeatureToggleNameTest {
  case object InvalidToggle1 extends FeatureToggleName {
    val asString = "invalid 1"
  }

  case object InvalidToggle2 extends FeatureToggleName {
    val asString = "invalid 2"
  }
}