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

package models

import models.OrgType.{
  CHARITABLE_INCORPORATED_ORGANISATION,
  OVERSEAS_COMPANY_NO_UK_BRANCH,
  OVERSEAS_COMPANY_UK_BRANCH,
  REGISTERED_SOCIETY,
  SOLE_TRADER,
  UK_COMPANY
}
import models.RegType.RegType
import models.eis.subscription.group.GroupPartnershipDetails.Relationship
import models.eis.subscription.{ChangeOfCircumstanceDetails, CustomerType, Subscription}
import models.group.{GroupMember, GroupMemberContactDetails, OrganisationDetails => GroupDetails}

import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.time.{Instant, LocalDate, LocalDateTime}
import java.util.UUID

case class Registration(
  id: String,
  dateOfRegistration: Option[LocalDate] = Some(LocalDate.now()),
  registrationType: Option[RegType] = None,
  groupDetail: Option[GroupDetail] = None,
  incorpJourneyId: Option[String] = None,
  liabilityDetails: LiabilityDetails = LiabilityDetails(),
  primaryContactDetails: PrimaryContactDetails = PrimaryContactDetails(),
  organisationDetails: OrganisationDetails = OrganisationDetails(),
  metaData: MetaData = MetaData(),
  lastModifiedDateTime: Option[Instant] = None,
  changeOfCircumstanceDetails: Option[ChangeOfCircumstanceDetails] = None,
  processingDate: Option[String] = None
) {

  def updateLastModified(): Registration =
    this.copy(lastModifiedDateTime =
      Some(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS))
    )

  def isGroup: Boolean = registrationType.contains(RegType.GROUP)

  def isPartnership: Boolean = organisationDetails.organisationType.contains(OrgType.PARTNERSHIP)

  def isPartnershipWithPartnerCollection: Boolean =
    organisationDetails.organisationType.contains(OrgType.PARTNERSHIP) &&
      (
        organisationDetails.partnershipDetails.exists(
          _.partnershipType == PartnerTypeEnum.GENERAL_PARTNERSHIP
        ) ||
          organisationDetails.partnershipDetails.exists(
            _.partnershipType == PartnerTypeEnum.SCOTTISH_PARTNERSHIP
          ) ||
          organisationDetails.partnershipDetails.exists(
            _.partnershipType == PartnerTypeEnum.LIMITED_PARTNERSHIP
          ) ||
          organisationDetails.partnershipDetails.exists(
            _.partnershipType == PartnerTypeEnum.SCOTTISH_LIMITED_PARTNERSHIP
          )
      )

}

object Registration {

  import play.api.libs.json._

  implicit val dateFormatDefault: Format[LocalDate] =
    Format(Reads.DefaultLocalDateReads, Writes.DefaultLocalDateWrites)

  implicit val format: OFormat[Registration] = Json.format[Registration]

