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

import play.api.libs.json._
import uk.gov.hmrc.auth.core.InternalError
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscriptionStatus.SubscriptionStatus.Status
import uk.gov.hmrc.plasticpackagingtaxregistration.models.OrgType.OrgType

object OrgType extends Enumeration {
  type OrgType = Value
  val UK_COMPANY: Value                           = Value("UkCompany")
  val SOLE_TRADER: Value                          = Value("SoleTrader")
  val PARTNERSHIP: Value                          = Value("Partnership")
  val REGISTERED_SOCIETY: Value                   = Value("RegisteredSociety")
  val TRUST: Value                                = Value("Trust")
  val CHARITABLE_INCORPORATED_ORGANISATION: Value = Value("CIO")
  val OVERSEAS_COMPANY_UK_BRANCH: Value           = Value("OverseasCompanyUkBranch")
  val OVERSEAS_COMPANY_NO_UK_BRANCH: Value        = Value("OverseasCompanyNoUKBranch")

  implicit val format: Format[OrgType] =
    Format(Reads.enumNameReads(OrgType), Writes.enumNameWrites)

}

case class OrganisationDetails(
  organisationType: Option[OrgType] = None,
  businessRegisteredAddress: Option[PPTAddress] = None,
  safeNumber: Option[String] = None,
  soleTraderDetails: Option[SoleTraderIncorporationDetails] = None,
  partnershipDetails: Option[PartnershipDetails] = None,
  incorporationDetails: Option[IncorporationDetails] = None,
  subscriptionStatus: Option[Status] = None,
  regWithoutIDFlag: Option[Boolean] = None
) {

  def registeredBusinessAddress: PPTAddress =
    businessRegisteredAddress.getOrElse(
      throw InternalError(s"The legal entity registered address is required.")
    )

}

object OrganisationDetails {
  implicit val format: OFormat[OrganisationDetails] = Json.format[OrganisationDetails]
}
