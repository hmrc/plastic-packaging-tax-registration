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

import java.time.{ZoneOffset, ZonedDateTime}

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post}
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.plasticpackagingtaxregistration.base.Injector
import uk.gov.hmrc.plasticpackagingtaxregistration.base.it.ConnectorISpec
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.EISError
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.{
  SubscriptionCreateFailureResponse,
  SubscriptionCreateFailureResponseWithStatusCode,
  SubscriptionCreateSuccessfulResponse
}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscriptionStatus.ETMPSubscriptionStatus.NO_FORM_BUNDLE_FOUND
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscriptionStatus.SubscriptionStatus.{
  NOT_SUBSCRIBED,
  UNKNOWN
}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscriptionStatus.SubscriptionStatusResponse

class SubscriptionsConnectorISpec extends ConnectorISpec with Injector with ScalaFutures {

  lazy val connector: SubscriptionsConnector = app.injector.instanceOf[SubscriptionsConnector]

  private val pptSubscriptionSubmissionTimer = "ppt.subscription.submission.timer"
  private val pptSubscriptionStatusTimer     = "ppt.subscription.status.timer"

  "Subscription connector" when {
    "requesting a subscription status" should {
      "handle a 200" in {
        stubFor(
          get("/cross-regime/subscription/PPT/SAFE/" + safeNumber + "/status")
            .willReturn(
              aResponse()
                .withStatus(Status.OK)
                .withBody(
                  Json.obj("subscriptionStatus" -> NO_FORM_BUNDLE_FOUND.toString,
                           "idType"             -> idType,
                           "idValue"            -> s"XXPPTP${safeNumber}789",
                           "channel"            -> "Online"
                  ).toString
                )
            )
        )

        val res: SubscriptionStatusResponse = await(connector.getSubscriptionStatus(safeNumber))

        res.status mustBe NOT_SUBSCRIBED
        res.pptReference mustBe Some("XXPPTP" + safeNumber + "789")

        getTimer(pptSubscriptionStatusTimer).getCount mustBe 1
      }

      "handle a 400" in {
        val errors =
          createErrorResponse(code = "INVALID_IDVALUE",
                              reason =
                                "Submission has not passed validation. Invalid parameter idValue."
          )

        stubSubscriptionStatusFailure(httpStatus = Status.BAD_REQUEST, errors = errors)

        val res: SubscriptionStatusResponse = await(connector.getSubscriptionStatus(safeNumber))
        res mustBe SubscriptionStatusResponse(UNKNOWN)

        getTimer(pptSubscriptionStatusTimer).getCount mustBe 1
      }

      "handle a 404" in {
        val errors =
          createErrorResponse(
            code = "NO_DATA_FOUND",
            reason =
              "The remote endpoint has indicated that the requested resource could  not be found."
          )

        stubSubscriptionStatusFailure(httpStatus = Status.NOT_FOUND, errors = errors)

        val res: SubscriptionStatusResponse = await(connector.getSubscriptionStatus(safeNumber))
        res mustBe SubscriptionStatusResponse(UNKNOWN)

        getTimer(pptSubscriptionStatusTimer).getCount mustBe 1
      }

      "handle a 500" in {
        val errors =
          createErrorResponse(code = "NO_DATA_FOUND",
                              reason =
                                "Dependent systems are currently not responding."
          )

        stubSubscriptionStatusFailure(httpStatus = Status.INTERNAL_SERVER_ERROR, errors = errors)

        val res: SubscriptionStatusResponse = await(connector.getSubscriptionStatus(safeNumber))
        res mustBe SubscriptionStatusResponse(UNKNOWN)

        getTimer(pptSubscriptionStatusTimer).getCount mustBe 1
      }

      "handle a 502" in {
        val errors =
          createErrorResponse(code = "BAD_GATEWAY",
                              reason =
                                "Dependent systems are currently not responding."
          )

        stubSubscriptionStatusFailure(httpStatus = Status.BAD_GATEWAY, errors = errors)

        val res: SubscriptionStatusResponse = await(connector.getSubscriptionStatus(safeNumber))
        res mustBe SubscriptionStatusResponse(UNKNOWN)

        getTimer(pptSubscriptionStatusTimer).getCount mustBe 1
      }

      "handle a 503" in {
        val errors =
          createErrorResponse(code = "SERVICE_UNAVAILABLE",
                              reason =
                                "Dependent systems are currently not responding."
          )

        stubSubscriptionStatusFailure(httpStatus = Status.SERVICE_UNAVAILABLE, errors = errors)

        val res: SubscriptionStatusResponse = await(connector.getSubscriptionStatus(safeNumber))
        res mustBe SubscriptionStatusResponse(UNKNOWN)

        getTimer(pptSubscriptionStatusTimer).getCount mustBe 1
      }
    }

    "submitting a subscription" should {
      "handle a 200" in {
        val pptReference               = "XDPPT123456789"
        val subscriptionProcessingDate = ZonedDateTime.now(ZoneOffset.UTC).toString
        val formBundleNumber           = "1234567890"
        stubFor(
          post(
            s"/plastic-packaging-tax/subscriptions/PPT/create?idType=SAFEID&idValue=${safeNumber}"
          )
            .willReturn(
              aResponse()
                .withStatus(Status.OK)
                .withBody(
                  Json.obj("pptReference"     -> pptReference,
                           "processingDate"   -> subscriptionProcessingDate,
                           "formBundleNumber" -> formBundleNumber
                  ).toString
                )
            )
        )

        val res: SubscriptionCreateSuccessfulResponse =
          await(connector.submitSubscription(safeNumber, ukLimitedCompaySubscription)).asInstanceOf[
            SubscriptionCreateSuccessfulResponse
          ]

        res.pptReference mustBe pptReference
        res.formBundleNumber mustBe formBundleNumber
        res.processingDate mustBe ZonedDateTime.parse(subscriptionProcessingDate)

        getTimer(pptSubscriptionSubmissionTimer).getCount mustBe 1
      }

      "handle a 400" in {
        val errors =
          createErrorResponse(code = "INVALID_IDVALUE",
                              reason =
                                "Submission has not passed validation. Invalid parameter idValue."
          )

        stubSubscriptionSubmissionFailure(httpStatus = Status.BAD_REQUEST, errors = errors)

        val resp = await(connector.submitSubscription(safeNumber, ukLimitedCompaySubscription))

        resp mustBe SubscriptionCreateFailureResponseWithStatusCode(
          SubscriptionCreateFailureResponse(
            List(
              EISError("INVALID_IDVALUE",
                       "Submission has not passed validation. Invalid parameter idValue."
              )
            )
          ),
          400
        )

        getTimer(pptSubscriptionSubmissionTimer).getCount mustBe 1
      }

      "handle a 409" in {
        val errors =
          createErrorResponse(
            code = "DUPLICATE_SUBMISSION",
            reason =
              "The remote endpoint has indicated that duplicate submission acknowledgment reference."
          )

        stubSubscriptionSubmissionFailure(httpStatus = Status.CONFLICT, errors = errors)

        val resp = await(connector.submitSubscription(safeNumber, ukLimitedCompaySubscription))

        resp mustBe SubscriptionCreateFailureResponseWithStatusCode(
          SubscriptionCreateFailureResponse(
            List(
              EISError("DUPLICATE_SUBMISSION",
                       "The remote endpoint has indicated that duplicate submission acknowledgment reference."
              )
            )
          ),
          409
        )

        getTimer(pptSubscriptionSubmissionTimer).getCount mustBe 1
      }

      "handle a 422" in {
        val errors =
          createErrorResponse(
            code = "ACTIVE_SUBSCRIPTION_EXISTS",
            reason =
              "The remote endpoint has indicated that Business Partner already has active subscription for this regime."
          )

        stubSubscriptionSubmissionFailure(httpStatus = Status.UNPROCESSABLE_ENTITY, errors = errors)

        val resp = await(connector.submitSubscription(safeNumber, ukLimitedCompaySubscription))

        resp mustBe SubscriptionCreateFailureResponseWithStatusCode(
          SubscriptionCreateFailureResponse(
            List(
              EISError("ACTIVE_SUBSCRIPTION_EXISTS",
                       "The remote endpoint has indicated that Business Partner already has active subscription for this regime."
              )
            )
          ),
          422
        )

        getTimer(pptSubscriptionSubmissionTimer).getCount mustBe 1
      }

      "handle a 500" in {
        val errors =
          createErrorResponse(code = "NO_DATA_FOUND",
                              reason =
                                "Dependent systems are currently not responding."
          )

        stubSubscriptionSubmissionFailure(httpStatus = Status.INTERNAL_SERVER_ERROR,
                                          errors = errors
        )

        val resp = await(connector.submitSubscription(safeNumber, ukLimitedCompaySubscription))

        resp mustBe SubscriptionCreateFailureResponseWithStatusCode(
          SubscriptionCreateFailureResponse(
            List(EISError("NO_DATA_FOUND", "Dependent systems are currently not responding."))
          ),
          500
        )

        getTimer(pptSubscriptionSubmissionTimer).getCount mustBe 1
      }

      "handle a 502" in {
        val errors =
          createErrorResponse(code = "BAD_GATEWAY",
                              reason =
                                "Dependent systems are currently not responding."
          )

        stubSubscriptionSubmissionFailure(httpStatus = Status.BAD_GATEWAY, errors = errors)

        val resp = await(connector.submitSubscription(safeNumber, ukLimitedCompaySubscription))

        resp mustBe SubscriptionCreateFailureResponseWithStatusCode(
          SubscriptionCreateFailureResponse(
            List(EISError("BAD_GATEWAY", "Dependent systems are currently not responding."))
          ),
          502
        )

        getTimer(pptSubscriptionSubmissionTimer).getCount mustBe 1
      }

      "handle a 503" in {
        val errors =
          createErrorResponse(code = "SERVICE_UNAVAILABLE",
                              reason =
                                "Dependent systems are currently not responding."
          )

        stubSubscriptionSubmissionFailure(httpStatus = Status.SERVICE_UNAVAILABLE, errors = errors)

        val resp = await(connector.submitSubscription(safeNumber, ukLimitedCompaySubscription))

        resp mustBe SubscriptionCreateFailureResponseWithStatusCode(
          SubscriptionCreateFailureResponse(
            List(EISError("SERVICE_UNAVAILABLE", "Dependent systems are currently not responding."))
          ),
          503
        )

        getTimer(pptSubscriptionSubmissionTimer).getCount mustBe 1
      }

      "return 500 for malformed successful responses" in {
        stubFor(
          post(
            s"/plastic-packaging-tax/subscriptions/PPT/create?idType=SAFEID&idValue=${safeNumber}"
          )
            .willReturn(
              aResponse()
                .withStatus(Status.OK)
                .withBody(Json.obj("xxx" -> "xxx").toString)
            )
        )

        intercept[UpstreamErrorResponse] {
          await(connector.submitSubscription(safeNumber, ukLimitedCompaySubscription))
        }.statusCode mustBe Status.INTERNAL_SERVER_ERROR
      }

      "return 500 for malformed failed responses" in {
        stubFor(
          post(
            s"/plastic-packaging-tax/subscriptions/PPT/create?idType=SAFEID&idValue=${safeNumber}"
          )
            .willReturn(
              aResponse()
                .withStatus(Status.CONFLICT)
                .withBody(Json.obj("xxx" -> "xxx").toString)
            )
        )

        intercept[UpstreamErrorResponse] {
          await(connector.submitSubscription(safeNumber, ukLimitedCompaySubscription))
        }.statusCode mustBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  private def createErrorResponse(code: String, reason: String): Seq[EISError] =
    Seq(EISError(code, reason))

  private def stubSubscriptionStatusFailure(httpStatus: Int, errors: Seq[EISError]): Any =
    stubFor(
      get(s"/cross-regime/subscription/PPT/SAFE/${safeNumber}/status")
        .willReturn(
          aResponse()
            .withStatus(httpStatus)
            .withBody(Json.obj("failures" -> errors).toString)
        )
    )

  private def stubSubscriptionSubmissionFailure(httpStatus: Int, errors: Seq[EISError]): Any =
    stubFor(
      post(s"/plastic-packaging-tax/subscriptions/PPT/create?idType=SAFEID&idValue=${safeNumber}")
        .willReturn(
          aResponse()
            .withStatus(httpStatus)
            .withBody(Json.obj("failures" -> errors).toString)
        )
    )

}
