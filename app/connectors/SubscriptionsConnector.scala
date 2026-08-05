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

import models.eis.subscription.Subscription
import models.eis.subscription.create.SubscriptionResponse
import models.eis.subscriptionStatus.SubscriptionStatusResponse
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait SubscriptionsConnector {

  def getSubscriptionStatus(
    safeId: String
  )(implicit hc: HeaderCarrier): Future[Either[Int, SubscriptionStatusResponse]]

  def submitSubscription(safeNumber: String, subscription: Subscription)(implicit
    hc: HeaderCarrier
  ): Future[SubscriptionResponse]

  def getSubscription(
    pptReference: String
  )(implicit hc: HeaderCarrier): Future[Either[Int, Subscription]]

  def updateSubscription(pptReference: String, subscription1: Subscription)(implicit
    hc: HeaderCarrier
  ): Future[SubscriptionResponse]


}
