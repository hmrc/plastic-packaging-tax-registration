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

package models.taxenrolments

import play.api.libs.json.{Json, OFormat}
import models.KeyValue

case class GroupEnrolment(
  userId: String,
  friendlyName: String = "PPT Manual Enrolment",
  `type`: String = "principal",
  verifiers: Seq[KeyValue]
)

object GroupEnrolment {
  implicit val format: OFormat[GroupEnrolment] = Json.format[GroupEnrolment]
}
