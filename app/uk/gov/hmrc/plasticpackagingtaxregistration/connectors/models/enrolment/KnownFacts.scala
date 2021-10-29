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

package uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.enrolment

import java.time.format.DateTimeFormatter

import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.KeyValue
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.KeyValue.{
  postcodeKey,
  registrationDateKey
}

object KnownFacts {

  private val registrationDateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

  def from(userEnrolmentRequest: UserEnrolmentRequest): Seq[KeyValue] =
    Seq(
      KeyValue(registrationDateKey,
               userEnrolmentRequest.registrationDate.format(registrationDateFormatter)
      ),
      KeyValue(postcodeKey, userEnrolmentRequest.postcode.getOrElse(""))
    ).filterNot(_.value.isEmpty)

}
