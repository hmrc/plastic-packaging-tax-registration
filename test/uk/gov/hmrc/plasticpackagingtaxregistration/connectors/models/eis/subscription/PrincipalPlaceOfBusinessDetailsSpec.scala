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
import uk.gov.hmrc.plasticpackagingtaxregistration.base.data.RegistrationTestData
import uk.gov.hmrc.plasticpackagingtaxregistration.builders.RegistrationBuilder

class PrincipalPlaceOfBusinessDetailsSpec
    extends AnyWordSpec with Matchers with RegistrationTestData with RegistrationBuilder {
  "PrincipalPlaceOfBusinessDetails" should {
    val registration = aRegistration(withOrganisationDetails(pptIncorporationDetails),
                                     withPrimaryContactDetails(pptPrimaryContactDetails),
                                     withLiabilityDetails(pptLiabilityDetails.copy(weight = None))
    )
    "build" in {
      val principalPlaceOfBusinessDetails = PrincipalPlaceOfBusinessDetails(registration)
      principalPlaceOfBusinessDetails.addressDetails.addressLine1 mustBe pptBusinessAddress.addressLine1
      principalPlaceOfBusinessDetails.addressDetails.addressLine2 mustBe pptBusinessAddress.addressLine2
      principalPlaceOfBusinessDetails.addressDetails.addressLine3 mustBe pptBusinessAddress.addressLine3
      principalPlaceOfBusinessDetails.addressDetails.addressLine4 mustBe Some(
        pptBusinessAddress.townOrCity
      )
      principalPlaceOfBusinessDetails.addressDetails.postalCode mustBe Some(
        pptBusinessAddress.postCode
      )
      principalPlaceOfBusinessDetails.addressDetails.countryCode mustBe pptBusinessAddress.country.get

      principalPlaceOfBusinessDetails.contactDetails.email mustBe pptPrimaryContactDetails.email.get
      principalPlaceOfBusinessDetails.contactDetails.telephone mustBe pptPrimaryContactDetails.phoneNumber.get
      principalPlaceOfBusinessDetails.contactDetails.mobileNumber mustBe None
    }

    "throw an exception when address is not available" in {
      val registration = aRegistration(
        withOrganisationDetails(pptIncorporationDetails.copy(businessRegisteredAddress = None)),
        withPrimaryContactDetails(pptPrimaryContactDetails),
        withLiabilityDetails(pptLiabilityDetails)
      )
      intercept[Exception] {
        PrincipalPlaceOfBusinessDetails(registration)
      }
    }
  }
}
