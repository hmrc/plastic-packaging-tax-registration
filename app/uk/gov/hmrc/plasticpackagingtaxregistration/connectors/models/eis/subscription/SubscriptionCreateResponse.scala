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

package uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.EISError

import java.time.ZonedDateTime

case class SubscriptionCreateResponse(
  pptReference: Option[String],
  processingDate: Option[ZonedDateTime],
  formBundleNumber: Option[String],
  failures: Option[Seq[EISError]] = None
) {

  def isSuccess: Boolean =
    this.failures match {
      case Some(errors) => errors.isEmpty
      case None         => true
    }

}

object SubscriptionCreateResponse {

  implicit val format: OFormat[SubscriptionCreateResponse] =
    Json.format[SubscriptionCreateResponse]

}
