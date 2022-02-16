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
import uk.gov.hmrc.plasticpackagingtaxregistration.models.Registration

case class PrimaryContactDetails(
  name: String,
  contactDetails: ContactDetails,
  positionInCompany: String
)

object PrimaryContactDetails {
  implicit val format: OFormat[PrimaryContactDetails] = Json.format[PrimaryContactDetails]

  def apply(registration: Registration): PrimaryContactDetails =
    if (registration.isPartnershipWithPartnerCollection) {
      val nominatedPartner = registration.organisationDetails.partnershipDetails.flatMap(
        _.partners.headOption
      ).getOrElse(throw new IllegalStateException("Nominated partner absent"))

      val nominatedPartnerContactDetails = nominatedPartner.contactDetails.getOrElse(
        throw new IllegalStateException("Nominated partner contact details absent")
      )

      val nominatedPartnerContactFirstName = nominatedPartnerContactDetails.firstName.getOrElse(
        throw new IllegalStateException("Nominated partner contact first name absent")
      )
      val nominatedPartnerContactLastName = nominatedPartnerContactDetails.lastName.getOrElse(
        throw new IllegalStateException("Nominated partner contact last name absent")
      )

      PrimaryContactDetails(
        name =
          s"$nominatedPartnerContactFirstName $nominatedPartnerContactLastName",
        positionInCompany =
          nominatedPartner.contactDetails.flatMap(_.jobTitle).getOrElse("Nominated Partner"),
        contactDetails = ContactDetails(nominatedPartnerContactDetails)
      )
    } else
      PrimaryContactDetails(
        name =
          registration.primaryContactDetails.name.getOrElse(
            throw new IllegalStateException("Primary contact name absent")
          ),
        positionInCompany =
          registration.primaryContactDetails.jobTitle.getOrElse(
            throw new IllegalStateException("Primary contact job title absent")
          ),
        contactDetails =
          ContactDetails(registration.primaryContactDetails)
      )

}
