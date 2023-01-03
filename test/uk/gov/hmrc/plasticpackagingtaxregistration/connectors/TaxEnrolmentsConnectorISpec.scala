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

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers.await
import uk.gov.hmrc.http.UpstreamErrorResponse
import base.Injector
import base.data.UserEnrolmentData
import base.it.ConnectorISpec
import connectors.TaxEnrolmentsConnector.{
  AssignEnrolmentToGroupTimerTag,
  AssignEnrolmentToUserTimerTag,
  SubscriberTimerTag
}
import models.KeyValue.{
  etmpPptReferenceKey,
  pptServiceName
}
import connectors.parsers.TaxEnrolmentsHttpParser.{
  FailedTaxEnrolment,
  SuccessfulTaxEnrolment
}

class TaxEnrolmentsConnectorISpec
    extends ConnectorISpec with Injector with ScalaFutures with TableDrivenPropertyChecks
    with UserEnrolmentData {

  private val enrolmentConnector = app.injector.instanceOf[TaxEnrolmentsConnector]

  "Tax Enrolments Connector" when {
    val pptReference = "XMPPT000123456"

    "enrolling via subscriber call" should {
      val safeId       = "SafeId"
      val formBundleId = "63535462345364"

      val requestPayload =
        Json.obj("serviceName" -> pptServiceName,
                 "callback"    -> s"http://localhost:8502/tax-enrolments-callback/$pptReference",
                 "etmpId"      -> safeId
        )

      "succeed when tax-enrolments returns 204 NO CONTENT" in {
        mockSuccessfulSubscriberEnrolment(formBundleId, requestPayload)

        val enrolmentResponse =
          await(enrolmentConnector.submitEnrolment(pptReference, safeId, formBundleId))

        enrolmentResponse mustBe Right(SuccessfulTaxEnrolment)
        getTimer(SubscriberTimerTag).getCount mustBe 1
      }

      "report enrolment failures" in {
        mockFailedSubscriberEnrolment(formBundleId)

        val enrolmentResponse =
          await(enrolmentConnector.submitEnrolment(pptReference, safeId, formBundleId))

        enrolmentResponse mustBe Left(FailedTaxEnrolment(500))
      }
    }

    "assigning user to existing enrolment" should {
      val userId = "user123"

      "succeed when tax-enrolments returns a 201 CREATED" in {
        val enrolmentKey = s"$pptServiceName~$etmpPptReferenceKey~$pptReference"
        mockSuccessfulUserEnrolmentAssignment(userId, enrolmentKey)

        await(enrolmentConnector.assignEnrolmentToUser(userId, pptReference))

        getTimer(AssignEnrolmentToUserTimerTag).getCount mustBe 1
      }

      "throw UpstreamErrorResponse when tax-enrolments returns something other than 201 CREATED" in {
        val enrolmentKey = s"$pptServiceName~$etmpPptReferenceKey~$pptReference"

        val failureStatuses = Table("status",
                                    Status.UNAUTHORIZED,
                                    Status.NOT_FOUND,
                                    Status.FORBIDDEN,
                                    Status.BAD_REQUEST
        )

        forAll(failureStatuses) { failureStatus =>
          mockFailedUserEnrolmentAssignment(userId, enrolmentKey, failureStatus)

          intercept[UpstreamErrorResponse] {
            await(enrolmentConnector.assignEnrolmentToUser(userId, pptReference))
          }
        }
        getTimer(AssignEnrolmentToUserTimerTag).getCount mustBe failureStatuses.size
      }
    }

    "allocate enrolment to group" should {
      val userId           = "user123"
      val groupId          = "group123"
      val enrolmentRequest = userEnrolmentRequest.copy(pptReference = pptReference)
      val enrolmentKey     = s"$pptServiceName~$etmpPptReferenceKey~$pptReference"

      "succeed when tax-enrolments returns a 201 CREATED" in {

        mockSuccessfulAllocateEnrolmentToGroup(groupId, enrolmentKey)

        await(enrolmentConnector.assignEnrolmentToGroup(userId, groupId, enrolmentRequest))

        getTimer(AssignEnrolmentToGroupTimerTag).getCount mustBe 1
      }

      "throw UpstreamErrorResponse when tax-enrolments returns something other than 201 CREATED" in {
        val enrolmentKey = s"$pptServiceName~$etmpPptReferenceKey~$pptReference"

        val failureStatuses = Table("status",
                                    Status.UNAUTHORIZED,
                                    Status.NOT_FOUND,
                                    Status.FORBIDDEN,
                                    Status.BAD_REQUEST
        )

        forAll(failureStatuses) { failureStatus =>
          mockFailedAllocateEnrolmentToGroup(groupId, enrolmentKey, failureStatus)

          intercept[UpstreamErrorResponse] {
            await(enrolmentConnector.assignEnrolmentToGroup(userId, groupId, enrolmentRequest))
          }
        }
        getTimer(AssignEnrolmentToGroupTimerTag).getCount mustBe failureStatuses.size
      }
    }
  }

  private def mockSuccessfulSubscriberEnrolment(
    formBundleId: String,
    requestPayload: JsObject
  ): StubMapping =
    stubFor(
      put(urlMatching(s"/tax-enrolments/subscriptions/$formBundleId/subscriber"))
        .withRequestBody(equalToJson(requestPayload.toString()))
        .willReturn(
          aResponse()
            .withStatus(Status.NO_CONTENT)
        )
    )

  private def mockFailedSubscriberEnrolment(formBundleId: String): StubMapping =
    stubFor(
      put(urlMatching(s"/tax-enrolments/subscriptions/$formBundleId/subscriber"))
        .willReturn(
          aResponse()
            .withStatus(Status.INTERNAL_SERVER_ERROR)
        )
    )

  private def mockSuccessfulUserEnrolmentAssignment(userId: String, enrolmentKey: String) =
    stubFor(
      post(urlMatching(s"/tax-enrolments/users/$userId/enrolments/$enrolmentKey"))
        .willReturn(
          aResponse()
            .withStatus(Status.CREATED)
        )
    )

  private def mockFailedUserEnrolmentAssignment(userId: String, enrolmentKey: String, status: Int) =
    stubFor(
      post(urlMatching(s"/tax-enrolments/users/$userId/enrolments/$enrolmentKey"))
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )

  private def mockSuccessfulAllocateEnrolmentToGroup(groupId: String, enrolmentKey: String) =
    stubFor(
      post(urlMatching(s"/tax-enrolments/groups/$groupId/enrolments/$enrolmentKey"))
        .willReturn(
          aResponse()
            .withStatus(Status.CREATED)
        )
    )

  private def mockFailedAllocateEnrolmentToGroup(
    groupId: String,
    enrolmentKey: String,
    status: Int
  ) =
    stubFor(
      post(urlMatching(s"/tax-enrolments/groups/$groupId/enrolments/$enrolmentKey"))
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )

}
