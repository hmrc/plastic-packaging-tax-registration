/*
 * Copyright 2026 HM Revenue & Customs
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

import base.Injector
import base.data.SubscriptionTestData
import base.it.ConnectorISpec
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, put}
import connectors.HipSubscriptionsConnector
import org.scalatest.EitherValues
import org.scalatest.Inspectors.forAll
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers.await
import models.eis.EISError
import models.eis.subscription.Subscription
import models.eis.subscription.create.{
  SubscriptionFailureResponseWithStatusCode,
  SubscriptionSuccessfulResponse
}
import uk.gov.hmrc.http.UpstreamErrorResponse

import java.time.{ZoneOffset, ZonedDateTime}
import java.util.UUID

class HipSubscriptionConnectorSpec
    extends ConnectorISpec with Injector with ScalaFutures with SubscriptionTestData
    with EitherValues {

  override def overrideConfig: Map[String, Any] =
    Map(
      "microservice.services.hip.host"                   -> wireHost,
      "microservice.services.hip.port"                   -> wirePort,
      "microservice.services.nrs.host"                   -> wireHost,
      "microservice.services.nrs.port"                   -> wirePort,
      "microservice.services.tax-enrolments.host"        -> wireHost,
      "microservice.services.tax-enrolments.port"        -> wirePort,
      "microservice.services.enrolment-store-proxy.port" -> wirePort
    )

  private lazy val connector =
    app.injector.instanceOf[HipSubscriptionsConnector]

  private val pptSubscriptionUpdateTimer  = "ppt.subscription.update.timer"
  private val pptSubscriptionDisplayTimer = "ppt.subscription.display.timer"

  "Subscription connector" when {

    "requesting a subscription" should {
      "handle a 200" in {

        val pptReference = UUID.randomUUID().toString
        stubSubscriptionDisplay(pptReference, ukLimitedCompanySubscription)

        val res: Either[Int, Subscription] = await(connector.getSubscription(pptReference))

        res.toOption mustBe Some(ukLimitedCompanySubscription)

        getTimer(pptSubscriptionDisplayTimer).getCount mustBe 1
      }

      forAll(Seq(400, 404, 422, 500)) { statusCode =>
        s"return $statusCode" when {
          s"$statusCode is returned from downstream service" in {
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

            res.left.value mustBe statusCode
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
          put(s"/etmp/RESTAdapter/plastic-packaging-tax/subscriptions/PPT/$pptReference")
            .willReturn(
              aResponse()
                .withStatus(Status.OK)
                .withBody(
                  s"""
                     |{
                     |  "success": {
                     |    "formBundleNumber": "$formBundleNumber",
                     |    "pptReferenceNumber": "$pptReference",
                     |    "processingDate": "$subscriptionProcessingDate"
                     |  }
                     |}
                     |""".stripMargin
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

      "handle a 400 with a single error payload" in {
        val pptReference = "XDPPT123456789"
        stubFor(
          put(s"/etmp/RESTAdapter/plastic-packaging-tax/subscriptions/PPT/$pptReference")
            .willReturn(
              aResponse()
                .withStatus(Status.BAD_REQUEST)
                .withBody(
                  """
                    |{
                    |  "origin": "HoD",
                    |  "response": {
                    |    "error": {
                    |      "code": "400",
                    |      "logID": "9",
                    |      "message": "foobar"
                    |    }
                    |  }
                    |}
                    |""".stripMargin
                )
            )
        )

        val res: SubscriptionFailureResponseWithStatusCode =
          await(
            connector.updateSubscription(pptReference, ukLimitedCompanySubscription)
          ).asInstanceOf[SubscriptionFailureResponseWithStatusCode]

        res.statusCode mustBe 400
        res.failureResponse.failures.head.code mustBe "400"
        res.failureResponse.failures.head.reason mustBe "foobar"
      }
      "handle a 500 with an array of failures payload" in {
        forAll(Seq(500, 503)) { status =>
          val pptReference = "XDPPT123456789"
          stubFor(
            put(s"/etmp/RESTAdapter/plastic-packaging-tax/subscriptions/PPT/$pptReference")
              .willReturn(
                aResponse()
                  .withStatus(status)
                  .withBody(
                    """
                      |{
                      |  "origin": "HIP",
                      |  "response": {
                      |    "failures": [
                      |      {
                      |        "type": "Type of Failure",
                      |        "reason": "Reason for Failure"
                      |      }
                      |    ]
                      |  }
                      |}
                      |""".stripMargin
                  )
              )
          )

          val res: SubscriptionFailureResponseWithStatusCode =
            await(
              connector.updateSubscription(pptReference, ukLimitedCompanySubscription)
            ).asInstanceOf[SubscriptionFailureResponseWithStatusCode]

          res.statusCode mustBe status
          res.failureResponse.failures.head.code mustBe "Type of Failure"
          res.failureResponse.failures.head.reason mustBe "Reason for Failure"
        }
      }
      "handle a errors with unreadable payload" in {
        forAll(Seq(500, 503)) { status =>
          val pptReference = "XDPPT123456789"
          stubFor(
            put(s"/etmp/RESTAdapter/plastic-packaging-tax/subscriptions/PPT/$pptReference")
              .willReturn(
                aResponse()
                  .withStatus(status)
                  .withBody(
                    """
                      |{ "foo": "bar" }
                      |""".stripMargin
                  )
              )
          )

          val err = intercept[UpstreamErrorResponse] {
            await(
              connector.updateSubscription(pptReference, ukLimitedCompanySubscription)
            )
          }
          err.statusCode mustBe Status.INTERNAL_SERVER_ERROR
          assert(err.message.contains("failed - error response in unexpected format"))
        }
      }
      "handle a 422 001" in {
        val pptReference = "XDPPT123456789"
        stubFor(
          put(s"/etmp/RESTAdapter/plastic-packaging-tax/subscriptions/PPT/$pptReference")
            .willReturn(
              aResponse()
                .withStatus(Status.UNPROCESSABLE_ENTITY)
                .withBody(
                  """
                    |{
                    |  "error": {
                    |    "errorId": "001",
                    |    "processingDate": "2026-07-09T09:26:17Z",
                    |    "text": "REGIME missing or invalid"
                    |  }
                    |}
                    |""".stripMargin
                )
            )
        )

        val res: SubscriptionFailureResponseWithStatusCode =
          await(
            connector.updateSubscription(pptReference, ukLimitedCompanySubscription)
          ).asInstanceOf[SubscriptionFailureResponseWithStatusCode]

        res.statusCode mustBe 422
        res.failureResponse.failures.head.code mustBe "INVALID_REGIME"
        res.failureResponse.failures.head.reason mustBe "The remote endpoint has indicated that the REGIME provided is invalid."
      }
      "handle a 422 004" in {
        val pptReference = "XDPPT123456789"
        stubFor(
          put(s"/etmp/RESTAdapter/plastic-packaging-tax/subscriptions/PPT/$pptReference")
            .willReturn(
              aResponse()
                .withStatus(Status.UNPROCESSABLE_ENTITY)
                .withBody(
                  """
                    |{
                    |  "error": {
                    |    "errorId": "004",
                    |    "processingDate": "2026-07-09T09:26:17Z",
                    |    "text": "Duplicate submission acknowledgment reference"
                    |  }
                    |}
                    |""".stripMargin
                )
            )
        )

        val res: SubscriptionFailureResponseWithStatusCode =
          await(
            connector.updateSubscription(pptReference, ukLimitedCompanySubscription)
          ).asInstanceOf[SubscriptionFailureResponseWithStatusCode]

        res.statusCode mustBe 409
        res.failureResponse.failures.head.code mustBe "DUPLICATE_SUBMISSION"
        res.failureResponse.failures.head.reason mustBe "The remote endpoint has indicated that duplicate submission acknowledgment reference."
      }
      "handle a 422 087" in {
        val pptReference = "XDPPT123456789"
        stubFor(
          put(s"/etmp/RESTAdapter/plastic-packaging-tax/subscriptions/PPT/$pptReference")
            .willReturn(
              aResponse()
                .withStatus(Status.UNPROCESSABLE_ENTITY)
                .withBody(
                  """
                    |{
                    |  "error": {
                    |    "errorId": "087",
                    |    "processingDate": "2026-07-09T09:26:17Z",
                    |    "text": "???"
                    |  }
                    |}
                    |""".stripMargin
                )
            )
        )

        val res: SubscriptionFailureResponseWithStatusCode =
          await(
            connector.updateSubscription(pptReference, ukLimitedCompanySubscription)
          ).asInstanceOf[SubscriptionFailureResponseWithStatusCode]

        res.statusCode mustBe 422
        res.failureResponse.failures.head.code mustBe "BUSINESS_VALIDATION"
        res.failureResponse.failures.head.reason mustBe "The remote endpoint has indicated cannot Create Group Subscription."
      }
      "handle a 422 089" in {
        val pptReference = "XDPPT123456789"
        stubFor(
          put(s"/etmp/RESTAdapter/plastic-packaging-tax/subscriptions/PPT/$pptReference")
            .willReturn(
              aResponse()
                .withStatus(Status.UNPROCESSABLE_ENTITY)
                .withBody(
                  """
                    |{
                    |  "error": {
                    |    "errorId": "089",
                    |    "processingDate": "2026-07-09T09:26:17Z",
                    |    "text": "ID Number missing or invalid"
                    |  }
                    |}
                    |""".stripMargin
                )
            )
        )

        val res: SubscriptionFailureResponseWithStatusCode =
          await(
            connector.updateSubscription(pptReference, ukLimitedCompanySubscription)
          ).asInstanceOf[SubscriptionFailureResponseWithStatusCode]

        res.statusCode mustBe 422
        res.failureResponse.failures.head.code mustBe "INVALID_PPT_REFERENCE_NUMBER"
        res.failureResponse.failures.head.reason mustBe "The remote endpoint has indicated that the PPT Reference Number provided is invalid."
      }
      "handle a 422 090" in {
        val pptReference = "XDPPT123456789"
        stubFor(
          put(s"/etmp/RESTAdapter/plastic-packaging-tax/subscriptions/PPT/$pptReference")
            .willReturn(
              aResponse()
                .withStatus(Status.UNPROCESSABLE_ENTITY)
                .withBody(
                  """
                    |{
                    |  "error": {
                    |    "errorId": "090",
                    |    "processingDate": "2026-07-09T09:26:17Z",
                    |    "text": "Cannot Create Partnership Subscription"
                    |  }
                    |}
                    |""".stripMargin
                )
            )
        )

        val res: SubscriptionFailureResponseWithStatusCode =
          await(
            connector.updateSubscription(pptReference, ukLimitedCompanySubscription)
          ).asInstanceOf[SubscriptionFailureResponseWithStatusCode]

        res.statusCode mustBe 422
        res.failureResponse.failures.head.code mustBe "CANNOT_CREATE_PARTNERSHIP_SUBSCRIPTION"
        res.failureResponse.failures.head.reason mustBe "The remote end point has indicated cannot Create Partnership Subscription."
      }
      "handle a 422 999" in {
        val pptReference = "XDPPT123456789"
        stubFor(
          put(s"/etmp/RESTAdapter/plastic-packaging-tax/subscriptions/PPT/$pptReference")
            .willReturn(
              aResponse()
                .withStatus(Status.UNPROCESSABLE_ENTITY)
                .withBody(
                  """
                    |{
                    |  "error": {
                    |    "errorId": "999",
                    |    "processingDate": "2026-07-09T09:26:17Z",
                    |    "text": "Technical System Error"
                    |  }
                    |}
                    |""".stripMargin
                )
            )
        )

        val res: SubscriptionFailureResponseWithStatusCode =
          await(
            connector.updateSubscription(pptReference, ukLimitedCompanySubscription)
          ).asInstanceOf[SubscriptionFailureResponseWithStatusCode]

        res.statusCode mustBe 500
        res.failureResponse.failures.head.code mustBe "SERVER_ERROR"
        res.failureResponse.failures.head.reason mustBe "IF is currently experiencing problems that require live service intervention."
      }
    }
  }

  private def createErrorResponse(code: String, reason: String): Seq[EISError] =
    Seq(EISError(code, reason))

  private def stubSubscriptionDisplay(pptReference: String, response: Subscription): Unit =
    stubFor(
      get(s"/etmp/RESTAdapter/plastic-packaging-tax/subscriptions/PPT/$pptReference")
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
            .withBody(
              Json.stringify(
                Json.obj(
                  "success" -> Json.toJson(response)
                )
              )
            )
        )
    )

  private def stubSubscriptionDisplayFailure(
    pptReference: String,
    httpStatus: Int,
    errors: Seq[EISError]
  ): Any =
    stubFor(
      get(s"/etmp/RESTAdapter/plastic-packaging-tax/subscriptions/PPT/$pptReference")
        .willReturn(
          aResponse()
            .withStatus(httpStatus)
            .withBody(Json.obj("failures" -> errors).toString)
        )
    )

}
