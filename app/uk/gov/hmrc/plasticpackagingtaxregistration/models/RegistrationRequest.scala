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
  userHeaders: Map[String, String] = Map.empty
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
  import play.api.libs.functional.syntax._

  implicit val reads: Reads[RegistrationRequest] =
    ((__ \ "dateOfRegistration").readNullable[LocalDate] and
    (__ \ "registrationType").readNullable[RegType] and
    (__ \ "groupDetail").readNullable[GroupDetail] and
    (__ \ "incorpJourneyId").readNullable[String] and
    (__ \ "liabilityDetails").read[LiabilityDetails] and
    (__ \ "primaryContactDetails").read[PrimaryContactDetails] and
    (__ \ "organisationDetails").read[OrganisationDetails] and
    (__ \ "metaData").read[MetaData] and
    (__ \ "userHeaders").readNullable[Map[String, String]].map {
      case None => Map.empty[String, String]
      case Some(x) => x
    }).apply(RegistrationRequest.apply _)

  implicit val writes = Json.writes[RegistrationRequest].transform{ js:JsObject =>
    if (js("userHeaders") == JsObject.empty) js - ("userHeaders")
    else js
  }
}
