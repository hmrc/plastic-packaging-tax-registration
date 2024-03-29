/*
 * Copyright 2024 HM Revenue & Customs
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

package models.eis.subscription.group

import models.eis.subscription.group.GroupPartnershipDetails.Relationship.Relationship
import models.eis.subscription.{
  AddressDetails,
  ContactDetails,
  IndividualDetails,
  OrganisationDetails
}
import play.api.libs.json._

case class GroupPartnershipDetails(
  relationship: Relationship,
  customerIdentification1: String,
  customerIdentification2: Option[String],
  organisationDetails: OrganisationDetails,
  individualDetails: IndividualDetails,
  addressDetails: AddressDetails,
  contactDetails: ContactDetails,
  regWithoutIDFlag: Option[Boolean] = None
)

object GroupPartnershipDetails {

  object Relationship extends Enumeration {
    type Relationship = Value
    val Member: Value         = Value("Member")
    val Representative: Value = Value("Representative")
    val Partner: Value        = Value("Partner")

    implicit val format: Format[Relationship] =
      Format(Reads.enumNameReads(Relationship), Writes.enumNameWrites)

  }

  implicit val format: OFormat[GroupPartnershipDetails] = Json.format[GroupPartnershipDetails]
}
