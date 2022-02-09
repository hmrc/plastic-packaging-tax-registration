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

package uk.gov.hmrc.plasticpackagingtaxregistration.models

import uk.gov.hmrc.plasticpackagingtaxregistration.models.RegType.RegType

import java.time.LocalDate

case class RegistrationRequest(
  dateOfRegistration: Option[LocalDate] = Some(LocalDate.now()),
  registrationType: Option[RegType] = None,
  groupDetail: Option[GroupDetail] = None,
  incorpJourneyId: Option[String],
  liabilityDetails: LiabilityDetails = LiabilityDetails(),
  primaryContactDetails: PrimaryContactDetails = PrimaryContactDetails(),
  organisationDetails: OrganisationDetails = OrganisationDetails(),
  metaData: MetaData = MetaData(),
  userHeaders: Option[Map[String, String]] = None
) {

  def toRegistration(providerId: String): Registration =
    Registration(id = providerId,
                 dateOfRegistration = this.dateOfRegistration,
                 registrationType = this.registrationType,
                 groupDetail = this.groupDetail,
                 incorpJourneyId = this.incorpJourneyId,
                 liabilityDetails = this.liabilityDetails,
                 primaryContactDetails = this.primaryContactDetails,
                 organisationDetails = this.organisationDetails,
                 metaData = this.metaData
    )

}

object RegistrationRequest {

  import play.api.libs.json._

  implicit val format: OFormat[RegistrationRequest] = Json.format[RegistrationRequest]
}
