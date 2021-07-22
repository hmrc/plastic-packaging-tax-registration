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

package uk.gov.hmrc.plasticpackagingtaxregistration.base.data

import uk.gov.hmrc.plasticpackagingtaxregistration.models._

trait RegistrationTestData {

  protected val pptAddress: Address =
    Address(addressLine1 = "addressLine1",
            addressLine2 = Some("addressLine2"),
            addressLine3 = Some("addressLine3"),
            townOrCity = "Town",
            postCode = "PostCode"
    )

  protected val incorporationRegistrationDetails: IncorporationRegistrationDetails =
    IncorporationRegistrationDetails(registrationStatus = "REGISTERED",
                                     registeredBusinessPartnerId = Some("1234567890")
    )

  protected val pptPrimaryContactDetails: PrimaryContactDetails = PrimaryContactDetails(
    fullName = Some(FullName(firstName = "Test", lastName = "User")),
    jobTitle = Some("Director"),
    email = Some("some@test"),
    phoneNumber = Some("1234567890"),
    address = Some(pptAddress)
  )

  protected val pptOrganisationDetails: OrganisationDetails = OrganisationDetails(
    isBasedInUk = Some(true),
    organisationType = Some(OrgType.UK_COMPANY),
    businessRegisteredAddress = Some(pptAddress),
    safeNumber = Some("1234567890"),
    incorporationDetails = Some(
      IncorporationDetails(companyNumber = "1234567890",
                           companyName = "Plastic Limited",
                           ctutr = "987654321",
                           companyAddress = IncorporationAddressDetails(),
                           registration = incorporationRegistrationDetails
      )
    )
  )

  protected val pptSoleTraderDetails: OrganisationDetails = OrganisationDetails(
    isBasedInUk = Some(true),
    organisationType = Some(OrgType.SOLE_TRADER),
    businessRegisteredAddress = Some(pptAddress),
    safeNumber = Some("1234567890"),
    soleTraderDetails = Some(
      SoleTraderIncorporationDetails(firstName = "Test",
                                     lastName = "User",
                                     dateOfBirth = "1978-01-01",
                                     nino = "567890123",
                                     Some("123456789"),
                                     registration = incorporationRegistrationDetails
      )
    )
  )

  protected val pptPartnershipDetails: OrganisationDetails = OrganisationDetails(
    isBasedInUk = Some(true),
    organisationType = Some(OrgType.PARTNERSHIP),
    businessRegisteredAddress = Some(pptAddress),
    safeNumber = Some("1234567890"),
    partnershipDetails = Some(
      PartnershipDetails(sautr = "1234567890",
                         postcode = "AA1 1AA",
                         registration = incorporationRegistrationDetails
      )
    )
  )

  protected val pptLiabilityDetails: LiabilityDetails = LiabilityDetails(
    weight = Some(LiabilityWeight(Some(10000))),
    startDate = Some(Date(day = Some(6), month = Some(4), year = Some(2022)))
  )

  protected val registrationReviewed: Boolean  = true
  protected val registrationCompleted: Boolean = true

  protected val pptUserHeaders: Map[String, String] = Map("testHeaderKey" -> "testHeaderValue")

}
