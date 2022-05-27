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

package uk.gov.hmrc.plasticpackagingtaxregistration.services

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.{ZoneId, ZonedDateTime}
import java.util.Base64

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.matchers.must
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.IM_A_TEAPOT
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}
import uk.gov.hmrc.plasticpackagingtaxregistration.base.data.NrsTestData
import uk.gov.hmrc.plasticpackagingtaxregistration.builders.RegistrationBuilder
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.EISError
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.Subscription
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.create.{SubscriptionCreateWithEnrolmentAndNrsStatusesResponse, EISSubscriptionFailureResponse, SubscriptionFailureResponseWithStatusCode, SubscriptionSuccessfulResponse}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.parsers.TaxEnrolmentsHttpParser.SuccessfulTaxEnrolment
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.{NonRepudiationConnector, SubscriptionsConnector, TaxEnrolmentsConnector}
import uk.gov.hmrc.plasticpackagingtaxregistration.models.nrs.{NonRepudiationMetadata, NonRepudiationSubmissionAccepted}
import uk.gov.hmrc.plasticpackagingtaxregistration.repositories.RegistrationRepository
import uk.gov.hmrc.plasticpackagingtaxregistration.services.nrs.NonRepudiationService

import scala.concurrent.ExecutionContext.{global => globalExecutionContext}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class SubscriptionServiceSpec extends AnyWordSpec with RegistrationBuilder with must.Matchers with NrsTestData{
    "SubscriptionService" when {
      "called, submits a subscription to the HOD API" in new Fixture {
        val registration = aValidRegistration()


        val result = SUT.submit(registration, "SAFE_ID", Map.empty)(hc)

        Await.ready(result, 1 second)
        verify(mockSubscriptionConnector, times(1)).submitSubscription(eqTo("SAFE_ID"), any())(any())
      }
    }

    "the subscription succeeds" should {
      "enrol the user" in new Fixture {
        when(mockSubscriptionConnector.submitSubscription(any[String], any[Subscription])(any[HeaderCarrier])).thenReturn(
          Future.successful(SubscriptionSuccessfulResponse("PPT_REF_2", ZonedDateTime.of(2022, 12, 13, 10, 32, 12, 765, ZoneId.of("UTC")), "FORM_BUNDLE_NO_2"))
        )

        val registration = aValidRegistration()


        val result = Await.result(SUT.submit(registration, "SAFE_ID", Map.empty)(hc), 1 second)

        verify(mockTaxEnrolmentsConnector, times(1)).submitEnrolment(eqTo("PPT_REF_2"), eqTo("SAFE_ID"), eqTo("FORM_BUNDLE_NO_2"))(any())
      }


      "notify NRS" in new Fixture {
        when(mockSubscriptionConnector.submitSubscription(any[String], any[Subscription])(any[HeaderCarrier])).thenReturn(
          Future.successful(SubscriptionSuccessfulResponse("PPT_REF", ZonedDateTime.of(2022, 12, 14, 10, 32, 12, 765, ZoneId.of("UTC")), "FORM_BUNDLE_NO"))
        )

        val registration = aValidRegistration()


        val result = Await.result(SUT.submit(registration, "SAFE_ID", Map.empty)(hc), 1 second)


        val payloadCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
        val metaDataCaptor: ArgumentCaptor[NonRepudiationMetadata] = ArgumentCaptor.forClass(classOf[NonRepudiationMetadata])
        verify(mockNonRepudiationConnector, times(1)).submitNonRepudiation(payloadCaptor.capture(), metaDataCaptor.capture())(any())

        //payload is base64 encoded UTF-8 Json
        val expectedJson = Json.toJson(registration)
        val expectedcheckSum = MessageDigest.getInstance("SHA-256")
                                .digest(expectedJson.toString().getBytes(StandardCharsets.UTF_8))
                                .map("%02x".format(_)).mkString
        val jsonStr: String = new String(Base64.getDecoder.decode(payloadCaptor.getValue), StandardCharsets.UTF_8)
        Json.parse(jsonStr) mustBe expectedJson

        val metadata = metaDataCaptor.getValue
        metadata.businessId mustBe ("ppt")
        metadata.notableEvent mustBe ("ppt-subscription")
        metadata.payloadContentType mustBe ("application/json")
        metadata.payloadSha256Checksum mustBe expectedcheckSum
        metadata.userSubmissionTimestamp mustBe ZonedDateTime.of(2022, 12, 14, 10, 32, 12, 765, ZoneId.of("UTC"))
        metadata.searchKeys mustBe Map("pptReference" -> "PPT_REF")

      }

      "delete the registration record" in new Fixture {
        when(mockSubscriptionConnector.submitSubscription(any[String], any[Subscription])(any[HeaderCarrier])).thenReturn(
          Future.successful(SubscriptionSuccessfulResponse("PPT_REF_2", ZonedDateTime.of(2022, 12, 13, 10, 32, 12, 765, ZoneId.of("UTC")), "FORM_BUNDLE_NO_2"))
        )

        val registration = aValidRegistration()


        val result = Await.result(SUT.submit(registration, "SAFE_ID", Map.empty)(hc), 1 second)

        verify(mockRegistrationRepository, times(1)).delete(eqTo(registration.id))
      }

      "return the success information" in new Fixture {
        when(mockSubscriptionConnector.submitSubscription(any[String], any[Subscription])(any[HeaderCarrier])).thenReturn(
          Future.successful(SubscriptionSuccessfulResponse("PPT_REF_4", ZonedDateTime.of(2022, 12, 22, 10, 32, 12, 765, ZoneId.of("UTC")), "FORM_BUNDLE_NO_4"))
        )

        val registration = aValidRegistration()

        val result = Await.result(SUT.submit(registration, "SAFE_ID", Map.empty)(hc), 1 second)

        result mustBe Right(SubscriptionCreateWithEnrolmentAndNrsStatusesResponse(
          pptReference = "PPT_REF_4",
          processingDate = ZonedDateTime.of(2022, 12, 22, 10, 32, 12, 765, ZoneId.of("UTC")),
          formBundleNumber = "FORM_BUNDLE_NO_4",
          nrsNotifiedSuccessfully = true,
          nrsSubmissionId = Some("NRS_SUBMISSION_ID"),
          nrsFailureReason = None,
          enrolmentInitiatedSuccessfully = true
        ))
      }
    }

    "the subscription fails" should {
      "return the failure unchanged from the connector" in new Fixture {

        when(mockSubscriptionConnector.submitSubscription(any[String], any[Subscription])(any[HeaderCarrier])).thenReturn(
          Future.successful(SubscriptionFailureResponseWithStatusCode(
            EISSubscriptionFailureResponse(
              Seq(
                EISError("CODE 1", "Reason 1"),
                EISError("CODE 2", "Reason 2"),
              )
              ),
              IM_A_TEAPOT
            )
          )
        )

        val registration = aValidRegistration()


        val result = Await.result(SUT.submit(registration, "SAFE_ID", Map.empty)(hc), 1 second)
        result mustBe Left(SubscriptionFailureResponseWithStatusCode(
                        EISSubscriptionFailureResponse(
                          Seq(
                            EISError("CODE 1", "Reason 1"),
                            EISError("CODE 2", "Reason 2"),
                          )
                        ),
                        IM_A_TEAPOT
                      ))
      }
    }

    trait Fixture {
      val mockSubscriptionConnector = mock[SubscriptionsConnector]
      val mockTaxEnrolmentsConnector = mock[TaxEnrolmentsConnector]
      val mockRegistrationRepository = mock[RegistrationRepository]
      val mockNonRepudiationConnector = mock[NonRepudiationConnector]
      val mockAuthConnector = mock[AuthConnector]

      val nonRepudiationService = new NonRepudiationService(mockNonRepudiationConnector, mockAuthConnector)(globalExecutionContext)

      val hc = HeaderCarrier(authorization = Some(Authorization("AUTH_TOKEN")))

      when(mockTaxEnrolmentsConnector.submitEnrolment(any(), any(), any())(any())).thenReturn(
        Future.successful(Right(SuccessfulTaxEnrolment))
      )

      when(mockAuthConnector.authorise[NonRepudiationService.NonRepudiationIdentityRetrievals](any(), any())(any(), any())).thenReturn(
        Future.successful(testAuthRetrievals)
      )

      when(mockSubscriptionConnector.submitSubscription(any[String], any[Subscription])(any[HeaderCarrier])).thenReturn(
        Future.successful(SubscriptionSuccessfulResponse("PPT_REF", ZonedDateTime.of(2022, 12, 13, 10, 32, 12, 765, ZoneId.of("UTC")), "FORM_BUNDLE_NO"))
      )

      when(mockNonRepudiationConnector.submitNonRepudiation(any(), any())(any())).thenReturn(
        Future.successful(NonRepudiationSubmissionAccepted("NRS_SUBMISSION_ID"))
      )

      when(mockRegistrationRepository.delete(any())).thenReturn(Future.successful(()))

      val SUT = new SubscriptionService(
        mockSubscriptionConnector,
        mockTaxEnrolmentsConnector,
        mockRegistrationRepository,
        nonRepudiationService
      )(globalExecutionContext)
    }
}
