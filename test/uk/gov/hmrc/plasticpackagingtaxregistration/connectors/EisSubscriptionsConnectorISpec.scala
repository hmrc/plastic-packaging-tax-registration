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

package connectors

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
import org.scalatest.Inspectors.forAll
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.http.Status.{CONFLICT, OK}
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers.await
import uk.gov.hmrc.http.UpstreamErrorResponse
import base.Injector
import base.data.SubscriptionTestData
import base.it.ConnectorISpec
import models.eis.EISError
import models.eis.subscription.Subscription
import models.eis.subscription.create.{
  EISSubscriptionFailureResponse,
  EisSubscriptionFailureResponseWithStatusCode
}
import models.hip.subscription.create.{
  HipBusinessValidationError,
  HipBusinessValidationFailure,
  HipSubscriptionFailureResponseWithStatusCode,
  HipSystemError,
  HipSystemFailure
}
import models.subscription.create.SubscriptionSuccessfulResponse
import models.eis.subscriptionStatus.ETMPSubscriptionStatus.NO_FORM_BUNDLE_FOUND
import models.eis.subscriptionStatus.SubscriptionStatus.NOT_SUBSCRIBED
import org.scalatest.EitherValues

import java.time.{ZoneOffset, ZonedDateTime}
import java.util.UUID
import scala.jdk.CollectionConverters.ListHasAsScala

