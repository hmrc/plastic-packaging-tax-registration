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

package uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscriptionStatus

import play.api.libs.json.{Json, OFormat}
import ETMPSubscriptionStatus.SubscriptionStatus
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.EISError
import ETMPSubscriptionChannel.SubscriptionChannel

case class SubscriptionStatusResponse(
  subscriptionStatus: Option[SubscriptionStatus] = None,
  idType: Option[String] = None,
  idValue: Option[String] = None,
  channel: Option[SubscriptionChannel] = None,
  failures: Option[Seq[EISError]] = None
)

object SubscriptionStatusResponse {
  implicit val format: OFormat[SubscriptionStatusResponse] = Json.format[SubscriptionStatusResponse]
}
