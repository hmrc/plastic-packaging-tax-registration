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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, verifyNoInteractions, when}
import org.mockito.{ArgumentMatchers, Mockito}
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsJson, route, status, _}
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}
import uk.gov.hmrc.plasticpackagingtaxregistration.base.unit.ControllerSpec
import uk.gov.hmrc.plasticpackagingtaxregistration.builders.{
  RegistrationBuilder,
  RegistrationRequestBuilder
}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.SubscriptionCreateWithEnrolmentAndNrsStatusesResponse
import uk.gov.hmrc.plasticpackagingtaxregistration.models.nrs.NonRepudiationSubmissionAccepted
import uk.gov.hmrc.plasticpackagingtaxregistration.models.{MetaData, RegistrationRequest}

import java.time.ZonedDateTime
import java.util.UUID
import scala.concurrent.Future

class SubscriptionControllerSpec
    extends ControllerSpec with RegistrationBuilder with RegistrationRequestBuilder {

  override def beforeEach(): Unit = {
    Mockito.reset(mockRepository, mockNonRepudiationService)
    super.beforeEach()

    when(mockRepository.delete(any())).thenReturn(Future.successful())
  }

  "Get subscription status" should {
    "return expected details" in {
      withAuthorizedUser()
      mockGetSubscriptionStatus(subscriptionStatusResponse)
      mockNonRepudiationSubmission(NonRepudiationSubmissionAccepted(UUID.randomUUID().toString))

      val result: Future[Result] = route(app, subscriptionStatusResponse_HttpGet).get

      status(result) must be(OK)
      contentAsJson(result) mustBe toJson(subscriptionStatusResponse)
      verify(mockSubscriptionsConnector).getSubscriptionStatus(ArgumentMatchers.eq(safeNumber))(
        any()
      )
    }

    "return 401" when {
      "user not authorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result: Future[Result] = route(app, subscriptionStatusResponse_HttpGet).get

        status(result) must be(UNAUTHORIZED)
        verifyNoInteractions(mockSubscriptionsConnector)
      }
    }

    "pass through exceptions" when {
      "an exception occurs during the subscription call" in {
        withAuthorizedUser()
        mockGetSubscriptionStatusFailure(new IllegalStateException("BANG!"))

        val result: Future[Result] = route(app, subscriptionStatusResponse_HttpGet).get

        intercept[IllegalStateException] {
          status(result)
        }
      }
    }
  }

  "Create subscription" should {
    val request = aRegistrationRequest(withLiabilityDetailsRequest(pptLiabilityDetails),
                                       withOrganisationDetailsRequest(pptIncorporationDetails),
                                       withPrimaryContactDetailsRequest(pptPrimaryContactDetails),
                                       withMetaDataRequest(
                                         MetaData(registrationReviewed, registrationCompleted)
                                       ),
                                       withUserHeaders(pptUserHeaders)
    )

    "return expected details" when {
      "EIS/IF subscription is successful" when {
        "and both NRS submission and enrolment are successful" in {
          val internalId     = "Int-ba17b467-90f3-42b6-9570-73be7b78eb2b"
          val nrSubmissionId = "nrSubmissionId"
          withAuthorizedUser(user = newUser())
          mockGetSubscriptionCreate(subscriptionCreateResponse)
          when(
            mockNonRepudiationService.submitNonRepudiation(any(), any(), any(), any())(any())
          ).thenReturn(Future.successful(NonRepudiationSubmissionAccepted(nrSubmissionId)))
          mockEnrolmentSuccess()

          val result: Future[Result] =
            route(app, subscriptionCreate_HttpPost.withJsonBody(toJson(request))).get

          status(result) must be(OK)
          val response =
            contentAsJson(result).as[SubscriptionCreateWithEnrolmentAndNrsStatusesResponse]
          response.pptReference mustBe subscriptionCreateResponse.pptReference
          response.formBundleNumber mustBe subscriptionCreateResponse.formBundleNumber
          response.processingDate mustBe subscriptionCreateResponse.processingDate
          response.nrsNotifiedSuccessfully mustBe true
          response.nrsSubmissionId mustBe Some(nrSubmissionId)
          response.nrsFailureReason mustBe None
          response.enrolmentInitiatedSuccessfully mustBe true

          verify(mockRepository).delete(internalId)
          verify(mockNonRepudiationService).submitNonRepudiation(
            ArgumentMatchers.contains(request.incorpJourneyId.get),
            any[ZonedDateTime],
            ArgumentMatchers.eq(subscriptionCreateResponse.pptReference),
            ArgumentMatchers.eq(pptUserHeaders)
          )(any[HeaderCarrier])
        }

        "but NRS submission fails and enrolment fails with failure response" in {
          mockEnrolmentFailure()
          assertExpectedResponseWhenNrsSubmissionAndEnrolmentFail(request)
        }

        "but NRS submission fails and enrolment fails with exception" in {
          mockEnrolmentFailureException()
          assertExpectedResponseWhenNrsSubmissionAndEnrolmentFail(request)
        }
      }
    }

    "return 400" when {
      "invalid json" in {
        withAuthorizedUser()
        val payload = Json.toJson(Map("incorpJourneyId" -> false)).as[JsObject]
        val result: Future[Result] =
          route(app, subscriptionCreate_HttpPost.withJsonBody(payload)).get

        status(result) must be(BAD_REQUEST)
        contentAsJson(result) mustBe Json.obj("statusCode" -> 400, "message" -> "Bad Request")
        verifyNoInteractions(mockSubscriptionsConnector)
      }
    }

    "return 401" when {
      "unauthorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result: Future[Result] =
          route(app, subscriptionCreate_HttpPost.withJsonBody(toJson(aRegistrationRequest()))).get

        status(result) must be(UNAUTHORIZED)
        verifyNoInteractions(mockSubscriptionsConnector)
      }
    }

    "return 500" when {
      "EIS/IF subscription call returns an exception" in {
        withAuthorizedUser()
        mockGetSubscriptionSubmitFailure(new RuntimeException("error"))
        intercept[Exception] {
          val result: Future[Result] =
            route(app, subscriptionCreate_HttpPost.withJsonBody(toJson(request))).get
          status(result)
        }
      }
    }
  }

  private def assertExpectedResponseWhenNrsSubmissionAndEnrolmentFail(
    request: RegistrationRequest
  ): Unit = {
    val internalId      = "Int-ba17b467-90f3-42b6-9570-73be7b78eb2b"
    val nrsErrorMessage = "Service unavailable"
    withAuthorizedUser(user = newUser())
    mockGetSubscriptionCreate(subscriptionCreateResponse)
    when(
      mockNonRepudiationService.submitNonRepudiation(any(), any(), any(), any())(any())
    ).thenReturn(Future.failed(new HttpException(nrsErrorMessage, SERVICE_UNAVAILABLE)))

    val result: Future[Result] =
      route(app, subscriptionCreate_HttpPost.withJsonBody(toJson(request))).get

    status(result) must be(OK)
    val response =
      contentAsJson(result).as[SubscriptionCreateWithEnrolmentAndNrsStatusesResponse]
    response.pptReference mustBe subscriptionCreateResponse.pptReference
    response.formBundleNumber mustBe subscriptionCreateResponse.formBundleNumber
    response.processingDate mustBe subscriptionCreateResponse.processingDate
    response.nrsNotifiedSuccessfully mustBe false
    response.nrsSubmissionId mustBe None
    response.nrsFailureReason mustBe Some(nrsErrorMessage)
    response.enrolmentInitiatedSuccessfully mustBe false

    verify(mockRepository).delete(internalId)
    verify(mockNonRepudiationService).submitNonRepudiation(
      ArgumentMatchers.contains(request.incorpJourneyId.get),
      any[ZonedDateTime],
      ArgumentMatchers.eq(subscriptionCreateResponse.pptReference),
      ArgumentMatchers.eq(pptUserHeaders)
    )(any[HeaderCarrier])
  }

}
