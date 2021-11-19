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

package uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.plasticpackagingtaxregistration.base.data.RegistrationTestData
import uk.gov.hmrc.plasticpackagingtaxregistration.builders.RegistrationBuilder
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.group.{
  GroupPartnershipDetails,
  GroupPartnershipSubscription
}
import uk.gov.hmrc.plasticpackagingtaxregistration.models.{GroupDetail, Registration}

class GroupPartnershipSubscriptionSpec
    extends AnyWordSpec with Matchers with RegistrationTestData with RegistrationBuilder {

  "GroupPartnershipSubscription" should {
    "build successfully" when {
      "subscribing a group" in {

        val registration =
          aRegistration(withOrganisationDetails(pptIncorporationDetails),
                        withPrimaryContactDetails(pptPrimaryContactDetails),
                        withLiabilityDetails(pptLiabilityDetails),
                        withGroupDetail(groupDetail)
          )

        val group = GroupPartnershipSubscription(registration).get
        group.allMembersControl mustBe true
        group.representativeControl mustBe true

        assertRepresentativeDetails(group.groupPartnershipDetails.head)
        assertMemberDetails(group.groupPartnershipDetails(1))
      }

      "return None when no group detail" in {
        val registration =
          aRegistration(withOrganisationDetails(pptIncorporationDetails),
                        withPrimaryContactDetails(pptPrimaryContactDetails),
                        withLiabilityDetails(pptLiabilityDetails)
          )

        val group = GroupPartnershipSubscription(registration)
        group.isDefined mustBe false
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

        val group = GroupPartnershipSubscription(registration).get
        group.groupPartnershipDetails.head.individualDetails.firstName mustBe "First"
        group.groupPartnershipDetails.head.individualDetails.lastName mustBe "Last"
        group.groupPartnershipDetails(1).individualDetails.firstName mustBe "First"
        group.groupPartnershipDetails(1).individualDetails.lastName mustBe "Last"
      }

      "when one word in name use it as full and last name" in {
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
        group.groupPartnershipDetails(1).individualDetails.firstName mustBe "OneName"
        group.groupPartnershipDetails(1).individualDetails.lastName mustBe "OneName"
      }
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

}
