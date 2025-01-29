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

package controllers.actions

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{FORBIDDEN, OK}
import play.api.mvc.AnyContent
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import service.SchemeService
import uk.gov.hmrc.domain.{PsaId, PspId}
import uk.gov.hmrc.http.HttpException
import utils.AuthUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PsaPspSchemeActionSpec extends PlaySpec with MockitoSugar with BeforeAndAfterAll with BeforeAndAfterEach {

  private val mockSchemeService = mock[SchemeService]
  override def beforeAll(): Unit = {
    super.beforeAll()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSchemeService)
  }

  private val psaAuthRequest = PsaPspAuthRequest[AnyContent](FakeRequest("", ""), Some(PsaId(AuthUtils.psaId)), None,  AuthUtils.externalId)
  private val pspAuthRequest = PsaPspAuthRequest[AnyContent](FakeRequest("", ""), None, Some(PspId(AuthUtils.pspId)),  AuthUtils.externalId)

  private val srn = AuthUtils.srn
  private def getResult(loggedInAsPsa: Boolean, req: PsaPspAuthRequest[AnyContent]) = {
    new PsaPspSchemeAuthAction(mockSchemeService)
      .apply(srn, loggedInAsPsa)
      .invokeBlock(req, { _: PsaPspAuthRequest[AnyContent] => Future.successful(Ok("success")) })
  }

  private def mockCheckForAssociationPsa = {
    when(mockSchemeService.isAssociated(ArgumentMatchers.eq(srn), ArgumentMatchers.eq(Left(PsaId(AuthUtils.psaId))))(ArgumentMatchers.any(), ArgumentMatchers.any()))
  }

  private def mockCheckForAssociationPsp = {
    when(mockSchemeService.isAssociated(ArgumentMatchers.eq(srn), ArgumentMatchers.eq(Right(PspId(AuthUtils.pspId))))(ArgumentMatchers.any(), ArgumentMatchers.any()))
  }

  "PsaPspSchemeActionSpec" must {
    "PSA" must {
      "return success response if pension scheme is associated with srn" in {
        mockCheckForAssociationPsa.thenReturn(Future.successful(Right(true)))
        val result = getResult(loggedInAsPsa = true, psaAuthRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe "success"
      }

      "return Forbidden if pension scheme is not associated with srn" in {
        mockCheckForAssociationPsa.thenReturn(Future.successful(Right(false)))
        status(getResult(loggedInAsPsa = true, psaAuthRequest)) mustBe FORBIDDEN
      }

      "return Forbidden if no id found" in {
        status(getResult(loggedInAsPsa = true, pspAuthRequest)) mustBe FORBIDDEN
      }

      "return recover from error if association call fails" in {
        mockCheckForAssociationPsa.thenReturn(Future.successful(Left(new HttpException("failed", 500))))
        getResult(loggedInAsPsa = true, psaAuthRequest).failed.map { _ mustBe new Exception("failed") }
      }
    }

    "PSP" must {
      "return success response if pension scheme is associated with srn" in {
        mockCheckForAssociationPsp.thenReturn(Future.successful(Right(true)))
        val result = getResult(loggedInAsPsa = false, pspAuthRequest)
        status(result) mustBe OK
        contentAsString(result) mustBe "success"
      }

      "return Forbidden if pension scheme is not associated with srn" in {
        mockCheckForAssociationPsp.thenReturn(Future.successful(Right(false)))
        status(getResult(loggedInAsPsa = false, pspAuthRequest)) mustBe FORBIDDEN
      }

      "return Forbidden if no id found" in {
        status(getResult(loggedInAsPsa = false, psaAuthRequest)) mustBe FORBIDDEN
      }

      "return recover from error if association call fails" in {
        mockCheckForAssociationPsp.thenReturn(Future.successful(Left(new HttpException("failed", 500))))
        getResult(loggedInAsPsa = false, pspAuthRequest).failed.map { _ mustBe new Exception("failed") }
      }
    }

  }
}
