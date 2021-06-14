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

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.plasticpackagingtaxregistration.models.{
  OrgType,
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
  groupSubscriptionFlag: Boolean = false
)

object LegalEntityDetails {

  implicit val format: OFormat[LegalEntityDetails] = Json.format[LegalEntityDetails]

  implicit def apply(pptOrganisationDetails: PPTOrganisationDetails): LegalEntityDetails =
    pptOrganisationDetails.organisationType match {
      case Some(OrgType.SOLE_TRADER) =>
        pptOrganisationDetails.soleTraderDetails.map { details =>
          LegalEntityDetails(dateOfApplication = getDateOfApplication,
                             customerIdentification1 = details.nino,
                             customerIdentification2 = details.sautr,
                             customerDetails = CustomerDetails(pptOrganisationDetails)
          )
        }.getOrElse(throw new Exception("Individual details are required"))
      case _ =>
        pptOrganisationDetails.incorporationDetails.map { details =>
          LegalEntityDetails(dateOfApplication = getDateOfApplication,
                             customerIdentification1 = details.companyNumber,
                             customerIdentification2 = Some(details.ctutr),
                             customerDetails = CustomerDetails(pptOrganisationDetails)
          )
        }.getOrElse(throw new Exception("Incorporation details are required"))
    }

  private def getDateOfApplication: String =
    ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

}
