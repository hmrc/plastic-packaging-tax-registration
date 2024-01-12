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

package base.unit

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{reset, when}
import org.mockito.stubbing.ScalaOngoingStubbing
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.gov.hmrc.http.HeaderCarrier
import models.eis.subscription.Subscription
import models.eis.subscription.create.{
  SubscriptionFailureResponseWithStatusCode,
  SubscriptionResponse,
  SubscriptionSuccessfulResponse
}
import models.eis.subscriptionStatus.SubscriptionStatusResponse
import connectors.parsers.TaxEnrolmentsHttpParser
import connectors.parsers.TaxEnrolmentsHttpParser.{
  FailedTaxEnrolment,
  SuccessfulTaxEnrolment,
  TaxEnrolmentsResponse
}
import connectors.{
  EnrolmentStoreProxyConnector,
  NonRepudiationConnector,
  SubscriptionsConnector,
  TaxEnrolmentsConnector
}
import models.nrs.{NonRepudiationMetadata, NonRepudiationSubmissionAccepted}
import org.scalatestplus.mockito.MockitoSugar.mock

import scala.concurrent.Future

trait MockConnectors extends BeforeAndAfterEach {
  self: Suite =>

  protected val mockSubscriptionsConnector: SubscriptionsConnector   = mock[SubscriptionsConnector]
  protected val mockNonRepudiationConnector: NonRepudiationConnector = mock[NonRepudiationConnector]
  protected val mockTaxEnrolmentsConnector: TaxEnrolmentsConnector   = mock[TaxEnrolmentsConnector]

  protected val mockEnrolmentStoreProxyConnector: EnrolmentStoreProxyConnector =
    mock[EnrolmentStoreProxyConnector]

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSubscriptionsConnector,
          mockNonRepudiationConnector,
          mockTaxEnrolmentsConnector,
          mockEnrolmentStoreProxyConnector
    )
  }

  protected def mockGetSubscriptionStatusFailure(ex: Exception) =
    when(mockSubscriptionsConnector.getSubscriptionStatus(any())(any())).thenThrow(ex)

  protected def mockGetSubscriptionFailure(ex: Exception) =
    when(mockSubscriptionsConnector.getSubscription(any())(any())).thenThrow(ex)

  protected def mockGetSubscriptionSubmitFailure(
    ex: Exception
  ): ScalaOngoingStubbing[Future[SubscriptionResponse]] =
    when(mockSubscriptionsConnector.submitSubscription(any(), any())(any()))
      .thenThrow(ex)

  protected def mockGetSubscriptionSubmitFailure(
    failedResponse: SubscriptionFailureResponseWithStatusCode
  ): ScalaOngoingStubbing[Future[SubscriptionResponse]] =
    when(mockSubscriptionsConnector.submitSubscription(any(), any())(any())).thenReturn(
      Future.successful(failedResponse)
    )

  protected def mockGetSubscriptionStatus(
    subscriptionStatusResponse: SubscriptionStatusResponse
  ): ScalaOngoingStubbing[Future[Either[Int, SubscriptionStatusResponse]]] =
    when(mockSubscriptionsConnector.getSubscriptionStatus(any())(any())).thenReturn(
      Future.successful(Right(subscriptionStatusResponse))
    )

  protected def mockGetSubscription(
    subscription: Subscription
  ): ScalaOngoingStubbing[Future[Either[Int, Subscription]]] =
    when(mockSubscriptionsConnector.getSubscription(any())(any())).thenReturn(
      Future.successful(Right(subscription))
    )

  protected def mockGetSubscriptionCreate(
    subscription: SubscriptionSuccessfulResponse
  ): ScalaOngoingStubbing[Future[SubscriptionResponse]] =
    when(mockSubscriptionsConnector.submitSubscription(any(), any())(any())).thenReturn(
      Future.successful(subscription)
    )

  protected def mockSubscriptionUpdate(
    subscription: SubscriptionSuccessfulResponse
  ): ScalaOngoingStubbing[Future[SubscriptionResponse]] =
    when(mockSubscriptionsConnector.updateSubscription(any(), any())(any())).thenReturn(
      Future.successful(subscription)
    )

  protected def mockSubscriptionUpdateFailure(
    failedResponse: SubscriptionFailureResponseWithStatusCode
  ): ScalaOngoingStubbing[Future[SubscriptionResponse]] =
    when(mockSubscriptionsConnector.updateSubscription(any(), any())(any())).thenReturn(
      Future.successful(failedResponse)
    )

  protected def mockSubscriptionUpdateFailure(
    ex: Exception
  ): ScalaOngoingStubbing[Future[SubscriptionResponse]] =
    when(mockSubscriptionsConnector.updateSubscription(any(), any())(any())).thenThrow(ex)

  protected def mockNonRepudiationSubmission(
    response: NonRepudiationSubmissionAccepted
  ): ScalaOngoingStubbing[Future[NonRepudiationSubmissionAccepted]] =
    when(mockNonRepudiationConnector.submitNonRepudiation(any(), any())(any())).thenReturn(
      Future.successful(response)
    )

  protected def mockNonRepudiationSubmission(
    testEncodedPayload: String,
    expectedMetadata: NonRepudiationMetadata,
    response: NonRepudiationSubmissionAccepted
  )(implicit hc: HeaderCarrier): ScalaOngoingStubbing[Future[NonRepudiationSubmissionAccepted]] =
    when(
      mockNonRepudiationConnector.submitNonRepudiation(ArgumentMatchers.eq(testEncodedPayload),
                                                       ArgumentMatchers.eq(expectedMetadata)
      )(ArgumentMatchers.eq(hc))
    ).thenReturn(Future.successful(response))

  protected def mockNonRepudiationSubmissionFailure(
    ex: Exception
  ): ScalaOngoingStubbing[Future[NonRepudiationSubmissionAccepted]] =
    when(mockNonRepudiationConnector.submitNonRepudiation(any(), any())(any()))
      .thenThrow(ex)

  protected def mockEnrolmentSuccess(): ScalaOngoingStubbing[Future[TaxEnrolmentsResponse]] =
    when(mockTaxEnrolmentsConnector.submitEnrolment(any(), any(), any())(any())).thenReturn(
      Future.successful(Right(SuccessfulTaxEnrolment))
    )

  protected def mockEnrolmentFailure()
    : ScalaOngoingStubbing[Future[TaxEnrolmentsHttpParser.TaxEnrolmentsResponse]] =
    when(mockTaxEnrolmentsConnector.submitEnrolment(any(), any(), any())(any())).thenReturn(
      Future.successful(Left(FailedTaxEnrolment(1)))
    )

  protected def mockEnrolmentFailureException()
    : ScalaOngoingStubbing[Future[TaxEnrolmentsHttpParser.TaxEnrolmentsResponse]] =
    when(mockTaxEnrolmentsConnector.submitEnrolment(any(), any(), any())(any())).thenReturn(
      Future.failed(new IllegalStateException("BANG!"))
    )

}
