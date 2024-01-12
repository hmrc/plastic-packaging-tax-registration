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

package models.eis.subscription

import play.api.libs.json.Json
import models.Registration

case class PrincipalPlaceOfBusinessDetails(
  addressDetails: AddressDetails,
  contactDetails: ContactDetails
)

object PrincipalPlaceOfBusinessDetails {
  implicit val format = Json.format[PrincipalPlaceOfBusinessDetails]

  def apply(registration: Registration): PrincipalPlaceOfBusinessDetails = {
    val registeredBusinessAddress = registration.organisationDetails.registeredBusinessAddress
    val emailAddress              = getEmailAddress(registration)
    val phoneNumber               = getPhoneNumber(registration)

    PrincipalPlaceOfBusinessDetails(addressDetails = AddressDetails(registeredBusinessAddress),
                                    contactDetails =
                                      ContactDetails(email = emailAddress, telephone = phoneNumber)
    )
  }

  private def getEmailAddress(registration: Registration): String =
    if (registration.isPartnershipWithPartnerCollection)
      registration.organisationDetails.partnershipDetails.flatMap(
        _.partners.headOption.flatMap(_.contactDetails.flatMap(_.emailAddress))
      ).getOrElse(throw new IllegalStateException("Nominated partner email address absent"))
    else
      registration.primaryContactDetails.email.getOrElse(
        throw new IllegalStateException("Primary contact details email address absent")
      )

  private def getPhoneNumber(registration: Registration): String =
    if (registration.isPartnershipWithPartnerCollection)
      registration.organisationDetails.partnershipDetails.flatMap(
        _.partners.headOption.flatMap(_.contactDetails.flatMap(_.phoneNumber))
      ).getOrElse(throw new IllegalStateException("Nominated partner phone number absent"))
    else
      registration.primaryContactDetails.phoneNumber.getOrElse(
        throw new IllegalStateException("Primary contact details phone number absent")
      )

}
