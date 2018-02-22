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

import com.google.inject.{ImplementedBy, Inject}
import config.AppConfig
import play.api.libs.json.JsValue
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import play.Logger
import java.util.UUID.randomUUID
import scala.concurrent.{ExecutionContext, Future}

class RegistrationConnectorImpl @Inject()(http: HttpClient, config: AppConfig) extends RegistrationConnector {

  val desHeader = Seq("Environment" -> config.desEnvironment, "Authorization" -> config.authorization,
    "Content-Type" -> "application/json")

  implicit val hc = HeaderCarrier(extraHeaders = desHeader)

  override def registerWithId(idType: String, idNumber: String, registerData: JsValue)(implicit hc: HeaderCarrier,
                                                                                       ec: ExecutionContext): Future[HttpResponse] = {
    val registerWithIdUrl = config.registerWithIdUrl.format(idType, idNumber)

    http.POST(registerWithIdUrl, registerData)
  }
}

@ImplementedBy(classOf[RegistrationConnectorImpl])
trait RegistrationConnector {
  def registerWithId(idType: String, idNumber: String, registerData: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse]
}

