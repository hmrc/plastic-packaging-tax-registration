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
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.plasticpackagingtaxregistration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.{
  Subscription,
  SubscriptionCreateResponse,
  SubscriptionCreateSuccessfulResponse
}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscriptionStatus.SubscriptionStatusResponse

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class SubscriptionsConnector @Inject() (
  httpClient: HttpClient,
  override val appConfig: AppConfig,
  metrics: Metrics
)(implicit ec: ExecutionContext)
    extends EISConnector {

  def getSubscriptionStatus(
    safeNumber: String
  )(implicit hc: HeaderCarrier): Future[SubscriptionStatusResponse] = {
    val timer               = metrics.defaultRegistry.timer("ppt.subscription.status.timer").time()
    val correlationIdHeader = correlationId -> UUID.randomUUID().toString
    httpClient.GET[SubscriptionStatusResponse](appConfig.subscriptionStatusUrl(safeNumber),
                                               headers = headers :+ correlationIdHeader
    )
      .andThen { case _ => timer.stop() }
      .andThen {
        case Success(response) => response
        case Failure(exception) =>
          throw new Exception(
            s"Subscription Status with Correlation ID [${correlationIdHeader._2}] and " +
              s"Safe ID [${safeNumber}] is currently unavailable due to [${exception.getMessage}]",
            exception
          )
      }
  }

  def submitSubscription(safeNumber: String, subscription: Subscription)(implicit
    hc: HeaderCarrier
  ): Future[SubscriptionCreateResponse] = {
    val timer               = metrics.defaultRegistry.timer("ppt.subscription.submission.timer").time()
    val correlationIdHeader = correlationId -> UUID.randomUUID().toString
    httpClient.POST[Subscription, SubscriptionCreateSuccessfulResponse](
      url = appConfig.subscriptionCreateUrl(safeNumber),
      body = subscription,
      headers = headers :+ correlationIdHeader
    )
      .andThen { case _ => timer.stop() }
      .andThen {
        case Success(response) => response
        case Failure(exception) =>
          throw new Exception(
            s"Subscription submission with Correlation ID [${correlationIdHeader._2}] and " +
              s"Safe ID [${safeNumber}] is currently experiencing issues due to [${exception.getMessage}]",
            exception
          )
      }
  }

}
