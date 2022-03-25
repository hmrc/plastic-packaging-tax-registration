/*
 * Copyright 2022 HM Revenue & Customs
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

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.Json.toJson
import play.api.test.Helpers.{await, route, status, writeableOf_AnyContentAsJson, OK}
import uk.gov.hmrc.plasticpackagingtaxregistration.base.unit.ControllerSpec
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.Subscription
import uk.gov.hmrc.plasticpackagingtaxregistration.models.DeregistrationReason

import java.time.{LocalDate, ZoneOffset}
import scala.concurrent.Future

class DeregistrationControllerSpec extends ControllerSpec {

  "Deregister" should {
    "obtain and update the PPT subscription and make a subscription variation (update) EIS call" in {
      withAuthorizedUser()
      mockGetSubscription(ukLimitedCompanySubscription)

      ukLimitedCompanySubscription.changeOfCircumstanceDetails mustBe None

      val subscriptionCaptor: ArgumentCaptor[Subscription] =
        ArgumentCaptor.forClass(classOf[Subscription])
      when(
        mockSubscriptionsConnector.updateSubscription(any(), subscriptionCaptor.capture())(any())
      ).thenReturn(Future.successful(subscriptionSuccessfulResponse))

      val result =
        await(
          route(
            app,
            subscriptionDelete_HttpDelete.withJsonBody(toJson(DeregistrationReason.CeasedTrading))
          ).get
        )

      status(Future.successful(result)) must be(OK)

      val updatedSubscription = subscriptionCaptor.getValue

      val changeOfCircumstanceDetails = updatedSubscription.changeOfCircumstanceDetails.get
      changeOfCircumstanceDetails.changeOfCircumstance mustBe "Deregistration"

      val deregistrationDetails = changeOfCircumstanceDetails.deregistrationDetails.get
      deregistrationDetails.deregistrationReason mustBe DeregistrationReason.CeasedTrading.toString
      deregistrationDetails.deregistrationDate mustBe LocalDate.now(ZoneOffset.UTC).toString
      deregistrationDetails.deregistrationDeclarationBox1 mustBe true
    }

    "throw an exception" when {
      "the attempt to get the existing subscription fails" in {
        withAuthorizedUser()
        mockGetSubscriptionFailure(new RuntimeException("Get subscription failed"))

        intercept[RuntimeException] {
          await(
            route(
              app,
              subscriptionDelete_HttpDelete.withJsonBody(toJson(DeregistrationReason.CeasedTrading))
            ).get
          )
        }
      }
      "the attempt to update the subscription fails" in {
        withAuthorizedUser()
        mockGetSubscription(ukLimitedCompanySubscription)
        mockSubscriptionUpdateFailure(new RuntimeException("Update subscription failed"))

        intercept[RuntimeException] {
          await(
            route(
              app,
              subscriptionDelete_HttpDelete.withJsonBody(toJson(DeregistrationReason.CeasedTrading))
            ).get
          )
        }
      }
    }
  }
}
