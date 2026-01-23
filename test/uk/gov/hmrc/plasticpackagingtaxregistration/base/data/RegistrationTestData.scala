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

package base.data

import models.PartnerTypeEnum.{
  GENERAL_PARTNERSHIP,
  LIMITED_LIABILITY_PARTNERSHIP,
  SCOTTISH_PARTNERSHIP
}
import models._
import models.group.{
  GroupMember,
  GroupMemberContactDetails,
  OrganisationDetails => GroupOrganisationDetails
}

import scala.language.implicitConversions

trait RegistrationTestData {

  implicit def toPostcode(value: String): PostCodeWithoutSpaces = PostCodeWithoutSpaces(value)

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

  protected val registrationDetails: RegistrationDetails =
    RegistrationDetails(identifiersMatch = true,
                        verificationStatus = Some("PASS"),
                        registrationStatus = "REGISTERED",
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

  protected val groupMemberContactDetails: GroupMemberContactDetails = GroupMemberContactDetails(
    firstName = "Test",
    lastName = "User",
    email = Some("some@test"),
    phoneNumber = Some("1234567890"),
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
                           ctutr = Some("987654321"),
                           companyAddress = IncorporationAddressDetails(),
                           registration = Some(registrationDetails)
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
                                     ninoOrTrn = "567890123",
                                     sautr = Some("123456789"),
                                     registration = Some(registrationDetails)
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
                         partnershipBusinessDetails = Some(
                           PartnershipBusinessDetails(sautr = "7454768902",
                                                      postcode = "AA1 1AA",
                                                      registration = Some(registrationDetails),
                                                      companyProfile = None
                           )
                         ),
                         partners =
                           Seq(aUkCompanyPartner(),
                               aSoleTraderPartner(),
                               aPartnershipPartner(),
                               aNonIncorpPartnershipPartner()
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
                         partnershipBusinessDetails = Some(
                           PartnershipBusinessDetails(sautr = "1435676545",
                                                      postcode = "BB1 1BB",
                                                      registration =
                                                        Some(registrationDetails),
                                                      companyProfile = None
                           )
                         ),
                         partners =
                           Seq(aUkCompanyPartner(),
                               aSoleTraderPartner(),
                               aPartnershipPartner(),
                               aNonIncorpPartnershipPartner()
                           )
      )
    )
  )

  protected val pptLimitedLiabilityDetails: OrganisationDetails = OrganisationDetails(
    organisationType = Some(OrgType.PARTNERSHIP),
    businessRegisteredAddress = Some(pptBusinessAddress),
    safeNumber = Some("1234567890"),
    partnershipDetails = Some(
      PartnershipDetails(partnershipType = LIMITED_LIABILITY_PARTNERSHIP,
                         partnershipBusinessDetails = Some(
                           PartnershipBusinessDetails(sautr = "1435676545",
                                                      postcode = "BB1 1BB",
                                                      registration =
                                                        Some(registrationDetails),
                                                      companyProfile = Some(
                                                        CompanyProfile(
                                                          companyNumber = "2387462",
                                                          companyName = "Plastics LLP",
                                                          companyAddress =
                                                            IncorporationAddressDetails()
                                                        )
                                                      )
                           )
                         )
      )
    )
  )

  protected def aUkCompanyPartner(): Partner =
    Partner(partnerType = Some(PartnerTypeEnum.UK_COMPANY),
            incorporationDetails = Some(
              IncorporationDetails(companyNumber = "12345678",
                                   companyName = "Plastics Inc",
                                   ctutr = Some("ABC123456"),
                                   companyAddress = IncorporationAddressDetails(),
                                   registration = Some(
                                     RegistrationDetails(identifiersMatch = true,
                                                         verificationStatus = Some("VERIFIED"),
                                                         registrationStatus = "REGISTERED",
                                                         registeredBusinessPartnerId =
                                                           Some("XM12345678")
                                     )
                                   )
              )
            ),
            contactDetails = Some(
              PartnerContactDetails(firstName = Some("Robert"),
                                    lastName = Some("Benkson"),
                                    emailAddress = Some("robertbenkson@plastics-inc.com"),
                                    phoneNumber = Some("07976123456"),
                                    address = Some(
                                      PPTAddress(addressLine1 = "200 Old Lane",
                                                 townOrCity = "Leeds",
                                                 postCode = Some("LS1 1HS")
                                      )
                                    ),
                                    jobTitle = Some("Director")
              )
            )
    )

  protected def aSoleTraderPartner(): Partner =
    Partner(partnerType = Some(PartnerTypeEnum.SOLE_TRADER),
            soleTraderDetails = Some(
              SoleTraderIncorporationDetails(firstName = "Steve",
                                             lastName = "Knight",
                                             dateOfBirth = Some("1971-02-03"),
                                             ninoOrTrn = "1234567890XYZ",
                                             sautr = Some("123ABC456DEF"),
                                             registration = Some(
                                               RegistrationDetails(
                                                 identifiersMatch = true,
                                                 verificationStatus = Some("VERIFIED"),
                                                 registrationStatus = "REGISTERED",
                                                 registeredBusinessPartnerId = Some("XM123456")
                                               )
                                             )
              )
            ),
            contactDetails = Some(
              PartnerContactDetails(firstName = Some("Steve"),
                                    lastName = Some("Knight"),
                                    emailAddress = Some("steve@sknight.com"),
                                    phoneNumber = Some("07976345345"),
                                    address = Some(
                                      PPTAddress(addressLine1 = "12 New Lane",
                                                 townOrCity = "Leeds",
                                                 postCode = Some("LS1 1RE")
                                      )
                                    )
              )
            )
    )

