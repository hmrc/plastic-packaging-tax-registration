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

package models.eis.subscription

import play.api.Logging
import play.api.libs.json.{Json, OFormat}
import models.OrgType.{OVERSEAS_COMPANY_NO_UK_BRANCH, OrgType}
import models.{OrgType, PartnerTypeEnum}

case class OrganisationDetails(organisationType: Option[String] = None, organisationName: String)
    extends Logging {

  def organisationTypeDisplayName(isGroup: Boolean): OrgType =
    organisationType match {
      case Some(organisationType) =>
        if (
          isGroup && organisationType.equals(PartnerTypeEnum.LIMITED_LIABILITY_PARTNERSHIP.toString)
        )
          OrgType.PARTNERSHIP
        else {
          // If OrgType was PARTNERSHIP during registration then Subscription / CustomerDetails.apply has written
          // the String value of a PartnerTypeEnum here; not an OrgType.
          // We need to try to map back from PartnerTypeEnum to OrgType
          val partnerTypeNames =
            PartnerTypeEnum.partnerTypesWhichRepresentPartnerships.map(_.toString)

          if (partnerTypeNames.contains(organisationType))
            OrgType.PARTNERSHIP
          else
            OrgType.withNameOpt(organisationType).getOrElse {
              logger.info(s"Defaulting '$organisationType' to OVERSEAS_COMPANY_NO_UK_BRANCH")
              OVERSEAS_COMPANY_NO_UK_BRANCH
            }
        }

      case None =>
        throw new IllegalStateException("Organisation type absent")
    }

}

object OrganisationDetails {
  implicit val format: OFormat[OrganisationDetails] = Json.format[OrganisationDetails]
}
