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

import base.unit.ControllerSpec
import builders.{RegistrationBuilder, RegistrationRequestBuilder}
import connectors.{
  EisSubscriptionsConnector,
  EnrolmentStoreProxyConnector,
  HipSubscriptionsConnector,
  NonRepudiationConnector,
  TaxEnrolmentsConnector
}
import models.MetaData
import models.eis.subscription.create.SubscriptionCreateWithEnrolmentAndNrsStatusesResponse
import models.nrs.NonRepudiationSubmissionAccepted
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verifyNoInteractions, when}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json.toJson
import play.api.mvc.Result
import play.api.test.Helpers.*
import repositories.RegistrationRepository
import services.nrs.NonRepudiationService
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.Future

/** Proves the controller to service to feature switch to HIP connector wiring inside a real
  * application. Every other controller suite runs with the switch off.
  */
class SubscriptionControllerHipSpec
    extends ControllerSpec with RegistrationBuilder with RegistrationRequestBuilder {

  override lazy val app: Application = GuiceApplicationBuilder()
    .overrides(
      bind[AuthConnector].to(mockAuthConnector),
      bind[EisSubscriptionsConnector].to(mockEisSubscriptionsConnector),
      bind[HipSubscriptionsConnector].to(mockHipSubscriptionsConnector),
      bind[NonRepudiationConnector].to(mockNonRepudiationConnector),
      bind[RegistrationRepository].to(mockRepository),
      bind[NonRepudiationService].to(mockNonRepudiationService),
      bind[TaxEnrolmentsConnector].to(mockTaxEnrolmentsConnector),
      bind[EnrolmentStoreProxyConnector].to(mockEnrolmentStoreProxyConnector)
    )
    .configure("features.hip.subscription" -> true)
    .build()

  override def beforeEach(): Unit = {
    reset(mockRepository, mockNonRepudiationService)
    super.beforeEach()

    when(mockRepository.delete(any())).thenReturn(Future.successful(()))
  }

  "Create subscription with the HIP feature switch on" should {
    val request = aRegistrationRequest(withLiabilityDetailsRequest(pptLiabilityDetails),
                                       withOrganisationDetailsRequest(pptIncorporationDetails),
                                       withPrimaryContactDetailsRequest(pptPrimaryContactDetails),
                                       withMetaDataRequest(
                                         MetaData(registrationReviewed, registrationCompleted)
                                       ),
                                       withUserHeaders(pptUserHeaders)
    )

    "subscribe through HIP and leave EIS untouched" in {
      withAuthorizedUser(user = newUser())
      mockHipSubscriptionCreate(subscriptionSuccessfulResponse)
      when(
        mockNonRepudiationService.submitNonRepudiation(any(), any(), any(), any())(using any())
      ).thenReturn(Future.successful(NonRepudiationSubmissionAccepted("nrSubmissionId")))
      mockEnrolmentSuccess()

      val result: Future[Result] =
        route(app, subscriptionCreate_HttpPost.withJsonBody(toJson(request))).get

      status(result) must be(OK)
      val response =
        contentAsJson(result).as[SubscriptionCreateWithEnrolmentAndNrsStatusesResponse]
      response.pptReference mustBe subscriptionSuccessfulResponse.pptReferenceNumber
      response.formBundleNumber mustBe subscriptionSuccessfulResponse.formBundleNumber
      response.enrolmentInitiatedSuccessfully mustBe true

      verifyNoInteractions(mockEisSubscriptionsConnector)
    }

    "return the mapped HIP failure in the EIS wire shape" in {
      withAuthorizedUser()
      mockHipSubscriptionCreateFailure(hipMappedBusinessValidationFailure)

      val rawResp = route(app, subscriptionCreate_HttpPost.withJsonBody(toJson(request))).get

      status(rawResp) mustBe UNPROCESSABLE_ENTITY
      contentAsJson(rawResp) mustBe toJson(hipMappedBusinessValidationFailure.failureResponse)
    }
  }

}
