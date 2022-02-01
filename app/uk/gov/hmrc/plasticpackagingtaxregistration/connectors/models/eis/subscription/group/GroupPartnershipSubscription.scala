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

package uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.group

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.{
  AddressDetails,
  ContactDetails,
  IndividualDetails,
  OrganisationDetails => SubscriptionOrganisationDetails
}
import uk.gov.hmrc.plasticpackagingtaxregistration.models.group.{
  GroupMember,
  GroupMemberContactDetails
}
import uk.gov.hmrc.plasticpackagingtaxregistration.models.{
  Partner,
  PrimaryContactDetails,
  Registration,
  OrganisationDetails => RegistrationOrganisationDetails
}

case class GroupPartnershipSubscription(
  representativeControl: Boolean,
  allMembersControl: Boolean,
  groupPartnershipDetails: Seq[GroupPartnershipDetails]
)

object GroupPartnershipSubscription {

  implicit val format: OFormat[GroupPartnershipSubscription] =
    Json.format[GroupPartnershipSubscription]

  def apply(registration: Registration): Option[GroupPartnershipSubscription] = {
    val groupReg   = isGroupRegistration(registration)
    val partnerReg = isPartnershipWithDefinedPartnerDetailRegistration(registration)

    if (groupReg || partnerReg)
      Some(
        GroupPartnershipSubscription(representativeControl = true,
                                     allMembersControl = true,
                                     groupPartnershipDetails =
                                       if (groupReg) createGroupDetails(registration)
                                       else createPartnersDetails(registration)
        )
      )
    else
      None
  }

  private def isGroupRegistration(registration: Registration) = registration.groupDetail.isDefined

  private def isPartnershipWithDefinedPartnerDetailRegistration(registration: Registration) =
    registration.organisationDetails.partnershipDetails.exists(_.partners.nonEmpty)

  private def createGroupDetails(registration: Registration): Seq[GroupPartnershipDetails] = {

    if (registration.groupDetail.nonEmpty && registration.groupDetail.get.members.isEmpty)
      throw new IllegalStateException("Group must have members")

    createRepresentative(registration.organisationDetails,
                         registration.primaryContactDetails
    ) +: registration.groupDetail.map {
      groupDetail =>
        groupDetail.members.map { member =>
          createMember(member)
        }
    }.get
  }

  private def createPartnersDetails(registration: Registration): Seq[GroupPartnershipDetails] =
    registration.organisationDetails.partnershipDetails.map(
      _.partners.map(partner => createPartner(partner))
    ).getOrElse(
      throw new IllegalStateException(
        "Partner details are required for non-corp partnership subscriptions"
      )
    )

  private def createPartner(partner: Partner): GroupPartnershipDetails =
    GroupPartnershipDetails(relationship = "Partner",
                            customerIdentification1 = partner.customerIdentification1,
                            customerIdentification2 = partner.customerIdentification2,
                            organisationDetails =
                              SubscriptionOrganisationDetails(organisationType =
                                                                partner.partnerType.map(_.toString),
                                                              organisationName = partner.name
                              ),
                            individualDetails = IndividualDetails(
                              firstName = partner.contactDetails.flatMap(_.firstName).getOrElse(
                                throw new IllegalStateException("Partner contact first name absent")
                              ),
                              lastName = partner.contactDetails.flatMap(_.lastName).getOrElse(
                                throw new IllegalStateException("Partner contact last name absent")
                              )
                            ),
                            addressDetails = AddressDetails(
                              partner.contactDetails.flatMap(_.address).getOrElse(
                                throw new IllegalStateException("Partner contact address absent")
                              )
                            ),
                            contactDetails = ContactDetails(
                              partner.contactDetails.getOrElse(
                                throw new IllegalStateException("Partner contact details absent")
                              )
                            )
    )

  private def createRepresentative(
    organisationDetails: RegistrationOrganisationDetails,
    primaryContactDetails: PrimaryContactDetails
  ): GroupPartnershipDetails =
    GroupPartnershipDetails(relationship = "Representative",
                            customerIdentification1 =
                              organisationDetails.incorporationDetails.map(
                                details => details.companyNumber
                              ).getOrElse(
                                throw new IllegalStateException(
                                  "Incorporation details are required for group representative"
                                )
                              ),
                            customerIdentification2 =
                              organisationDetails.incorporationDetails.map(_.ctutr),
                            organisationDetails = toGroupOrganisationDetails(organisationDetails),
                            individualDetails = toIndividualDetails(primaryContactDetails),
                            addressDetails =
                              AddressDetails(organisationDetails.registeredBusinessAddress),
                            contactDetails = ContactDetails(primaryContactDetails),
                            regWithoutIDFlag = organisationDetails.regWithoutIDFlag
    )

  private def createMember(member: GroupMember): GroupPartnershipDetails = {
    val groupMemberContactDetails =
      member.contactDetails.getOrElse(
        throw new IllegalStateException("Contact details are required for group member")
      )
    GroupPartnershipDetails(relationship = "Member",
                            customerIdentification1 = member.customerIdentification1,
                            customerIdentification2 = member.customerIdentification2,
                            organisationDetails = member.organisationDetails.map { details =>
                              SubscriptionOrganisationDetails(Some(details.organisationType),
                                                              details.organisationName
                              )
                            }.getOrElse(
                              throw new IllegalStateException(
                                "Group member must have an organisation"
                              )
                            ),
                            individualDetails =
                              toIndividualDetails(groupMemberContactDetails),
                            addressDetails = AddressDetails(member.addressDetails),
                            contactDetails = ContactDetails(groupMemberContactDetails),
                            regWithoutIDFlag = member.regWithoutIDFlag
    )
  }

  private def toGroupOrganisationDetails(
    regOrgDetails: RegistrationOrganisationDetails
  ): SubscriptionOrganisationDetails =
    SubscriptionOrganisationDetails(regOrgDetails.organisationType.map(orgType => orgType.toString),
                                    regOrgDetails.incorporationDetails.map { details =>
                                      details.companyName
                                    }.getOrElse(
                                      throw new IllegalStateException(
                                        "Incorporation details are required for group representative"
                                      )
                                    )
    )

  private def toIndividualDetails(primary: GroupMemberContactDetails): IndividualDetails = {
    val firstName = primary.firstName
    val lastName  = primary.lastName
    IndividualDetails(title = None, firstName = firstName, middleName = None, lastName = lastName)
  }

  private def toIndividualDetails(primary: PrimaryContactDetails): IndividualDetails = {
    val name =
      primary.name.getOrElse(throw new IllegalStateException("Primary contact name required"))

    val allNames: Array[String] = name.split(" ")
    allNames.length match {
      case 1 =>
        IndividualDetails(None, allNames(0), None, allNames(0))
      case _ =>
        IndividualDetails(None, allNames(0), None, allNames(allNames.length - 1))
    }
  }

}
