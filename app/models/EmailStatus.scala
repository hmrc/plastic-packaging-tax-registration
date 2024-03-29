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

package models

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.Reads._
import play.api.libs.json._

case class EmailStatus(emailAddress: String, verified: Boolean, locked: Boolean)

object EmailStatus {

  val apiReads: Reads[EmailStatus] = (
    (__ \ "emailAddress").read[String] and
      (__ \ "verified").read[Boolean] and
      (__ \ "locked").read[Boolean]
  )(EmailStatus.apply _)

  val apiWrites: Writes[EmailStatus] = (
    (__ \ "emailAddress").write[String] and
      (__ \ "verified").write[Boolean] and
      (__ \ "locked").write[Boolean]
  )(unlift(EmailStatus.unapply))

  val apiFormat: Format[EmailStatus] =
    Format[EmailStatus](apiReads, apiWrites)

  implicit val format: Format[EmailStatus] =
    Json.format[EmailStatus]

}
