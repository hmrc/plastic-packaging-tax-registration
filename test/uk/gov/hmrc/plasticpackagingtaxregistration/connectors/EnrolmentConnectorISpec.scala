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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalToJson, put, urlMatching}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers.await
import uk.gov.hmrc.plasticpackagingtaxregistration.base.Injector
import uk.gov.hmrc.plasticpackagingtaxregistration.base.it.ConnectorISpec
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.EnrolmentConnector.{
  EnrolmentConnectorTimerTag,
  PPTServiceName
}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.parsers.TaxEnrolmentsHttpParser.{
  FailedTaxEnrolment,
  SuccessfulTaxEnrolment
}

class EnrolmentConnectorISpec extends ConnectorISpec with Injector with ScalaFutures {

  private val enrolmentConnector = app.injector.instanceOf[EnrolmentConnector]

  "Enrolment Connector" should {
    val pptReference = "PPTRef"
    val safeId       = "SafeId"
    val formBundleId = "63535462345364"

    val requestPayload =
      Json.obj("serviceName" -> PPTServiceName,
               "callback"    -> s"http://localhost:8502/tax-enrolments-callback/$pptReference",
               "etmpId"      -> safeId
      )

    "enrol successfully" in {
      mockSuccessfulEnrolment(formBundleId, requestPayload)

      val enrolmentResponse =
        await(enrolmentConnector.submitEnrolment(pptReference, safeId, formBundleId))

      enrolmentResponse mustBe Right(SuccessfulTaxEnrolment)
      getTimer(EnrolmentConnectorTimerTag).getCount mustBe 1
    }

    "report enrolment failures" in {
      mockFailedEnrolment(formBundleId)

      val enrolmentResponse =
        await(enrolmentConnector.submitEnrolment(pptReference, safeId, formBundleId))

      enrolmentResponse mustBe Left(FailedTaxEnrolment(500))
    }
  }

  private def mockSuccessfulEnrolment(formBundleId: String, requestPayload: JsObject): StubMapping =
    stubFor(
      put(urlMatching(s"/tax-enrolments/subscriptions/$formBundleId/subscriber"))
        .withRequestBody(equalToJson(requestPayload.toString()))
        .willReturn(
          aResponse()
            .withStatus(Status.NO_CONTENT)
        )
    )

  private def mockFailedEnrolment(formBundleId: String): StubMapping =
    stubFor(
      put(urlMatching(s"/tax-enrolments/subscriptions/$formBundleId/subscriber"))
        .willReturn(
          aResponse()
            .withStatus(Status.INTERNAL_SERVER_ERROR)
        )
    )

}
