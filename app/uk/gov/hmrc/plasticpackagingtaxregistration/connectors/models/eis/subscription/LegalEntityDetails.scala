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

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.CustomerType.{
  Individual,
  Organisation
}
import uk.gov.hmrc.plasticpackagingtaxregistration.models.{
  OrgType,
  PartnershipBusinessDetails,
  OrganisationDetails => PPTOrganisationDetails
}

import java.time.format.DateTimeFormatter
import java.time.{ZoneOffset, ZonedDateTime}
import scala.language.implicitConversions

case class LegalEntityDetails(
  dateOfApplication: String,
  customerIdentification1: String,
  customerIdentification2: Option[String] = None,
  customerDetails: CustomerDetails,
  groupSubscriptionFlag: Boolean,
  partnershipSubscriptionFlag: Boolean = false,
  regWithoutIDFlag: Option[Boolean] = None
) {

  val name: String = customerDetails.customerType match {
    case Organisation => customerDetails.organisationDetails.get.organisationName
    case Individual =>
      val name = customerDetails.individualDetails.get
      s"${name.firstName} ${name.lastName}"
  }

}

object LegalEntityDetails {

  implicit val format: OFormat[LegalEntityDetails] = Json.format[LegalEntityDetails]

  implicit def apply(
    pptOrganisationDetails: PPTOrganisationDetails,
    isGroup: Boolean,
    isUpdate: Boolean
  ): LegalEntityDetails =
    pptOrganisationDetails.organisationType match {
      case Some(OrgType.SOLE_TRADER) =>
        pptOrganisationDetails.soleTraderDetails.map { details =>
          updateLegalEntityDetails(customerIdentification1 = details.ninoOrTrn,
                                   customerIdentification2 = details.sautr,
                                   pptOrganisationDetails = pptOrganisationDetails,
                                   isUpdate = isUpdate
          )
        }.getOrElse(throw new Exception("Individual details are required"))
      case Some(OrgType.PARTNERSHIP) =>
        pptOrganisationDetails.partnershipDetails match {
          case Some(partnershipDetails) =>
            partnershipDetails.partnershipBusinessDetails.map { details =>
              updateLegalEntityDetails(customerIdentification1 =
                                         getCustomerIdentification1(details),
                                       customerIdentification2 =
                                         partnershipDetails.customerIdentification2,
                                       pptOrganisationDetails = pptOrganisationDetails,
                                       isUpdate = isUpdate,
                                       isGroup = isGroup,
                                       isPartnership = partnershipDetails.partners.nonEmpty
              )
            }.getOrElse(
              throw new IllegalStateException("Incorporated partnership details are required")
            )
          case _ => throw new IllegalStateException("Partnership details missing")
        }
      case _ =>
        pptOrganisationDetails.incorporationDetails.map { details =>
          updateLegalEntityDetails(customerIdentification1 = details.companyNumber,
                                   customerIdentification2 = details.ctutr,
                                   pptOrganisationDetails = pptOrganisationDetails,
                                   isUpdate = isUpdate,
                                   isGroup = isGroup
          )

        }.getOrElse(throw new IllegalStateException("Incorporation details are required"))
    }

  private def getCustomerIdentification1(
    partnershipBusinessDetails: PartnershipBusinessDetails
  ): String =
    partnershipBusinessDetails.sautr

  private def getDateOfApplication: String =
    ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

  private def updateLegalEntityDetails(
    customerIdentification1: String,
    customerIdentification2: Option[String],
    pptOrganisationDetails: PPTOrganisationDetails,
    isUpdate: Boolean,
    isGroup: Boolean = false,
    isPartnership: Boolean = false
  ): LegalEntityDetails =
    LegalEntityDetails(dateOfApplication = getDateOfApplication,
                       customerIdentification1 = customerIdentification1,
                       customerIdentification2 =
                         customerIdentification2,
                       customerDetails = CustomerDetails(pptOrganisationDetails),
                       groupSubscriptionFlag = isGroup,
                       partnershipSubscriptionFlag = isPartnership,
                       regWithoutIDFlag =
                         if (isUpdate && isGroup) Some(true)
                         else pptOrganisationDetails.regWithoutIDFlag
    )

}
