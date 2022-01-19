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

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.plasticpackagingtaxregistration.models.PartnerTypeEnum.PartnerTypeEnum

case class PartnershipDetails(
  partnershipType: PartnerTypeEnum,
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
