/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscriptionStatus.ETMPSubscriptionStatus.{
  NO_FORM_BUNDLE_FOUND,
  SUCCESSFUL
}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscriptionStatus.SubscriptionStatus.{
  NOT_SUBSCRIBED,
  SUBSCRIBED,
  Status,
  UNKNOWN
}

case class SubscriptionStatusResponse(status: Status, pptReference: Option[String] = None)

object SubscriptionStatusResponse {
  implicit val format: OFormat[SubscriptionStatusResponse] = Json.format[SubscriptionStatusResponse]

  def fromETMPResponse(etmpResponse: ETMPSubscriptionStatusResponse) = {

    val status = etmpResponse.subscriptionStatus match {
      case Some(NO_FORM_BUNDLE_FOUND) => NOT_SUBSCRIBED
      case Some(SUCCESSFUL)           => SUBSCRIBED
      case _                          => UNKNOWN
    }

    val pptRef = etmpResponse.idType match {
      case Some("ZPPT") => etmpResponse.idValue
      case _            => None
    }

    SubscriptionStatusResponse(status, pptRef)
  }

}
