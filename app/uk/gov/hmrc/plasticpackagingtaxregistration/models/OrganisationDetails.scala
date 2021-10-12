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

import play.api.libs.json._
import uk.gov.hmrc.plasticpackagingtaxregistration.models.OrgType.OrgType

object OrgType extends Enumeration {
  type OrgType = Value
  val UK_COMPANY: Value                = Value("UkCompany")
  val SOLE_TRADER: Value               = Value("SoleTrader")
  val PARTNERSHIP: Value               = Value("Partnership")
  val CHARITY_OR_NOT_FOR_PROFIT: Value = Value("RegisteredSociety")
  val OVERSEAS_COMPANY: Value          = Value("OverseasCompany")

  implicit val format: Format[OrgType] =
    Format(Reads.enumNameReads(OrgType), Writes.enumNameWrites)

}

case class OrganisationDetails(
  organisationType: Option[OrgType] = None,
  businessRegisteredAddress: Option[Address] = None,
  safeNumber: Option[String] = None,
  soleTraderDetails: Option[SoleTraderIncorporationDetails] = None,
  partnershipDetails: Option[PartnershipDetails] = None,
  incorporationDetails: Option[IncorporationDetails] = None
)

object OrganisationDetails {
  implicit val format: OFormat[OrganisationDetails] = Json.format[OrganisationDetails]
}
