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
import models.{PPTAddress, PostCodeWithoutSpaces}

class BusinessCorrespondenceDetailsSpec
    extends AnyWordSpec with Matchers with RegistrationTestData with RegistrationBuilder {

  "BusinessCorrespondenceDetails" when {
    "provided with some empty strings" in {
      val details = BusinessCorrespondenceDetails(
        PPTAddress(addressLine1 = "Line 1",
                   addressLine2 = Some(""),
                   addressLine3 = Some("Line 3"),
                   townOrCity = "",
                   postCode = Some(""),
                   countryCode = "GB"
        )
      )

      details.addressLine1 mustBe "Line 1"
      details.addressLine2 mustBe "Line 3"
      details.addressLine3 mustBe None
      details.addressLine4 mustBe None
      details.postalCode.get.postcode mustBe ""
      details.countryCode mustBe "GB"
    }

    "provided with all empty strings" in {
      val details = BusinessCorrespondenceDetails(
        PPTAddress(addressLine1 = "",
                   addressLine2 = Some(""),
                   addressLine3 = Some(""),
                   townOrCity = "",
                   postCode = None,
                   countryCode = "GB"
        )
      )

      details.addressLine1 mustBe " "
      details.addressLine2 mustBe " "
      details.addressLine3 mustBe None
      details.addressLine4 mustBe None
      details.postalCode mustBe None
      details.countryCode mustBe "GB"
    }

    "building from PPTAddress" should {
      "map address with one line" in {

        val businessCorrespondenceDetails = BusinessCorrespondenceDetails(
          PPTAddress(addressLine1 = "line1", townOrCity = "town", postCode = Some("postcode"))
        )
        businessCorrespondenceDetails.addressLine1 mustBe "line1"
        businessCorrespondenceDetails.addressLine2 mustBe "town"
        businessCorrespondenceDetails.addressLine3 mustBe None
        businessCorrespondenceDetails.addressLine4 mustBe None
        businessCorrespondenceDetails.postalCode mustBe Some(PostCodeWithoutSpaces("postcode"))
        businessCorrespondenceDetails.countryCode mustBe "GB" // default

      }

      "map address with two lines" in {

        val businessCorrespondenceDetails = BusinessCorrespondenceDetails(
          PPTAddress(addressLine1 = "line1",
                     addressLine2 = Some("line2"),
                     townOrCity = "town",
                     postCode = Some("postcode")
          )
        )
        businessCorrespondenceDetails.addressLine1 mustBe "line1"
        businessCorrespondenceDetails.addressLine2 mustBe "line2"
        businessCorrespondenceDetails.addressLine3 mustBe Some("town")
        businessCorrespondenceDetails.addressLine4 mustBe None
        businessCorrespondenceDetails.postalCode mustBe Some(PostCodeWithoutSpaces("postcode"))
        businessCorrespondenceDetails.countryCode mustBe "GB" // default

      }

      "map address with three lines" in {

        val businessCorrespondenceDetails = BusinessCorrespondenceDetails(
          PPTAddress(addressLine1 = "line1",
                     addressLine2 = Some("line2"),
                     addressLine3 = Some("line3"),
                     townOrCity = "town",
                     postCode = Some("postcode")
          )
        )
        businessCorrespondenceDetails.addressLine1 mustBe "line1"
        businessCorrespondenceDetails.addressLine2 mustBe "line2"
        businessCorrespondenceDetails.addressLine3 mustBe Some("line3")
        businessCorrespondenceDetails.addressLine4 mustBe Some("town")
        businessCorrespondenceDetails.postalCode mustBe Some(PostCodeWithoutSpaces("postcode"))
        businessCorrespondenceDetails.countryCode mustBe "GB" // default

      }
    }

    "building from non-partnership registration" should {
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
          businessCorrespondenceDetails.addressLine2 mustBe pptBusinessAddress.addressLine2.getOrElse(
            ""
          )
          businessCorrespondenceDetails.addressLine3 mustBe pptBusinessAddress.addressLine3
          businessCorrespondenceDetails.addressLine4 mustBe Some(pptBusinessAddress.townOrCity)
          businessCorrespondenceDetails.postalCode mustBe pptBusinessAddress.postCode
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
          businessCorrespondenceDetails.addressLine2 mustBe pptPrimaryContactAddress.addressLine2.getOrElse(
            ""
          )
          businessCorrespondenceDetails.addressLine3 mustBe pptPrimaryContactAddress.addressLine3
          businessCorrespondenceDetails.addressLine4 mustBe Some(
            pptPrimaryContactAddress.townOrCity
          )
          businessCorrespondenceDetails.postalCode mustBe pptPrimaryContactAddress.postCode
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

    "building from partnership registration" should {
      "map from nominated partner" in {
        val partnershipRegistration =
          aRegistration(withLiabilityDetails(pptLiabilityDetails),
                        withOrganisationDetails(pptGeneralPartnershipDetails)
          )

        val businessCorrespondenceDetails =
          BusinessCorrespondenceDetails(partnershipRegistration)

        val nominatedPartner = pptGeneralPartnershipDetails.partnershipDetails.get.partners.head

        businessCorrespondenceDetails mustBe BusinessCorrespondenceDetails(
          nominatedPartner.contactDetails.get.address.get
        )
      }
    }
  }
}
