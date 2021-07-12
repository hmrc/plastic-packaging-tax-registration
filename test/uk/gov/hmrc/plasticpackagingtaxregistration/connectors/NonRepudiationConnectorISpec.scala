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

package uk.gov.hmrc.plasticpackagingtaxregistration.connectors

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, _}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR}
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.Helpers.await
import uk.gov.hmrc.plasticpackagingtaxregistration.base.data.NrsTestData
import uk.gov.hmrc.plasticpackagingtaxregistration.base.it.ConnectorISpec
import uk.gov.hmrc.plasticpackagingtaxregistration.base.{AuthTestSupport, Injector}
import uk.gov.hmrc.plasticpackagingtaxregistration.models.nrs.{
  NonRepudiationMetadata,
  NonRepudiationSubmissionAccepted
}

class NonRepudiationConnectorISpec
    extends ConnectorISpec with Injector with AuthTestSupport with NrsTestData with ScalaFutures {
  lazy val config                               = Map("microservice.services.nrs.api-key" -> testNonRepudiationApiKey)
  lazy val connector: NonRepudiationConnector   = app.injector.instanceOf[NonRepudiationConnector]
  private implicit val testNonRepudiationApiKey = "test-key"

  private val pptNrsSubmissionTimer = "ppt.nrs.submission.timer"

  "submitNonRepudiation" should {
    val testEncodedPayload  = "testEncodedPayload"
    val testPayloadChecksum = "testPayloadChecksum"
    val headerData          = Map("testHeaderKey" -> "testHeaderValue")

    val testNonRepudiationMetadata = NonRepudiationMetadata(businessId = "vrs",
                                                            notableEvent = "vat-registration",
                                                            payloadContentType =
                                                              "application/json",
                                                            payloadSha256Checksum =
                                                              testPayloadChecksum,
                                                            userSubmissionTimestamp =
                                                              testDateTime,
                                                            identityData =
                                                              testNonRepudiationIdentityData,
                                                            userAuthToken = testAuthToken,
                                                            headerData = headerData,
                                                            searchKeys =
                                                              Map("postCode" -> testPPTReference)
    )

    val expectedRequestJson: JsObject =
      Json.obj("payload" -> testEncodedPayload, "metadata" -> testNonRepudiationMetadata)

    "return a success" in {
      val testNonRepudiationSubmissionId = "testNonRepudiationSubmissionId"
      stubNonRepudiationSubmission(ACCEPTED,
                                   expectedRequestJson,
                                   Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId)
      )

      val res = connector.submitNonRepudiation(testEncodedPayload, testNonRepudiationMetadata)

      await(res) mustBe NonRepudiationSubmissionAccepted(testNonRepudiationSubmissionId)
      getTimer(pptNrsSubmissionTimer).getCount mustBe 1
    }

    "handle a NRS exception" in {
      stubNonRepudiationSubmissionFailure(INTERNAL_SERVER_ERROR, expectedRequestJson)

      intercept[Exception] {
        await(connector.submitNonRepudiation(testEncodedPayload, testNonRepudiationMetadata))
      }
      getTimer(pptNrsSubmissionTimer).getCount mustBe 1
    }

  }

  private def stubNonRepudiationSubmission(status: Int, request: JsValue, response: JsObject)(
    implicit apiKey: String
  ): StubMapping =
    stubFor(
      post(urlMatching(s"/submission"))
        .withRequestBody(equalToJson(request.toString()))
        .withHeader("X-API-Key", equalTo(apiKey))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(response.toString())
        )
    )

  private def stubNonRepudiationSubmissionFailure(status: Int, requestJson: JsValue)(implicit
    apiKey: String
  ): StubMapping =
    stubFor(
      post(urlMatching(s"/submission"))
        .withRequestBody(equalToJson(requestJson.toString()))
        .withHeader("X-API-Key", equalTo(apiKey))
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )

}
