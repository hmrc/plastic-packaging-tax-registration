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
import uk.gov.hmrc.plasticpackagingtaxregistration.models.{
  PPTAddress,
  PostCodeWithoutSpaces,
  Registration
}

case class BusinessCorrespondenceDetails(
  addressLine1: String,
  addressLine2: String,
  addressLine3: Option[String] = None,
  addressLine4: Option[String] = None,
  postalCode: Option[PostCodeWithoutSpaces] = None,
  countryCode: String
)

object BusinessCorrespondenceDetails {

  implicit val format: OFormat[BusinessCorrespondenceDetails] =
    Json.format[BusinessCorrespondenceDetails]

  def apply(registration: Registration): BusinessCorrespondenceDetails =
    if (registration.isPartnershipWithPartnerCollection) {
      val nominatedPartner = registration.organisationDetails.partnershipDetails.flatMap(
        _.partners.headOption
      ).getOrElse(throw new IllegalStateException("Nominated partner name absent"))
      val nominatedPartnerContactDetails = nominatedPartner.contactDetails.getOrElse(
        throw new IllegalStateException("Nominated partner contact details absent")
      )
      val nominatedPartnerContactAddress = nominatedPartnerContactDetails.address.getOrElse(
        throw new IllegalStateException("Nominated partner contact address absent")
      )

      BusinessCorrespondenceDetails(nominatedPartnerContactAddress)
    } else {
      val address =
        if (registration.primaryContactDetails.useRegisteredAddress.getOrElse(false))
          registration.organisationDetails.businessRegisteredAddress.getOrElse(
            throw new IllegalStateException("The legal entity registered address is required.")
          )
        else
          registration.primaryContactDetails.address.getOrElse(
            throw new IllegalStateException("The primary contact details address is required.")
          )
      BusinessCorrespondenceDetails(address)
    }

  def apply(address: PPTAddress): BusinessCorrespondenceDetails =
    new BusinessCorrespondenceDetails(addressLine1 = address.eisAddressLines._1,
                                      addressLine2 = address.eisAddressLines._2,
                                      addressLine3 = address.eisAddressLines._3,
                                      addressLine4 = address.eisAddressLines._4,
                                      postalCode = address.postCode.filter(_.trim != ""),
                                      countryCode = address.countryCode
    )

}
