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

class BusinessCorrespondenceDetailsSpec
    extends AnyWordSpec with Matchers with SubscriptionTestData with RegistrationTestData {

  "BusinessCorrespondenceDetails" should {
    "map from PPT Address " when {
      "all  PPT address fields are available" in {
        val businessCorrespondenceDetails = BusinessCorrespondenceDetails(pptOrganisationDetails)
        businessCorrespondenceDetails.addressLine1 mustBe pptAddress.addressLine1
        businessCorrespondenceDetails.addressLine2 mustBe pptAddress.addressLine2.get
        businessCorrespondenceDetails.addressLine3 mustBe pptAddress.addressLine3
        businessCorrespondenceDetails.addressLine4 mustBe Some(pptAddress.townOrCity)
        businessCorrespondenceDetails.postalCode mustBe Some(pptAddress.postCode)
        businessCorrespondenceDetails.countryCode mustBe "GB"
      }
    }

    "throw exception if PPT Address is not available" in {
      intercept[Exception] {
        BusinessCorrespondenceDetails(pptOrganisationDetails.copy(businessRegisteredAddress = None))
      }
    }

  }
}
