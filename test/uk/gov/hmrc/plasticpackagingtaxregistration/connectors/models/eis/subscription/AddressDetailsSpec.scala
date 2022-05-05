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

package uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.plasticpackagingtaxregistration.base.data.RegistrationTestData
import uk.gov.hmrc.plasticpackagingtaxregistration.models.PPTAddress

class AddressDetailsSpec extends AnyWordSpec with Matchers with RegistrationTestData {

  "AddressDetails" should {
    "map from PPT Address" when {
      "provided with missing town or city" in {
        val addressDetails = AddressDetails(
          PPTAddress(addressLine1 = "Line 1",
                     addressLine2 = None,
                     addressLine3 = Some("Basingstoke"),
                     townOrCity = "",
                     postCode = Some("ZZ1 1ZZ"),
                     countryCode = "GB"
          )
        )

        addressDetails.addressLine1 mustBe "Line 1"
        addressDetails.addressLine2 mustBe "Basingstoke"
        addressDetails.addressLine3 mustBe None
        addressDetails.addressLine4 mustBe None
        addressDetails.postalCode mustBe Some("ZZ1 1ZZ")
        addressDetails.countryCode mustBe "GB"
      }

      "provided with some empty strings" in {
        val addressDetails = AddressDetails(
          PPTAddress(addressLine1 = "Line 1",
                     addressLine2 = Some(""),
                     addressLine3 = Some("Line 3"),
                     townOrCity = "",
                     postCode = Some(""),
                     countryCode = "GB"
          )
        )

        addressDetails.addressLine1 mustBe "Line 1"
        addressDetails.addressLine2 mustBe "Line 3"
        addressDetails.addressLine3 mustBe None
        addressDetails.addressLine4 mustBe None
        addressDetails.postalCode mustBe None
        addressDetails.countryCode mustBe "GB"
      }

      "provided with all empty strings" in {
        val addressDetails = AddressDetails(
          PPTAddress(addressLine1 = "",
                     addressLine2 = Some(""),
                     addressLine3 = Some(""),
                     townOrCity = "",
                     postCode = Some(""),
                     countryCode = "GB"
          )
        )

        addressDetails.addressLine1 mustBe " "
        addressDetails.addressLine2 mustBe " "
        addressDetails.addressLine3 mustBe None
        addressDetails.addressLine4 mustBe None
        addressDetails.postalCode mustBe None
        addressDetails.countryCode mustBe "GB"
      }

      "only 'addressLine1', 'townOrCity' and 'PostCode' are available" in {
        val pptAddress =
          PPTAddress(addressLine1 = "addressLine1",
                     townOrCity = "Town",
                     postCode = Some("PostCode")
          )
        val addressDetails = AddressDetails(pptAddress)
        addressDetails.addressLine1 mustBe "addressLine1"
        addressDetails.addressLine2 mustBe "Town"
        addressDetails.addressLine3 mustBe None
        addressDetails.addressLine4 mustBe None
        addressDetails.postalCode mustBe Some("PostCode")
      }

      "only 'addressLine1', 'addressLine2', 'townOrCity' and 'PostCode' are available" in {
        val pptAddress =
          PPTAddress(addressLine1 = "addressLine1",
                     addressLine2 = Some("addressLine2"),
                     townOrCity = "Town",
                     postCode = Some("PostCode")
          )
        val addressDetails = AddressDetails(pptAddress)
        addressDetails.addressLine1 mustBe "addressLine1"
        addressDetails.addressLine2 mustBe "addressLine2"
        addressDetails.addressLine3 mustBe Some("Town")
        addressDetails.addressLine4 mustBe None
        addressDetails.postalCode mustBe Some("PostCode")
      }

      "all  PPT address fields are available" in {
        val pptAddress =
          PPTAddress(addressLine1 = "addressLine1",
                     addressLine2 = Some("addressLine2"),
                     addressLine3 = Some("addressLine3"),
                     townOrCity = "Town",
                     postCode = Some("PostCode")
          )
        val addressDetails = AddressDetails(pptAddress)
        addressDetails.addressLine1 mustBe "addressLine1"
        addressDetails.addressLine2 mustBe "addressLine2"
        addressDetails.addressLine3 mustBe Some("addressLine3")
        addressDetails.addressLine4 mustBe Some("Town")
        addressDetails.postalCode mustBe Some("PostCode")
      }
    }

  }
}
