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

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.plasticpackagingtaxregistration.base.data.RegistrationTestData
import uk.gov.hmrc.plasticpackagingtaxregistration.builders.RegistrationBuilder
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.Subscription

class RegistrationSpec extends AnyWordSpec with RegistrationTestData with RegistrationBuilder {

  "Registration" should {

    "convert from UK company subscription" in {

      val ukCompanyRegistration =
        aRegistration(withOrganisationDetails(pptIncorporationDetails),
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

    "convert from general partner subscription" in {

      val generalPartnershipRegistration =
        aRegistration(withOrganisationDetails(pptGeneralPartnershipDetails),
                      withPrimaryContactDetails(pptPrimaryContactDetails),
                      withLiabilityDetails(pptLiabilityDetails)
        )

      assertConversion(generalPartnershipRegistration)

    }

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
      val existingSubscription = Subscription(registration)

      val rehydratedRegistration = Registration(existingSubscription)

      val updatedSubscription = Subscription(rehydratedRegistration)

      updatedSubscription mustBe existingSubscription
    }
  }
}