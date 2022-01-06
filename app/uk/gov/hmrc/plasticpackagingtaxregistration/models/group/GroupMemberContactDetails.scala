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

package uk.gov.hmrc.plasticpackagingtaxregistration.models.group

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.{
  AddressDetails,
  ContactDetails,
  IndividualDetails
}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.group.GroupPartnershipDetails
import uk.gov.hmrc.plasticpackagingtaxregistration.models.PPTAddress

case class GroupMemberContactDetails(
  firstName: String,
  lastName: String,
  phoneNumber: Option[String] = None,
  email: Option[String] = None,
  address: Option[PPTAddress] = None
)

object GroupMemberContactDetails {
  implicit val format: OFormat[GroupMemberContactDetails] = Json.format[GroupMemberContactDetails]

  def apply(groupPartnershipDetails: GroupPartnershipDetails): GroupMemberContactDetails = {
    val individualDetails: IndividualDetails = groupPartnershipDetails.individualDetails
    val contactDetails: ContactDetails       = groupPartnershipDetails.contactDetails
    val addressDetails: AddressDetails       = groupPartnershipDetails.addressDetails
    GroupMemberContactDetails(firstName = individualDetails.firstName,
                              lastName = individualDetails.lastName,
                              phoneNumber = Some(contactDetails.telephone),
                              email = Some(contactDetails.email),
                              address = Some(PPTAddress(addressDetails))
    )
  }

}
