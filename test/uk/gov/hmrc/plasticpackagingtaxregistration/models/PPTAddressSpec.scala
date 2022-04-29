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

package uk.gov.hmrc.plasticpackagingtaxregistration.models

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.{
  AddressDetails,
  BusinessCorrespondenceDetails
}

import scala.language.implicitConversions

class PPTAddressSpec extends AnyWordSpec {

  implicit def toPostcode(value: String): PostCodeWithoutSpaces = PostCodeWithoutSpaces(value)

  "PPTAddress" should {

    "convert from AddressDetails" when {

      "address has 2 lines" in {

        val addr = PPTAddress(
          AddressDetails(addressLine1 = "line1",
                         addressLine2 = "town",
                         postalCode = Some("AB12CD"),
                         countryCode = "FR"
          )
        )

        addr mustBe PPTAddress(addressLine1 = "line1",
                               townOrCity = "town",
                               postCode = Some("AB12CD"),
                               countryCode = "FR"
        )
      }

      "address has 3 lines" in {

        val addr = PPTAddress(
          AddressDetails(addressLine1 = "line1",
                         addressLine2 = "line2",
                         addressLine3 = Some("town"),
                         postalCode = Some("AB12CD"),
                         countryCode = "FR"
          )
        )

        addr mustBe PPTAddress(addressLine1 = "line1",
                               addressLine2 = Some("line2"),
                               townOrCity = "town",
                               postCode = Some("AB12CD"),
                               countryCode = "FR"
        )
      }

      "address has 4 lines" in {

        val addr = PPTAddress(
          AddressDetails(addressLine1 = "line1",
                         addressLine2 = "line2",
                         addressLine3 = Some("line3"),
                         addressLine4 = Some("town"),
                         postalCode = Some("AB12CD"),
                         countryCode = "IT"
          )
        )

        addr mustBe PPTAddress(addressLine1 = "line1",
                               addressLine2 = Some("line2"),
                               addressLine3 = Some("line3"),
                               townOrCity = "town",
                               postCode = Some("AB12CD"),
                               countryCode = "IT"
        )
      }
    }

    "convert from BusinessCorrespondenceDetails" when {

      "address has 2 lines" in {

        val addr = PPTAddress(
          BusinessCorrespondenceDetails(addressLine1 = "line1",
                                        addressLine2 = "town",
                                        postalCode = Some("AB12CD"),
                                        countryCode = "IT"
          )
        )

        addr mustBe PPTAddress(addressLine1 = "line1",
                               townOrCity = "town",
                               postCode = Some("AB12CD"),
                               countryCode = "IT"
        )
      }

      "address has 3 lines" in {

        val addr = PPTAddress(
          BusinessCorrespondenceDetails(addressLine1 = "line1",
                                        addressLine2 = "line2",
                                        addressLine3 = Some("town"),
                                        postalCode = Some("AB12CD"),
                                        countryCode = "IT"
          )
        )

        addr mustBe PPTAddress(addressLine1 = "line1",
                               addressLine2 = Some("line2"),
                               townOrCity = "town",
                               postCode = Some("AB12CD"),
                               countryCode = "IT"
        )
      }

      "address has 4 lines" in {

        val addr = PPTAddress(
          BusinessCorrespondenceDetails(addressLine1 = "line1",
                                        addressLine2 = "line2",
                                        addressLine3 = Some("line3"),
                                        addressLine4 = Some("town"),
                                        postalCode = Some("AB12CD"),
                                        countryCode = "IT"
          )
        )

        addr mustBe PPTAddress(addressLine1 = "line1",
                               addressLine2 = Some("line2"),
                               addressLine3 = Some("line3"),
                               townOrCity = "town",
                               postCode = Some("AB12CD"),
                               countryCode = "IT"
        )
      }
    }
  }
}
