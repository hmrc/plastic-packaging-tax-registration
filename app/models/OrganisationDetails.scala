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

package models

import play.api.libs.json._
import models.eis.subscriptionStatus.SubscriptionStatus.Status
import models.OrgType.{OrgType, PARTNERSHIP}

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

  def withNameOpt(name: String): Option[Value] = values.find(_.toString == name)

}

case class OrganisationDetails(
  organisationType: Option[OrgType] = None,
  businessRegisteredAddress: Option[PPTAddress] = None,
  safeNumber: Option[String] = None,
  soleTraderDetails: Option[SoleTraderIncorporationDetails] = None,
  partnershipDetails: Option[PartnershipDetails] = None,
  incorporationDetails: Option[IncorporationDetails] = None,
  subscriptionStatus: Option[Status] = None,
  regWithoutIDFlag: Option[Boolean] = None,
  isBusinessAddressFromGrs: Option[Boolean] = None
) {

  def registeredBusinessAddress: PPTAddress =
    businessRegisteredAddress.getOrElse(
      throw new IllegalStateException(s"The legal entity registered address is required.")
    )

  lazy val customerIdentification1: String =
    extractData(partnershipDetails => partnershipDetails.partnershipBusinessDetails.map(_.sautr),
                incorpDetails => Some(incorpDetails.companyNumber)
    ).getOrElse(throw new IllegalStateException("First identifier is absent"))

  lazy val customerIdentification2: Option[String] =
    extractData(partnershipDetails => partnershipDetails.customerIdentification2,
                incorpDetails => incorpDetails.ctutr
    )

  lazy val name: String = extractData(partnershipDetails => partnershipDetails.name,
                                      incorpDetails => Some(incorpDetails.companyName)
  ).getOrElse(throw new IllegalStateException("Partner name is absent"))

  private def extractData(
    partnershipExtractor: PartnershipDetails => Option[String],
    incorpExtractor: IncorporationDetails => Option[String]
  ): Option[String] =
    organisationType match {
      case Some(PARTNERSHIP) =>
        partnershipExtractor(
          this.partnershipDetails.getOrElse(
            throw new IllegalStateException("Partnership details absent")
          )
        )
      case Some(_) =>
        incorpExtractor(
          incorporationDetails.getOrElse(
            throw new IllegalStateException("Incorporation details absent")
          )
        )
      case _ => None
    }

}

object OrganisationDetails {
  implicit val format: OFormat[OrganisationDetails] = Json.format[OrganisationDetails]
}
