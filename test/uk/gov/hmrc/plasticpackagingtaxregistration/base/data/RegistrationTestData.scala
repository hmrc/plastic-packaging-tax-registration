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

import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.{
  AddressDetails,
  BusinessCorrespondenceDetails,
  ContactDetails
}
import uk.gov.hmrc.plasticpackagingtaxregistration.models.PartnershipTypeEnum.{
  GENERAL_PARTNERSHIP,
  SCOTTISH_PARTNERSHIP
}
import uk.gov.hmrc.plasticpackagingtaxregistration.models._
import uk.gov.hmrc.plasticpackagingtaxregistration.models.group.{
  GroupMember,
  OrganisationDetails => GroupOrganisationDetails
}

trait RegistrationTestData {

  protected val pptBusinessAddress: PPTAddress =
    PPTAddress(addressLine1 = "1 Some Street",
               addressLine2 = Some("Some Place"),
               addressLine3 = Some("Some Area"),
               townOrCity = "Leeds",
               postCode = Some("LS1 1AA")
    )

  protected val pptPrimaryContactAddress: PPTAddress =
    PPTAddress(addressLine1 = "2 Some Other Street",
               addressLine2 = Some("Some Other Place"),
               addressLine3 = Some("Some Other Area"),
               townOrCity = "Bradford",
               postCode = Some("BD1 1AA")
    )

  protected val incorporationRegistrationDetails: IncorporationRegistrationDetails =
    IncorporationRegistrationDetails(registrationStatus = "REGISTERED",
                                     registeredBusinessPartnerId = Some("1234567890")
    )

  protected val pptPrimaryContactDetails: PrimaryContactDetails = PrimaryContactDetails(
    name = Some("Test User"),
    jobTitle = Some("Director"),
    email = Some("some@test"),
    phoneNumber = Some("1234567890"),
    useRegisteredAddress = None,
    address = Some(pptPrimaryContactAddress)
  )

  protected val pptPrimaryContactDetailsSharingBusinessAddress: PrimaryContactDetails =
    PrimaryContactDetails(name = Some("Test User"),
                          jobTitle = Some("Director"),
                          email = Some("some@test"),
                          phoneNumber = Some("1234567890"),
                          useRegisteredAddress = Some(true),
                          address = None
    )

  protected val pptIncorporationDetails: OrganisationDetails = OrganisationDetails(
    organisationType = Some(OrgType.UK_COMPANY),
    businessRegisteredAddress = Some(pptBusinessAddress),
    safeNumber = Some("1234567890"),
    incorporationDetails = Some(
      IncorporationDetails(companyNumber = "1234567890",
                           companyName = "Plastic Limited",
                           ctutr = "987654321",
                           businessVerificationStatus = "PASS",
                           companyAddress = IncorporationAddressDetails(),
                           registration = Some(incorporationRegistrationDetails)
      )
    )
  )

  protected val pptSoleTraderDetails: OrganisationDetails = OrganisationDetails(
    organisationType = Some(OrgType.SOLE_TRADER),
    businessRegisteredAddress = Some(pptBusinessAddress),
    safeNumber = Some("1234567890"),
    soleTraderDetails = Some(
      SoleTraderIncorporationDetails(firstName = "Test",
                                     lastName = "User",
                                     dateOfBirth = Some("1978-01-01"),
                                     nino = "567890123",
                                     sautr = Some("123456789"),
                                     registration = Some(incorporationRegistrationDetails)
      )
    )
  )

  protected val pptGeneralPartnershipDetails: OrganisationDetails = OrganisationDetails(
    organisationType = Some(OrgType.PARTNERSHIP),
    businessRegisteredAddress = Some(pptBusinessAddress),
    safeNumber = Some("1234567890"),
    partnershipDetails = Some(
      PartnershipDetails(partnershipType = GENERAL_PARTNERSHIP,
                         partnershipName = Some("A general partnership"),
                         generalPartnershipDetails = Some(
                           GeneralPartnershipDetails(sautr = "7454768902",
                                                     postcode = "AA1 1AA",
                                                     registration =
                                                       Some(incorporationRegistrationDetails)
                           )
                         )
      )
    )
  )

  protected val pptScottishPartnershipDetails: OrganisationDetails = OrganisationDetails(
    organisationType = Some(OrgType.PARTNERSHIP),
    businessRegisteredAddress = Some(pptBusinessAddress),
    safeNumber = Some("1234567890"),
    partnershipDetails = Some(
      PartnershipDetails(partnershipType = SCOTTISH_PARTNERSHIP,
                         partnershipName = Some("A Scottish partnership"),
                         scottishPartnershipDetails = Some(
                           ScottishPartnershipDetails(sautr = "1435676545",
                                                      postcode = "BB1 1BB",
                                                      registration =
                                                        Some(incorporationRegistrationDetails)
                           )
                         )
      )
    )
  )

  protected val pptLiabilityDetails: LiabilityDetails = LiabilityDetails(
    weight = Some(LiabilityWeight(Some(10000))),
    startDate = Some(Date(day = Some(6), month = Some(4), year = Some(2022)))
  )

  protected val groupAddressDetails: PPTAddress = PPTAddress(addressLine1 = "Line 1",
                                                             addressLine2 = Some("Line 2"),
                                                             addressLine3 = Some("Line 3"),
                                                             townOrCity = "Line 4",
                                                             Some("postcode"),
                                                             "GB"
  )

  protected val groupDetail = GroupDetail(membersUnderGroupControl = Some(true),
                                          currentMemberOrganisationType = None,
                                          members = Seq(
                                            GroupMember(id = "some-id",
                                                        customerIdentification1 = "customerId-1",
                                                        customerIdentification2 =
                                                          Some("customerId-2"),
                                                        organisationDetails = Some(
                                                          GroupOrganisationDetails(
                                                            organisationType = "UkCompany",
                                                            organisationName = "Plastic Company 1",
                                                            businessPartnerId = None
                                                          )
                                                        ),
                                                        addressDetails = groupAddressDetails
                                            )
                                          )
  )

  protected val pptLiabilityDetailsWithExpectedWeight: LiabilityDetails = LiabilityDetails(
    weight = None,
    expectedWeight = Some(LiabilityExpectedWeight(Some(true), Some(20000))),
    startDate = Some(Date(day = Some(6), month = Some(4), year = Some(2022)))
  )

  protected val registrationReviewed: Boolean  = true
  protected val registrationCompleted: Boolean = true

  protected val pptUserHeaders: Map[String, String] = Map("testHeaderKey" -> "testHeaderValue")

  protected val ukAddressDetails: AddressDetails = AddressDetails(addressLine1 = "2-3 Scala Street",
                                                                  addressLine2 = "London",
                                                                  postalCode = Some("W1T 2HN"),
                                                                  countryCode = "GB"
  )

  protected val ukBusinessCorrespondenceDetails: BusinessCorrespondenceDetails =
    BusinessCorrespondenceDetails(addressLine1 = "2-3 Scala Street",
                                  addressLine2 = "London",
                                  postalCode = Some("W1T 2HN"),
                                  countryCode = "GB"
    )

  protected val ukContactDetails: ContactDetails =
    ContactDetails(email = "test@test.com", telephone = "02034567890")

}
