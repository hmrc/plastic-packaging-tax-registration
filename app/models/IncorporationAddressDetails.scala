/*
 * Copyright 2023 HM Revenue & Customs
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

package models

import play.api.libs.json.{Format, Json}

case class IncorporationAddressDetails(
  address_line_1: Option[String] = None,
  address_line_2: Option[String] = None,
  locality: Option[String] = None,
  care_of: Option[String] = None,
  po_box: Option[String] = None,
  postal_code: Option[PostCodeWithoutSpaces] = None,
  premises: Option[String] = None,
  region: Option[String] = None,
  country: Option[String] = None
)

object IncorporationAddressDetails {

  implicit val format: Format[IncorporationAddressDetails] =
    Json.format[IncorporationAddressDetails]

}
