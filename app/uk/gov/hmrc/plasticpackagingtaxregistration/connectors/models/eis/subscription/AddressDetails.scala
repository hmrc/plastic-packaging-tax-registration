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

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.auth.core.InternalError
import uk.gov.hmrc.plasticpackagingtaxregistration.models.{Address => PPTAddress}

case class AddressDetails(
  addressLine1: String,
  addressLine2: String,
  addressLine3: Option[String] = None,
  addressLine4: Option[String] = None,
  postalCode: Option[String] = None,
  countryCode: String
)

object AddressDetails {
  implicit val format: OFormat[AddressDetails] = Json.format[AddressDetails]

  def apply(address: Option[PPTAddress]): AddressDetails =
    address match {
      case Some(addressDetails) =>
        AddressDetails(addressLine1 = addressDetails.addressLine1,
                       addressLine2 = addressDetails.addressLine2.getOrElse(""),
                       addressLine3 = addressDetails.addressLine3,
                       addressLine4 = Some(addressDetails.townOrCity),
                       postalCode = Some(addressDetails.postCode),
                       countryCode = "GB"
        )
      case None => throw InternalError(s"The legal entity registered address is required.")
    }

}