  protected def aPartnershipPartner(): Partner =
    Partner(partnerType = Some(PartnerTypeEnum.LIMITED_LIABILITY_PARTNERSHIP),
            partnerPartnershipDetails = Some(
              PartnerPartnershipDetails(partnershipName = None,
                                        partnershipBusinessDetails = Some(
                                          PartnershipBusinessDetails(sautr = "876DFS629HGE",
                                                                     postcode = "LS1 1AA",
                                                                     companyProfile = Some(
                                                                       CompanyProfile(
                                                                         companyNumber = "87654321",
                                                                         companyName =
                                                                           "Partners in Plastic",
                                                                         companyAddress =
                                                                           IncorporationAddressDetails()
                                                                       )
                                                                     ),
                                                                     registration = Some(
                                                                       RegistrationDetails(
                                                                         identifiersMatch = true,
                                                                         verificationStatus =
                                                                           Some("VERIFIED"),
                                                                         registrationStatus =
                                                                           "REGISTERED",
                                                                         registeredBusinessPartnerId =
                                                                           Some("XM123456")
                                                                       )
                                                                     )
                                          )
                                        )
              )
            ),
            contactDetails = Some(
              PartnerContactDetails(firstName = Some("Steve"),
                                    lastName = Some("Knight"),
                                    emailAddress = Some("steve@sknight.com"),
                                    phoneNumber = Some("07976345345"),
                                    address = Some(
                                      PPTAddress(addressLine1 = "12 New Lane",
                                                 townOrCity = "Leeds",
                                                 postCode = Some("LS1 1RE")
                                      )
                                    )
              )
            )
    )

  protected def aNonIncorpPartnershipPartner(): Partner =
    Partner(partnerType = Some(PartnerTypeEnum.SCOTTISH_PARTNERSHIP),
            partnerPartnershipDetails = Some(
              PartnerPartnershipDetails(partnershipName = Some("Scottish Plastic Partners"),
                                        partnershipBusinessDetails = Some(
                                          PartnershipBusinessDetails(sautr = "23947GDFD22",
                                                                     postcode = "HD1 7TE",
                                                                     companyProfile = None,
                                                                     registration = Some(
                                                                       RegistrationDetails(
                                                                         identifiersMatch = true,
                                                                         verificationStatus =
                                                                           Some("VERIFIED"),
                                                                         registrationStatus =
                                                                           "REGISTERED",
                                                                         registeredBusinessPartnerId =
                                                                           Some("XM454345")
                                                                       )
                                                                     )
                                          )
                                        )
              )
            ),
            contactDetails = Some(
              PartnerContactDetails(firstName = Some("Jonas"),
                                    lastName = Some("Jenkson"),
                                    emailAddress = Some("jonas@spp.com"),
                                    phoneNumber = Some("0765123765"),
                                    address = Some(
                                      PPTAddress(addressLine1 = "The Big House",
                                                 townOrCity = "Huddersfield",
                                                 postCode = Some("HD2 2JD")
                                      )
                                    )
              )
            )
    )

  protected val pptLimitedLiabilityPartnershipDetails: OrganisationDetails = OrganisationDetails(
    organisationType = Some(OrgType.PARTNERSHIP),
    businessRegisteredAddress = Some(pptBusinessAddress),
    safeNumber = Some("1234567890"),
    partnershipDetails = Some(
      PartnershipDetails(partnershipType = LIMITED_LIABILITY_PARTNERSHIP,
                         partnershipName = None,
                         partnershipBusinessDetails = Some(
                           PartnershipBusinessDetails(sautr = "1435676545",
                                                      postcode = "BB1 1BB",
                                                      registration =
                                                        Some(registrationDetails),
                                                      companyProfile = Some(
                                                        CompanyProfile(
                                                          companyNumber = "154648",
                                                          companyName = "Test Company",
                                                          companyAddress =
                                                            IncorporationAddressDetails()
                                                        )
                                                      )
                           )
                         )
      )
    )
  )

  protected val pptLiabilityDetails: LiabilityDetails = LiabilityDetails(
    expectedWeightNext12m = Some(LiabilityWeight(Some(10000))),
    startDate = Some(OldDate(day = Some(6), month = Some(4), year = Some(2022)))
  )

  protected val groupAddressDetails: PPTAddress = PPTAddress(addressLine1 = "Line 1",
                                                             addressLine2 = Some("Line 2"),
                                                             addressLine3 = Some("Line 3"),
                                                             townOrCity = "Line 4",
                                                             Some("post code"),
                                                             "GB"
  )

  protected val groupDetail = GroupDetail(membersUnderGroupControl = Some(true),
                                          currentMemberOrganisationType = None,
                                          members = Seq(aGroupMember())
  )

  protected def aGroupMember() =
    GroupMember(id = "some-id",
                customerIdentification1 = "customerId-1",
                customerIdentification2 =
                  Some("customerId-2"),
                organisationDetails = Some(
                  GroupOrganisationDetails(organisationType = "UkCompany",
                                           organisationName = "Plastic Company 1",
                                           businessPartnerId = None
                  )
                ),
                addressDetails = groupAddressDetails,
                contactDetails =
                  Some(groupMemberContactDetails)
    )

  protected val pptLiabilityDetailsWithExpectedWeight: LiabilityDetails = LiabilityDetails(
    weight = None,
    expectedWeightNext12m = Some(LiabilityWeight(Some(20000))),
    startDate = Some(OldDate(day = Some(6), month = Some(4), year = Some(2022)))
  )

  protected val registrationReviewed: Boolean  = true
  protected val registrationCompleted: Boolean = true

  protected val pptUserHeaders: Map[String, String] = Map("testHeaderKey" -> "testHeaderValue")

}
