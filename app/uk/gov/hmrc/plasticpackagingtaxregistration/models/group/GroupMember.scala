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

package uk.gov.hmrc.plasticpackagingtaxregistration.models.group

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.plasticpackagingtaxregistration.models.{PPTAddress, PrimaryContactDetails}

case class GroupMember(
  id: String,
  customerIdentification1: String,
  customerIdentification2: Option[String],
  organisationDetails: Option[OrganisationDetails],
  primaryContactDetails: Option[PrimaryContactDetails],
  addressDetails: PPTAddress,
  regWithoutIDFlag: Option[Boolean] = None
)

object GroupMember {
  implicit val format: OFormat[GroupMember] = Json.format[GroupMember]
}
