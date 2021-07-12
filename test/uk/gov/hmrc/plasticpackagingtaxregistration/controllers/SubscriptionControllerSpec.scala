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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtaxregistration.base.unit.ControllerSpec
import uk.gov.hmrc.plasticpackagingtaxregistration.builders.{
  RegistrationBuilder,
  RegistrationRequestBuilder
}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.SubscriptionCreateSuccessfulResponse
import uk.gov.hmrc.plasticpackagingtaxregistration.models.MetaData
import uk.gov.hmrc.plasticpackagingtaxregistration.models.nrs.NonRepudiationSubmissionAccepted

import java.time.ZonedDateTime
import java.util.UUID
import scala.concurrent.Future

class SubscriptionControllerSpec
    extends ControllerSpec with RegistrationBuilder with RegistrationRequestBuilder {

  override def beforeEach(): Unit = {
    Mockito.reset(mockRepository, mockNonRepudiationService)
    super.beforeEach()
  }

  "GET /subscriptions/status/:id " should {
    "return 200" when {
      "request is valid" in {
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

  "POST /subscriptions/:safeNumber" should {
    val request = aRegistrationRequest(withLiabilityDetailsRequest(pptLiabilityDetails),
                                       withOrganisationDetailsRequest(pptOrganisationDetails),
                                       withPrimaryContactDetailsRequest(pptPrimaryContactDetails),
                                       withMetaDataRequest(
                                         MetaData(registrationReviewed, registrationCompleted)
                                       ),
                                       withUserHeaders(pptUserHeaders)
    )

    "return 200 make NRS submission and delete transient registration" when {
      "EIS/IF subscription is successful" in {
        val utr            = "999"
        val nrSubmissionId = "nrSubmissionId"
        withAuthorizedUser(user = newUser(Some(pptEnrolment(utr))))
        mockGetSubscriptionCreate(subscriptionCreateResponse)
        when(
          mockNonRepudiationService.submitNonRepudiation(any(), any(), any(), any())(any())
        ).thenReturn(Future.successful(NonRepudiationSubmissionAccepted(nrSubmissionId)))

        val result: Future[Result] =
          route(app, subscriptionCreate_HttpPost.withJsonBody(toJson(request))).get

        status(result) must be(OK)
        val response = contentAsJson(result).as[SubscriptionCreateSuccessfulResponse]
        response.pptReference mustBe subscriptionCreateResponse.pptReference
        response.formBundleNumber mustBe subscriptionCreateResponse.formBundleNumber
        response.processingDate mustBe subscriptionCreateResponse.processingDate
        verify(mockRepository).delete(utr)
        verify(mockNonRepudiationService).submitNonRepudiation(
          ArgumentMatchers.contains(request.incorpJourneyId.get),
          any[ZonedDateTime],
          ArgumentMatchers.eq(subscriptionCreateResponse.pptReference),
          ArgumentMatchers.eq(pptUserHeaders)
        )(any[HeaderCarrier])
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

}
