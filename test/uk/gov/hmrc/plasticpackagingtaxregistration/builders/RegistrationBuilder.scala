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

package uk.gov.hmrc.plasticpackagingtaxregistration.builders

import org.joda.time.DateTime
import uk.gov.hmrc.plasticpackagingtaxregistration.models.{
  GroupDetail,
  LiabilityDetails,
  OrganisationDetails,
  PrimaryContactDetails,
  RegType,
  Registration
}

import java.util.UUID

//noinspection ScalaStyle
trait RegistrationBuilder {

  private type RegistrationModifier = Registration => Registration

  def aRegistration(modifiers: RegistrationModifier*): Registration =
    modifiers.foldLeft(modelWithDefaults)((current, modifier) => modifier(current))

  private def modelWithDefaults: Registration =
    Registration(id = "id", incorpJourneyId = Some(UUID.randomUUID().toString))

  def withId(id: String): RegistrationModifier = _.copy(id = id)

  def withTimestamp(dateTime: DateTime): RegistrationModifier =
    _.copy(lastModifiedDateTime = Some(dateTime))

  def withIncorpJourneyId(incorpJourneyId: String): RegistrationModifier =
    _.copy(incorpJourneyId = Some(incorpJourneyId))

  def withPrimaryContactDetails(
    primaryContactDetails: PrimaryContactDetails
  ): RegistrationModifier =
    _.copy(primaryContactDetails = primaryContactDetails)

  def withLiabilityDetails(liabilityDetails: LiabilityDetails): RegistrationModifier =
    _.copy(liabilityDetails = liabilityDetails)

  def withOrganisationDetails(organisationDetails: OrganisationDetails): RegistrationModifier =
    _.copy(organisationDetails = organisationDetails)

  def withGroupDetail(groupDetail: GroupDetail): RegistrationModifier =
    _.copy(registrationType = Some(RegType.GROUP), groupDetail = Some(groupDetail))

}
