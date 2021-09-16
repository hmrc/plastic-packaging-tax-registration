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

package uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.group.GroupOrPartnershipSubscription
import uk.gov.hmrc.plasticpackagingtaxregistration.models.{Date, LiabilityWeight, Registration}

import scala.language.implicitConversions

case class Subscription(
  legalEntityDetails: LegalEntityDetails,
  principalPlaceOfBusinessDetails: PrincipalPlaceOfBusinessDetails,
  primaryContactDetails: PrimaryContactDetails,
  businessCorrespondenceDetails: BusinessCorrespondenceDetails,
  declaration: Declaration,
  taxObligationStartDate: String,
  last12MonthTotalTonnageAmt: Option[Long] = None,
  groupOrPartnershipSubscription: Option[GroupOrPartnershipSubscription] = None
)

object Subscription {
  implicit val format: OFormat[Subscription] = Json.format[Subscription]

  implicit def convertLiabilityWeightToLong(weight: Option[LiabilityWeight]): Option[Long] =
    weight match {
      case Some(liabilityWeight) => liabilityWeight.totalKg
      case None                  => None
    }

  implicit def convertDateToString(liabilityDate: Option[Date]): String =
    liabilityDate match {
      case Some(date) => date.pretty
      case None       => throw new Exception("A PPT liability Start date is required")
    }

  def apply(registration: Registration): Subscription =
    Subscription(legalEntityDetails = LegalEntityDetails(registration.organisationDetails),
                 principalPlaceOfBusinessDetails = PrincipalPlaceOfBusinessDetails(registration),
                 primaryContactDetails = PrimaryContactDetails(registration.primaryContactDetails),
                 businessCorrespondenceDetails = BusinessCorrespondenceDetails(registration),
                 declaration = Declaration(true),
                 taxObligationStartDate = registration.liabilityDetails.startDate,
                 last12MonthTotalTonnageAmt = registration.liabilityDetails.weight,
                 groupOrPartnershipSubscription = None
    )

}
