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

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscription.{
  AddressDetails,
  BusinessCorrespondenceDetails
}

case class PPTAddress(
  addressLine1: String,
  addressLine2: Option[String] = None,
  addressLine3: Option[String] = None,
  townOrCity: String,
  postCode: Option[PostCodeWithoutSpaces],
  countryCode: String = "GB"
) {

  val eisAddressLines: (String, String, Option[String], Option[String]) = {
    val list = Seq(Some(addressLine1), addressLine2, addressLine3, Some(townOrCity)).flatten
    (list.head, list(1), list.lift(2), list.lift(3))
  }

}

object PPTAddress {
  implicit val format: OFormat[PPTAddress] = Json.format[PPTAddress]

  def apply(businessCorrespondenceDetails: BusinessCorrespondenceDetails): PPTAddress = {
    val lines = Seq(Some(businessCorrespondenceDetails.addressLine1),
                    Some(businessCorrespondenceDetails.addressLine2),
                    businessCorrespondenceDetails.addressLine3,
                    businessCorrespondenceDetails.addressLine4
    ).flatten

    PPTAddress(lines,
               businessCorrespondenceDetails.postalCode,
               businessCorrespondenceDetails.countryCode
    )
  }

  def apply(addressDetail: AddressDetails): PPTAddress = {

    val lines = Seq(Some(addressDetail.addressLine1),
                    Some(addressDetail.addressLine2),
                    addressDetail.addressLine3,
                    addressDetail.addressLine4
    ).flatten

    PPTAddress(lines, addressDetail.postalCode, addressDetail.countryCode)
  }

  private def apply(
    lines: Seq[String],
    postCode: Option[PostCodeWithoutSpaces],
    countryCode: String
  ): PPTAddress =
    PPTAddress(addressLine1 = lines.head,
               addressLine2 =
                 if (lines.size > 2) lines.lift(1) else None,
               addressLine3 =
                 if (lines.size > 3) lines.lift(2) else None,
               townOrCity = lines.last,
               postCode = postCode,
               countryCode = countryCode
    )

}
