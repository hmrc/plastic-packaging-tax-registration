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

import java.time.LocalDate

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.test.Helpers.await
import uk.gov.hmrc.plasticpackagingtaxregistration.base.Injector
import uk.gov.hmrc.plasticpackagingtaxregistration.base.it.ConnectorISpec
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.EnrolmentStoreProxyConnector.KnownFactsTimerTag
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.EnrolmentStoreProxyConnectorISpec._
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.enrolment.UserEnrolmentRequest

class EnrolmentStoreProxyConnectorISpec extends ConnectorISpec with Injector with ScalaFutures {

  private val enrolmentStoreProxyConnector = app.injector.instanceOf[EnrolmentStoreProxyConnector]

  "Enrolment StoreProxy Connector" should {

    "query known facts successfully" when {

      val allFactsUserEnrolmentRequest =
        UserEnrolmentRequest("XYPPT0000000283", LocalDate.parse("2021-10-01"), Some("AA11AA"))

      val minimumFactsUserEnrolmentRequest =
        UserEnrolmentRequest("XYPPT0000000283", LocalDate.parse("2021-10-01"))

      "user supplies all known facts" in {
        mockSuccessfulKnownFactResponse(enrolmentStoreProxyAllFactsRequest,
                                        enrolmentStoreProxyResponse
        )

        val knownFactsResponse =
          await(enrolmentStoreProxyConnector.queryKnownFacts(allFactsUserEnrolmentRequest))

        knownFactsResponse.map(_.pptEnrolmentReferences) mustBe Some(Seq("XYPPT0000000283"))
        getTimer(KnownFactsTimerTag).getCount mustBe 1
      }

      "user supplies minimal known facts" in {
        mockSuccessfulKnownFactResponse(enrolmentStoreProxyMinimumFactsRequest,
                                        enrolmentStoreProxyResponse
        )

        val knownFactsResponse =
          await(enrolmentStoreProxyConnector.queryKnownFacts(minimumFactsUserEnrolmentRequest))

        knownFactsResponse.map(_.pptEnrolmentReferences) mustBe Some(Seq("XYPPT0000000283"))
        getTimer(KnownFactsTimerTag).getCount mustBe 1
      }

      "enrolment store proxy responds with no enrolments" in {
        mockSuccessfulKnownFactResponse(enrolmentStoreProxyAllFactsRequest,
                                        enrolmentStoreProxyEmptyResponse
        )

        val knownFactsResponse =
          await(enrolmentStoreProxyConnector.queryKnownFacts(allFactsUserEnrolmentRequest))

        knownFactsResponse.map(_.pptEnrolmentReferences) mustBe Some(Seq.empty)
        getTimer(KnownFactsTimerTag).getCount mustBe 1
      }

      "enrolment store proxy responds with no facts" in {
        mockNoKnownFactsResponse(enrolmentStoreProxyAllFactsRequest)

        val knownFactsResponse =
          await(enrolmentStoreProxyConnector.queryKnownFacts(allFactsUserEnrolmentRequest))

        knownFactsResponse mustBe None
        getTimer(KnownFactsTimerTag).getCount mustBe 1
      }
    }

  }

  private def mockSuccessfulKnownFactResponse(
    requestJson: String,
    responseJson: String
  ): StubMapping =
    stubFor(
      post(urlMatching("/enrolment-store-proxy/enrolment-store/enrolments"))
        .withRequestBody(equalToJson(requestJson))
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
            .withBody(responseJson)
        )
    )

  private def mockNoKnownFactsResponse(requestJson: String): StubMapping =
    stubFor(
      post(urlMatching("/enrolment-store-proxy/enrolment-store/enrolments"))
        .withRequestBody(equalToJson(requestJson))
        .willReturn(
          aResponse()
            .withStatus(Status.NO_CONTENT)
        )
    )

}

object EnrolmentStoreProxyConnectorISpec {

  private val enrolmentStoreProxyAllFactsRequest =
    """
      |{
      |  "service": "HMRC-PPT-ORG",
      |  "knownFacts": [
      |    {
      |      "key": "PPTRegistrationDate",
      |      "value": "20211001"
      |    },
      |    {
      |      "key": "BusinessPostCode",
      |      "value": "AA11AA"
      |    }
      |  ]
      |}
      |""".stripMargin

  private val enrolmentStoreProxyMinimumFactsRequest =
    """
      |{
      |  "service": "HMRC-PPT-ORG",
      |  "knownFacts": [
      |    {
      |      "key": "PPTRegistrationDate",
      |      "value": "20211001"
      |    }
      |  ]
      |}
      |""".stripMargin

  private val enrolmentStoreProxyResponse =
    """
      |{
      |  "service": "HMRC-PPT-ORG",
      |  "enrolments": [
      |    {
      |      "identifiers": [
      |        {
      |          "key": "EtmpRegistrationNumber",
      |          "value": "XYPPT0000000283"
      |        }
      |      ],
      |      "verifiers": [
      |        {
      |          "key": "PPTRegistrationDate",
      |          "value": "20211001"
      |        },
      |        {
      |          "key": "BusinessPostCode",
      |          "value": "AA11AA"
      |        }
      |      ]
      |    }
      |  ]
      |}
      |""".stripMargin

  private val enrolmentStoreProxyEmptyResponse =
    """
      |{
      |  "service": "HMRC-PPT-ORG",
      |  "enrolments": []
      |}
      |""".stripMargin

}
