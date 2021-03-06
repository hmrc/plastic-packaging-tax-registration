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

package uk.gov.hmrc.plasticpackagingtaxregistration.base.unit

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.{
  SubscriptionCreateResponse,
  SubscriptionCreateSuccessfulResponse
}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscriptionStatus.SubscriptionStatusResponse
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.{
  NonRepudiationConnector,
  SubscriptionsConnector
}
import uk.gov.hmrc.plasticpackagingtaxregistration.models.nrs.{
  NonRepudiationMetadata,
  NonRepudiationSubmissionAccepted
}

import scala.concurrent.Future

trait MockConnectors extends MockitoSugar with BeforeAndAfterEach {
  self: Suite =>

  protected val mockSubscriptionsConnector: SubscriptionsConnector   = mock[SubscriptionsConnector]
  protected val mockNonRepudiationConnector: NonRepudiationConnector = mock[NonRepudiationConnector]

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSubscriptionsConnector, mockNonRepudiationConnector)
  }

  protected def mockGetSubscriptionStatusFailure(
    ex: Exception
  ): OngoingStubbing[Future[SubscriptionStatusResponse]] =
    when(mockSubscriptionsConnector.getSubscriptionStatus(any())(any()))
      .thenThrow(ex)

  protected def mockGetSubscriptionSubmitFailure(
    ex: Exception
  ): OngoingStubbing[Future[SubscriptionCreateResponse]] =
    when(mockSubscriptionsConnector.submitSubscription(any(), any())(any()))
      .thenThrow(ex)

  protected def mockGetSubscriptionStatus(
    subscription: SubscriptionStatusResponse
  ): OngoingStubbing[Future[SubscriptionStatusResponse]] =
    when(mockSubscriptionsConnector.getSubscriptionStatus(any())(any())).thenReturn(
      Future.successful(subscription)
    )

  protected def mockGetSubscriptionCreate(
    subscription: SubscriptionCreateSuccessfulResponse
  ): OngoingStubbing[Future[SubscriptionCreateResponse]] =
    when(mockSubscriptionsConnector.submitSubscription(any(), any())(any())).thenReturn(
      Future.successful(subscription)
    )

  protected def mockNonRepudiationSubmission(
    response: NonRepudiationSubmissionAccepted
  ): OngoingStubbing[Future[NonRepudiationSubmissionAccepted]] =
    when(mockNonRepudiationConnector.submitNonRepudiation(any(), any())(any())).thenReturn(
      Future.successful(response)
    )

  protected def mockNonRepudiationSubmission(
    testEncodedPayload: String,
    expectedMetadata: NonRepudiationMetadata,
    response: NonRepudiationSubmissionAccepted
  )(implicit hc: HeaderCarrier): OngoingStubbing[Future[NonRepudiationSubmissionAccepted]] =
    when(
      mockNonRepudiationConnector.submitNonRepudiation(ArgumentMatchers.eq(testEncodedPayload),
                                                       ArgumentMatchers.eq(expectedMetadata)
      )(ArgumentMatchers.eq(hc))
    ).thenReturn(Future.successful(response))

  protected def mockNonRepudiationSubmissionFailure(
    ex: Exception
  ): OngoingStubbing[Future[NonRepudiationSubmissionAccepted]] =
    when(mockNonRepudiationConnector.submitNonRepudiation(any(), any())(any()))
      .thenThrow(ex)

}
