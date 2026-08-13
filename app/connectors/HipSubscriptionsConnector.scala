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
import models.eis.subscription.Subscription
import models.eis.subscription.create.SubscriptionResponse
import models.eis.subscriptionStatus.SubscriptionStatusResponse
import play.api.http.Status
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HipSubscriptionsConnector @Inject()(
  httpClient: HttpClientV2,
  override val appConfig: AppConfig,
  metrics: Metrics
)(implicit ec: ExecutionContext)
    extends SubscriptionsConnector with HipConnector {

  def getSubscriptionStatus(
    safeId: String
  )(implicit hc: HeaderCarrier): Future[Either[Int, SubscriptionStatusResponse]] = ???

  def submitSubscription(safeNumber: String, subscription: Subscription)(implicit
    hc: HeaderCarrier
  ): Future[SubscriptionResponse] = ???

  def getSubscription(
    pptReferenceNumber: String
  )(implicit hc: HeaderCarrier): Future[Either[Int, Subscription]] = {
    val timer               = metrics.defaultRegistry.timer("ppt.subscription.display.timer").time()
    val url = url"${appConfig.hipPPTBaseUrl}/RESTAdapter/plastic-packaging-tax/subscriptions/PPT/${pptReferenceNumber}"
    httpClient
      .get(url)
      .setHeader(headers*)
      .execute[HttpResponse]
      .andThen { case _ => timer.stop() }
      .map { response =>
        response.status match {
          case 200 =>
            logger.info(
              s"PPT view subscription with correlationid [${correlationid}] and pptReference [$pptReferenceNumber]"
            )
            Right((response.json \ "success").as[Subscription])
          case _ =>
            Left(response.status) // TODO work out if this needs refining
        }
      }
      .recover {
        case httpEx: UpstreamErrorResponse =>
          logger.warn(
            s"Upstream error returned on viewing subscription with correlationId [${correlationid}] and " +
              s"pptReference [$pptReferenceNumber], status: ${httpEx.statusCode}, body: ${httpEx.getMessage}"
          )
          Left(httpEx.statusCode)
        case ex: Exception =>
          logger.warn(
            s"Subscription display with correlationId [$correlationid}] and " +
              s"pptReference [$pptReferenceNumber] is currently unavailable due to [${ex.getMessage}]",
            ex
          )
          Left(Status.INTERNAL_SERVER_ERROR)
      }
  }

  override def updateSubscription(
    pptReference: String,
    subscription1: Subscription
  )(implicit hc: HeaderCarrier): Future[SubscriptionResponse] = ???

}
