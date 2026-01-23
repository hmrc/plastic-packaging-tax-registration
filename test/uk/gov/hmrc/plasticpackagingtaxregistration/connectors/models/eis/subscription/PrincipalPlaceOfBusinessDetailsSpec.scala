/*
 * Copyright 2026 HM Revenue & Customs
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
import builders.RegistrationBuilder

class PrincipalPlaceOfBusinessDetailsSpec
    extends AnyWordSpec with Matchers with RegistrationTestData with RegistrationBuilder {

  "PrincipalPlaceOfBusinessDetails" when {
    "building from non-partnership registration" should {
      val registration = aRegistration(withOrganisationDetails(pptIncorporationDetails),
                                       withPrimaryContactDetails(pptPrimaryContactDetails),
                                       withLiabilityDetails(pptLiabilityDetails.copy(weight = None))
      )
      "build as expected" in {
        val principalPlaceOfBusinessDetails = PrincipalPlaceOfBusinessDetails(registration)
        principalPlaceOfBusinessDetails.addressDetails.addressLine1 mustBe pptBusinessAddress.addressLine1
        principalPlaceOfBusinessDetails.addressDetails.addressLine2 mustBe pptBusinessAddress.addressLine2.getOrElse(
          ""
        )
        principalPlaceOfBusinessDetails.addressDetails.addressLine3 mustBe pptBusinessAddress.addressLine3
        principalPlaceOfBusinessDetails.addressDetails.addressLine4 mustBe Some(
          pptBusinessAddress.townOrCity
        )
        principalPlaceOfBusinessDetails.addressDetails.postalCode mustBe pptBusinessAddress.postCode
        principalPlaceOfBusinessDetails.addressDetails.countryCode mustBe pptBusinessAddress.countryCode

        principalPlaceOfBusinessDetails.contactDetails.email mustBe pptPrimaryContactDetails.email.get
        principalPlaceOfBusinessDetails.contactDetails.telephone mustBe pptPrimaryContactDetails.phoneNumber.get
        principalPlaceOfBusinessDetails.contactDetails.mobileNumber mustBe None
      }

      "throw IllegalStateException" when {
        "business registered address is absent" in {
          val registration = aRegistration(
            withOrganisationDetails(pptIncorporationDetails.copy(businessRegisteredAddress = None)),
            withPrimaryContactDetails(pptPrimaryContactDetails),
            withLiabilityDetails(pptLiabilityDetails)
          )
          intercept[IllegalStateException] {
            PrincipalPlaceOfBusinessDetails(registration)
          }
        }
      }
      "email address is absent" in {
        val registration = aRegistration(withOrganisationDetails(pptIncorporationDetails),
                                         withPrimaryContactDetails(
                                           pptPrimaryContactDetails.copy(email = None)
                                         ),
                                         withLiabilityDetails(pptLiabilityDetails)
        )
        intercept[IllegalStateException] {
          PrincipalPlaceOfBusinessDetails(registration)
        }
      }
      "phone number is absent" in {
        val registration = aRegistration(withOrganisationDetails(pptIncorporationDetails),
                                         withPrimaryContactDetails(
                                           pptPrimaryContactDetails.copy(phoneNumber = None)
                                         ),
                                         withLiabilityDetails(pptLiabilityDetails)
        )
        intercept[IllegalStateException] {
          PrincipalPlaceOfBusinessDetails(registration)
        }
      }
    }

    "building from partnership registration" should {
      val registration = aRegistration(withLiabilityDetails(pptLiabilityDetails),
                                       withOrganisationDetails(pptGeneralPartnershipDetails)
      )
      "build as expected" in {
        val principalPlaceOfBusinessDetails = PrincipalPlaceOfBusinessDetails(registration)
        principalPlaceOfBusinessDetails.addressDetails.addressLine1 mustBe pptBusinessAddress.addressLine1
        principalPlaceOfBusinessDetails.addressDetails.addressLine2 mustBe pptBusinessAddress.addressLine2.getOrElse(
          ""
        )
        principalPlaceOfBusinessDetails.addressDetails.addressLine3 mustBe pptBusinessAddress.addressLine3
        principalPlaceOfBusinessDetails.addressDetails.addressLine4 mustBe Some(
          pptBusinessAddress.townOrCity
        )
        principalPlaceOfBusinessDetails.addressDetails.postalCode mustBe pptBusinessAddress.postCode
        principalPlaceOfBusinessDetails.addressDetails.countryCode mustBe pptBusinessAddress.countryCode

        val nominatedPartner = pptGeneralPartnershipDetails.partnershipDetails.get.partners.head
        principalPlaceOfBusinessDetails.contactDetails.email mustBe nominatedPartner.contactDetails.get.emailAddress.get
        principalPlaceOfBusinessDetails.contactDetails.telephone mustBe nominatedPartner.contactDetails.get.phoneNumber.get
        principalPlaceOfBusinessDetails.contactDetails.mobileNumber mustBe None
      }

      "throw IllegalStateException" when {
        "business registered address is absent" in {
          val registration =
            aRegistration(withLiabilityDetails(pptLiabilityDetails),
                          withOrganisationDetails(
                            pptGeneralPartnershipDetails.copy(businessRegisteredAddress = None)
                          )
            )
          intercept[IllegalStateException] {
            PrincipalPlaceOfBusinessDetails(registration)
          }
        }
      }
      "email address is absent" in {
        val registration = aRegistration(withLiabilityDetails(pptLiabilityDetails),
                                         withOrganisationDetails(pptGeneralPartnershipDetails),
                                         withPartnerModifications(
                                           partner =>
                                             partner.copy(contactDetails =
                                               partner.contactDetails.map(
                                                 _.copy(emailAddress = None)
                                               )
                                             )
                                         )
        )
        intercept[IllegalStateException] {
          PrincipalPlaceOfBusinessDetails(registration)
        }
      }
      "phone number is absent" in {
        val registration = aRegistration(withLiabilityDetails(pptLiabilityDetails),
                                         withOrganisationDetails(pptGeneralPartnershipDetails),
                                         withPartnerModifications(
                                           partner =>
                                             partner.copy(contactDetails =
                                               partner.contactDetails.map(
                                                 _.copy(phoneNumber = None)
                                               )
                                             )
                                         )
        )
        intercept[IllegalStateException] {
          PrincipalPlaceOfBusinessDetails(registration)
        }
      }
    }
  }
}
