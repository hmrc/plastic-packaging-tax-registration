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

import com.kenshoo.play.metrics.Metrics
import play.api.http.Status
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.plasticpackagingtaxregistration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.EISError
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.{
  Subscription,
  SubscriptionCreateFailureResponse,
  SubscriptionCreateSuccessfulResponse
}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscriptionStatus.SubscriptionStatusResponse

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

@Singleton
class SubscriptionsConnector @Inject() (
  httpClient: HttpClient,
  override val appConfig: AppConfig,
  metrics: Metrics
)(implicit ec: ExecutionContext)
    extends EISConnector {

  def getSubscriptionStatus(
    safeId: String
  )(implicit hc: HeaderCarrier): Future[SubscriptionStatusResponse] = {

    val timer               = metrics.defaultRegistry.timer("ppt.subscription.status.timer").time()
    val correlationIdHeader = correlationId -> UUID.randomUUID().toString

    httpClient.GET[SubscriptionStatusResponse](appConfig.subscriptionStatusUrl(safeId),
                                               headers = headers :+ correlationIdHeader
    )
      .andThen { case _ => timer.stop() }
  }

  def submitSubscription(safeNumber: String, subscription: Subscription)(implicit
    hc: HeaderCarrier
  ): Future[SubscriptionCreateSuccessfulResponse] = {

    val timer               = metrics.defaultRegistry.timer("ppt.subscription.submission.timer").time()
    val correlationIdHeader = correlationId -> UUID.randomUUID().toString

    httpClient.POST[Subscription, HttpResponse](url = appConfig.subscriptionCreateUrl(safeNumber),
                                                body = subscription,
                                                headers = headers :+ correlationIdHeader
    )
      .andThen { case _ => timer.stop() }
      .map {
        subscriptionResponse =>
          if (Status.isSuccessful(subscriptionResponse.status))
            Try(subscriptionResponse.json.as[SubscriptionCreateSuccessfulResponse]) match {
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
            Try(subscriptionResponse.json.as[SubscriptionCreateFailureResponse]) match {
              case Success(failedCreateResponse) =>
                failedCreateResponse.failures.head match {
                  case EISError(_, reason) =>
                    throw UpstreamErrorResponse.apply(
                      buildCreateSubscriptionErrorMessage(correlationIdHeader._2,
                                                          safeNumber,
                                                          reason
                      ),
                      subscriptionResponse.status
                    )
                }
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

  private def buildCreateSubscriptionErrorMessage(
    correlationId: String,
    safeId: String,
    errorMessage: String
  ) =
    s"PPT subscription with Correlation ID [$correlationId] and Safe ID [$safeId] failed - $errorMessage"

}
