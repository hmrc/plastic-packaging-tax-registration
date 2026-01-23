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

import play.api.libs.json.{Json, OFormat}
import models.eis.subscription.IndividualDetails
import models.eis.subscription.group.GroupPartnershipDetails

case class PrimaryContactDetails(
  name: Option[String] = None,
  jobTitle: Option[String] = None,
  email: Option[String] = None,
  phoneNumber: Option[String] = None,
  useRegisteredAddress: Option[Boolean] = None,
  address: Option[PPTAddress] = None,
  // The following fields are used for email verification only
  journeyId: Option[String] = None,
  prospectiveEmail: Option[String] = None
)

object PrimaryContactDetails {
  implicit val format: OFormat[PrimaryContactDetails] = Json.format[PrimaryContactDetails]

  def apply(groupPartnershipDetails: GroupPartnershipDetails): PrimaryContactDetails = {
    val individualDetails = groupPartnershipDetails.individualDetails
    val contactDetails    = groupPartnershipDetails.contactDetails
    val addressDetails    = groupPartnershipDetails.addressDetails
    PrimaryContactDetails(name = Some(name(individualDetails)),
                          jobTitle = individualDetails.title,
                          phoneNumber = Some(contactDetails.telephone),
                          email = Some(contactDetails.email),
                          address = Some(PPTAddress(addressDetails))
    )
  }

  private def name(individualDetails: IndividualDetails): String =
    individualDetails.middleName match {
      case Some(middleName) =>
        s"${individualDetails.firstName} ${middleName} ${individualDetails.lastName}"
      case None => s"${individualDetails.firstName} ${individualDetails.lastName}"
    }

}
