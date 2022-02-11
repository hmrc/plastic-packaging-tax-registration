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

package uk.gov.hmrc.plasticpackagingtaxregistration.models

import org.joda.time.{DateTime, DateTimeZone}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.{
  ChangeOfCircumstanceDetails,
  CustomerType,
  Subscription
}
import uk.gov.hmrc.plasticpackagingtaxregistration.models.OrgType.{
  OVERSEAS_COMPANY_UK_BRANCH,
  REGISTERED_SOCIETY,
  SOLE_TRADER,
  UK_COMPANY
}
import uk.gov.hmrc.plasticpackagingtaxregistration.models.RegType.RegType
import uk.gov.hmrc.plasticpackagingtaxregistration.models.group.{
  GroupMember,
  GroupMemberContactDetails,
  OrganisationDetails => GroupDetails
}

import java.time.LocalDate
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
  lastModifiedDateTime: Option[DateTime] = None,
  changeOfCircumstanceDetails: Option[ChangeOfCircumstanceDetails] = None,
  processingDate: Option[String] = None
) {

  def updateLastModified(): Registration =
    this.copy(lastModifiedDateTime = Some(DateTime.now(DateTimeZone.UTC)))

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

  implicit val dateFormatDefault: Format[DateTime] = new Format[DateTime] {

    override def reads(json: JsValue): JsResult[DateTime] =
      JodaReads.DefaultJodaDateTimeReads.reads(json)

    override def writes(o: DateTime): JsValue = JodaWrites.JodaDateTimeNumberWrites.writes(o)
  }

  implicit val format: OFormat[Registration] = Json.format[Registration]

  def apply(subscription: Subscription): Registration = {

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
    }

    val incorporationDetails = organisationType match {
      case UK_COMPANY | REGISTERED_SOCIETY | OVERSEAS_COMPANY_UK_BRANCH =>
        Some(
          IncorporationDetails(
            companyNumber = subscription.legalEntityDetails.customerIdentification1,
            companyName = subscription.legalEntityDetails.customerDetails.organisationDetails.map(
              _.organisationName
            ).getOrElse(illegalState("Missing organisation name")),
            ctutr = subscription.legalEntityDetails.customerIdentification2.getOrElse(
              illegalState("Missing organisation UTR")
            ),
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
        // Subscription presents Partners on the groupPartnershipDetails field
        val subscriptionPartners = subscription.groupPartnershipSubscription.map(
          _.groupPartnershipDetails
        ).getOrElse(Seq.empty)
        val partners = subscriptionPartners.map { subscriptionPartner =>
          val partnerType = subscriptionPartner.organisationDetails.organisationType.map(
            PartnerTypeEnum.withName
          ).getOrElse {
            throw new IllegalStateException("Partner partner type absent")
          }

          val partnerContactDetails =
            PartnerContactDetails(firstName =
                                    Option(subscriptionPartner.individualDetails.firstName),
                                  lastName = Option(subscriptionPartner.individualDetails.lastName),
                                  emailAddress = Option(subscriptionPartner.contactDetails.email),
                                  phoneNumber =
                                    Option(subscriptionPartner.contactDetails.telephone),
                                  address = Some(PPTAddress(subscriptionPartner.addressDetails))
            )

          val isIncorporatedType =
            PartnerTypeEnum.partnerTypesWhichMightContainIncorporationDetails.contains(partnerType)
          val customerIdentification1      = subscriptionPartner.customerIdentification1
          val mayBeCustomerIdentification2 = subscriptionPartner.customerIdentification2

          val partnerIncorporationDetails = if (isIncorporatedType) {
            val customerIdentification2 = mayBeCustomerIdentification2.getOrElse {
              throw new IllegalStateException(
                "Incorporation details required customerIdentification2 which was absent"
              )
            }
            Some(
              IncorporationDetails(companyNumber = customerIdentification1,
                                   companyName =
                                     subscriptionPartner.organisationDetails.organisationName,
                                   ctutr =
                                     customerIdentification2,
                                   companyAddress = IncorporationAddressDetails(),
                                   registration = None
              )
            )
          } else
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
                partnershipName = None, // Not set in test data; is it used?,
                partnershipBusinessDetails = Some(
                  PartnershipBusinessDetails(postcode = customerIdentification2,
                                             sautr = customerIdentification1,
                                             companyProfile = Some(
                                               CompanyProfile(
                                                 companyNumber = customerIdentification2,
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
              UUID.randomUUID().toString, // TODO Partner.id is not mapped in Subscription so no stable ids or urls
            partnerType = Some(partnerType),
            contactDetails = Some(partnerContactDetails),
            incorporationDetails = partnerIncorporationDetails,
            soleTraderDetails = partnerSoleTraderDetails,
            partnerPartnershipDetails = partnerPartnershipDetails
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
                                   subscription.legalEntityDetails.customerIdentification2.getOrElse(
                                     illegalState("Missing partnership postcode")
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
                                                  regWithoutIDFlag =
                                                    subscription.legalEntityDetails.regWithoutIDFlag
    )
    val liabilityDetails = LiabilityDetails(
      startDate =
        Some(Date(LocalDate.parse(subscription.taxObligationStartDate))),
      weight = Some(LiabilityWeight(Some(subscription.last12MonthTotalTonnageAmt.longValue())))
    )
    val groupDetail =
      if (subscription.legalEntityDetails.groupSubscriptionFlag)
        subscription.groupPartnershipSubscription match {
          case Some(groupPartnershipSubscription) =>
            Some(
              GroupDetail(
                membersUnderGroupControl = Some(groupPartnershipSubscription.allMembersControl),
                members = groupPartnershipSubscription.groupPartnershipDetails.filterNot(
                  _.relationship == "Representative"
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

    Registration(id = "UPDATE",
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
