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

package models.enrolmentstoreproxy

import play.api.libs.json.{Json, OFormat}
import models.KeyValue
import models.KeyValue._
import models.enrolment.{
  KnownFacts,
  UserEnrolmentRequest
}

case class QueryKnownFactsRequest(service: String, knownFacts: Seq[KeyValue])

object QueryKnownFactsRequest {
  implicit val format: OFormat[QueryKnownFactsRequest] = Json.format[QueryKnownFactsRequest]

  def apply(userEnrolment: UserEnrolmentRequest): QueryKnownFactsRequest =
    new QueryKnownFactsRequest(service = pptServiceName,
                               knownFacts = KnownFacts.from(userEnrolment)
    )

}
