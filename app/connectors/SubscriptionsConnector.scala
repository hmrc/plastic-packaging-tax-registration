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

import java.util.UUID
import com.codahale.metrics.Timer
import com.kenshoo.play.metrics.Metrics
import config.AppConfig
import models.eis.subscriptionStatus.SubscriptionStatusResponse

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.json.Json._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}
import models.eis.subscription._
import models.eis.subscription.create.{EISSubscriptionFailureResponse, SubscriptionFailureResponseWithStatusCode, SubscriptionResponse, SubscriptionSuccessfulResponse}
import models.eis.subscriptionStatus.ETMPSubscriptionStatusResponse

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

@Singleton
class SubscriptionsConnector @Inject() (
  httpClient: HttpClient,
  override val appConfig: AppConfig,
  metrics: Metrics
)(implicit ec: ExecutionContext)
    extends EISConnector {

  private val logger = Logger(this.getClass)

  def getSubscriptionStatus(
    safeId: String
  )(implicit hc: HeaderCarrier): Future[Either[Int, SubscriptionStatusResponse]] = {
    val timer               = metrics.defaultRegistry.timer("ppt.subscription.status.timer").time()
    val correlationIdHeader = correlationIdHeaderName -> UUID.randomUUID().toString

    httpClient.GET[ETMPSubscriptionStatusResponse](appConfig.subscriptionStatusUrl(safeId), 
      headers = headers :+ correlationIdHeader)
      .map { etmpResponse =>
        logger.info(s"PPT subscription status sent with correlationId [${correlationIdHeader._2}] and " +
          s"safeId [$safeId] had response payload ${toJson(etmpResponse)}")
        Right(SubscriptionStatusResponse.fromETMPResponse(etmpResponse))
      }
      .recover {
        case httpEx: UpstreamErrorResponse =>
          httpEx.statusCode match {
            case _ => handleHttpError(safeId, correlationIdHeader, httpEx)
          }
        case ex: Exception => handleException(safeId, correlationIdHeader, ex)
      }
      .andThen { case _ => timer.stop() }
  }

  private def handleException(safeId: String, correlationIdHeader: (String, String), ex: Exception) = {
    // Hard internal errors which are not from upstream are signalled with a 500.
    logger.warn(
      s"Get subscription status failed with correlationId [${correlationIdHeader._2}] and " +
        s"safeId [$safeId] is currently unavailable due to exception [${ex.getMessage}]",
      ex
    )
    Left(Status.INTERNAL_SERVER_ERROR)
  }

  private def handleHttpError(safeId: String, correlationIdHeader: (String, String), httpEx: UpstreamErrorResponse) = {
    // Upstream errors should be echoed to the frontend so that user facing error handling is aware of them
    logger.warn(
      s"Upstream error returned from get subscription status with correlationId [${correlationIdHeader._2}] and " +
        s"safeId [$safeId], status: ${httpEx.statusCode}, body: ${httpEx.getMessage()}"
    )
    Left(httpEx.statusCode)
  }

  def submitSubscription(safeNumber: String, subscription: Subscription)(implicit
                                                                         hc: HeaderCarrier
  ): Future[SubscriptionResponse] = {

    val timer               = metrics.defaultRegistry.timer("ppt.subscription.submission.timer").time()
    val correlationIdHeader = correlationIdHeaderName -> UUID.randomUUID().toString

    val msgCommon =
      s"PPT subscription create sent with correlationId [${correlationIdHeader._2}] and"
    val (createUrl, msg) =
      if (subscription.legalEntityDetails.groupSubscriptionFlag)
        (appConfig.subscriptionCreateWithoutSafeIdUrl(), s"$msgCommon no safeId")
      else (appConfig.subscriptionCreateUrl(safeNumber), s"$msgCommon safeId [$safeNumber]")

    httpClient.POST[Subscription, HttpResponse](url = createUrl,
                                                body = subscription,
                                                headers = headers :+ correlationIdHeader
    )
      .andThen { case _ => timer.stop() }
      .map {
        subscriptionResponse =>
          logger.info(s"$msg had response payload ${subscriptionResponse.json}")

          if (Status.isSuccessful(subscriptionResponse.status))
            Try(subscriptionResponse.json.as[SubscriptionSuccessfulResponse]) match {
              case Success(successfulCreateResponse) => successfulCreateResponse
              case _ =>
                throw UpstreamErrorResponse.apply(
                  buildCreateSubscriptionErrorMessage(correlationIdHeader._2,
                                                      safeNumber,
                                                      "successful response in unexpected format"
                  ),
                  Status.INTERNAL_SERVER_ERROR
                )
            }
          else
            Try(subscriptionResponse.json.as[EISSubscriptionFailureResponse]) match {
              case Success(failedCreateResponse) =>
                SubscriptionFailureResponseWithStatusCode(failedCreateResponse,
                                                          subscriptionResponse.status
                )
              case _ =>
                throw UpstreamErrorResponse.apply(
                  buildCreateSubscriptionErrorMessage(correlationIdHeader._2,
                                                      safeNumber,
                                                      "failed response in unexpected format"
                  ),
                  Status.INTERNAL_SERVER_ERROR
                )
            }
      }
  }

  def getSubscription(
    pptReference: String
  )(implicit hc: HeaderCarrier): Future[Either[Int, Subscription]] = {
    val timer               = metrics.defaultRegistry.timer("ppt.subscription.display.timer").time()
    val correlationIdHeader = correlationIdHeaderName -> UUID.randomUUID().toString
    httpClient.GET[HttpResponse](appConfig.subscriptionDisplayUrl(pptReference),
                                 headers = headers :+ correlationIdHeader
    )
      .andThen { case _ => timer.stop() }
      .map { response =>
        if (Status.isSuccessful(response.status)) {
          logger.info(
            s"PPT view subscription with correlationId [${correlationIdHeader._2}] and pptReference [$pptReference]"
          )
          val json = Json.parse(response.body.replaceAll("\\s", " ")) //subscription data can come back un sanitised for json.
          Right(json.as[Subscription])
        } else {
          Left(response.status)
        }
      }
      .recover {
        case httpEx: UpstreamErrorResponse =>
          logger.warn(
            s"Upstream error returned on viewing subscription with correlationId [${correlationIdHeader._2}] and " +
              s"pptReference [$pptReference], status: ${httpEx.statusCode}, body: ${httpEx.getMessage()}"
          )
          Left(httpEx.statusCode)
        case ex: Exception =>
          logger.warn(
            s"Subscription display with correlationId [${correlationIdHeader._2}] and " +
              s"pptReference [$pptReference] is currently unavailable due to [${ex.getMessage}]",
            ex
          )
          Left(Status.INTERNAL_SERVER_ERROR)
      }
  }

  def updateSubscription(pptReference: String, subscription1: Subscription)(implicit
    hc: HeaderCarrier
  ): Future[SubscriptionResponse] = {
    val timer: Timer.Context = metrics.defaultRegistry.timer("ppt.subscription.update.timer").time()
    val correlationIdHeader: (String, String) =
      correlationIdHeaderName -> UUID.randomUUID().toString

    //the update-subscription API does not accept processingDate, which is returned on display API.
    val subscription = subscription1.copy(processingDate = None)

    httpClient.PUT[Subscription, HttpResponse](url = appConfig.subscriptionUpdateUrl(pptReference),
                                               body = subscription,
                                               headers = headers :+ correlationIdHeader
    )
      .andThen { case _ => timer.stop() }
      .map {
        subscriptionUpdateResponse =>
          logger.info(
            s"Update PPT subscription sent with correlationId [${correlationIdHeader._2}] " +
              s"and pptReference [$pptReference] had response payload had response payload ${subscriptionUpdateResponse.json}"
          )

          if (Status.isSuccessful(subscriptionUpdateResponse.status))
            Try(subscriptionUpdateResponse.json.as[SubscriptionSuccessfulResponse]) match {
              case Success(successfulCreateResponse) => successfulCreateResponse
              case _ =>
                throw UpstreamErrorResponse.apply(
                  s"PPT subscription update with correlationId [${correlationIdHeader._2}] " +
                    s"and pptReference [$pptReference] failed - successful response in unexpected format",
                  Status.INTERNAL_SERVER_ERROR
                )
            }
          else
            Try(subscriptionUpdateResponse.json.as[EISSubscriptionFailureResponse]) match {
              case Success(failedCreateResponse) =>
                SubscriptionFailureResponseWithStatusCode(failedCreateResponse,
                                                          subscriptionUpdateResponse.status
                )
              case _ =>
                throw UpstreamErrorResponse.apply(
                  s"PPT subscription update with correlationId [${correlationIdHeader._2}] " +
                    s"and pptReference [$pptReference] failed - failed response in unexpected format",
                  Status.INTERNAL_SERVER_ERROR
                )
            }
      }
  }

  private def buildCreateSubscriptionErrorMessage(
    correlationId: String,
    safeId: String,
    errorMessage: String
  ) =
    s"PPT subscription create with correlationId [$correlationId] and safeId [$safeId] failed - $errorMessage"

}
