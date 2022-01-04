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

package uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.plasticpackagingtaxregistration.models.group.GroupMemberContactDetails
import uk.gov.hmrc.plasticpackagingtaxregistration.models.{
  PrimaryContactDetails => PPTPrimaryContactDetails
}

case class ContactDetails(email: String, telephone: String, mobileNumber: Option[String] = None)

object ContactDetails {
  implicit val format: OFormat[ContactDetails] = Json.format[ContactDetails]

  def apply(pptPrimaryContactDetails: PPTPrimaryContactDetails): ContactDetails =
    ContactDetails(
      email = pptPrimaryContactDetails.email.getOrElse(throw new Exception("Email is required")),
      telephone = pptPrimaryContactDetails.phoneNumber.getOrElse(
        throw new Exception("Phone Number is required")
      )
    )

  def apply(groupMemberContactDetails: GroupMemberContactDetails): ContactDetails =
    ContactDetails(
      email = groupMemberContactDetails.email.getOrElse(throw new Exception("Email is required")),
      telephone = groupMemberContactDetails.phoneNumber.getOrElse(
        throw new Exception("Phone Number is required")
      )
    )

}
