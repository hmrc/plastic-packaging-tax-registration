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

package uk.gov.hmrc.plasticpackagingtaxregistration.controllers

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, verifyNoInteractions}
import play.api.libs.json.Json.toJson
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsJson, route, status, _}
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import uk.gov.hmrc.plasticpackagingtaxregistration.base.unit.ControllerSpec

import scala.concurrent.Future

class SubscriptionControllerSpec extends ControllerSpec {

  override def beforeEach(): Unit =
    super.beforeEach()

  "GET /subscriptions/status/:id " should {
    "return 200" when {
      "request is valid" in {
        withAuthorizedUser()
        mockGetSubscriptionStatus(subscriptionStatusResponse)

        val result: Future[Result] = route(app, subscriptionStatusResponse_HttpGet).get

        status(result) must be(OK)
        contentAsJson(result) mustBe toJson(subscriptionStatusResponse)
        verify(mockSubscriptionsConnector).getSubscriptionStatus(ArgumentMatchers.eq(safeNumber))(
          any()
        )
      }
    }

    "return 401" when {
      "unauthorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result: Future[Result] = route(app, subscriptionStatusResponse_HttpGet).get

        status(result) must be(UNAUTHORIZED)
        verifyNoInteractions(mockSubscriptionsConnector)
      }
    }

    "return 500" when {
      "a failure has occurred at EIS" in {
        withAuthorizedUser()
        mockGetSubscriptionStatusFailure(new RuntimeException("error"))

        val result: Future[Result] = route(app, subscriptionStatusResponse_HttpGet).get

        intercept[RuntimeException](status(result))
      }
    }
  }

}
