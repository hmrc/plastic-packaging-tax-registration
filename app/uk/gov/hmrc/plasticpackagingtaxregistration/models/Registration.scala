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

package uk.gov.hmrc.plasticpackagingtaxregistration.models

import org.joda.time.{DateTime, DateTimeZone}
import uk.gov.hmrc.plasticpackagingtaxregistration.models.RegType.RegType

case class Registration(
  id: String,
  registrationType: Option[RegType] = None,
  groupDetail: Option[GroupDetail] = None,
  incorpJourneyId: Option[String],
  liabilityDetails: LiabilityDetails = LiabilityDetails(),
  primaryContactDetails: PrimaryContactDetails = PrimaryContactDetails(),
  organisationDetails: OrganisationDetails = OrganisationDetails(),
  metaData: MetaData = MetaData(),
  lastModifiedDateTime: Option[DateTime] = None
) {

  def updateLastModified(): Registration =
    this.copy(lastModifiedDateTime = Some(DateTime.now(DateTimeZone.UTC)))

}

object Registration {

  import play.api.libs.json._

  implicit val dateFormatDefault: Format[DateTime] = new Format[DateTime] {

    override def reads(json: JsValue): JsResult[DateTime] =
      JodaReads.DefaultJodaDateTimeReads.reads(json)

    override def writes(o: DateTime): JsValue = JodaWrites.JodaDateTimeNumberWrites.writes(o)
  }

  implicit val format: OFormat[Registration] = Json.format[Registration]
}
