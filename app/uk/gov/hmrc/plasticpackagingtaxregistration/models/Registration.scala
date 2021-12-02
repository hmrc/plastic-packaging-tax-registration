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

import org.joda.time.{DateTime, DateTimeZone}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.{ChangeOfCircumstanceDetails, Subscription}
import uk.gov.hmrc.plasticpackagingtaxregistration.models.RegType.RegType

import java.time.LocalDate

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
  changeOfCircumstanceDetails: Option[ChangeOfCircumstanceDetails] = None
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
    val organisationType =
      subscription.legalEntityDetails.customerDetails.organisationDetails.flatMap(
        _.organisationType
      ).map(OrgType.withName)

    val incorporationDetails = organisationType match {
      case Some(OrgType.UK_COMPANY) =>
        Some(
          IncorporationDetails(
            companyNumber = subscription.legalEntityDetails.customerIdentification1,
            companyName = subscription.legalEntityDetails.customerDetails.organisationDetails.map(
              _.organisationName
            ).getOrElse(illegalState("Missing organisation name")),
            ctutr = subscription.legalEntityDetails.customerIdentification2.getOrElse(
              illegalState("Missing organisation UTR")
            ),
            businessVerificationStatus = "UPDATE",
            companyAddress = IncorporationAddressDetails(),
            registration = None
          )
        )

      // TODO - other OrgTypes
      case _ => None
    }
    val organisationDetails = OrganisationDetails(organisationType = organisationType,
      businessRegisteredAddress =
        Some(
          PPTAddress(
            subscription.principalPlaceOfBusinessDetails.addressDetails
          )
        ),
      safeNumber = None,
      soleTraderDetails = None,  // TODO
      partnershipDetails = None, // TODO
      incorporationDetails = incorporationDetails,
      subscriptionStatus = None
    )

    val liabilityDetails = LiabilityDetails(
      startDate =
        Some(Date(LocalDate.parse(subscription.taxObligationStartDate))),
      weight = Some(LiabilityWeight(Some(subscription.last12MonthTotalTonnageAmt.longValue())))
    )

    Registration(id = "UPDATE",
      registrationType = regType,
      groupDetail = None, //TODO
      incorpJourneyId = None,
      liabilityDetails = liabilityDetails,
      primaryContactDetails = contactDetails,
      organisationDetails = organisationDetails,
      metaData = MetaData(),
      lastModifiedDateTime = None
    )
  }
}