class EisSubscriptionsConnectorISpec
    extends ConnectorISpec with Injector with ScalaFutures with SubscriptionTestData
    with EitherValues {

  private lazy val eisConnector: EisSubscriptionsConnector =
    app.injector.instanceOf[EisSubscriptionsConnector]

  private lazy val hipConnector: HipSubscriptionsConnector =
    app.injector.instanceOf[HipSubscriptionsConnector]

  private val pptSubscriptionSubmissionTimer = "ppt.subscription.submission.timer"
  private val pptSubscriptionStatusTimer     = "ppt.subscription.status.timer"
  private val pptSubscriptionDisplayTimer    = "ppt.subscription.display.timer"
  private val pptSubscriptionUpdateTimer     = "ppt.subscription.update.timer"

  private val hipCreateUrlWithSafeId =
    s"/etmp/RESTAdapter/plastic-packaging-tax/subscriptions/PPT?idType=SAFEID&idValue=${safeNumber}"

  private val hipCreateUrlWithoutSafeId =
    "/etmp/RESTAdapter/plastic-packaging-tax/subscriptions/PPT"

  private val hipPptReference     = "XDPPT123456789"
  private val hipFormBundleNumber = "1234567890"
  private val hipProcessingDate   = "2026-07-09T09:26:17Z"
  private val hipLogId            = "0123456789ABCDEF0123456789ABCDEF"

  "EIS subscription connector" when {
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

        val res = await(eisConnector.getSubscriptionStatus(safeNumber)).value

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

        val res = await(eisConnector.getSubscriptionStatus(safeNumber)).left.value
        res mustBe Status.BAD_REQUEST

        getTimer(pptSubscriptionStatusTimer).getCount mustBe 1
      }

      "map a 404 to an error" in {
        val errors = createErrorResponse(
          code = "NO_DATA_FOUND",
          reason =
            "The remote endpoint has indicated that the requested resource could  not be found."
        )
        stubSubscriptionStatusFailure(httpStatus = Status.NOT_FOUND, errors = errors)

        val res = await(eisConnector.getSubscriptionStatus(safeNumber)).left.value
        res mustBe Status.NOT_FOUND

        getTimer(pptSubscriptionStatusTimer).getCount mustBe 1
      }

      "handle a 500" in {
        val errors =
          createErrorResponse(code = "NO_DATA_FOUND",
                              reason =
                                "Dependent systems are currently not responding."
          )

        stubSubscriptionStatusFailure(httpStatus = Status.INTERNAL_SERVER_ERROR, errors = errors)

        val res = await(eisConnector.getSubscriptionStatus(safeNumber)).left.value
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

        val res = await(eisConnector.getSubscriptionStatus(safeNumber)).left.value
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

        val res = await(eisConnector.getSubscriptionStatus(safeNumber)).left.value
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
            eisConnector.submitSubscription(safeNumber, ukLimitedCompanySubscription)
          ).asInstanceOf[SubscriptionSuccessfulResponse]

        res.pptReferenceNumber mustBe pptReference
        res.formBundleNumber mustBe formBundleNumber
        res.processingDate mustBe ZonedDateTime.parse(subscriptionProcessingDate)

        getTimer(pptSubscriptionSubmissionTimer).getCount mustBe 1
      }

      forAll(Seq(400, 404, 422, 409, 500, 502, 503)) { statusCode =>
        s"return $statusCode" when {
          s"$statusCode is returned from downstream service" in {
            val errors =
              createErrorResponse(code = statusCode.toString,
                                  reason =
                                    "Error reason."
              )

            stubSubscriptionSubmissionFailure(httpStatus = statusCode, errors = errors)

            val resp =
              await(eisConnector.submitSubscription(safeNumber, ukLimitedCompanySubscription))

            resp mustBe EisSubscriptionFailureResponseWithStatusCode(
              EISSubscriptionFailureResponse(List(EISError(statusCode.toString, "Error reason."))),
              statusCode
            )

            getTimer(pptSubscriptionSubmissionTimer).getCount mustBe 1
          }
        }
      }

      "return 500 for malformed successful responses" in {
        stubSubscriptionSubmitException(OK)

        intercept[UpstreamErrorResponse] {
          await(eisConnector.submitSubscription(safeNumber, ukLimitedCompanySubscription))
        }.statusCode mustBe Status.INTERNAL_SERVER_ERROR
      }

      "return 500 for malformed failed responses" in {
        stubSubscriptionSubmitException(CONFLICT)

        intercept[UpstreamErrorResponse] {
          await(eisConnector.submitSubscription(safeNumber, ukLimitedCompanySubscription))
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
            eisConnector.submitSubscription(safeNumber, ukLimitedCompanyGroupSubscription)
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

        val res: Either[Int, Subscription] = await(eisConnector.getSubscription(pptReference))

        res.toOption mustBe Some(ukLimitedCompanySubscription)

        getTimer(pptSubscriptionDisplayTimer).getCount mustBe 1
      }

      forAll(Seq(400, 404, 422, 409, 500, 502, 503)) { statusCode =>
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

            val res = await(eisConnector.getSubscription(pptReference))

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
            eisConnector.updateSubscription(pptReference, ukLimitedCompanySubscription)
          ).asInstanceOf[SubscriptionSuccessfulResponse]

        res.pptReferenceNumber mustBe pptReference
        res.formBundleNumber mustBe formBundleNumber
        res.processingDate mustBe ZonedDateTime.parse(subscriptionProcessingDate)

        getTimer(pptSubscriptionUpdateTimer).getCount mustBe 1
      }

      forAll(Seq(400, 404, 422, 409, 500, 502, 503)) { statusCode =>
        s"return $statusCode" when {
          s"$statusCode is returned from downstream service" in {
            val errors =
              createErrorResponse(code = statusCode.toString,
                                  reason =
                                    "Error reason."
              )

            stubSubscriptionUpdateFailure(httpStatus = statusCode, errors = errors)

            val resp =
              await(eisConnector.updateSubscription(pptReference, ukLimitedCompanySubscription))

            resp mustBe EisSubscriptionFailureResponseWithStatusCode(
              EISSubscriptionFailureResponse(List(EISError(statusCode.toString, "Error reason."))),
              statusCode
            )
            getTimer(pptSubscriptionUpdateTimer).getCount mustBe 1
          }
        }
      }

      "return 500 for malformed successful responses" in {
        stubSubscriptionUpdateException(Status.OK)

        intercept[UpstreamErrorResponse] {
          await(eisConnector.updateSubscription(pptReference, ukLimitedCompanySubscription))
        }.statusCode mustBe Status.INTERNAL_SERVER_ERROR
      }

      "return 500 for malformed failed responses" in {
        stubSubscriptionUpdateException(Status.CONFLICT)

        intercept[UpstreamErrorResponse] {
          await(eisConnector.updateSubscription(pptReference, ukLimitedCompanySubscription))
        }.statusCode mustBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  "HIP subscription connector" when {
    "submitting a subscription" should {
      "handle a 201" in {
        stubHipSubscriptionSubmission(
          hipSuccessBody(hipPptReference, hipProcessingDate, hipFormBundleNumber)
        )

        val res: SubscriptionSuccessfulResponse =
          await(
            hipConnector.submitSubscription(safeNumber, ukLimitedCompanySubscription)
          ).asInstanceOf[SubscriptionSuccessfulResponse]

        res.pptReferenceNumber mustBe hipPptReference
        res.formBundleNumber mustBe hipFormBundleNumber
        res.processingDate mustBe ZonedDateTime.parse(hipProcessingDate)

        getTimer(pptSubscriptionSubmissionTimer).getCount mustBe 1
      }

      "send the headers HIP requires" in {
        stubHipSubscriptionSubmission(
          hipSuccessBody(hipPptReference, hipProcessingDate, hipFormBundleNumber)
        )

        await(hipConnector.submitSubscription(safeNumber, ukLimitedCompanySubscription))

        wireMockServer.verify(
          postRequestedFor(urlEqualTo(hipCreateUrlWithSafeId))
            .withHeader("correlationid", matching(".+"))
            .withHeader("X-Originating-System", equalTo("PPT"))
            .withHeader("X-Transmitting-System", equalTo("HIP"))
            .withHeader("X-Receipt-Date", matching(".+"))
            .withHeader("Authorization", matching("Basic .+"))
        )
      }

      "send a fresh correlationid on every request" in {
        stubHipSubscriptionSubmission(
          hipSuccessBody(hipPptReference, hipProcessingDate, hipFormBundleNumber)
        )
        wireMockServer.resetRequests()

        await(hipConnector.submitSubscription(safeNumber, ukLimitedCompanySubscription))
        await(hipConnector.submitSubscription(safeNumber, ukLimitedCompanySubscription))

        val correlationIds = wireMockServer
          .findAll(postRequestedFor(urlEqualTo(hipCreateUrlWithSafeId)))
          .asScala
          .map(_.getHeader("correlationid"))
          .toList

        correlationIds.size mustBe 2
        correlationIds.distinct.size mustBe 2
      }

      "return 500 for malformed successful responses" in {
        stubHipSubscriptionSubmission(Json.obj("xxx" -> "xxx"))

        intercept[UpstreamErrorResponse] {
          await(hipConnector.submitSubscription(safeNumber, ukLimitedCompanySubscription))
        }.statusCode mustBe Status.INTERNAL_SERVER_ERROR
      }

      "return 500 for malformed failed responses" in {
        stubHipSubscriptionSubmission(Json.obj("xxx" -> "xxx"), httpStatus = Status.BAD_REQUEST)

        intercept[UpstreamErrorResponse] {
          await(hipConnector.submitSubscription(safeNumber, ukLimitedCompanySubscription))
        }.statusCode mustBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "submitting a group subscription" should {
      "post to the URL without the safeId query params" in {
        stubHipSubscriptionSubmission(
          hipSuccessBody(hipPptReference, hipProcessingDate, hipFormBundleNumber),
          url = hipCreateUrlWithoutSafeId
        )

        val res: SubscriptionSuccessfulResponse =
          await(
            hipConnector.submitSubscription(safeNumber, ukLimitedCompanyGroupSubscription)
          ).asInstanceOf[SubscriptionSuccessfulResponse]

        res.pptReferenceNumber mustBe hipPptReference
        res.formBundleNumber mustBe hipFormBundleNumber
        res.processingDate mustBe ZonedDateTime.parse(hipProcessingDate)

        wireMockServer.verify(postRequestedFor(urlEqualTo(hipCreateUrlWithoutSafeId)))
        getTimer(pptSubscriptionSubmissionTimer).getCount mustBe 1
      }
    }

    "the subscription is rejected on business grounds" should {
      "return the 422 business validation body with its status" in {
        stubHipSubscriptionSubmission(hipBusinessValidationBody("007", "Some business reason"),
                                      httpStatus = Status.UNPROCESSABLE_ENTITY
        )

        val resp = await(hipConnector.submitSubscription(safeNumber, ukLimitedCompanySubscription))

        resp mustBe HipSubscriptionFailureResponseWithStatusCode(
          HipBusinessValidationFailure(
            HipBusinessValidationError(hipProcessingDate, "007", "Some business reason")
          ),
          Status.UNPROCESSABLE_ENTITY
        )

        getTimer(pptSubscriptionSubmissionTimer).getCount mustBe 1
      }

      // TODO(DCA-68): these are the errorId values schema 422SubscriptionCreate documents, but the
      // mapping back to the old EIS codes is unconfirmed. The one that matters downstream is
      // whichever id replaces ACTIVE_SUBSCRIPTION_EXISTS, since the frontend keys its duplicate
      // registration page off it. 007 reads like the match but needs confirming with the team.
      forAll(Seq("001", "003", "004", "007", "087", "088", "089", "090", "999")) { errorId =>
        s"parse errorId $errorId" when {
          s"$errorId is returned from downstream service" in {
            stubHipSubscriptionSubmission(hipBusinessValidationBody(errorId, "Error reason."),
                                          httpStatus = Status.UNPROCESSABLE_ENTITY
            )

            val resp =
              await(hipConnector.submitSubscription(safeNumber, ukLimitedCompanySubscription))

            resp mustBe HipSubscriptionFailureResponseWithStatusCode(
              HipBusinessValidationFailure(
                HipBusinessValidationError(hipProcessingDate, errorId, "Error reason.")
              ),
              Status.UNPROCESSABLE_ENTITY
            )
          }
        }
      }
    }

    "the subscription fails with a system error" should {
      forAll(Seq(Status.BAD_REQUEST, Status.INTERNAL_SERVER_ERROR)) { statusCode =>
        s"return $statusCode" when {
          s"$statusCode is returned from downstream service" in {
            stubHipSubscriptionSubmission(hipSystemErrorBody(statusCode.toString, "Error reason."),
                                          httpStatus = statusCode
            )

            val resp =
              await(hipConnector.submitSubscription(safeNumber, ukLimitedCompanySubscription))

            resp mustBe HipSubscriptionFailureResponseWithStatusCode(
              HipSystemFailure(
                HipSystemError(statusCode.toString, "Error reason.", hipLogId)
              ),
              statusCode
            )

            getTimer(pptSubscriptionSubmissionTimer).getCount mustBe 1
          }
        }
      }

      // TODO(DCA-68): EPID1789 defines no response body for 401, 403 and 404, so the failure parse
      // fails and we surface a 500 rather than the original status. Confirm whether the frontend
      // needs the downstream status passed through instead.
      forAll(Seq(Status.UNAUTHORIZED, Status.FORBIDDEN, Status.NOT_FOUND)) { statusCode =>
        s"return 500 when a bodyless $statusCode is returned from downstream service" in {
          stubHipSubscriptionSubmissionWithoutBody(statusCode)

          intercept[UpstreamErrorResponse] {
            await(hipConnector.submitSubscription(safeNumber, ukLimitedCompanySubscription))
          }.statusCode mustBe Status.INTERNAL_SERVER_ERROR
        }
      }

      // TODO(DCA-68): the old EIS create was tested against 409, 502 and 503 but EPID1789 documents
      // none of them. They currently fall through to the failure parse, which fails, giving a 500.
      // Confirm with the team whether HIP can emit these and whether the status should survive.
      forAll(Seq(Status.CONFLICT, Status.BAD_GATEWAY, Status.SERVICE_UNAVAILABLE)) { statusCode =>
        s"return 500 when an undocumented $statusCode is returned from downstream service" in {
          stubHipSubscriptionSubmissionWithoutBody(statusCode)

          intercept[UpstreamErrorResponse] {
            await(hipConnector.submitSubscription(safeNumber, ukLimitedCompanySubscription))
          }.statusCode mustBe Status.INTERNAL_SERVER_ERROR
        }
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

  private def hipSuccessBody(
    pptReferenceNumber: String,
    processingDate: String,
    formBundleNumber: String
  ): JsObject =
    Json.obj("success" -> Json.obj("pptReferenceNumber" -> pptReferenceNumber,
                                   "processingDate"   -> processingDate,
                                   "formBundleNumber" -> formBundleNumber
    ))

  private def hipBusinessValidationBody(errorId: String, text: String): JsObject =
    Json.obj("error" -> Json.obj("processingDate" -> hipProcessingDate,
                                 "errorId" -> errorId,
                                 "text"    -> text
    ))

  private def hipSystemErrorBody(code: String, message: String): JsObject =
    Json.obj("error" -> Json.obj("code" -> code, "message" -> message, "logID" -> hipLogId))

  private def stubHipSubscriptionSubmission(
    body: JsObject,
    httpStatus: Int = Status.CREATED,
    url: String = hipCreateUrlWithSafeId
  ): Any =
    stubFor(
      post(url)
        .willReturn(
          aResponse()
            .withStatus(httpStatus)
            .withBody(body.toString)
        )
    )

  private def stubHipSubscriptionSubmissionWithoutBody(httpStatus: Int): Any =
    stubFor(
      post(hipCreateUrlWithSafeId)
        .willReturn(
          aResponse()
            .withStatus(httpStatus)
        )
    )

}
