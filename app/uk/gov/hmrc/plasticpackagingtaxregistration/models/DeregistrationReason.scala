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

package uk.gov.hmrc.plasticpackagingtaxregistration.models

import play.api.libs.json.{Format, Reads, Writes}

object DeregistrationReason extends Enumeration {
  type DeregistrationReason = Value

  val RegisteredIncorrectly               = Value(1, "Registered Incorrectly")
  val CeasedTrading                       = Value(2, "Ceased Trading")
  val NotMetAndDoNotExpectToMeetThreshold = Value(3, "Below De-minimus")
  val WantToRegisterAsGroup               = Value(4, "Taken into Group Registration")

  implicit val format: Format[DeregistrationReason] =
    Format(Reads.enumNameReads(DeregistrationReason), Writes.enumNameWrites)

  def apply(deregistrationReason: String) =
    values.find(_.toString == deregistrationReason).getOrElse(
      throw new IllegalStateException("Unsupported deregistration reason")
    )

}
