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

import java.time.format.DateTimeFormatter
import java.time.{ZoneOffset, ZonedDateTime}

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.plasticpackagingtaxregistration.models.PartnershipTypeEnum.{
  GENERAL_PARTNERSHIP,
  SCOTTISH_PARTNERSHIP
}
import uk.gov.hmrc.plasticpackagingtaxregistration.models.{
  OrgType,
  OrganisationDetails => PPTOrganisationDetails
}

import scala.language.implicitConversions

case class LegalEntityDetails(
  dateOfApplication: String,
  customerIdentification1: String,
  customerIdentification2: Option[String] = None,
  customerDetails: CustomerDetails,
  groupSubscriptionFlag: Boolean,
  partnershipSubscriptionFlag: Boolean = false
)

object LegalEntityDetails {

  implicit val format: OFormat[LegalEntityDetails] = Json.format[LegalEntityDetails]

  implicit def apply(
    pptOrganisationDetails: PPTOrganisationDetails,
    isGroup: Boolean
  ): LegalEntityDetails =
    pptOrganisationDetails.organisationType match {
      case Some(OrgType.SOLE_TRADER) =>
        pptOrganisationDetails.soleTraderDetails.map { details =>
          updateLegalEntityDetails(customerIdentification1 = details.nino,
                                   customerIdentification2 = details.sautr,
                                   pptOrganisationDetails = pptOrganisationDetails
          )
        }.getOrElse(throw new Exception("Individual details are required"))
      case Some(OrgType.PARTNERSHIP) =>
        pptOrganisationDetails.partnershipDetails match {
          case Some(partnershipDetails) =>
            partnershipDetails.partnershipType match {
              case GENERAL_PARTNERSHIP =>
                partnershipDetails.generalPartnershipDetails.map { details =>
                  updateLegalEntityDetails(customerIdentification1 = details.sautr,
                                           customerIdentification2 = Some(details.postcode),
                                           pptOrganisationDetails = pptOrganisationDetails
                  )
                }.getOrElse(
                  throw new IllegalStateException("General partnership details are required")
                )
              case SCOTTISH_PARTNERSHIP =>
                partnershipDetails.scottishPartnershipDetails.map { details =>
                  updateLegalEntityDetails(customerIdentification1 = details.sautr,
                                           customerIdentification2 = Some(details.postcode),
                                           pptOrganisationDetails = pptOrganisationDetails
                  )
                }.getOrElse(
                  throw new IllegalStateException("Scottish partnership details are required")
                )
              case _ => throw new IllegalStateException("Unsupported partnership type")
            }
          case _ => throw new IllegalStateException("Partnership details missing")
        }
      case _ =>
        pptOrganisationDetails.incorporationDetails.map { details =>
          updateLegalEntityDetails(customerIdentification1 = details.companyNumber,
                                   customerIdentification2 = Some(details.ctutr),
                                   pptOrganisationDetails = pptOrganisationDetails,
                                   isGroup
          )

        }.getOrElse(throw new IllegalStateException("Incorporation details are required"))
    }

  private def getDateOfApplication: String =
    ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

  private def updateLegalEntityDetails(
    customerIdentification1: String,
    customerIdentification2: Option[String],
    pptOrganisationDetails: PPTOrganisationDetails,
    isGroup: Boolean = false
  ): LegalEntityDetails =
    LegalEntityDetails(dateOfApplication = getDateOfApplication,
                       customerIdentification1 = customerIdentification1,
                       customerIdentification2 =
                         customerIdentification2,
                       customerDetails = CustomerDetails(pptOrganisationDetails),
                       groupSubscriptionFlag = isGroup
    )

}
