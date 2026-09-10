/*
 * Copyright 2026 HM Revenue & Customs
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

package base.unit

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.gov.hmrc.http.HeaderCarrier
import models.eis.subscription.Subscription
import models.eis.subscription.create.{SubscriptionFailureResponseWithStatusCode, SubscriptionResponse, SubscriptionSuccessfulResponse}
import models.eis.subscriptionStatus.SubscriptionStatusResponse
import connectors.parsers.TaxEnrolmentsHttpParser
import connectors.parsers.TaxEnrolmentsHttpParser.{FailedTaxEnrolment, SuccessfulTaxEnrolment, TaxEnrolmentsResponse}
import connectors.{EisSubscriptionsConnector, EnrolmentStoreProxyConnector, HipSubscriptionsConnector, NonRepudiationConnector, TaxEnrolmentsConnector}
import models.nrs.{NonRepudiationMetadata, NonRepudiationSubmissionAccepted}
import org.scalatestplus.mockito.MockitoSugar.mock

import scala.concurrent.Future

trait MockConnectors extends BeforeAndAfterEach {
  self: Suite =>

  protected val mockEisSubscriptionsConnector: EisSubscriptionsConnector =
    mock[EisSubscriptionsConnector]
  protected val mockHipSubscriptionsConnector: HipSubscriptionsConnector =
    mock[HipSubscriptionsConnector]

  protected val mockNonRepudiationConnector: NonRepudiationConnector = mock[NonRepudiationConnector]
  protected val mockTaxEnrolmentsConnector: TaxEnrolmentsConnector   = mock[TaxEnrolmentsConnector]

  protected val mockEnrolmentStoreProxyConnector: EnrolmentStoreProxyConnector =
    mock[EnrolmentStoreProxyConnector]

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockEisSubscriptionsConnector,
          mockNonRepudiationConnector,
          mockTaxEnrolmentsConnector,
          mockEnrolmentStoreProxyConnector
    )
  }

  protected def mockGetSubscriptionStatusFailure(ex: Exception) =
    when(mockEisSubscriptionsConnector.getSubscriptionStatus(any())(using any())).thenThrow(ex)

  protected def mockGetSubscriptionFailure(ex: Exception) =
    when(mockEisSubscriptionsConnector.getSubscription(any())(using any())).thenThrow(ex)

  protected def mockHipGetSubscriptionFailure(ex: Exception) =
    when(mockHipSubscriptionsConnector.getSubscription(any())(using any())).thenThrow(ex)

  protected def mockGetSubscriptionSubmitFailure(
    ex: Exception
  ): OngoingStubbing[Future[SubscriptionResponse]] =
    when(mockEisSubscriptionsConnector.submitSubscription(any(), any())(using any()))
      .thenThrow(ex)

  protected def mockGetSubscriptionSubmitFailure(
    failedResponse: SubscriptionFailureResponseWithStatusCode
  ): OngoingStubbing[Future[SubscriptionResponse]] =
    when(mockEisSubscriptionsConnector.submitSubscription(any(), any())(using any())).thenReturn(
      Future.successful(failedResponse)
    )

  protected def mockGetSubscriptionStatus(
    subscriptionStatusResponse: SubscriptionStatusResponse
  ): OngoingStubbing[Future[Either[Int, SubscriptionStatusResponse]]] =
    when(mockEisSubscriptionsConnector.getSubscriptionStatus(any())(using any())).thenReturn(
      Future.successful(Right(subscriptionStatusResponse))
    )

  protected def mockGetSubscription(
    subscription: Subscription
  ): OngoingStubbing[Future[Either[Int, Subscription]]] =
    when(mockEisSubscriptionsConnector.getSubscription(any())(using any())).thenReturn(
      Future.successful(Right(subscription))
    )

  protected def mockGetSubscriptionCreate(
    subscription: SubscriptionSuccessfulResponse
  ): OngoingStubbing[Future[SubscriptionResponse]] =
    when(mockEisSubscriptionsConnector.submitSubscription(any(), any())(using any())).thenReturn(
      Future.successful(subscription)
    )

  protected def mockSubscriptionUpdate(
    subscription: SubscriptionSuccessfulResponse
  ): OngoingStubbing[Future[SubscriptionResponse]] =
    when(mockEisSubscriptionsConnector.updateSubscription(any(), any())(using any())).thenReturn(
      Future.successful(subscription)
    )

  protected def mockSubscriptionUpdateFailure(
    failedResponse: SubscriptionFailureResponseWithStatusCode
  ): OngoingStubbing[Future[SubscriptionResponse]] =
    when(mockEisSubscriptionsConnector.updateSubscription(any(), any())(using any())).thenReturn(
      Future.successful(failedResponse)
    )

  protected def mockSubscriptionUpdateFailure(
    ex: Exception
  ): OngoingStubbing[Future[SubscriptionResponse]] =
    when(mockEisSubscriptionsConnector.updateSubscription(any(), any())(using any())).thenThrow(ex)

  protected def mockNonRepudiationSubmission(
    response: NonRepudiationSubmissionAccepted
  ): OngoingStubbing[Future[NonRepudiationSubmissionAccepted]] =
    when(mockNonRepudiationConnector.submitNonRepudiation(any(), any())(using any())).thenReturn(
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
      )(using ArgumentMatchers.eq(hc))
    ).thenReturn(Future.successful(response))

  protected def mockNonRepudiationSubmissionFailure(
    ex: Exception
  ): OngoingStubbing[Future[NonRepudiationSubmissionAccepted]] =
    when(mockNonRepudiationConnector.submitNonRepudiation(any(), any())(using any()))
      .thenThrow(ex)

  protected def mockEnrolmentSuccess(): OngoingStubbing[Future[TaxEnrolmentsResponse]] =
    when(mockTaxEnrolmentsConnector.submitEnrolment(any(), any(), any())(using any())).thenReturn(
      Future.successful(Right(SuccessfulTaxEnrolment))
    )

  protected def mockEnrolmentFailure()
    : OngoingStubbing[Future[TaxEnrolmentsHttpParser.TaxEnrolmentsResponse]] =
    when(mockTaxEnrolmentsConnector.submitEnrolment(any(), any(), any())(using any())).thenReturn(
      Future.successful(Left(FailedTaxEnrolment(1)))
    )

  protected def mockEnrolmentFailureException()
    : OngoingStubbing[Future[TaxEnrolmentsHttpParser.TaxEnrolmentsResponse]] =
    when(mockTaxEnrolmentsConnector.submitEnrolment(any(), any(), any())(using any())).thenReturn(
      Future.failed(new IllegalStateException("BANG!"))
    )

}
