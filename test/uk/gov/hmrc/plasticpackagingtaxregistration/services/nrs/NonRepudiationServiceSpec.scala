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

package uk.gov.hmrc.plasticpackagingtaxregistration.services.nrs

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, NotFoundException}
import uk.gov.hmrc.plasticpackagingtaxregistration.base.AuthTestSupport
import uk.gov.hmrc.plasticpackagingtaxregistration.base.data.NrsTestData
import uk.gov.hmrc.plasticpackagingtaxregistration.base.unit.MockConnectors
import uk.gov.hmrc.plasticpackagingtaxregistration.builders.{
  RegistrationBuilder,
  RegistrationRequestBuilder
}
import uk.gov.hmrc.plasticpackagingtaxregistration.models.nrs.{
  NonRepudiationMetadata,
  NonRepudiationSubmissionAccepted
}

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import scala.concurrent.{ExecutionContext, Future}

class NonRepudiationServiceSpec
    extends AnyWordSpec with GuiceOneAppPerSuite with AuthTestSupport with NrsTestData
    with BeforeAndAfterEach with ScalaFutures with Matchers with MockConnectors
    with RegistrationBuilder with RegistrationRequestBuilder {

  protected implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val nonRepudiationService = NonRepudiationService(mockNonRepudiationConnector, mockAuthConnector)

  implicit val hc: HeaderCarrier            = HeaderCarrier(authorization = Some(Authorization(testAuthToken)))
  implicit val request: Request[AnyContent] = FakeRequest()

  "submitNonRepudiation" should {
    "call the nonRepudiationConnector with the correctly formatted metadata" in {
      val testSubmissionId  = "testSubmissionId"
      val testPayloadString = "testPayloadString"

      val testPayloadChecksum = MessageDigest.getInstance("SHA-256")
        .digest(testPayloadString.getBytes(StandardCharsets.UTF_8))
        .map("%02x".format(_)).mkString

      val testEncodedPayload =
        Base64.getEncoder.encodeToString(testPayloadString.getBytes(StandardCharsets.UTF_8))

      val expectedMetadata = NonRepudiationMetadata(businessId = "ppt",
                                                    notableEvent = "ppt-subscription",
                                                    payloadContentType = "application/json",
                                                    payloadSha256Checksum = testPayloadChecksum,
                                                    userSubmissionTimestamp = testDateTime,
                                                    identityData = testNonRepudiationIdentityData,
                                                    userAuthToken = testAuthToken,
                                                    headerData = testUserHeaders,
                                                    searchKeys =
                                                      Map("pptReference" -> testPPTReference)
      )

      when(
        mockNonRepudiationConnector.submitNonRepudiation(ArgumentMatchers.eq(testEncodedPayload),
                                                         ArgumentMatchers.eq(expectedMetadata)
        )(ArgumentMatchers.eq(hc))
      )
        .thenReturn(Future.successful(NonRepudiationSubmissionAccepted(testSubmissionId)))

      when(
        mockAuthConnector.authorise(
          ArgumentMatchers.eq(EmptyPredicate),
          ArgumentMatchers.eq(NonRepudiationService.nonRepudiationIdentityRetrievals)
        )(ArgumentMatchers.eq(hc), ArgumentMatchers.eq(ec))
      ).thenReturn(Future.successful(testAuthRetrievals))

      val res = nonRepudiationService.submitNonRepudiation(testPayloadString,
                                                           testDateTime,
                                                           testPPTReference,
                                                           testUserHeaders
      )

      await(res) mustBe NonRepudiationSubmissionAccepted(testSubmissionId)
    }
    "audit when the non repudiation call fails" in {
      val testPayloadString = "testPayloadString"

      val testPayloadChecksum = MessageDigest.getInstance("SHA-256")
        .digest(testPayloadString.getBytes(StandardCharsets.UTF_8))
        .map("%02x".format(_)).mkString

      val testEncodedPayload =
        Base64.getEncoder.encodeToString(testPayloadString.getBytes(StandardCharsets.UTF_8))

      val expectedMetadata = NonRepudiationMetadata(businessId = "ppt",
                                                    notableEvent = "ppt-subscription",
                                                    payloadContentType = "application/json",
                                                    payloadSha256Checksum = testPayloadChecksum,
                                                    userSubmissionTimestamp = testDateTime,
                                                    identityData = testNonRepudiationIdentityData,
                                                    userAuthToken = testAuthToken,
                                                    headerData = testUserHeaders,
                                                    searchKeys =
                                                      Map("pptReference" -> testPPTReference)
      )

      val testExceptionMessage = "testExceptionMessage"

      when(
        mockAuthConnector.authorise(
          ArgumentMatchers.eq(EmptyPredicate),
          ArgumentMatchers.eq(NonRepudiationService.nonRepudiationIdentityRetrievals)
        )(ArgumentMatchers.eq(hc), ArgumentMatchers.eq(ec))
      ).thenReturn(Future.successful(testAuthRetrievals))

      when(
        mockNonRepudiationConnector.submitNonRepudiation(ArgumentMatchers.eq(testEncodedPayload),
                                                         ArgumentMatchers.eq(expectedMetadata)
        )(ArgumentMatchers.eq(hc))
      )
        .thenReturn(Future.failed(new NotFoundException(testExceptionMessage)))

      val res = nonRepudiationService.submitNonRepudiation(testPayloadString,
                                                           testDateTime,
                                                           testPPTReference,
                                                           testUserHeaders
      )

      intercept[NotFoundException](await(res))
    }
  }
}
