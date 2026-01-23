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

package models.eis.subscription

import play.api.libs.json.{Json, OFormat}
import models.group.GroupMemberContactDetails
import models.{PartnerContactDetails, PrimaryContactDetails => PPTPrimaryContactDetails}

case class ContactDetails(email: String, telephone: String, mobileNumber: Option[String] = None)

object ContactDetails {
  implicit val format: OFormat[ContactDetails] = Json.format[ContactDetails]

  def apply(pptPrimaryContactDetails: PPTPrimaryContactDetails): ContactDetails =
    ContactDetails(
      email = pptPrimaryContactDetails.email.getOrElse(
        throw new IllegalStateException("Email is required")
      ),
      telephone = pptPrimaryContactDetails.phoneNumber.getOrElse(
        throw new IllegalStateException("Phone Number is required")
      )
    )

  def apply(groupMemberContactDetails: GroupMemberContactDetails): ContactDetails =
    ContactDetails(
      email = groupMemberContactDetails.email.getOrElse(
        throw new IllegalStateException("Email is required")
      ),
      telephone = groupMemberContactDetails.phoneNumber.getOrElse(
        throw new IllegalStateException("Phone Number is required")
      )
    )

  def apply(partnerContactDetails: PartnerContactDetails): ContactDetails =
    ContactDetails(
      email = partnerContactDetails.emailAddress.getOrElse(
        throw new IllegalStateException("Email is required")
      ),
      telephone = partnerContactDetails.phoneNumber.getOrElse(
        throw new IllegalStateException("Phone Number is required")
      )
    )

}
