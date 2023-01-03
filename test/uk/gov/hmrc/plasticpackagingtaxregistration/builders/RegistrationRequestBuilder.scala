/*
 * Copyright 2023 HM Revenue & Customs
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

package builders

import models.{
  GroupDetail,
  LiabilityDetails,
  MetaData,
  OrganisationDetails,
  PrimaryContactDetails,
  RegistrationRequest
}

//noinspection ScalaStyle
trait RegistrationRequestBuilder {

  private type RegistrationModifier = RegistrationRequest => RegistrationRequest

  def aRegistrationRequest(modifiers: RegistrationModifier*): RegistrationRequest =
    modifiers.foldLeft(modelWithDefaults)((current, modifier) => modifier(current))

  private def modelWithDefaults: RegistrationRequest =
    RegistrationRequest(incorpJourneyId = Some("f368e653-790a-4a95-af62-4132f0ffd433"))

  def withIncorpJourneyIdRequest(incorpJourneyId: String): RegistrationModifier =
    _.copy(incorpJourneyId = Some(incorpJourneyId))

  def withPrimaryContactDetailsRequest(
    primaryContactDetails: PrimaryContactDetails
  ): RegistrationModifier =
    _.copy(primaryContactDetails = primaryContactDetails)

  def withLiabilityDetailsRequest(liabilityDetails: LiabilityDetails): RegistrationModifier =
    _.copy(liabilityDetails = liabilityDetails)

  def withMetaDataRequest(metaData: MetaData): RegistrationModifier =
    _.copy(metaData = metaData)

  def withOrganisationDetailsRequest(
    organisationDetails: OrganisationDetails
  ): RegistrationModifier =
    _.copy(organisationDetails = organisationDetails)

  def withUserHeaders(headers: Map[String, String]): RegistrationModifier =
    _.copy(userHeaders = headers)

  def withNoUserHeaders(): RegistrationModifier =
    _.copy(userHeaders = Map.empty)

  def withGroupDetailsRequest(groupDetail: GroupDetail): RegistrationModifier =
    _.copy(groupDetail = Some(groupDetail))

}
