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

package uk.gov.hmrc.plasticpackagingtaxregistration.models.nrs

import play.api.libs.json._

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId, ZonedDateTime}

case class NonRepudiationMetadata(
  businessId: String,
  notableEvent: String,
  payloadContentType: String,
  payloadSha256Checksum: String,
  userSubmissionTimestamp: ZonedDateTime,
  identityData: IdentityData,
  userAuthToken: String,
  headerData: Map[String, String],
  searchKeys: Map[String, String]
)

object NonRepudiationMetadata {
  implicit val format: OFormat[NonRepudiationMetadata] = Json.format[NonRepudiationMetadata]

  implicit object LocalDateTimeFormat extends Format[LocalDateTime] {

    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter
      .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
      .withZone(ZoneId.of("UTC"))

    override def writes(localDateTime: LocalDateTime): JsValue =
      JsString(localDateTime.format(dateTimeFormatter))

    override def reads(json: JsValue): JsResult[LocalDateTime] =
      json.validate[String].map(LocalDateTime.parse(_, dateTimeFormatter))

  }

}
