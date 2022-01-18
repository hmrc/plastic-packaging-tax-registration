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

import play.api.libs.json.{Format, Json, OFormat, Reads, Writes}
import uk.gov.hmrc.plasticpackagingtaxregistration.models.PartnershipTypeEnum.PartnershipTypeEnum

object PartnershipTypeEnum extends Enumeration {
  type PartnershipTypeEnum = Value
  val GENERAL_PARTNERSHIP: Value           = Value("GeneralPartnership")
  val LIMITED_LIABILITY_PARTNERSHIP: Value = Value("LimitedLiabilityPartnership")
  val LIMITED_PARTNERSHIP: Value           = Value("LimitedPartnership")
  val SCOTTISH_PARTNERSHIP: Value          = Value("ScottishPartnership")
  val SCOTTISH_LIMITED_PARTNERSHIP: Value  = Value("ScottishLimitedPartnership")

  implicit val format: Format[PartnershipTypeEnum] =
    Format(Reads.enumNameReads(PartnershipTypeEnum), Writes.enumNameWrites)

}

object PartnershipPartnerTypeEnum extends Enumeration {
  type PartnershipPartnerTypeEnum = Value
  val SOLE_TRADER: Value                          = Value("SoleTrader")
  val UK_COMPANY: Value                           = Value("UkCompany")
  val LIMITED_LIABILITY_PARTNERSHIP: Value        = Value("LimitedLiabilityPartnership")
  val SCOTTISH_PARTNERSHIP: Value                 = Value("ScottishPartnership")
  val SCOTTISH_LIMITED_PARTNERSHIP: Value         = Value("ScottishLimitedPartnership")
  val CHARITABLE_INCORPORATED_ORGANISATION: Value = Value("CIO")
  val OVERSEAS_COMPANY_UK_BRANCH: Value           = Value("OverseasCompanyUkBranch")
  val OVERSEAS_COMPANY_NO_UK_BRANCH: Value        = Value("OverseasCompanyNoUKBranch")

  implicit val format: Format[PartnershipPartnerTypeEnum] =
    Format(Reads.enumNameReads(PartnershipPartnerTypeEnum), Writes.enumNameWrites)

}

case class PartnershipDetails(
  partnershipType: PartnershipTypeEnum,
  partnershipName: Option[String] = None,
  partnershipBusinessDetails: Option[PartnershipBusinessDetails] = None,
  nominatedPartner: Option[Partner] = None,
  otherPartners: Option[Seq[Partner]] = None,
  inflightPartner: Option[Partner] = None // Scratch area for newly added partner
)

object PartnershipDetails {
  implicit val format: Format[PartnershipDetails] = Json.format[PartnershipDetails]
}

case class CompanyProfile(
  companyNumber: String,
  companyName: String,
  companyAddress: IncorporationAddressDetails
)

object CompanyProfile {
  implicit val format: OFormat[CompanyProfile] = Json.format[CompanyProfile]
}

case class PartnershipBusinessDetails(
  sautr: String,
  postcode: String,
  companyProfile: Option[CompanyProfile],
  override val registration: Option[RegistrationDetails]
) extends HasRegistrationDetails

object PartnershipBusinessDetails {

  implicit val format: OFormat[PartnershipBusinessDetails] =
    Json.format[PartnershipBusinessDetails]

}
