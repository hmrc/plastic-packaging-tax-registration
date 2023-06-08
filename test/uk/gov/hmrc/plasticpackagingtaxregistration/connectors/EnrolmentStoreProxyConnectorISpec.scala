/*
 * Copyright 2023 HM Revenue & Customs
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

package connectors

import base.Injector
import base.it.ConnectorISpec
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import connectors.EnrolmentStoreProxyConnector.GroupsWithEnrolmentsTimerTag
import connectors.EnrolmentStoreProxyConnectorISpec.{allGroupsWithEnrolmentsResponse, principalGroupsWithEnrolmentsResponse}
import models.enrolmentstoreproxy.GroupsWithEnrolmentsResponse
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.test.Helpers.await
import uk.gov.hmrc.http.UpstreamErrorResponse


class EnrolmentStoreProxyConnectorISpec extends ConnectorISpec with Injector with ScalaFutures {

  private val enrolmentStoreProxyConnector = app.injector.instanceOf[EnrolmentStoreProxyConnector]

  val enrolmentKey = "HMRC-PPT-ORG~EtmpRegistrationNumber~XYPPT0000000283"

  "Enrolment StoreProxy Connector" should {

    "query groups with enrolment" when {

      "no groups exist" in {
        mockNoGroupsWithEnrolmentsResponse()

        val groupsResponse =
          await(enrolmentStoreProxyConnector.queryGroupsWithEnrolment("XYPPT0000000283"))

        groupsResponse mustBe None
        getTimer(GroupsWithEnrolmentsTimerTag).getCount mustBe 1
      }

      "all groups exist" in {
        mockGroupsWithEnrolmentsResponse(allGroupsWithEnrolmentsResponse)

        val groupsResponse =
          await(enrolmentStoreProxyConnector.queryGroupsWithEnrolment("XYPPT0000000283"))

        groupsResponse mustBe Some(
          GroupsWithEnrolmentsResponse(Some(Seq("ABCEDEFGI1234567", "ABCEDEFGI1234568")),
                                       Some(Seq("ABCEDEFGI1234567", "ABCEDEFGI1234568"))
          )
        )
        getTimer(GroupsWithEnrolmentsTimerTag).getCount mustBe 1
      }

      "principle groups exist" in {
        mockGroupsWithEnrolmentsResponse(principalGroupsWithEnrolmentsResponse)

        val groupsResponse =
          await(enrolmentStoreProxyConnector.queryGroupsWithEnrolment("XYPPT0000000283"))

        groupsResponse mustBe Some(
          GroupsWithEnrolmentsResponse(Some(Seq("ABCEDEFGI1234567", "ABCEDEFGI1234568")), None)
        )
        getTimer(GroupsWithEnrolmentsTimerTag).getCount mustBe 1
      }

    }

    "bubble error" in {
      mockErrorGroupsWithEnrolmentsResponse("HMRC-PPT-ORG~EtmpRegistrationNumber~XYPPT0000000283")

      val exception = intercept[UpstreamErrorResponse](await(enrolmentStoreProxyConnector.queryGroupsWithEnrolment("XYPPT0000000283")))

      exception.statusCode mustBe 500
      getTimer(GroupsWithEnrolmentsTimerTag).getCount mustBe 1
    }

  }

  private def mockNoGroupsWithEnrolmentsResponse(): StubMapping =
    stubFor(
      get(urlMatching(s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey/groups"))
        .willReturn(
          aResponse()
            .withStatus(Status.NO_CONTENT)
        )
    )

  private def mockErrorGroupsWithEnrolmentsResponse(enrolmentKey: String): StubMapping =
    stubFor(
      get(urlMatching(s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey/groups"))
        .willReturn(
          aResponse()
            .withStatus(Status.INTERNAL_SERVER_ERROR)
        )
    )

  private def mockGroupsWithEnrolmentsResponse(
    response: String
  ): StubMapping =
    stubFor(
      get(urlMatching(s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey/groups"))
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
            .withBody(response)
        )
    )

}

object EnrolmentStoreProxyConnectorISpec {

  private val allGroupsWithEnrolmentsResponse =
    """
      |{
      |    "principalGroupIds": [
      |       "ABCEDEFGI1234567",
      |       "ABCEDEFGI1234568"
      |    ],
      |    "delegatedGroupIds": [
      |       "ABCEDEFGI1234567",
      |       "ABCEDEFGI1234568"
      |    ]
      |}
      |""".stripMargin

  private val principalGroupsWithEnrolmentsResponse =
    """
      |{
      |    "principalGroupIds": [
      |       "ABCEDEFGI1234567",
      |       "ABCEDEFGI1234568"
      |    ]
      |}
      |""".stripMargin

}
