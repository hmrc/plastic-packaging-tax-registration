/*
 * Copyright 2026 HM Revenue & Customs
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

package models

import play.api.libs.json.{Format, Json, OFormat}
import models.PartnerTypeEnum.PartnerTypeEnum

case class PartnershipDetails(
  partnershipType: PartnerTypeEnum,
  partnershipName: Option[String] = None,
  partnershipBusinessDetails: Option[PartnershipBusinessDetails] = None,
  partners: Seq[Partner] = Seq(),
  inflightPartner: Option[Partner] = None // Scratch area for newly added partner
) {

  lazy val name: Option[String] = partnershipName match {
    case Some(name) => Some(name)
    case _          => partnershipBusinessDetails.flatMap(_.name)
  }

  lazy val customerIdentification2: Option[String] = {
    val companyNumber: Option[String] = partnershipBusinessDetails.flatMap(_.companyNumber)
    companyNumber match {
      case Some(companyNumber) => Some(companyNumber)
      case _                   => partnershipBusinessDetails.map(_.postcode.postcode)
    }
  }

}

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
  postcode: PostCodeWithoutSpaces,
  companyProfile: Option[CompanyProfile],
  override val registration: Option[RegistrationDetails]
) extends HasRegistrationDetails {

  lazy val name: Option[String] = companyProfile.map(_.companyName)

  lazy val companyNumber: Option[String] = companyProfile.map(_.companyNumber)

}

object PartnershipBusinessDetails {

  implicit val format: OFormat[PartnershipBusinessDetails] =
    Json.format[PartnershipBusinessDetails]

}

case class PartnerPartnershipDetails(
  partnershipName: Option[String] = None,
  partnershipBusinessDetails: Option[PartnershipBusinessDetails] = None
) {

  lazy val name: Option[String] = partnershipName match {
    case Some(name) => Some(name)
    case _          => partnershipBusinessDetails.flatMap(_.name)
  }

}

object PartnerPartnershipDetails {
  implicit val format: OFormat[PartnerPartnershipDetails] = Json.format[PartnerPartnershipDetails]
}