  def apply(subscription: Subscription): Registration = {
    val updateID                      = "UPDATE"
    def illegalState(message: String) = throw new IllegalStateException(message)

    val regType =
      if (subscription.legalEntityDetails.groupSubscriptionFlag) Some(RegType.GROUP)
      else Some(RegType.SINGLE_ENTITY)

    val contact = subscription.primaryContactDetails

    val contactDetails = PrimaryContactDetails(name = Some(contact.name),
                                               jobTitle = Some(contact.positionInCompany),
                                               email = Some(contact.contactDetails.email),
                                               phoneNumber = Some(contact.contactDetails.telephone),
                                               address = Some(
                                                 PPTAddress(
                                                   subscription.businessCorrespondenceDetails
                                                 )
                                               )
    )
    val organisationType = subscription.legalEntityDetails.customerDetails.customerType match {
      case CustomerType.Individual => SOLE_TRADER
      case CustomerType.Organisation =>
        subscription.legalEntityDetails.customerDetails.organisationDetails.map(
          _.organisationTypeDisplayName(regType.exists(_.equals(RegType.GROUP)))
        ).getOrElse(illegalState("Missing organisation type"))
      case ct => illegalState(s"invalid customer type: $ct")
    }

    val incorporationDetails = organisationType match {
      case UK_COMPANY | REGISTERED_SOCIETY | OVERSEAS_COMPANY_UK_BRANCH |
          OVERSEAS_COMPANY_NO_UK_BRANCH | CHARITABLE_INCORPORATED_ORGANISATION |
          REGISTERED_SOCIETY =>
        Some(
          IncorporationDetails(
            companyNumber = subscription.legalEntityDetails.customerIdentification1,
            companyName = subscription.legalEntityDetails.customerDetails.organisationDetails.map(
              _.organisationName
            ).getOrElse(illegalState("Missing organisation name")),
            ctutr = subscription.legalEntityDetails.customerIdentification2,
            companyAddress = IncorporationAddressDetails(),
            registration = None
          )
        )
      case _ => None
    }
    val soleTraderDetails = organisationType match {
      case OrgType.SOLE_TRADER =>
        Some(
          SoleTraderIncorporationDetails(
            firstName = subscription.legalEntityDetails.customerDetails.individualDetails.map(
              _.firstName
            ).getOrElse(illegalState("Missing firstName")),
            lastName = subscription.legalEntityDetails.customerDetails.individualDetails.map(
              _.lastName
            ).getOrElse(illegalState("Missing lastName")),
            dateOfBirth = None,
            ninoOrTrn = subscription.legalEntityDetails.customerIdentification1,
            sautr = subscription.legalEntityDetails.customerIdentification2,
            registration = None
          )
        )
      case _ => None
    }
    val partnershipDetails = organisationType match {
      case OrgType.PARTNERSHIP =>
        // Subscription partners are stored on the groupPartnershipDetails field
        val subscriptionPartners = subscription.groupPartnershipSubscription.map(
          _.groupPartnershipDetails
        ).getOrElse(Seq.empty)
        val partners = subscriptionPartners.map { subscriptionPartner =>
          val partnerType = subscriptionPartner.organisationDetails.organisationType.map(
            PartnerTypeEnum.withName
          ).getOrElse {
            throw new IllegalStateException("Partner partner type absent")
          }

          val isNominatedPartner = subscriptionPartner == subscriptionPartners.head
          val positionInCompany =
            if (isNominatedPartner)
              Some(contact.positionInCompany)
            else
              None

          val partnerContactDetails =
            PartnerContactDetails(firstName =
                                    Option(subscriptionPartner.individualDetails.firstName),
                                  lastName = Option(subscriptionPartner.individualDetails.lastName),
                                  emailAddress = Option(subscriptionPartner.contactDetails.email),
                                  phoneNumber =
                                    Option(subscriptionPartner.contactDetails.telephone),
                                  address = Some(PPTAddress(subscriptionPartner.addressDetails)),
                                  jobTitle = positionInCompany
            )

          val isIncorporatedType =
            PartnerTypeEnum.partnerTypesWhichMightContainIncorporationDetails.contains(partnerType)
          val customerIdentification1      = subscriptionPartner.customerIdentification1
          val mayBeCustomerIdentification2 = subscriptionPartner.customerIdentification2

          val partnerIncorporationDetails =
            if (isIncorporatedType)
              Some(
                IncorporationDetails(companyNumber = customerIdentification1,
                                     companyName =
                                       subscriptionPartner.organisationDetails.organisationName,
                                     ctutr = mayBeCustomerIdentification2,
                                     companyAddress = IncorporationAddressDetails(),
                                     registration = None
                )
              )
            else
              None

          val isSoleTraderType = partnerType == PartnerTypeEnum.SOLE_TRADER
          val partnerSoleTraderDetails =
            if (isSoleTraderType)
              Some(
                SoleTraderIncorporationDetails(
                  firstName = subscriptionPartner.individualDetails.firstName,
                  lastName = subscriptionPartner.individualDetails.lastName,
                  dateOfBirth = None, // Not persisted on Subscription; cannot be be round tripped
                  ninoOrTrn = customerIdentification1,
                  sautr = subscriptionPartner.customerIdentification2,
                  registration = None
                )
              )
            else
              None

          val isPartnershipType =
            PartnerTypeEnum.partnerTypesWhichRepresentPartnerships.contains(partnerType)
          val partnerPartnershipDetails = if (isPartnershipType) {
            val customerIdentification2 = mayBeCustomerIdentification2.getOrElse {
              throw new IllegalStateException(
                "Partner Partnership details required customerIdentification2 which was absent"
              )
            }

            Some(
              PartnerPartnershipDetails(
                partnershipName = Some(subscriptionPartner.organisationDetails.organisationName),
                partnershipBusinessDetails = Some(
                  PartnershipBusinessDetails(
                    postcode = PostCodeWithoutSpaces(customerIdentification2),
                    sautr = customerIdentification1,
                    companyProfile = Some(
                      CompanyProfile(companyNumber = customerIdentification2,
                                     companyName =
                                       subscriptionPartner.organisationDetails.organisationName,
                                     companyAddress = IncorporationAddressDetails()
                      )
                    ),
                    registration = None
                  )
                )
              )
            )
          } else
            None

          Partner(
            id =
              UUID.randomUUID().toString, // Partner.id is not mapped in Subscription so ids and urls will not be stable
            partnerType = Some(partnerType),
            contactDetails = Some(partnerContactDetails),
            incorporationDetails = partnerIncorporationDetails,
            soleTraderDetails = partnerSoleTraderDetails,
            partnerPartnershipDetails = partnerPartnershipDetails,
            regWithoutIDFlag = subscriptionPartner.regWithoutIDFlag
          )
        }

        val maybePartnershipTypeField =
          subscription.legalEntityDetails.customerDetails.organisationDetails.flatMap(
            _.organisationType
          )
        val partnershipType = maybePartnershipTypeField.map { t =>
          PartnerTypeEnum.withName(t)
        }.getOrElse {
          illegalState("Missing partnershipType")
        }

        Some(
          PartnershipDetails(partnershipType = partnershipType,
                             subscription.legalEntityDetails.customerDetails.organisationDetails.map(
                               _.organisationName
                             ),
                             partnershipBusinessDetails = Some(
                               PartnershipBusinessDetails(
                                 sautr = subscription.legalEntityDetails.customerIdentification1,
                                 postcode =
                                   PostCodeWithoutSpaces(
                                     subscription.legalEntityDetails.customerIdentification2.getOrElse(
                                       illegalState("Missing partnership postcode")
                                     )
                                   ),
                                 registration = None,
                                 companyProfile = None
                               )
                             ),
                             partners = partners
          )
        )
      case _ => None
    }

    def isGroupSubscription(subscription: Subscription) =
      subscription.legalEntityDetails.groupSubscriptionFlag

    val organisationDetails = OrganisationDetails(organisationType = Some(organisationType),
                                                  businessRegisteredAddress =
                                                    Some(
                                                      PPTAddress(
                                                        subscription.principalPlaceOfBusinessDetails.addressDetails
                                                      )
                                                    ),
                                                  safeNumber = None,
                                                  soleTraderDetails = soleTraderDetails,
                                                  partnershipDetails = partnershipDetails,
                                                  incorporationDetails = incorporationDetails,
                                                  subscriptionStatus = None,
                                                  regWithoutIDFlag = if (
                                                    isGroupSubscription(subscription)
                                                  )
                                                    subscription.groupPartnershipSubscription.flatMap(
                                                      _.groupPartnershipDetails.headOption.flatMap(
                                                        _.regWithoutIDFlag
                                                      )
                                                    )
                                                  else
                                                    subscription.legalEntityDetails.regWithoutIDFlag
    )

    val liabilityDetails = LiabilityDetails(
      startDate =
        Some(OldDate(LocalDate.parse(subscription.taxObligationStartDate))),
      expectedWeightNext12m =
        Some(LiabilityWeight(Some(subscription.last12MonthTotalTonnageAmt.longValue())))
    )

    val groupDetail =
      if (subscription.legalEntityDetails.groupSubscriptionFlag)
        subscription.groupPartnershipSubscription match {
          case Some(groupPartnershipSubscription) =>
            Some(
              GroupDetail(
                membersUnderGroupControl = Some(groupPartnershipSubscription.allMembersControl),
                members = groupPartnershipSubscription.groupPartnershipDetails.filterNot(
                  _.relationship == Relationship.Representative
                ).map(
                  detail =>
                    GroupMember(id = UUID.randomUUID().toString,
                                customerIdentification1 = detail.customerIdentification1,
                                customerIdentification2 = detail.customerIdentification2,
                                organisationDetails =
                                  Some(GroupDetails(detail.organisationDetails)),
                                addressDetails = PPTAddress(detail.addressDetails),
                                contactDetails = Some(GroupMemberContactDetails(detail)),
                                regWithoutIDFlag = detail.regWithoutIDFlag
                    )
                )
              )
            )
          case _ => None
        }
      else
        None

    Registration(id = updateID,
                 dateOfRegistration =
                   Some(LocalDate.parse(subscription.legalEntityDetails.dateOfApplication)),
                 registrationType = regType,
                 groupDetail = groupDetail,
                 incorpJourneyId = None,
                 liabilityDetails = liabilityDetails,
                 primaryContactDetails = contactDetails,
                 organisationDetails = organisationDetails,
                 metaData = MetaData(),
                 lastModifiedDateTime = None,
                 changeOfCircumstanceDetails = subscription.changeOfCircumstanceDetails,
                 processingDate = subscription.processingDate
    )
  }

}
