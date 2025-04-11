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

package models.enumeration

import play.api.mvc.PathBindable

sealed trait SchemeJourneyType

object SchemeJourneyType:
  private case object RAC_DAC_SCHEME extends SchemeJourneyType
  private case object NON_RAC_DAC_SCHEME extends SchemeJourneyType

  implicit def pathBindable: PathBindable[SchemeJourneyType] =
    new PathBindable[SchemeJourneyType] {
      override def bind(key: String, value: String): Either[String, SchemeJourneyType] =
        value match {
          case "rac-dac" => Right(SchemeJourneyType.RAC_DAC_SCHEME)
          case "non-rac-dac" => Right(SchemeJourneyType.NON_RAC_DAC_SCHEME)
          case _ => Left("invalid SchemeJourneyType")
        }

      override def unbind(key: String, value: SchemeJourneyType): String =
        value match {
          case SchemeJourneyType.RAC_DAC_SCHEME => "rac-dac"
          case SchemeJourneyType.NON_RAC_DAC_SCHEME => "non-rac-dac"
        }
    }
