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

package controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.mockito.{ArgumentCaptor, ArgumentMatchers, Mockito}
import play.api.libs.json.Json.toJson
import play.api.mvc.Result
import play.api.test.Helpers.{
  await,
  contentAsJson,
  route,
  status,
  writeableOf_AnyContentAsJson,
  OK,
  SERVICE_UNAVAILABLE
}
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}
import base.unit.ControllerSpec
import models.eis.EISError
import models.eis.subscription.create.{
  EISSubscriptionFailureResponse,
  SubscriptionFailureResponseWithStatusCode
}
import models.eis.subscription.update.SubscriptionUpdateWithNrsStatusResponse
import models.eis.subscription.{ChangeOfCircumstanceDetails, DeregistrationDetails, Subscription}
import models.DeregistrationReason
import models.nrs.NonRepudiationSubmissionAccepted

import java.time.{LocalDate, ZoneOffset, ZonedDateTime}
import java.util.UUID
import scala.concurrent.Future

class DeregistrationControllerSpec extends ControllerSpec {

  override def beforeEach(): Unit = {
    Mockito.reset(mockNonRepudiationService)
    super.beforeEach()
  }

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

      when(
        mockNonRepudiationService.submitNonRepudiation(any(), any(), any(), any())(any())
      ).thenReturn(Future.successful(NonRepudiationSubmissionAccepted(UUID.randomUUID().toString)))

      val result =
        await(route(app, subscriptionDeregister_HttpPUT.withJsonBody(toJson("Ceased Trading"))).get)

      status(Future.successful(result)) must be(OK)

      val updatedSubscription = subscriptionCaptor.getValue

      val changeOfCircumstanceDetails = updatedSubscription.changeOfCircumstanceDetails.get
      changeOfCircumstanceDetails.changeOfCircumstance mustBe "Deregistration"

      val deregistrationDetails = changeOfCircumstanceDetails.deregistrationDetails.get
      deregistrationDetails.deregistrationReason mustBe DeregistrationReason.CeasedTrading.toString
      deregistrationDetails.deregistrationDate mustBe LocalDate.now(ZoneOffset.UTC).toString
      deregistrationDetails.deregistrationDeclarationBox1 mustBe true
    }

    "return expected deregistration response " when {
      " NRS submission is successful" in {
        val nrSubmissionId = "nrSubmissionId"
        withAuthorizedUser(user = newUser())
        mockGetSubscription(ukLimitedCompanySubscription)
        mockSubscriptionUpdate(subscriptionSuccessfulResponse)
        when(
          mockNonRepudiationService.submitNonRepudiation(any(), any(), any(), any())(any())
        ).thenReturn(Future.successful(NonRepudiationSubmissionAccepted(nrSubmissionId)))

        val result: Future[Result] =
          route(app, subscriptionDeregister_HttpPUT.withJsonBody(toJson("Ceased Trading"))).get

        status(result) must be(OK)
        val response =
          contentAsJson(result).as[SubscriptionUpdateWithNrsStatusResponse]
        response.pptReference mustBe subscriptionSuccessfulResponse.pptReferenceNumber
        response.formBundleNumber mustBe subscriptionSuccessfulResponse.formBundleNumber
        response.processingDate mustBe subscriptionSuccessfulResponse.processingDate
        response.nrsNotifiedSuccessfully mustBe true
        response.nrsSubmissionId mustBe Some(nrSubmissionId)
        response.nrsFailureReason mustBe None

        val updatedSubscription = ukLimitedCompanySubscription.copy(changeOfCircumstanceDetails =
          Some(
            ChangeOfCircumstanceDetails(
              "Deregistration",
              Some(
                DeregistrationDetails("Ceased Trading",
                                      LocalDate.now(ZoneOffset.UTC).toString,
                                      true
                )
              )
            )
          )
        )
        verify(mockNonRepudiationService).submitNonRepudiation(
          ArgumentMatchers.eq(toJson(updatedSubscription).toString()),
          any[ZonedDateTime],
          ArgumentMatchers.eq(subscriptionSuccessfulResponse.pptReferenceNumber),
          any()
        )(any[HeaderCarrier])
      }

      " NRS submission fails with failure response" in {
        val nrsErrorMessage = "Service unavailable"
        withAuthorizedUser(user = newUser())
        mockGetSubscription(ukLimitedCompanySubscription)
        mockSubscriptionUpdate(subscriptionSuccessfulResponse)
        when(
          mockNonRepudiationService.submitNonRepudiation(any(), any(), any(), any())(any())
        ).thenReturn(Future.failed(new HttpException(nrsErrorMessage, SERVICE_UNAVAILABLE)))

        val result: Future[Result] =
          route(app, subscriptionDeregister_HttpPUT.withJsonBody(toJson("Ceased Trading"))).get

        status(result) must be(OK)
        val response =
          contentAsJson(result).as[SubscriptionUpdateWithNrsStatusResponse]
        response.pptReference mustBe subscriptionSuccessfulResponse.pptReferenceNumber
        response.formBundleNumber mustBe subscriptionSuccessfulResponse.formBundleNumber
        response.processingDate mustBe subscriptionSuccessfulResponse.processingDate
        response.nrsNotifiedSuccessfully mustBe false
        response.nrsSubmissionId mustBe None
        response.nrsFailureReason mustBe Some(nrsErrorMessage)

        val updatedSubscription = ukLimitedCompanySubscription.copy(changeOfCircumstanceDetails =
          Some(
            ChangeOfCircumstanceDetails(
              "Deregistration",
              Some(
                DeregistrationDetails("Ceased Trading",
                                      LocalDate.now(ZoneOffset.UTC).toString,
                                      true
                )
              )
            )
          )
        )
        verify(mockNonRepudiationService).submitNonRepudiation(
          ArgumentMatchers.eq(toJson(updatedSubscription).toString()),
          any[ZonedDateTime],
          ArgumentMatchers.eq(subscriptionSuccessfulResponse.pptReferenceNumber),
          any()
        )(any[HeaderCarrier])
      }
    }

    "throw an exception" when {
      "the attempt to get the existing subscription fails" in {
        withAuthorizedUser()
        mockGetSubscriptionFailure(new RuntimeException("Get subscription failed"))

        intercept[RuntimeException] {
          await(
            route(app,
                  subscriptionDeregister_HttpPUT.withJsonBody(
                    toJson(DeregistrationReason.CeasedTrading)
                  )
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
            route(app,
                  subscriptionDeregister_HttpPUT.withJsonBody(
                    toJson(DeregistrationReason.CeasedTrading.toString)
                  )
            ).get
          )
        }
      }
      "return underlying status code and error response when we receive an error response from EIS" in {
        withAuthorizedUser()
        mockGetSubscription(ukLimitedCompanySubscription)

        mockSubscriptionUpdateFailure(
          SubscriptionFailureResponseWithStatusCode(
            failureResponse = EISSubscriptionFailureResponse(failures =
              List(
                EISError(
                  "INVALID_PPT_REFERENCE_NUMBER",
                  "Submission has not passed validation. Invalid parameter pptReferenceNumber."
                )
              )
            ),
            422
          )
        )

        val rawResp = route(
          app,
          subscriptionDeregister_HttpPUT.withJsonBody(toJson(DeregistrationReason.CeasedTrading))
        ).get

        status(rawResp) mustBe 422
        val resp = contentAsJson(rawResp).as[EISSubscriptionFailureResponse]
        resp.failures mustBe List(
          EISError("INVALID_PPT_REFERENCE_NUMBER",
                   "Submission has not passed validation. Invalid parameter pptReferenceNumber."
          )
        )
      }
    }
  }
}
