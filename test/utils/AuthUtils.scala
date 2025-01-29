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

package utils

import audit.AuditServiceSpec.mock
import com.google.inject.Inject
import controllers.actions.{PsaAuthRequest, PsaPspAuthRequest, PsaPspSchemeAuthAction, PsaSchemeAuthAction}
import models.SchemeReferenceNumber
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import play.api.mvc.{ActionFunction, Result}
import service.SchemeService
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.domain.{PsaId, PspId}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

object AuthUtils {
  val psaId = "A2123456"
  val pspId = "21000005"
  val srn: SchemeReferenceNumber = SchemeReferenceNumber("S2400000006")
  val externalId = "externalId"

  def failedAuthStub(mockAuthConnector: AuthConnector): OngoingStubbing[Future[Unit]] =
    when(mockAuthConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed(InsufficientEnrolments())

  def authStub(mockAuthConnector: AuthConnector): OngoingStubbing[Future[Enrolments ~ Option[String]]] =
    when(mockAuthConnector.authorise[Enrolments ~ Option[String]](any(), any())(any(), any())) thenReturn Future.successful(AuthUtils.authResponse)
  val authResponse = new ~(
    Enrolments(
      Set(
        new Enrolment("HMRC-PODS-ORG", Seq(EnrolmentIdentifier("PsaId", psaId)), "Activated")
      )
    ), Some(externalId)
  )

  def authStubPsp(mockAuthConnector: AuthConnector): OngoingStubbing[Future[Enrolments ~ Option[String]]] =
    when(mockAuthConnector.authorise[Enrolments ~ Option[String]](any(), any())(any(), any())) thenReturn
      Future.successful(AuthUtils.authResponsePsp)
  val authResponsePsp = new ~(
    Enrolments(
      Set(
        new Enrolment("HMRC-PODSPP-ORG", Seq(EnrolmentIdentifier("PspId", pspId)), "Activated")
      )
    ), Some(externalId)
  )

  def noEnrolmentAuthStub(mockAuthConnector: AuthConnector): OngoingStubbing[Future[Option[String]]] =
    when(mockAuthConnector.authorise[Option[String]](any(), any())(any(), any())) thenReturn
      Future.successful(AuthUtils.noEnrolmentAuthResponse)

  val noEnrolmentAuthResponse: Option[String] = Some(externalId)

  class FakeFailingAuthConnector @Inject()(exceptionToReturn: Throwable) extends AuthConnector {
    val serviceUrl: String = ""

    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      Future.failed(exceptionToReturn)
  }

  case class FakePsaSchemeAuthAction() extends PsaSchemeAuthAction(mock[SchemeService]) {
    override def apply(srn: SchemeReferenceNumber): ActionFunction[PsaAuthRequest, PsaAuthRequest] = new ActionFunction[PsaAuthRequest, PsaAuthRequest] {
      override def invokeBlock[A](request: PsaAuthRequest[A], block: PsaAuthRequest[A] => Future[Result]): Future[Result] =
        block(PsaAuthRequest(request, PsaId(psaId), externalId))

      override protected def executionContext: ExecutionContext = global
    }
  }

  case class FakePsaPspSchemeAuthAction() extends PsaPspSchemeAuthAction(mock[SchemeService]) {
    override def apply(srn: SchemeReferenceNumber, loggedInAsPsa: Boolean): ActionFunction[PsaPspAuthRequest, PsaPspAuthRequest] = new ActionFunction[PsaPspAuthRequest, PsaPspAuthRequest] {
      override def invokeBlock[A](request: PsaPspAuthRequest[A], block: PsaPspAuthRequest[A] => Future[Result]): Future[Result] =
        block(PsaPspAuthRequest(request, Some(PsaId(psaId)), Some(PspId(pspId)), externalId))

      override protected def executionContext: ExecutionContext = global
    }
  }
}
