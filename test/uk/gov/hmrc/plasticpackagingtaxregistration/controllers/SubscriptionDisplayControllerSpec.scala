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
import play.api.mvc.Result
import play.api.test.Helpers.{route, status, _}
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import uk.gov.hmrc.plasticpackagingtaxregistration.base.unit.ControllerSpec
import uk.gov.hmrc.plasticpackagingtaxregistration.builders.{
  RegistrationBuilder,
  RegistrationRequestBuilder
}

import scala.concurrent.Future

class SubscriptionDisplayControllerSpec
    extends ControllerSpec with RegistrationBuilder with RegistrationRequestBuilder {

  override def beforeEach(): Unit =
    super.beforeEach()

  "Get subscription" should {
    "return expected details" in {
      withAuthorizedUser()
      mockGetSubscription(ukLimitedCompanySubscription)

      val result: Future[Result] = route(app, subscriptionResponse_HttpGet).get

      status(result) must be(OK)
//      contentAsJson(result) mustBe toJson(subscriptionStatusResponse)
      verify(mockSubscriptionsConnector).getSubscription(ArgumentMatchers.eq(pptReference))(any())
    }

    "return 401" when {
      "user not authorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result: Future[Result] = route(app, subscriptionResponse_HttpGet).get

        status(result) must be(UNAUTHORIZED)
        verifyNoInteractions(mockSubscriptionsConnector)
      }
    }

    "pass through exceptions" when {
      "an exception occurs during the subscription call" in {
        withAuthorizedUser()
        mockGetSubscriptionFailure(new IllegalStateException("BANG!"))

        val result: Future[Result] = route(app, subscriptionResponse_HttpGet).get

        intercept[IllegalStateException] {
          status(result)
        }
      }
    }
  }

}
