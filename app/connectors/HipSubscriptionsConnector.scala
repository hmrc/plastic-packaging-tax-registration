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

import config.AppConfig
import models.eis.EISError
import models.eis.subscription.Subscription
import models.eis.subscription.create.*
import models.eis.subscriptionStatus.SubscriptionStatusResponse
import models.hip.HipPlatformErrors.*
import play.api.http.Status
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import java.net.{URI, URL}
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HipSubscriptionsConnector @Inject() (
  httpClient: HttpClientV2,
  override val appConfig: AppConfig,
  metrics: Metrics
)(implicit ec: ExecutionContext)
    extends SubscriptionsConnector with HipConnector {

  private def subscriptionsUrl(pptRef: String): URL =
    url"${appConfig.hipPPTBaseUrl}/etmp/RESTAdapter/plastic-packaging-tax/subscriptions/PPT/$pptRef"

  def getSubscriptionStatus(
    safeId: String
  )(implicit hc: HeaderCarrier): Future[Either[Int, SubscriptionStatusResponse]] = ???

  def submitSubscription(safeNumber: String, subscription: Subscription)(implicit
    hc: HeaderCarrier
  ): Future[SubscriptionResponse] = {
    val timer         = metrics.defaultRegistry.timer("ppt.subscription.submission.timer").time()
    val correlationId = UUID.randomUUID().toString

    val msgCommon = s"Hip create PPT subscription sent with correlationid [$correlationId] and"
    val (createUrl, msg) =
      if (subscription.legalEntityDetails.groupSubscriptionFlag)
        (appConfig.hipSubscriptionCreateWithoutSafeIdUrl(), s"$msgCommon no safeId")
      else (appConfig.hipSubscriptionCreateUrl(safeNumber), s"$msgCommon safeId [$safeNumber]")

    httpClient
      .post(new URI(createUrl).toURL())
      .withBody(Json.toJson(subscription))
      .setHeader(hipHeaders(correlationId)*)
      .execute[HttpResponse]
      .andThen { case _ => timer.stop() }
      .map {
        subscriptionCreateResponse =>
          logger.info(s"$msg had response payload ${subscriptionCreateResponse.body}")
          subscriptionCreateResponse.status match {
            case CREATED =>
              val hip = subscriptionCreateResponse.json.as[HipSubscriptionSuccessfulResponse]
              hip.success
            case BAD_REQUEST | INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE =>
              parseHipErrorEnvelopeResponse(subscriptionCreateResponse) match {
                case HipUnexpectedError(status, body) =>
                  val errorMsg =
                    s"$msg failed - error response in unexpected format: status: $status body: $body"
                  logger.warn(errorMsg)
                  throw UpstreamErrorResponse.apply(errorMsg, Status.INTERNAL_SERVER_ERROR)
                case HipSystemErrorObject(error) =>
                  logger.warn(
                    s"Hip PPT system error code: ${error.code}, logID: ${error.logID} message: ${error.message}"
                  )
                  SubscriptionFailureResponseWithStatusCode(
                    EISSubscriptionFailureResponse(
                      Array(EISError(error.code, error.message)).toSeq
                    ),
                    subscriptionCreateResponse.status
                  )
                case HipFailuresErrorArray(failures) =>
                  logger.warn(s"Hip PPT failures: ${failures.mkString("[", ";", "]")}")
                  SubscriptionFailureResponseWithStatusCode(
                    EISSubscriptionFailureResponse(failures.map(x =>
                      EISError(x.`type`, x.reason)
                    ).toSeq),
                    subscriptionCreateResponse.status
                  )
              }
            case UNPROCESSABLE_ENTITY =>
              subscriptionCreateResponse.json.as[Hip422Error].error match {
                case HipInner422Err(code, processingDate, text) =>
                  logger.warn(s"Hip returned 422 $code $processingDate, $text ")
                  subscriptionCreate422ResponseMappings.getOrElse(
                    code,
                    throw UpstreamErrorResponse(text, Status.INTERNAL_SERVER_ERROR)
                  )
              }
            case e => // 401, 403, 404
              throw UpstreamErrorResponse(s"Hip returned $e",
                                          Status.INTERNAL_SERVER_ERROR
              )
          }
      }
      .recover {
        case httpEx: UpstreamErrorResponse =>
          logger.warn(s"$msg failed - ${httpEx.getMessage}")
          serverError(httpEx.getMessage)
        case ex: Exception =>
          logger.warn(s"$msg is currently unavailable due to [${ex.getMessage}]", ex)
          serverError(ex.getMessage)
      }
  }

  private def serverError(reason: String): SubscriptionFailureResponseWithStatusCode =
    SubscriptionFailureResponseWithStatusCode(
      EISSubscriptionFailureResponse(Seq(EISError("SERVER_ERROR", reason))),
      Status.INTERNAL_SERVER_ERROR
    )

  def getSubscription(
    pptReferenceNumber: String
  )(implicit hc: HeaderCarrier): Future[Either[Int, Subscription]] = {
    val timer         = metrics.defaultRegistry.timer("ppt.subscription.display.timer").time()
    val correlationId = UUID.randomUUID().toString

    httpClient
      .get(subscriptionsUrl(pptReferenceNumber))
      .setHeader(hipHeaders(correlationId)*)
      .execute[HttpResponse]
      .andThen { case _ => timer.stop() }
      .map { response =>
        response.status match {
          case 200 =>
            logger.info(
              s"PPT view subscription with correlationid [$correlationId] and pptReference [$pptReferenceNumber]"
            )
            Right((response.json \ "success").as[Subscription])
          case _ =>
            logger.error(s"PPT view subscription failed response: ${response.body}")
            Left(response.status)
        }
      }
      .recover {
        case httpEx: UpstreamErrorResponse =>
          logger.warn(
            s"Upstream error returned on viewing subscription with correlationid [$correlationId] and " +
              s"pptReference [$pptReferenceNumber], status: ${httpEx.statusCode}, body: ${httpEx.getMessage}"
          )
          Left(httpEx.statusCode)
        case ex: Exception =>
          logger.warn(
            s"Subscription display with correlationid [$correlationId] and " +
              s"pptReference [$pptReferenceNumber] is currently unavailable due to [${ex.getMessage}]",
            ex
          )
          Left(Status.INTERNAL_SERVER_ERROR)
      }
  }

  def updateSubscription(
    pptReference: String,
    subscription1: Subscription
  )(implicit hc: HeaderCarrier): Future[SubscriptionResponse] = {
    val timer         = metrics.defaultRegistry.timer("ppt.subscription.update.timer").time()
    val correlationId = UUID.randomUUID().toString

    // the update-subscription API does not accept processingDate, which is returned on display API.
    val subscription = subscription1.copy(processingDate = None)

    httpClient
      .put(subscriptionsUrl(pptReference))
      .withBody(Json.toJson(subscription))
      .setHeader(hipHeaders(correlationId)*)
      .execute[HttpResponse]
      .andThen { case _ => timer.stop() }
      .map {
        subscriptionUpdateResponse =>
          logger.info(
            s"Hip update PPT subscription sent with correlationid [$correlationId] " +
              s"and pptReference [$pptReference] had response payload had response payload ${subscriptionUpdateResponse.json}"
          )
          subscriptionUpdateResponse.status match {
            case OK =>
              val hip = subscriptionUpdateResponse.json.as[HipSubscriptionSuccessfulResponse]
              hip.success
            case BAD_REQUEST | INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE =>
              parseHipErrorEnvelopeResponse(subscriptionUpdateResponse) match {
                case HipUnexpectedError(status, body) =>
                  val errorMsg =
                    s"Hip PPT subscription update with correlationid [$correlationId] " +
                      s"and pptReference [$pptReference] failed - error response in unexpected format: " +
                      s"status: $status body: $body"
                  logger.warn(errorMsg)
                  throw UpstreamErrorResponse.apply(errorMsg, Status.INTERNAL_SERVER_ERROR)
                case HipSystemErrorObject(error) =>
                  logger.warn(
                    s"Hip PPT system error code: ${error.code}, logID: ${error.logID} message: ${error.message}"
                  )
                  SubscriptionFailureResponseWithStatusCode(
                    EISSubscriptionFailureResponse(
                      Array(EISError(error.code, error.message)).toSeq
                    ),
                    subscriptionUpdateResponse.status
                  )
                case HipFailuresErrorArray(failures) =>
                  logger.warn(s"Hip PPT failures: ${failures.mkString("[", ";", "]")}")
                  SubscriptionFailureResponseWithStatusCode(
                    EISSubscriptionFailureResponse(failures.map(x =>
                      EISError(x.`type`, x.reason)
                    ).toSeq),
                    subscriptionUpdateResponse.status
                  )
              }
            case UNPROCESSABLE_ENTITY =>
              subscriptionUpdateResponse.json.as[Hip422Error].error match {
                case HipInner422Err(code, processingDate, text) =>
                  logger.warn(s"Hip returned 422 $code $processingDate, $text ")
                  subscriptionUpdate422ResponseMappings.getOrElse(
                    code,
                    throw UpstreamErrorResponse(text, Status.INTERNAL_SERVER_ERROR)
                  )
              }
            case e => // 401, 403, 404
              throw UpstreamErrorResponse(s"PPT returned $e which we pass on as 500",
                                          Status.INTERNAL_SERVER_ERROR
              )
          }
      }
  }

}
