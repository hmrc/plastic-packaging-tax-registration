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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.plasticpackagingtaxregistration.base.data.{
  RegistrationTestData,
  SubscriptionTestData
}

class PrimaryContactDetailsSpec extends AnyWordSpec with Matchers with SubscriptionTestData {

  "PrimaryContactDetails" should {
    "build successfully" when {
      "there is no middle name " in {
        val individualDetails     = groupPartnershipDetailsMember.individualDetails
        val primaryContactDetails = PrimaryContactDetails(groupPartnershipDetailsMember)
        primaryContactDetails.name mustBe Some(
          s"${individualDetails.firstName} ${individualDetails.lastName}"
        )
        primaryContactDetails.jobTitle mustBe None
        primaryContactDetails.address mustBe Some(
          PPTAddress(groupPartnershipDetailsMember.addressDetails)
        )
        primaryContactDetails.phoneNumber mustBe Some(
          groupPartnershipDetailsMember.contactDetails.telephone
        )
        primaryContactDetails.email mustBe Some(groupPartnershipDetailsMember.contactDetails.email)
      }

      "there is middle name " in {
        val updatedIndividualDetails =
          groupPartnershipDetailsMember.individualDetails.copy(middleName = Some("Test"))
        val updatedDetails =
          groupPartnershipDetailsMember.copy(individualDetails = updatedIndividualDetails)
        val primaryContactDetails = PrimaryContactDetails(updatedDetails)
        primaryContactDetails.name mustBe Some("Arthur Test Surname")
        primaryContactDetails.jobTitle mustBe None
        primaryContactDetails.address mustBe Some(
          PPTAddress(groupPartnershipDetailsMember.addressDetails)
        )
        primaryContactDetails.phoneNumber mustBe Some(
          groupPartnershipDetailsMember.contactDetails.telephone
        )
        primaryContactDetails.email mustBe Some(groupPartnershipDetailsMember.contactDetails.email)
      }
    }
  }
}
