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

class PrimaryContactDetailsSpec
    extends AnyWordSpec with Matchers with SubscriptionTestData with RegistrationTestData {

  "PrimaryContactDetails" should {
    "build successfully" in {
      val primaryContactDetails = PrimaryContactDetails(pptPrimaryContactDetails)
      primaryContactDetails.name mustBe pptPrimaryContactDetails.fullName.get.fullName
      primaryContactDetails.positionInCompany mustBe pptPrimaryContactDetails.jobTitle.get
      primaryContactDetails.contactDetails.email mustBe pptPrimaryContactDetails.email.get
      primaryContactDetails.contactDetails.telephone mustBe pptPrimaryContactDetails.phoneNumber.get
      primaryContactDetails.contactDetails.mobileNumber mustBe None
    }

    "throw exception" when {
      "'FullName' is not available" in {
        intercept[Exception] {
          PrimaryContactDetails(pptPrimaryContactDetails.copy(fullName = None))
        }
      }

      "'JobTitle' is not available" in {
        intercept[Exception] {
          PrimaryContactDetails(pptPrimaryContactDetails.copy(jobTitle = None))
        }
      }

    }
  }
}
