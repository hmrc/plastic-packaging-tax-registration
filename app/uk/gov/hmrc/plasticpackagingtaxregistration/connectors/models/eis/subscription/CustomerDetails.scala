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
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.CustomerType.CustomerType
import uk.gov.hmrc.plasticpackagingtaxregistration.models.{
  OrgType,
  OrganisationDetails => PPTOrganisationDetails
}

case class CustomerDetails(
  customerType: CustomerType,
  individualDetails: Option[IndividualDetails] = None,
  organisationDetails: Option[OrganisationDetails] = None
)

object CustomerDetails {
  implicit val format: OFormat[CustomerDetails] = Json.format[CustomerDetails]

  def apply(organisationDetails: PPTOrganisationDetails): CustomerDetails =
    organisationDetails.organisationType match {
      case Some(OrgType.SOLE_TRADER) =>
        organisationDetails.soleTraderDetails.map { details =>
          CustomerDetails(
            CustomerType.Individual,
            individualDetails =
              Some(IndividualDetails(firstName = details.firstName, lastName = details.lastName))
          )
        }.getOrElse(throw new Exception("Individual details are required"))
      case Some(OrgType.PARTNERSHIP) =>
        organisationDetails.partnershipDetails.map { details =>
          CustomerDetails(customerType = CustomerType.Organisation,
                          organisationDetails = Some(
                            OrganisationDetails(
                              organisationType =
                                Some(OrgType.PARTNERSHIP.toString),
                              // TODO: can we do this? I think partnership names might be optional, especially for general partnerships.
                              organisationName = details.partnershipName.getOrElse("")
                            )
                          )
          )
        }.getOrElse(throw new Exception("Partnership organisation details are required"))
      case Some(orgType) =>
        organisationDetails.incorporationDetails.map { details =>
          CustomerDetails(customerType = CustomerType.Organisation,
                          organisationDetails = Some(
                            OrganisationDetails(organisationType = Some(orgType.toString),
                                                organisationName = details.companyName
                            )
                          )
          )
        }.getOrElse(throw new Exception("Incorporation details are required"))

      case None => throw new Exception("Organisation Type is required")
    }

}
