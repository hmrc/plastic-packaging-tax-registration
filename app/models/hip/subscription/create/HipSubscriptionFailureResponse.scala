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

package models.hip.subscription.create

import play.api.libs.json.{Json, OFormat, Reads, Writes}

sealed trait HipSubscriptionFailureResponse

case class HipBusinessValidationError(processingDate: String, errorId: String, text: String)

object HipBusinessValidationError {

  implicit val format: OFormat[HipBusinessValidationError] =
    Json.format[HipBusinessValidationError]

}

/** Schema `422SubscriptionCreate` - ETMP rejected the request on business grounds. */
case class HipBusinessValidationFailure(error: HipBusinessValidationError)
    extends HipSubscriptionFailureResponse

object HipBusinessValidationFailure {

  implicit val format: OFormat[HipBusinessValidationFailure] =
    Json.format[HipBusinessValidationFailure]

}

case class HipSystemError(code: String, message: String, logID: String)

object HipSystemError {
  implicit val format: OFormat[HipSystemError] = Json.format[HipSystemError]
}

/** Schema `SystemErrorResponse` - returned for 400 and 500. */
case class HipSystemFailure(error: HipSystemError) extends HipSubscriptionFailureResponse

object HipSystemFailure {
  implicit val format: OFormat[HipSystemFailure] = Json.format[HipSystemFailure]
}

object HipSubscriptionFailureResponse {

  private val businessValidationReads: Reads[HipSubscriptionFailureResponse] =
    HipBusinessValidationFailure.format.widen

  private val systemReads: Reads[HipSubscriptionFailureResponse] = HipSystemFailure.format.widen

  implicit val reads: Reads[HipSubscriptionFailureResponse] =
    businessValidationReads.orElse(systemReads)

  implicit val writes: Writes[HipSubscriptionFailureResponse] = Writes {
    case failure: HipBusinessValidationFailure => HipBusinessValidationFailure.format.writes(failure)
    case failure: HipSystemFailure             => HipSystemFailure.format.writes(failure)
  }

}
