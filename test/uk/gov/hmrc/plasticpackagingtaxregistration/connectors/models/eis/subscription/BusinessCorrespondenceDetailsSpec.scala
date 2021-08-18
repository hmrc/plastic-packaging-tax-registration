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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.plasticpackagingtaxregistration.base.data.{
  RegistrationTestData,
  SubscriptionTestData
}
import uk.gov.hmrc.plasticpackagingtaxregistration.builders.RegistrationBuilder

class BusinessCorrespondenceDetailsSpec
    extends AnyWordSpec with Matchers with SubscriptionTestData with RegistrationTestData
    with RegistrationBuilder {

  "BusinessCorrespondenceDetails" should {
    "map from registration business entity registered address" when {
      "primary contact marked as same as registered business entity address" in {
        val registrationUsingBusinessAddress =
          aRegistration(withOrganisationDetails(pptIncorporationDetails),
                        withPrimaryContactDetails(pptPrimaryContactDetailsSharingBusinessAddress),
                        withLiabilityDetails(pptLiabilityDetails)
          )

        val businessCorrespondenceDetails =
          BusinessCorrespondenceDetails(registrationUsingBusinessAddress)

        businessCorrespondenceDetails.addressLine1 mustBe pptBusinessAddress.addressLine1
        businessCorrespondenceDetails.addressLine2 mustBe pptBusinessAddress.addressLine2.get
        businessCorrespondenceDetails.addressLine3 mustBe pptBusinessAddress.addressLine3
        businessCorrespondenceDetails.addressLine4 mustBe Some(pptBusinessAddress.townOrCity)
        businessCorrespondenceDetails.postalCode mustBe Some(pptBusinessAddress.postCode)
        businessCorrespondenceDetails.countryCode mustBe "GB"
      }
    }

    "map from supplied primary contact address" when {
      "primary contact supplied with discrete primary contact address" in {
        val registrationWithDifferentPrimaryContractAddress =
          aRegistration(withOrganisationDetails(pptIncorporationDetails),
                        withPrimaryContactDetails(pptPrimaryContactDetails),
                        withLiabilityDetails(pptLiabilityDetails)
          )

        val businessCorrespondenceDetails =
          BusinessCorrespondenceDetails(registrationWithDifferentPrimaryContractAddress)

        businessCorrespondenceDetails.addressLine1 mustBe pptPrimaryContactAddress.addressLine1
        businessCorrespondenceDetails.addressLine2 mustBe pptPrimaryContactAddress.addressLine2.get
        businessCorrespondenceDetails.addressLine3 mustBe pptPrimaryContactAddress.addressLine3
        businessCorrespondenceDetails.addressLine4 mustBe Some(pptPrimaryContactAddress.townOrCity)
        businessCorrespondenceDetails.postalCode mustBe Some(pptPrimaryContactAddress.postCode)
        businessCorrespondenceDetails.countryCode mustBe "GB"
      }
    }

    "throw IllegalStateException" when {
      "registration suggests primary contact address same as business address but no business address provided" in {
        val registrationWithMissingBusinessAddress =
          aRegistration(withPrimaryContactDetails(pptPrimaryContactDetailsSharingBusinessAddress),
                        withOrganisationDetails(
                          pptIncorporationDetails.copy(businessRegisteredAddress = None)
                        )
          )

        intercept[IllegalStateException] {
          BusinessCorrespondenceDetails(registrationWithMissingBusinessAddress)
        }
      }

      "registration suggests discrete primary contact address supplied but it is missing" in {
        val registrationWithMissingPrimaryContactAddress =
          aRegistration(withPrimaryContactDetails(pptPrimaryContactDetails.copy(address = None)))

        intercept[IllegalStateException] {
          BusinessCorrespondenceDetails(registrationWithMissingPrimaryContactAddress)
        }
      }
    }
  }
}
