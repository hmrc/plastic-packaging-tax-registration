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

package uk.gov.hmrc.plasticpackagingtaxregistration.models

import java.time.LocalDate
import java.util.UUID
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
  OrganisationDetails => GroupDetails
}

case class Registration(
  id: String,
  registrationType: Option[RegType] = None,
  groupDetail: Option[GroupDetail] = None,
  incorpJourneyId: Option[String],
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
        subscription.legalEntityDetails.customerDetails.organisationDetails.flatMap(
          _.organisationType
        ).map(OrgType.withName).getOrElse(illegalState("Missing organisation type"))
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
        Some(
          //TODO - how to work out other partnership types?
          PartnershipDetails(partnershipType = PartnershipTypeEnum.GENERAL_PARTNERSHIP,
                             partnershipName =
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
                             )
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
    val groupDetail = subscription.groupPartnershipSubscription match {
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
                            organisationDetails = Some(GroupDetails(detail.organisationDetails)),
                            addressDetails = PPTAddress(detail.addressDetails),
                            primaryContactDetails = Some(PrimaryContactDetails(detail)),
                            regWithoutIDFlag = detail.regWithoutIDFlag
                )
            )
          )
        )
      case _ => None
    }

    Registration(id = "UPDATE",
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
