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

package uk.gov.hmrc.plasticpackagingtaxregistration.connectors

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, put}
import org.scalatest.Inspectors.forAll
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.http.Status.{CONFLICT, OK}
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.plasticpackagingtaxregistration.base.Injector
import uk.gov.hmrc.plasticpackagingtaxregistration.base.data.SubscriptionTestData
import uk.gov.hmrc.plasticpackagingtaxregistration.base.it.ConnectorISpec
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.EISError
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.Subscription
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.create.{
  SubscriptionFailureResponse,
  SubscriptionFailureResponseWithStatusCode,
  SubscriptionSuccessfulResponse
}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscriptionStatus.ETMPSubscriptionStatus.NO_FORM_BUNDLE_FOUND
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscriptionStatus.SubscriptionStatus.{
  NOT_SUBSCRIBED,
  UNKNOWN
}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscriptionStatus.SubscriptionStatusResponse

import java.time.{ZoneOffset, ZonedDateTime}
import java.util.UUID

class SubscriptionsConnectorISpec
    extends ConnectorISpec with Injector with ScalaFutures with SubscriptionTestData {

  lazy val connector: SubscriptionsConnector = app.injector.instanceOf[SubscriptionsConnector]

  private val pptSubscriptionSubmissionTimer = "ppt.subscription.submission.timer"
  private val pptSubscriptionStatusTimer     = "ppt.subscription.status.timer"
  private val pptSubscriptionDisplayTimer    = "ppt.subscription.display.timer"
  private val pptSubscriptionUpdateTimer     = "ppt.subscription.update.timer"

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

        val res = await(connector.getSubscriptionStatus(safeNumber)).right.get

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

        val res = await(connector.getSubscriptionStatus(safeNumber)).left.get
        res mustBe Status.BAD_REQUEST

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

        val res = await(connector.getSubscriptionStatus(safeNumber)).right.get
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

        val res = await(connector.getSubscriptionStatus(safeNumber)).left.get
        res mustBe Status.INTERNAL_SERVER_ERROR

        getTimer(pptSubscriptionStatusTimer).getCount mustBe 1
      }

      "handle a 502" in {
        val errors =
          createErrorResponse(code = "BAD_GATEWAY",
                              reason =
                                "Dependent systems are currently not responding."
          )

        stubSubscriptionStatusFailure(httpStatus = Status.BAD_GATEWAY, errors = errors)

        val res = await(connector.getSubscriptionStatus(safeNumber)).left.get
        res mustBe Status.BAD_GATEWAY

        getTimer(pptSubscriptionStatusTimer).getCount mustBe 1
      }

      "handle a 503" in {
        val errors =
          createErrorResponse(code = "SERVICE_UNAVAILABLE",
                              reason =
                                "Dependent systems are currently not responding."
          )

        stubSubscriptionStatusFailure(httpStatus = Status.SERVICE_UNAVAILABLE, errors = errors)

        val res = await(connector.getSubscriptionStatus(safeNumber)).left.get
        res mustBe Status.SERVICE_UNAVAILABLE

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
                  Json.obj("pptReferenceNumber" -> pptReference,
                           "processingDate"     -> subscriptionProcessingDate,
                           "formBundleNumber"   -> formBundleNumber
                  ).toString
                )
            )
        )

        val res: SubscriptionSuccessfulResponse =
          await(
            connector.submitSubscription(safeNumber, ukLimitedCompanySubscription)
          ).asInstanceOf[SubscriptionSuccessfulResponse]

        res.pptReferenceNumber mustBe pptReference
        res.formBundleNumber mustBe formBundleNumber
        res.processingDate mustBe ZonedDateTime.parse(subscriptionProcessingDate)

        getTimer(pptSubscriptionSubmissionTimer).getCount mustBe 1
      }

      forAll(Seq(400, 404, 422, 409, 500, 502, 503)) { statusCode =>
        "return " + statusCode when {
          statusCode + " is returned from downstream service" in {
            val errors =
              createErrorResponse(code = statusCode.toString,
                                  reason =
                                    "Error reason."
              )

            stubSubscriptionSubmissionFailure(httpStatus = statusCode, errors = errors)

            val resp = await(connector.submitSubscription(safeNumber, ukLimitedCompanySubscription))

            resp mustBe SubscriptionFailureResponseWithStatusCode(
              SubscriptionFailureResponse(List(EISError(statusCode.toString, "Error reason."))),
              statusCode
            )

            getTimer(pptSubscriptionSubmissionTimer).getCount mustBe 1
          }
        }
      }

      "return 500 for malformed successful responses" in {
        stubSubscriptionSubmitException(OK)

        intercept[UpstreamErrorResponse] {
          await(connector.submitSubscription(safeNumber, ukLimitedCompanySubscription))
        }.statusCode mustBe Status.INTERNAL_SERVER_ERROR
      }

      "return 500 for malformed failed responses" in {
        stubSubscriptionSubmitException(CONFLICT)

        intercept[UpstreamErrorResponse] {
          await(connector.submitSubscription(safeNumber, ukLimitedCompanySubscription))
        }.statusCode mustBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "submitting a group subscription" should {
      "handle a 200" in {
        val pptReference               = "XDPPT123456789"
        val subscriptionProcessingDate = ZonedDateTime.now(ZoneOffset.UTC).toString
        val formBundleNumber           = "1234567890"
        stubFor(
          post(s"/plastic-packaging-tax/subscriptions/PPT/create")
            .willReturn(
              aResponse()
                .withStatus(Status.OK)
                .withBody(
                  Json.obj("pptReferenceNumber" -> pptReference,
                           "processingDate"     -> subscriptionProcessingDate,
                           "formBundleNumber"   -> formBundleNumber
                  ).toString
                )
            )
        )

        val res: SubscriptionSuccessfulResponse =
          await(
            connector.submitSubscription(safeNumber, ukLimitedCompanyGroupSubscription)
          ).asInstanceOf[SubscriptionSuccessfulResponse]

        res.pptReferenceNumber mustBe pptReference
        res.formBundleNumber mustBe formBundleNumber
        res.processingDate mustBe ZonedDateTime.parse(subscriptionProcessingDate)

        getTimer(pptSubscriptionSubmissionTimer).getCount mustBe 1
      }
    }

    "requesting a subscription" should {
      "handle a 200" in {

        val pptReference = UUID.randomUUID().toString
        stubSubscriptionDisplay(pptReference, ukLimitedCompanySubscription)

        val res: Either[Int, Subscription] = await(connector.getSubscription(pptReference))

        res.toOption mustBe Some(ukLimitedCompanySubscription)

        getTimer(pptSubscriptionDisplayTimer).getCount mustBe 1
      }

      forAll(Seq(400, 404, 422, 409, 500, 502, 503)) { statusCode =>
        "return " + statusCode when {
          statusCode + " is returned from downstream service" in {
            val pptReference = UUID.randomUUID().toString
            val errors =
              createErrorResponse(code = "INVALID_VALUE",
                                  reason =
                                    "Some errors occurred"
              )

            stubSubscriptionDisplayFailure(httpStatus = statusCode,
                                           errors = errors,
                                           pptReference = pptReference
            )

            val res = await(connector.getSubscription(pptReference))

            res.left.get mustBe statusCode
            getTimer(pptSubscriptionDisplayTimer).getCount mustBe 1
          }
        }
      }

    }

    "updating a subscription" should {
      "handle a 200" in {
        val pptReference                       = "XDPPT123456789"
        val subscriptionProcessingDate: String = ZonedDateTime.now(ZoneOffset.UTC).toString
        val formBundleNumber                   = "1234567890"
        stubFor(
          put(s"/plastic-packaging-tax/subscriptions/PPT/${pptReference}/update")
            .willReturn(
              aResponse()
                .withStatus(Status.OK)
                .withBody(
                  Json.obj("pptReferenceNumber" -> pptReference,
                           "processingDate"     -> subscriptionProcessingDate,
                           "formBundleNumber"   -> formBundleNumber
                  ).toString
                )
            )
        )

        val res: SubscriptionSuccessfulResponse =
          await(
            connector.updateSubscription(pptReference, ukLimitedCompanySubscription)
          ).asInstanceOf[SubscriptionSuccessfulResponse]

        res.pptReferenceNumber mustBe pptReference
        res.formBundleNumber mustBe formBundleNumber
        res.processingDate mustBe ZonedDateTime.parse(subscriptionProcessingDate)

        getTimer(pptSubscriptionUpdateTimer).getCount mustBe 1
      }

      forAll(Seq(400, 404, 422, 409, 500, 502, 503)) { statusCode =>
        "return " + statusCode when {
          statusCode + " is returned from downstream service" in {
            val errors =
              createErrorResponse(code = statusCode.toString,
                                  reason =
                                    "Error reason."
              )

            stubSubscriptionUpdateFailure(httpStatus = statusCode, errors = errors)

            val resp =
              await(connector.updateSubscription(pptReference, ukLimitedCompanySubscription))

            resp mustBe SubscriptionFailureResponseWithStatusCode(
              SubscriptionFailureResponse(List(EISError(statusCode.toString, "Error reason."))),
              statusCode
            )
            getTimer(pptSubscriptionUpdateTimer).getCount mustBe 1
          }
        }
      }

      "return 500 for malformed successful responses" in {
        stubSubscriptionUpdateException(Status.OK)

        intercept[UpstreamErrorResponse] {
          await(connector.updateSubscription(pptReference, ukLimitedCompanySubscription))
        }.statusCode mustBe Status.INTERNAL_SERVER_ERROR
      }

      "return 500 for malformed failed responses" in {
        stubSubscriptionUpdateException(Status.CONFLICT)

        intercept[UpstreamErrorResponse] {
          await(connector.updateSubscription(pptReference, ukLimitedCompanySubscription))
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

  private def stubSubscriptionDisplay(pptReference: String, response: Subscription): Unit =
    stubFor(
      get(s"/plastic-packaging-tax/subscriptions/PPT/$pptReference/display")
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
            .withBody(Subscription.format.writes(response).toString())
        )
    )

  private def stubSubscriptionDisplayFailure(
    pptReference: String,
    httpStatus: Int,
    errors: Seq[EISError]
  ): Any =
    stubFor(
      get(s"/plastic-packaging-tax/subscriptions/PPT/$pptReference/display")
        .willReturn(
          aResponse()
            .withStatus(httpStatus)
            .withBody(Json.obj("failures" -> errors).toString)
        )
    )

  private def stubSubscriptionUpdateFailure(httpStatus: Int, errors: Seq[EISError]): Any =
    stubFor(
      put(s"/plastic-packaging-tax/subscriptions/PPT/${pptReference}/update")
        .willReturn(
          aResponse()
            .withStatus(httpStatus)
            .withBody(Json.obj("failures" -> errors).toString)
        )
    )

  private def stubSubscriptionUpdateException(httpStatus: Int): Any =
    stubFor(
      put(s"/plastic-packaging-tax/subscriptions/PPT/${pptReference}/update")
        .willReturn(
          aResponse()
            .withStatus(httpStatus)
            .withBody(Json.obj("xxx" -> "xxx").toString)
        )
    )

  private def stubSubscriptionSubmitException(httpStatus: Int): Any =
    stubFor(
      post(s"/plastic-packaging-tax/subscriptions/PPT/create?idType=SAFEID&idValue=${safeNumber}")
        .willReturn(
          aResponse()
            .withStatus(httpStatus)
            .withBody(Json.obj("xxx" -> "xxx").toString)
        )
    )

}
