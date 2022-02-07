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

import org.scalatest.Ignore
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.plasticpackagingtaxregistration.base.data.{
  RegistrationTestData,
  SubscriptionTestData
}
import uk.gov.hmrc.plasticpackagingtaxregistration.builders.RegistrationBuilder
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.{
  ChangeOfCircumstance,
  ChangeOfCircumstanceDetails,
  Subscription
}

import java.time.{ZoneOffset, ZonedDateTime}

class RegistrationSpec
    extends AnyWordSpec with RegistrationTestData with RegistrationBuilder
    with SubscriptionTestData {

  "Registration" should {

    "convert from subscription to registration and then to subscription " in {
      val amendedGroupSubscription =
        ukLimitedCompanySubscription.copy(
          legalEntityDetails =
            ukLimitedCompanySubscription.legalEntityDetails.copy(groupSubscriptionFlag = true),
          groupPartnershipSubscription =
            Some(groupPartnershipSubscription),
          changeOfCircumstanceDetails =
            Some(
              ChangeOfCircumstanceDetails(changeOfCircumstance =
                ChangeOfCircumstance.UPDATE_TO_DETAILS.toString
              )
            ),
          processingDate = Some(ZonedDateTime.now(ZoneOffset.UTC).toLocalDate.toString)
        )
      val rehydratedRegistration = Registration(amendedGroupSubscription)

      val updatedSubscription = Subscription(rehydratedRegistration, isSubscriptionUpdate = false)

      updatedSubscription mustBe amendedGroupSubscription

    }

    "convert from UK company subscription" in {

      val ukCompanyRegistration =
        aRegistration(withOrganisationDetails(pptIncorporationDetails),
                      withPrimaryContactDetails(pptPrimaryContactDetails),
                      withLiabilityDetails(pptLiabilityDetails)
        )

      assertConversion(ukCompanyRegistration)

    }

    "convert from overseas company with UK branch subscription" in {

      val ukCompanyRegistration =
        aRegistration(
          withOrganisationDetails(
            pptIncorporationDetails.copy(organisationType =
              Some(OrgType.OVERSEAS_COMPANY_UK_BRANCH)
            )
          ),
          withPrimaryContactDetails(pptPrimaryContactDetails),
          withLiabilityDetails(pptLiabilityDetails)
        )

      assertConversion(ukCompanyRegistration)

    }

    "convert from registered society subscription" in {

      val ukCompanyRegistration =
        aRegistration(
          withOrganisationDetails(
            pptIncorporationDetails.copy(organisationType =
              Some(OrgType.REGISTERED_SOCIETY)
            )
          ),
          withPrimaryContactDetails(pptPrimaryContactDetails),
          withLiabilityDetails(pptLiabilityDetails)
        )

      assertConversion(ukCompanyRegistration)

    }

    "convert from sole trader subscription" in {

      val soleTraderRegistration =
        aRegistration(withOrganisationDetails(pptSoleTraderDetails),
                      withPrimaryContactDetails(pptPrimaryContactDetails),
                      withLiabilityDetails(pptLiabilityDetails)
        )

      assertConversion(soleTraderRegistration)

    }

    // TODO: reintroduce when we can deal with partnership registration -> subscription -> registration
//    "convert from general partner subscription" in {
//
//      val generalPartnershipRegistration =
//        aRegistration(withOrganisationDetails(pptGeneralPartnershipDetails),
//                      withPrimaryContactDetails(pptPrimaryContactDetails),
//                      withLiabilityDetails(pptLiabilityDetails)
//        )
//
//      assertConversion(generalPartnershipRegistration)
//
//    }

    "convert from UK company group subscription" in {

      val ukCompanyGroupRegistration =
        aRegistration(withOrganisationDetails(pptIncorporationDetails),
                      withPrimaryContactDetails(pptPrimaryContactDetails),
                      withLiabilityDetails(pptLiabilityDetails),
                      withGroupDetail(groupDetail)
        )

      assertConversion(ukCompanyGroupRegistration)

    }

    def assertConversion(registration: Registration) = {
      val existingSubscription = Subscription(registration, isSubscriptionUpdate = false)

      val rehydratedRegistration = Registration(existingSubscription)

      val updatedSubscription = Subscription(rehydratedRegistration, isSubscriptionUpdate = false)

      updatedSubscription mustBe existingSubscription
    }
  }
}
