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

package models.eis.subscription.group

import models.eis.subscription
import models.eis.subscription.group.GroupPartnershipDetails.Relationship
import models.eis.subscription.{OrganisationDetails => SubscriptionOrganisationDetails}
import models.group.{GroupMember, GroupMemberContactDetails}
import models.{
  Partner,
  PrimaryContactDetails,
  Registration,
  OrganisationDetails => RegistrationOrganisationDetails
}
import play.api.libs.json.{Json, OFormat}

case class GroupPartnershipSubscription(
  representativeControl: Boolean,
  allMembersControl: Boolean,
  groupPartnershipDetails: Seq[GroupPartnershipDetails]
)

object GroupPartnershipSubscription {

  implicit val format: OFormat[GroupPartnershipSubscription] =
    Json.format[GroupPartnershipSubscription]

  def apply(
    registration: Registration,
    isSubscriptionUpdate: Boolean = false
  ): Option[GroupPartnershipSubscription] = {
    val groupReg   = isGroupRegistration(registration)
    val partnerReg = isPartnershipWithDefinedPartnerDetailRegistration(registration)

    if (groupReg || partnerReg)
      Some(
        GroupPartnershipSubscription(representativeControl = true,
                                     allMembersControl = true,
                                     groupPartnershipDetails =
                                       if (groupReg)
                                         createGroupDetails(registration, isSubscriptionUpdate)
                                       else
                                         createPartnersDetails(registration, isSubscriptionUpdate)
        )
      )
    else
      None
  }

  private def isGroupRegistration(registration: Registration) = registration.groupDetail.isDefined

  private def isPartnershipWithDefinedPartnerDetailRegistration(registration: Registration) =
    registration.organisationDetails.partnershipDetails.exists(_.partners.nonEmpty)

  private def createGroupDetails(
    registration: Registration,
    isSubscriptionUpdate: Boolean
  ): Seq[GroupPartnershipDetails] = {

    if (registration.groupDetail.nonEmpty && registration.groupDetail.get.members.isEmpty)
      throw new IllegalStateException("Group must have members")

    createRepresentative(registration.organisationDetails,
                         registration.primaryContactDetails,
                         isSubscriptionUpdate
    ) +: registration.groupDetail.map {
      groupDetail =>
        groupDetail.members.map { member =>
          createMember(member, isSubscriptionUpdate)
        }
    }.get
  }

  private def createPartnersDetails(
    registration: Registration,
    isSubscriptionUpdate: Boolean
  ): Seq[GroupPartnershipDetails] =
    registration.organisationDetails.partnershipDetails.map(
      _.partners.map(partner => createPartner(partner, isSubscriptionUpdate))
    ).getOrElse(
      throw new IllegalStateException(
        "Partner details are required for non-corp partnership subscriptions"
      )
    )

  private def createPartner(
    partner: Partner,
    isSubscriptionUpdate: Boolean
  ): GroupPartnershipDetails =
    GroupPartnershipDetails(relationship = Relationship.Partner,
                            customerIdentification1 = partner.customerIdentification1,
                            customerIdentification2 = partner.customerIdentification2,
                            organisationDetails =
                              SubscriptionOrganisationDetails(organisationType =
                                                                partner.partnerType.map(_.toString),
                                                              organisationName = partner.name
                              ),
                            individualDetails = subscription.IndividualDetails(
                              firstName = partner.contactDetails.flatMap(_.firstName).getOrElse(
                                throw new IllegalStateException("Partner contact first name absent")
                              ),
                              lastName = partner.contactDetails.flatMap(_.lastName).getOrElse(
                                throw new IllegalStateException("Partner contact last name absent")
                              )
                            ),
                            addressDetails = subscription.AddressDetails(
                              partner.contactDetails.flatMap(_.address).getOrElse(
                                throw new IllegalStateException("Partner contact address absent")
                              )
                            ),
                            contactDetails = subscription.ContactDetails(
                              partner.contactDetails.getOrElse(
                                throw new IllegalStateException("Partner contact details absent")
                              )
                            ),
                            regWithoutIDFlag =
                              if (isSubscriptionUpdate) Some(false)
                              else partner.regWithoutIDFlag
    )

  private def createRepresentative(
    organisationDetails: RegistrationOrganisationDetails,
    primaryContactDetails: PrimaryContactDetails,
    isSubscriptionUpdate: Boolean
  ): GroupPartnershipDetails =
    GroupPartnershipDetails(relationship = Relationship.Representative,
                            customerIdentification1 =
                              organisationDetails.customerIdentification1,
                            customerIdentification2 =
                              organisationDetails.customerIdentification2,
                            organisationDetails = toGroupOrganisationDetails(organisationDetails),
                            individualDetails = toIndividualDetails(primaryContactDetails),
                            addressDetails =
                              subscription.AddressDetails(
                                organisationDetails.registeredBusinessAddress
                              ),
                            contactDetails = subscription.ContactDetails(primaryContactDetails),
                            regWithoutIDFlag =
                              if (isSubscriptionUpdate)
                                getFromOrganisationDetailsOrDefaultToFalseIfNotPresent(
                                  organisationDetails
                                )
                              else organisationDetails.regWithoutIDFlag
    )

  private def getFromOrganisationDetailsOrDefaultToFalseIfNotPresent(
    organisationDetails: RegistrationOrganisationDetails
  ) =
    Some(organisationDetails.regWithoutIDFlag.getOrElse(false))

  private def createMember(
    member: GroupMember,
    isSubscriptionUpdate: Boolean
  ): GroupPartnershipDetails = {
    val groupMemberContactDetails =
      member.contactDetails.getOrElse(
        throw new IllegalStateException("Contact details are required for group member")
      )
    GroupPartnershipDetails(relationship = Relationship.Member,
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
                            addressDetails = subscription.AddressDetails(member.addressDetails),
                            contactDetails = subscription.ContactDetails(groupMemberContactDetails),
                            regWithoutIDFlag =
                              if (isSubscriptionUpdate)
                                getFromGroupMemberOrDefaultToFalseIfNotPresent(member)
                              else member.regWithoutIDFlag
    )
  }

  private def getFromGroupMemberOrDefaultToFalseIfNotPresent(groupMember: GroupMember) =
    Some(groupMember.regWithoutIDFlag.getOrElse(false))

  private def toGroupOrganisationDetails(
    regOrgDetails: RegistrationOrganisationDetails
  ): SubscriptionOrganisationDetails =
    SubscriptionOrganisationDetails(regOrgDetails.organisationType.map(orgType => orgType.toString),
                                    regOrgDetails.name
    )

  private def toIndividualDetails(
    primary: GroupMemberContactDetails
  ): subscription.IndividualDetails = {
    val firstName = primary.firstName
    val lastName  = primary.lastName
    subscription.IndividualDetails(title = None,
                                   firstName = firstName,
                                   middleName = None,
                                   lastName = lastName
    )
  }

  private def toIndividualDetails(
    primary: PrimaryContactDetails
  ): subscription.IndividualDetails = {
    val name =
      primary.name.getOrElse(throw new IllegalStateException("Primary contact name required"))

    val allNames: Array[String] = name.split(" ")
    allNames.length match {
      case 1 =>
        subscription.IndividualDetails(None, allNames(0), None, allNames(0))
      case _ =>
        subscription.IndividualDetails(None, allNames(0), None, allNames(allNames.length - 1))
    }
  }

}
