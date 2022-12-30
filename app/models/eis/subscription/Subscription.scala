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

package models.eis.subscription

import play.api.libs.json.{Json, OFormat}
import models.eis.subscription.group.GroupPartnershipSubscription
import models.{OldDate, RegType, Registration}

import scala.language.implicitConversions

case class Subscription(
                         changeOfCircumstanceDetails: Option[ChangeOfCircumstanceDetails] = None,
                         legalEntityDetails: LegalEntityDetails,
                         principalPlaceOfBusinessDetails: PrincipalPlaceOfBusinessDetails,
                         primaryContactDetails: PrimaryContactDetails,
                         businessCorrespondenceDetails: BusinessCorrespondenceDetails,
                         declaration: Declaration,
                         taxObligationStartDate: String,
                         last12MonthTotalTonnageAmt: Long,
                         groupPartnershipSubscription: Option[group.GroupPartnershipSubscription] = None,
                         processingDate: Option[String] = None
)

object Subscription {
  implicit val format: OFormat[Subscription] = Json.format[Subscription]

  implicit def convertDateToString(liabilityDate: Option[OldDate]): String =
    liabilityDate match {
      case Some(date) => date.pretty
      case None       => throw new Exception("A PPT liability Start date is required")
    }

  def apply(registration: Registration, isSubscriptionUpdate: Boolean): Subscription = {
    val changeOfCircumstanceDetails = if (isSubscriptionUpdate) {
      // A mandatory field is required when calling the subscription variation API
      val updateToDetailsChangeOfCircumstance = registration.changeOfCircumstanceDetails.getOrElse(
        ChangeOfCircumstanceDetails(changeOfCircumstance =
          ChangeOfCircumstance.UPDATE_TO_DETAILS.toString
        )
      ).copy(changeOfCircumstance = ChangeOfCircumstance.UPDATE_TO_DETAILS.toString)
      Some(updateToDetailsChangeOfCircumstance)
    } else
      registration.changeOfCircumstanceDetails

    Subscription(changeOfCircumstanceDetails = changeOfCircumstanceDetails,
                 processingDate = registration.processingDate,
                 legalEntityDetails =
                   LegalEntityDetails(registration.organisationDetails,
                                      isGroup = isGroup(registration),
                                      isUpdate = isSubscriptionUpdate
                   ),
                 principalPlaceOfBusinessDetails = PrincipalPlaceOfBusinessDetails(registration),
                 primaryContactDetails = PrimaryContactDetails(registration),
                 businessCorrespondenceDetails = BusinessCorrespondenceDetails(registration),
                 declaration = Declaration(true),
                 taxObligationStartDate = registration.liabilityDetails.startDate,
                 last12MonthTotalTonnageAmt =
                   registration.liabilityDetails.liabilityWeight,
                 groupPartnershipSubscription =
                   group.GroupPartnershipSubscription(registration, isSubscriptionUpdate)
    )
  }

  private def isGroup(registration: Registration): Boolean =
    registration.registrationType.isDefined && registration.registrationType.get == RegType.GROUP

}
