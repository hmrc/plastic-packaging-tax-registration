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

import play.api.libs.json.{Json, OFormat}
import models.{PPTAddress, PostCodeWithoutSpaces}

case class AddressDetails(
  addressLine1: String,
  addressLine2: String,
  addressLine3: Option[String] = None,
  addressLine4: Option[String] = None,
  postalCode: Option[PostCodeWithoutSpaces] = None,
  countryCode: String
)

object AddressDetails {
  implicit val format: OFormat[AddressDetails] = Json.format[AddressDetails]

  def apply(address: PPTAddress): AddressDetails =
    AddressDetails(addressLine1 = address.eisAddressLines._1,
                   addressLine2 = address.eisAddressLines._2,
                   addressLine3 = address.eisAddressLines._3,
                   addressLine4 = address.eisAddressLines._4,
                   postalCode = address.postCode,
                   countryCode = address.countryCode
    )

}
