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

package builders

import models._

import java.time.{Instant, LocalDate}
import java.util.UUID

//noinspection ScalaStyle
trait RegistrationBuilder
    extends OrganisationDetailsBuilder with IncorporationDetailsBuilder with PPTAddressBuilder
    with PrimaryContactDetailsBuilder with LiabilityDetailsBuilder {
  private type RegistrationModifier = Registration => Registration

  private val modelWithDefaults: Registration =
    Registration(id = "id", incorpJourneyId = Some(UUID.randomUUID().toString)).updateLastModified()

  def aRegistration(modifiers: RegistrationModifier*): Registration =
    modifiers.foldLeft(modelWithDefaults)((current, nextFunction) => nextFunction(current))

  def aValidRegistration(modifiers: RegistrationModifier*): Registration = {
    val baseModifiers = Seq(
      withOrganisationDetails(
        anOrganisation(withBusinessRegisteredAddress(anAddress()),
                       withIncorporationDetails(someIncorporationDetails()),
                       withOrganisationType(OrgType.UK_COMPANY)
        )
      ),
      withPrimaryContactDetails(
        somePrimaryContactDetails(withJobTitle("JOB_TITLE"),
                                  withName("NAME"),
                                  withEmailAddress("EMAIL_ADDRESS"),
                                  withPhoneNumber("PHONE_NUMBER"),
                                  withAddress(anAddress())
        )
      ),
      withLiabilityDetails(
        aLiability(withStartDate(LocalDate.of(0, 1, 1)), withExpectedWeightNext12m(0))
      )
    )

    aRegistration(baseModifiers ++ modifiers: _*)
  }

  def withId(id: String): RegistrationModifier = _.copy(id = id)

  def withTimestamp(instant: Instant): RegistrationModifier =
    _.copy(lastModifiedDateTime = Some(instant))

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

  def withPartnerModifications(partnerModifier: Partner => Partner): RegistrationModifier =
    registration =>
      registration.copy(organisationDetails =
        registration.organisationDetails.copy(partnershipDetails =
          registration.organisationDetails.partnershipDetails.map(
            pd => pd.copy(partners = pd.partners.map(partnerModifier(_)))
          )
        )
      )

}
