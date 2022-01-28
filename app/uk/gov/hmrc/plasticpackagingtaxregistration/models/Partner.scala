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
import uk.gov.hmrc.plasticpackagingtaxregistration.models.PartnerTypeEnum.PartnerTypeEnum

import java.util.UUID

object PartnerTypeEnum extends Enumeration {
  type PartnerTypeEnum = Value
  val SOLE_TRADER: Value                          = Value("SoleTrader")
  val UK_COMPANY: Value                           = Value("UkCompany")
  val GENERAL_PARTNERSHIP: Value                  = Value("GeneralPartnership")
  val LIMITED_LIABILITY_PARTNERSHIP: Value        = Value("LimitedLiabilityPartnership")
  val LIMITED_PARTNERSHIP: Value                  = Value("LimitedPartnership")
  val SCOTTISH_PARTNERSHIP: Value                 = Value("ScottishPartnership")
  val SCOTTISH_LIMITED_PARTNERSHIP: Value         = Value("ScottishLimitedPartnership")
  val CHARITABLE_INCORPORATED_ORGANISATION: Value = Value("CIO")
  val OVERSEAS_COMPANY_UK_BRANCH: Value           = Value("OverseasCompanyUkBranch")
  val OVERSEAS_COMPANY_NO_UK_BRANCH: Value        = Value("OverseasCompanyNoUKBranch")

  implicit val format: Format[PartnerTypeEnum] =
    Format(Reads.enumNameReads(PartnerTypeEnum), Writes.enumNameWrites)

}

case class Partner(
  id: String = UUID.randomUUID().toString,
  partnerType: Option[PartnerTypeEnum],
  soleTraderDetails: Option[SoleTraderIncorporationDetails] = None,
  incorporationDetails: Option[IncorporationDetails] = None,
  partnerPartnershipDetails: Option[PartnerPartnershipDetails] = None,
  contactDetails: Option[PartnerContactDetails] = None,
  userSuppliedName: Option[String] = None
)

object Partner {
  implicit val format: OFormat[Partner] = Json.format[Partner]
}
