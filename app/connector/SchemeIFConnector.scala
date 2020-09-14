/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[SchemeIFConnectorImpl])
trait SchemeIFConnector {

  def listOfSchemes(idType: String, idValue: String)(implicit
                                   headerCarrier: HeaderCarrier,
                                   ec: ExecutionContext,
                                   request: RequestHeader): Future[HttpResponse]

}

class SchemeIFConnectorImpl @Inject()(
                                     http: HttpClient,
                                     config: AppConfig,
                                     headerUtils: HeaderUtils
                                   ) extends SchemeIFConnector with HttpErrorFunctions {

  override def listOfSchemes(idType: String, idValue: String)(implicit
                                                     headerCarrier: HeaderCarrier,
                                                     ec: ExecutionContext,
                                                     request: RequestHeader): Future[HttpResponse] = {
    val listOfSchemesUrl = config.listOfSchemesIFUrl.format(idType, idValue)

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders =
      headerUtils.integrationFrameworkHeader(implicitly[HeaderCarrier](headerCarrier)))

    http.GET[HttpResponse](listOfSchemesUrl)(implicitly[HttpReads[HttpResponse]], implicitly[HeaderCarrier](hc),
      implicitly[ExecutionContext])

  }
}
