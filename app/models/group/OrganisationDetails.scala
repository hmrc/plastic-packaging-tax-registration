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

package models.group

import play.api.libs.json.{Json, OFormat}
import models.eis.subscription.{
  OrganisationDetails => EISOrganisationDetails
}

case class OrganisationDetails(
  organisationType: String,
  organisationName: String,
  businessPartnerId: Option[String]
)

object OrganisationDetails {
  implicit val format: OFormat[OrganisationDetails] = Json.format[OrganisationDetails]

  def apply(details: EISOrganisationDetails): OrganisationDetails =
    new OrganisationDetails(organisationType = details.organisationType.getOrElse(
                              throw new IllegalStateException("Missing organisationType")
                            ),
                            organisationName = details.organisationName,
                            businessPartnerId = None
    )

}
