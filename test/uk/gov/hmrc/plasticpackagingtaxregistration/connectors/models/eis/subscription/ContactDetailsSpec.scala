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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import base.data.RegistrationTestData

class ContactDetailsSpec extends AnyWordSpec with Matchers with RegistrationTestData {
  "ContactDetails" when {
    "built from primary contact details (single entity registration)" should {
      "map from PPT PrimaryContactDetails" in {
        val contactDetails = ContactDetails(pptPrimaryContactDetails)
        contactDetails.email mustBe pptPrimaryContactDetails.email.get
        contactDetails.telephone mustBe pptPrimaryContactDetails.phoneNumber.get
        contactDetails.mobileNumber mustBe None
      }

      "throw exception " when {
        "'email' is not available " in {
          intercept[IllegalStateException] {
            ContactDetails(pptPrimaryContactDetails.copy(email = None))
          }
        }

        "'PhoneNumber' is not available " in {
          intercept[IllegalStateException] {
            ContactDetails(pptPrimaryContactDetails.copy(phoneNumber = None))
          }
        }
      }
    }

    "built from group registration" should {
      "map from group member contact details" in {
        val groupMember = aGroupMember()

        val contactDetails = ContactDetails(groupMember.contactDetails.get)

        contactDetails.email mustBe groupMember.contactDetails.get.email.get
        contactDetails.telephone mustBe groupMember.contactDetails.get.phoneNumber.get
        contactDetails.mobileNumber mustBe None
      }

      "throw exception " when {
        "'email' is not available " in {
          intercept[IllegalStateException] {
            ContactDetails(
              aGroupMember().copy(contactDetails =
                aGroupMember().contactDetails.map(_.copy(email = None))
              ).contactDetails.get
            )
          }
        }

        "'PhoneNumber' is not available " in {
          intercept[IllegalStateException] {
            ContactDetails(
              aGroupMember().copy(contactDetails =
                aGroupMember().contactDetails.map(_.copy(phoneNumber = None))
              ).contactDetails.get
            )
          }
        }
      }
    }

    "built from partnership registration" should {
      "map from partner contact details" in {
        val partner = aUkCompanyPartner()

        val contactDetails = ContactDetails(partner.contactDetails.get)

        contactDetails.email mustBe partner.contactDetails.get.emailAddress.get
        contactDetails.telephone mustBe partner.contactDetails.get.phoneNumber.get
        contactDetails.mobileNumber mustBe None
      }

      "throw exception " when {
        "'email' is not available " in {
          intercept[IllegalStateException] {
            ContactDetails(
              aUkCompanyPartner().copy(contactDetails =
                aUkCompanyPartner().contactDetails.map(_.copy(emailAddress = None))
              ).contactDetails.get
            )
          }
        }

        "'PhoneNumber' is not available " in {
          intercept[IllegalStateException] {
            ContactDetails(
              aUkCompanyPartner().copy(contactDetails =
                aUkCompanyPartner().contactDetails.map(_.copy(phoneNumber = None))
              ).contactDetails.get
            )
          }
        }
      }
    }
  }
}
