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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.plasticpackagingtaxregistration.base.data.RegistrationTestData
import uk.gov.hmrc.plasticpackagingtaxregistration.builders.RegistrationBuilder
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.group.{
  GroupPartnershipDetails,
  GroupPartnershipSubscription
}
import uk.gov.hmrc.plasticpackagingtaxregistration.models.{GroupDetail, Partner, Registration}

class GroupPartnershipSubscriptionSpec
    extends AnyWordSpec with Matchers with RegistrationTestData with RegistrationBuilder {

  "A GroupPartnershipSubscription" when {
    "building group subscription" should {
      "transform as expected for UK Company" in {
        val groupRegistration =
          aRegistration(withOrganisationDetails(pptIncorporationDetails),
                        withPrimaryContactDetails(pptPrimaryContactDetails),
                        withLiabilityDetails(pptLiabilityDetails),
                        withGroupDetail(groupDetail)
          )

        val groupSubscription = GroupPartnershipSubscription(groupRegistration).get
        groupSubscription.allMembersControl mustBe true
        groupSubscription.representativeControl mustBe true

        assertRepresentativeDetails(groupSubscription.groupPartnershipDetails.head)
        assertMemberDetails(groupSubscription.groupPartnershipDetails(1))
      }

      "transform as expected for Limited Liability Partnership" in {
        val groupRegistration =
          aRegistration(withOrganisationDetails(pptLimitedLiabilityPartnershipDetails),
                        withPrimaryContactDetails(pptPrimaryContactDetails),
                        withLiabilityDetails(pptLiabilityDetails),
                        withGroupDetail(groupDetail)
          )

        val groupSubscription = GroupPartnershipSubscription(groupRegistration).get
        groupSubscription.allMembersControl mustBe true
        groupSubscription.representativeControl mustBe true

        val representativeMember = groupSubscription.groupPartnershipDetails.head
        representativeMember.organisationDetails.organisationType mustBe Some("Partnership")
        representativeMember.relationship mustBe "Representative"
        assertMemberDetails(groupSubscription.groupPartnershipDetails(1))
      }

      "return None when no group detail" in {
        val registration =
          aRegistration(withOrganisationDetails(pptIncorporationDetails),
                        withPrimaryContactDetails(pptPrimaryContactDetails),
                        withLiabilityDetails(pptLiabilityDetails)
          )

        val groupSubscription = GroupPartnershipSubscription(registration)
        groupSubscription.isDefined mustBe false
      }

      "split name when there are middle names" in {
        val registration =
          aRegistration(withOrganisationDetails(pptIncorporationDetails),
                        withPrimaryContactDetails(
                          pptPrimaryContactDetails.copy(name = Some("First Middle Last"))
                        ),
                        withLiabilityDetails(pptLiabilityDetails),
                        withGroupDetail(groupDetail)
          )

        val groupSubscription = GroupPartnershipSubscription(registration).get
        groupSubscription.groupPartnershipDetails.head.individualDetails.firstName mustBe "First"
        groupSubscription.groupPartnershipDetails.head.individualDetails.lastName mustBe "Last"
        groupSubscription.groupPartnershipDetails(1).individualDetails.firstName mustBe "Test"
        groupSubscription.groupPartnershipDetails(1).individualDetails.lastName mustBe "User"
      }

      "when one word in name use it as first and last name" in {
        val registration =
          aRegistration(withOrganisationDetails(pptIncorporationDetails),
                        withPrimaryContactDetails(
                          pptPrimaryContactDetails.copy(name = Some("OneName"))
                        ),
                        withLiabilityDetails(pptLiabilityDetails),
                        withGroupDetail(groupDetail)
          )

        val group = GroupPartnershipSubscription(registration).get
        group.groupPartnershipDetails.head.individualDetails.firstName mustBe "OneName"
        group.groupPartnershipDetails.head.individualDetails.lastName mustBe "OneName"
        group.groupPartnershipDetails(1).individualDetails.firstName mustBe "Test"
        group.groupPartnershipDetails(1).individualDetails.lastName mustBe "User"
      }

      "add missing regWithoutIDFlags on updates" in {
        val registration =
          aRegistration(withOrganisationDetails(pptIncorporationDetails),
                        withPrimaryContactDetails(pptPrimaryContactDetails),
                        withLiabilityDetails(pptLiabilityDetails),
                        withGroupDetail(groupDetail)
          )

        val group = GroupPartnershipSubscription(registration, isSubscriptionUpdate = true).get

        group.groupPartnershipDetails.seq.foreach(gpd => gpd.regWithoutIDFlag mustBe Some(false))
      }

      "throw an exception" when {
        "group detail members is empty" in {
          val registration: Registration =
            aRegistration(withOrganisationDetails(pptIncorporationDetails),
                          withPrimaryContactDetails(pptPrimaryContactDetails),
                          withLiabilityDetails(pptLiabilityDetails),
                          withGroupDetail(GroupDetail(None, Seq.empty, None))
            )

          intercept[Exception] {
            GroupPartnershipSubscription(registration)
          }
        }

        "incorporationDetails is None" in {
          val registration: Registration =
            aRegistration(
              withOrganisationDetails(pptIncorporationDetails.copy(incorporationDetails = None)),
              withPrimaryContactDetails(pptPrimaryContactDetails),
              withLiabilityDetails(pptLiabilityDetails),
              withGroupDetail(groupDetail)
            )

          intercept[Exception] {
            GroupPartnershipSubscription(registration)
          }
        }

        "group member organisation details is None" in {

          val registration: Registration =
            aRegistration(
              withOrganisationDetails(pptIncorporationDetails.copy(incorporationDetails = None)),
              withPrimaryContactDetails(pptPrimaryContactDetails),
              withLiabilityDetails(pptLiabilityDetails),
              withGroupDetail(
                groupDetail.copy(members =
                  Seq(groupDetail.members.head.copy(organisationDetails = None))
                )
              )
            )

          intercept[Exception] {
            GroupPartnershipSubscription(registration)
          }
        }

      }
    }

    "building partnership subscription" should {

      "transform as expected" in {
        val partnershipRegistration =
          aRegistration(withLiabilityDetails(pptLiabilityDetails),
                        withOrganisationDetails(pptGeneralPartnershipDetails)
          )

        val partnershipSubscription = GroupPartnershipSubscription(partnershipRegistration).get
        partnershipSubscription.allMembersControl mustBe true
        partnershipSubscription.representativeControl mustBe true

        assertPartnersDetails(
          partnershipSubscription,
          partnershipRegistration.organisationDetails.partnershipDetails.get.partners
        )
      }

      "throw IllegalStateException" when {
        Seq(
          ("first name",
           { partner: Partner =>
             partner.copy(contactDetails = partner.contactDetails.map(_.copy(firstName = None)))
           }
          ),
          ("last name",
           { partner: Partner =>
             partner.copy(contactDetails = partner.contactDetails.map(_.copy(lastName = None)))
           }
          ),
          ("contact address",
           { partner: Partner =>
             partner.copy(contactDetails = partner.contactDetails.map(_.copy(address = None)))
           }
          ),
          ("contact details",
           { partner: Partner =>
             partner.copy(contactDetails = None)
           }
          )
        ).foreach { testData =>
          s"partners ${testData._1} absent" in {
            val invalidPartnershipRegistration = withUpdatedPartners(
              aRegistration(withLiabilityDetails(pptLiabilityDetails),
                            withOrganisationDetails(pptGeneralPartnershipDetails)
              ),
              testData._2
            )

            intercept[IllegalStateException] {
              GroupPartnershipSubscription(invalidPartnershipRegistration)
            }
          }

        }
      }
    }

    "building single entity subscription" should {
      "return None" in {
        val singleEntityRegistration =
          aRegistration(withLiabilityDetails(pptLiabilityDetails),
                        withOrganisationDetails(pptIncorporationDetails),
                        withPrimaryContactDetails(pptPrimaryContactDetails)
          )

        GroupPartnershipSubscription(singleEntityRegistration) mustBe None
      }
    }
  }

  //TODO consider comparing with fields from Registration rather than values
  private def assertRepresentativeDetails(representative: GroupPartnershipDetails) = {
    representative.relationship mustBe "Representative"
    representative.customerIdentification1 mustBe "1234567890"
    representative.customerIdentification2 mustBe Some("987654321")

    representative.organisationDetails.organisationType mustBe Some("UkCompany")
    representative.organisationDetails.organisationName mustBe "Plastic Limited"

    representative.individualDetails.title mustBe None
    representative.individualDetails.firstName mustBe "Test"
    representative.individualDetails.middleName mustBe None
    representative.individualDetails.lastName mustBe "User"

    representative.addressDetails.addressLine1 mustBe "1 Some Street"
    representative.addressDetails.addressLine2 mustBe "Some Place"
    representative.addressDetails.addressLine3 mustBe Some("Some Area")
    representative.addressDetails.addressLine4 mustBe Some("Leeds")
    representative.addressDetails.postalCode mustBe Some("LS1 1AA")
    representative.addressDetails.countryCode mustBe "GB"

    representative.contactDetails.email mustBe "some@test"
    representative.contactDetails.telephone mustBe "1234567890"
    representative.contactDetails.mobileNumber mustBe None
  }

  private def assertMemberDetails(member: GroupPartnershipDetails) = {
    member.relationship mustBe "Member"
    member.customerIdentification1 mustBe "customerId-1"
    member.customerIdentification2 mustBe Some("customerId-2")

    member.organisationDetails.organisationType mustBe Some("UkCompany")
    member.organisationDetails.organisationName mustBe "Plastic Company 1"

    member.individualDetails.title mustBe None
    member.individualDetails.firstName mustBe "Test"
    member.individualDetails.middleName mustBe None
    member.individualDetails.lastName mustBe "User"

    member.addressDetails.addressLine1 mustBe "Line 1"
    member.addressDetails.addressLine2 mustBe "Line 2"
    member.addressDetails.addressLine3 mustBe Some("Line 3")
    member.addressDetails.addressLine4 mustBe Some("Line 4")
    member.addressDetails.postalCode mustBe Some("postcode")
    member.addressDetails.countryCode mustBe "GB"

    member.contactDetails.email mustBe "some@test"
    member.contactDetails.telephone mustBe "1234567890"
    member.contactDetails.mobileNumber mustBe None
  }

  private def assertPartnersDetails(
    subscription: GroupPartnershipSubscription,
    partners: Seq[Partner]
  ) =
    subscription.groupPartnershipDetails zip partners foreach {
      case (groupPartner, partner) =>
        groupPartner.relationship mustBe "Partner"
        groupPartner.customerIdentification1 mustBe partner.customerIdentification1
        groupPartner.customerIdentification2 mustBe partner.customerIdentification2

        groupPartner.organisationDetails.organisationType mustBe partner.partnerType.map(_.toString)
        groupPartner.organisationDetails.organisationName mustBe partner.name

        groupPartner.individualDetails.title mustBe partner.contactDetails.flatMap(_.jobTitle)
        groupPartner.individualDetails.middleName mustBe None
        groupPartner.individualDetails.firstName mustBe partner.contactDetails.flatMap(
          _.firstName
        ).get
        groupPartner.individualDetails.lastName mustBe partner.contactDetails.flatMap(
          _.lastName
        ).get

        groupPartner.addressDetails mustBe AddressDetails(
          partner.contactDetails.flatMap(_.address).get
        )

        groupPartner.contactDetails.email mustBe partner.contactDetails.flatMap(_.emailAddress).get
        groupPartner.contactDetails.telephone mustBe partner.contactDetails.flatMap(
          _.phoneNumber
        ).get
        groupPartner.contactDetails.mobileNumber mustBe None
    }

  private def withUpdatedPartners(
    registration: Registration,
    partnerUpdator: Partner => Partner
  ): Registration =
    registration.copy(organisationDetails =
      registration.organisationDetails.copy(partnershipDetails =
        registration.organisationDetails.partnershipDetails.map(
          pd => pd.copy(partners = pd.partners.map(partnerUpdator(_)))
        )
      )
    )

}
