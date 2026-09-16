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
import com.github.tomakehurst.wiremock.client.WireMock.{
  aResponse,
  equalTo,
  get,
  matching,
  post,
  postRequestedFor,
  put,
  urlEqualTo
}
import connectors.HipSubscriptionsConnector
import org.scalatest.EitherValues
import org.scalatest.Inspectors.forAll
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
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
import scala.jdk.CollectionConverters.ListHasAsScala

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
  private val pptSubscriptionSubmissionTimer = "ppt.subscription.submission.timer"

  private val createUrlWithSafeId =
    s"/etmp/RESTAdapter/plastic-packaging-tax/subscriptions/PPT?idType=SAFEID&idValue=$safeNumber"

  private val createUrlWithoutSafeId =
    "/etmp/RESTAdapter/plastic-packaging-tax/subscriptions/PPT"

  private val hipPptReference     = "XDPPT123456789"
  private val hipFormBundleNumber = "1234567890"
  private val hipProcessingDate   = "2026-07-09T09:26:17Z"
  private val hipLogId            = "0123456789ABCDEF0123456789ABCDEF"

  /** Every errorId EPID1789's 422SubscriptionCreate enum documents, against the api-1711 code and
    * status the frontend saw when this service still talked to IFS.
    */
  private val create422Mappings = Seq(
    ("001", "INVALID_REGIME", "The remote endpoint has indicated that the REGIME provided is invalid.", 422),
    ("003", "BAD_GATEWAY", "Dependent systems are currently not responding.", 502),
    ("004", "DUPLICATE_SUBMISSION", "The remote endpoint has indicated that duplicate submission acknowledgment reference.", 409),
    ("007", "ACTIVE_SUBSCRIPTION_EXISTS", "The remote endpoint has indicated that Business Partner already has active subscription for this regime.", 422),
    ("087", "BUSINESS_VALIDATION", "The remote endpoint has indicated cannot Create Group Subscription.", 422),
    ("088", "ACTIVE_GROUP_SUBSCRIPTION_EXISTS", "The remote endpoint has indicated that Business Partner already has an active Group Subscription.", 422),
    ("089", "INVALID_SAFEID", "The remote endpoint has indicated that the SAFEID provided is invalid.", 422),
    ("090", "CANNOT_CREATE_PARTNERSHIP_SUBSCRIPTION", "The remote end point has indicated cannot Create Partnership Subscription.", 422),
    ("999", "SERVER_ERROR", "IF is currently experiencing problems that require live service intervention.", 500)
  )

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

    "creating a subscription" should {
      "handle a 201" in {
        stubSubscriptionCreate(hipSuccessBody)

        val res: SubscriptionSuccessfulResponse =
          await(
            connector.submitSubscription(safeNumber, ukLimitedCompanySubscription)
          ).asInstanceOf[SubscriptionSuccessfulResponse]

        res.pptReferenceNumber mustBe hipPptReference
        res.formBundleNumber mustBe hipFormBundleNumber
        res.processingDate mustBe ZonedDateTime.parse(hipProcessingDate)

        getTimer(pptSubscriptionSubmissionTimer).getCount mustBe 1
      }

      "post a group subscription to the URL without the safeId query params" in {
        stubSubscriptionCreate(hipSuccessBody, url = createUrlWithoutSafeId)

        val res: SubscriptionSuccessfulResponse =
          await(
            connector.submitSubscription(safeNumber, ukLimitedCompanyGroupSubscription)
          ).asInstanceOf[SubscriptionSuccessfulResponse]

        res.pptReferenceNumber mustBe hipPptReference

        wireMockServer.verify(postRequestedFor(urlEqualTo(createUrlWithoutSafeId)))
        getTimer(pptSubscriptionSubmissionTimer).getCount mustBe 1
      }

      "send the headers HIP requires" in {
        stubSubscriptionCreate(hipSuccessBody)

        await(connector.submitSubscription(safeNumber, ukLimitedCompanySubscription))

        wireMockServer.verify(
          postRequestedFor(urlEqualTo(createUrlWithSafeId))
            .withHeader("correlationid", matching(".+"))
            .withHeader("X-Originating-System", equalTo("PPT"))
            .withHeader("X-Transmitting-System", equalTo("HIP"))
            .withHeader("X-Receipt-Date", matching(".+"))
            .withHeader("Authorization", matching("Basic .+"))
        )
      }

      "send a fresh correlationid on every request" in {
        stubSubscriptionCreate(hipSuccessBody)
        wireMockServer.resetRequests()

        await(connector.submitSubscription(safeNumber, ukLimitedCompanySubscription))
        await(connector.submitSubscription(safeNumber, ukLimitedCompanySubscription))

        val correlationIds = wireMockServer
          .findAll(postRequestedFor(urlEqualTo(createUrlWithSafeId)))
          .asScala
          .map(_.getHeader("correlationid"))
          .toList

        correlationIds.size mustBe 2
        correlationIds.distinct.size mustBe 2
      }

      "handle a 400 with a single error payload" in {
        stubSubscriptionCreate(hipSystemErrorBody("400", "foobar"), httpStatus = Status.BAD_REQUEST)

        val res: SubscriptionFailureResponseWithStatusCode =
          await(
            connector.submitSubscription(safeNumber, ukLimitedCompanySubscription)
          ).asInstanceOf[SubscriptionFailureResponseWithStatusCode]

        res.statusCode mustBe 400
        res.failureResponse.failures.head.code mustBe "400"
        res.failureResponse.failures.head.reason mustBe "foobar"
      }

      "handle a 500 with an array of failures payload" in {
        forAll(Seq(500, 503)) { status =>
          stubSubscriptionCreate(hipFailuresArrayBody, httpStatus = status)

          val res: SubscriptionFailureResponseWithStatusCode =
            await(
              connector.submitSubscription(safeNumber, ukLimitedCompanySubscription)
            ).asInstanceOf[SubscriptionFailureResponseWithStatusCode]

          res.statusCode mustBe status
          res.failureResponse.failures.head.code mustBe "Type of Failure"
          res.failureResponse.failures.head.reason mustBe "Reason for Failure"
        }
      }

      forAll(create422Mappings) { (errorId, expectedCode, expectedReason, expectedStatus) =>
        s"map a 422 $errorId onto $expectedCode" in {
          stubSubscriptionCreate(hipBusinessValidationBody(errorId, "Error reason."),
                                 httpStatus = Status.UNPROCESSABLE_ENTITY
          )

          val res: SubscriptionFailureResponseWithStatusCode =
            await(
              connector.submitSubscription(safeNumber, ukLimitedCompanySubscription)
            ).asInstanceOf[SubscriptionFailureResponseWithStatusCode]

          res.statusCode mustBe expectedStatus
          res.failureResponse.failures.head.code mustBe expectedCode
          res.failureResponse.failures.head.reason mustBe expectedReason
        }
      }

      "fall back to an EIS shaped SERVER_ERROR for an unmapped 422 errorId" in {
        stubSubscriptionCreate(hipBusinessValidationBody("111", "Not in the mapping table"),
                               httpStatus = Status.UNPROCESSABLE_ENTITY
        )

        expectServerError(
          await(connector.submitSubscription(safeNumber, ukLimitedCompanySubscription))
        )
      }

      "fall back to an EIS shaped SERVER_ERROR for an unreadable error payload" in {
        forAll(Seq(400, 500, 503)) { status =>
          stubSubscriptionCreate(Json.obj("foo" -> "bar"), httpStatus = status)

          expectServerError(
            await(connector.submitSubscription(safeNumber, ukLimitedCompanySubscription))
          )
        }
      }

      "fall back to an EIS shaped SERVER_ERROR for a bodiless 401, 403 or 404" in {
        forAll(Seq(Status.UNAUTHORIZED, Status.FORBIDDEN, Status.NOT_FOUND)) { status =>
          stubSubscriptionCreate(Json.obj(), httpStatus = status)

          expectServerError(
            await(connector.submitSubscription(safeNumber, ukLimitedCompanySubscription))
          )
        }
      }

      "fall back to an EIS shaped SERVER_ERROR for a malformed success payload" in {
        stubSubscriptionCreate(Json.obj("xxx" -> "xxx"))

        expectServerError(
          await(connector.submitSubscription(safeNumber, ukLimitedCompanySubscription))
        )
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

  private val hipSuccessBody: JsObject =
    Json.obj("success" -> Json.obj("pptReferenceNumber" -> hipPptReference,
                                   "processingDate"   -> hipProcessingDate,
                                   "formBundleNumber" -> hipFormBundleNumber
    ))

  private val hipFailuresArrayBody: JsObject =
    Json.obj("origin" -> "HIP",
             "response" -> Json.obj(
               "failures" -> Json.arr(
                 Json.obj("type" -> "Type of Failure", "reason" -> "Reason for Failure")
               )
             )
    )

  private def hipSystemErrorBody(code: String, message: String): JsObject =
    Json.obj("origin" -> "HoD",
             "response" -> Json.obj(
               "error" -> Json.obj("code" -> code, "message" -> message, "logID" -> hipLogId)
             )
    )

  private def hipBusinessValidationBody(errorId: String, text: String): JsObject =
    Json.obj("error" -> Json.obj("processingDate" -> hipProcessingDate,
                                 "errorId" -> errorId,
                                 "text"    -> text
    ))

  private def stubSubscriptionCreate(
    body: JsObject,
    httpStatus: Int = Status.CREATED,
    url: String = createUrlWithSafeId
  ): Any =
    stubFor(
      post(url)
        .willReturn(
          aResponse()
            .withStatus(httpStatus)
            .withBody(body.toString)
        )
    )

  private def expectServerError(response: Any): Unit = {
    val res = response.asInstanceOf[SubscriptionFailureResponseWithStatusCode]
    res.statusCode mustBe Status.INTERNAL_SERVER_ERROR
    res.failureResponse.failures.head.code mustBe "SERVER_ERROR"
  }

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
