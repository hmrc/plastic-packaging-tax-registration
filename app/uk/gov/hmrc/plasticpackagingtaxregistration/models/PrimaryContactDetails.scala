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

import play.api.libs.json.{Json, OFormat}

case class Address(
  addressLine1: String,
  addressLine2: Option[String] = None,
  addressLine3: Option[String] = None,
  townOrCity: String,
  postCode: Option[String],
  countryCode: String = "GB"
) {

  val eisAddressLines: (String, String, Option[String], Option[String]) = {
    val list = Seq(Some(addressLine1), addressLine2, addressLine3, Some(townOrCity)).flatten
    (list.head, list(1), list.lift(2), list.lift(3))
  }

}

object Address {
  implicit val format: OFormat[Address] = Json.format[Address]
}

case class PrimaryContactDetails(
  name: Option[String] = None,
  jobTitle: Option[String] = None,
  email: Option[String] = None,
  phoneNumber: Option[String] = None,
  useRegisteredAddress: Option[Boolean] = None,
  address: Option[Address] = None,
  journeyId: Option[String] = None
)

object PrimaryContactDetails {
  implicit val format: OFormat[PrimaryContactDetails] = Json.format[PrimaryContactDetails]
}
