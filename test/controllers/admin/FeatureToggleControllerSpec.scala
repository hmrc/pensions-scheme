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

package controllers.admin

import base.SpecBase
import models.FeatureToggle.Enabled
import models.FeatureToggleName.DUMMY_TOGGLE
import models.OperationSucceeded
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.{JsBoolean, Json}
import play.api.mvc.ControllerComponents
import play.api.test.Helpers._
import repositories.AdminDataRepository
import service.FeatureToggleService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FeatureToggleControllerSpec
  extends SpecBase
    with MockitoSugar
    with BeforeAndAfterEach {

  private val mockAdminDataRepository = mock[AdminDataRepository]

  private val mockFeatureToggleService = mock[FeatureToggleService]

  def controllerComponents: ControllerComponents = injector.instanceOf[ControllerComponents]

  override def beforeEach(): Unit = {
    reset(mockAdminDataRepository, mockFeatureToggleService)
    when(mockAdminDataRepository.getFeatureToggles)
      .thenReturn(Future.successful(Seq(Enabled(DUMMY_TOGGLE))))
    when(mockFeatureToggleService.getAll)
      .thenReturn(Future.successful(Seq(Enabled(DUMMY_TOGGLE))))
  }

  "FeatureToggleController.getAll" must {
    "return OK and the feature toggles when they exist" in {

      val controller = new FeatureToggleController(controllerComponents, mockFeatureToggleService)

      val result = controller.getAll()(fakeRequest)

      status(result) mustBe OK
    }
  }

  "FeatureToggleController.get" must {
    "get the feature toggle value and return OK" in {
      when(mockAdminDataRepository.setFeatureToggles(any()))
        .thenReturn(Future.successful(true))

      when(mockFeatureToggleService.get(any()))
        .thenReturn(Future.successful(Enabled(DUMMY_TOGGLE)))

      val controller = new FeatureToggleController(controllerComponents, mockFeatureToggleService)

      val result = controller.get(DUMMY_TOGGLE)(fakeRequest)

      status(result) mustBe OK

      verify(mockFeatureToggleService, times(1))
        .get(name = DUMMY_TOGGLE)
    }
  }

  "FeatureToggleController.put" must {
    "set the feature toggles and return NO_CONTENT" in {
      when(mockAdminDataRepository.setFeatureToggles(any()))
        .thenReturn(Future.successful(true))

      when(mockFeatureToggleService.set(any(), any()))
        .thenReturn(Future.successful(OperationSucceeded))

      val controller = new FeatureToggleController(controllerComponents, mockFeatureToggleService)

      val result = controller.put(DUMMY_TOGGLE)(fakeRequest.withJsonBody(JsBoolean(true)))

      status(result) mustBe NO_CONTENT

      verify(mockFeatureToggleService, times(1))
        .set(toggleName = DUMMY_TOGGLE, enabled = true)
    }

    "not set the feature toggles and return BAD_REQUEST" in {
      val controller = new FeatureToggleController(controllerComponents, mockFeatureToggleService)

      val result = controller.put(DUMMY_TOGGLE)(fakeRequest.withJsonBody(Json.obj("blah" -> "blah")))

      status(result) mustBe BAD_REQUEST

      verify(mockFeatureToggleService, times(0))
        .set(toggleName = DUMMY_TOGGLE, enabled = true)
    }
  }
}
