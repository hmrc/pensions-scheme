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

package connector

import com.google.inject.Inject
import config.AppConfig

import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId, ZonedDateTime}
import java.util.{Base64, UUID}
import java.util.UUID.randomUUID

class HeaderUtils @Inject()(config: AppConfig) {
  private val maxLengthCorrelationIdIF = 36

  def integrationFrameworkHeader: Seq[(String, String)] = {
    Seq("Environment" -> config.integrationframeworkEnvironment,
      "Authorization" -> config.integrationframeworkAuthorization,
      "Content-Type" -> "application/json",
      "CorrelationId" -> getCorrelationId)
  }

  def hipHeader: Seq[(String, String)] = {
    Seq(
      "X-Transmitting-System" -> "HIP",
      "X-Originating-System" -> "MDTP",
      "X-Receipt-Date" -> xReceiptDate,
      "correlationid" -> UUID.randomUUID().toString,
      "Authorization" -> s"Basic $token"
    )
  }

  private val token: String =
    Base64
      .getEncoder
      .encodeToString(s"${config.hipClientId}:${config.hipClientSecret}".getBytes(StandardCharsets.UTF_8))
      
  private val xReceiptDate: String =
    ZonedDateTime
      .ofInstant(Instant.now(), ZoneId.of("UTC"))
      .withNano(0)
      .format(DateTimeFormatter.ISO_INSTANT)
      
  private def getCorrelationId: String = randomUUID.toString.slice(0, maxLengthCorrelationIdIF)
}
