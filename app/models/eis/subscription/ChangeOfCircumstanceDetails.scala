/*
 * Copyright 2024 HM Revenue & Customs
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

package models.eis.subscription

import play.api.libs.json.{Format, Json, OFormat, Reads, Writes}

object ChangeOfCircumstance extends Enumeration {
  type ChangeOfCircumstance = Value
  val UPDATE_TO_DETAILS: Value = Value("Update to details")
  val MANUAL_TO_ONLINE: Value  = Value("Manual to Online")

  implicit val format: Format[ChangeOfCircumstance] =
    Format(Reads.enumNameReads(ChangeOfCircumstance), Writes.enumNameWrites)

}

case class ChangeOfCircumstanceDetails(
  changeOfCircumstance: String,
  deregistrationDetails: Option[DeregistrationDetails] = None
)

object ChangeOfCircumstanceDetails {

  implicit val format: OFormat[ChangeOfCircumstanceDetails] =
    Json.format[ChangeOfCircumstanceDetails]

}
