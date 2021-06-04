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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get}
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.plasticpackagingtaxregistration.base.Injector
import uk.gov.hmrc.plasticpackagingtaxregistration.base.it.ConnectorISpec
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.subscriptionStatus.ETMPSubscriptionStatus.NO_FORM_BUNDLE_FOUND
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.subscriptionStatus.{
  SubscriptionStatusError,
  SubscriptionStatusResponse
}

class SubscriptionsConnectorISpec extends ConnectorISpec with Injector with ScalaFutures {

  lazy val connector: SubscriptionsConnector = app.injector.instanceOf[SubscriptionsConnector]

  private val idType     = "ZPPT"
  private val safeNumber = "123456"

  "Subscription connector" when {
    "requesting a subscription status" should {
      "handle a 200" in {
        stubFor(
          get("/cross-regime/subscription/ZPPT/SAFE/" + safeNumber + "/status")
            .willReturn(
              aResponse()
                .withStatus(Status.OK)
                .withBody(
                  Json.obj("subscriptionStatus" -> NO_FORM_BUNDLE_FOUND.toString,
                           "idType"             -> idType,
                           "idValue"            -> s"XXPPTP${safeNumber}789"
                  ).toString
                )
            )
        )

        val res: SubscriptionStatusResponse = await(connector.getSubscriptionStatus(safeNumber))

        res.idType mustBe Some(idType)
        res.idValue mustBe Some("XXPPTP" + safeNumber + "789")
        res.subscriptionStatus mustBe Some(NO_FORM_BUNDLE_FOUND)
        res.failures mustBe None

        getTimer("ppt.subscription.status.timer").getCount mustBe 1
      }

      "handle a 400" in {
        val errors =
          createErrorResponse(code = "INVALID_IDVALUE",
                              reason =
                                "Submission has not passed validation. Invalid parameter idValue."
          )

        stubSubscriptionStatusFailure(httpStatus = Status.BAD_REQUEST, errors = errors)

        intercept[Exception] {
          await(connector.getSubscriptionStatus(safeNumber))
        }
        getTimer("ppt.subscription.status.timer").getCount mustBe 1
      }

      "handle a 404" in {
        val errors =
          createErrorResponse(
            code = "NO_DATA_FOUND",
            reason =
              "The remote endpoint has indicated that the requested resource could  not be found."
          )

        stubSubscriptionStatusFailure(httpStatus = Status.NOT_FOUND, errors = errors)

        intercept[Exception] {
          await(connector.getSubscriptionStatus(safeNumber))
        }
        getTimer("ppt.subscription.status.timer").getCount mustBe 1
      }

      "handle a 500" in {
        val errors =
          createErrorResponse(code = "NO_DATA_FOUND",
                              reason =
                                "Dependent systems are currently not responding."
          )

        stubSubscriptionStatusFailure(httpStatus = Status.INTERNAL_SERVER_ERROR, errors = errors)

        intercept[Exception] {
          await(connector.getSubscriptionStatus(safeNumber))
        }
        getTimer("ppt.subscription.status.timer").getCount mustBe 1
      }

      "handle a 502" in {
        val errors =
          createErrorResponse(code = "BAD_GATEWAY",
                              reason =
                                "Dependent systems are currently not responding."
          )

        stubSubscriptionStatusFailure(httpStatus = Status.BAD_GATEWAY, errors = errors)

        intercept[Exception] {
          await(connector.getSubscriptionStatus(safeNumber))
        }
        getTimer("ppt.subscription.status.timer").getCount mustBe 1
      }

      "handle a 503" in {
        val errors =
          createErrorResponse(code = "SERVICE_UNAVAILABLE",
                              reason =
                                "Dependent systems are currently not responding."
          )

        stubSubscriptionStatusFailure(httpStatus = Status.SERVICE_UNAVAILABLE, errors = errors)

        intercept[Exception] {
          await(connector.getSubscriptionStatus(safeNumber))
        }
        getTimer("ppt.subscription.status.timer").getCount mustBe 1
      }
    }
  }

  private def createErrorResponse(code: String, reason: String): Seq[SubscriptionStatusError] =
    Seq(SubscriptionStatusError(code, reason))

  private def stubSubscriptionStatusFailure(
    httpStatus: Int,
    errors: Seq[SubscriptionStatusError]
  ): Any =
    stubFor(
      get("/cross-regime/subscription/ZPPT/SAFE/" + safeNumber + "/status")
        .willReturn(
          aResponse()
            .withStatus(httpStatus)
            .withBody(Json.obj("failures" -> errors).toString)
        )
    )

}
