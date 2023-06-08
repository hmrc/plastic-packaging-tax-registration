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

import play.api.libs.json._
import models.PartnerTypeEnum.{
  LIMITED_LIABILITY_PARTNERSHIP,
  PartnerTypeEnum,
  SCOTTISH_LIMITED_PARTNERSHIP,
  SCOTTISH_PARTNERSHIP,
  SOLE_TRADER
}

import java.util.UUID

object PartnerTypeEnum extends Enumeration {
  type PartnerTypeEnum = Value
  val SOLE_TRADER: Value                          = Value("SoleTrader")
  val UK_COMPANY: Value                           = Value("UkCompany")
  val REGISTERED_SOCIETY: Value                   = Value("RegisteredSociety")
  val GENERAL_PARTNERSHIP: Value                  = Value("GeneralPartnership")
  val LIMITED_LIABILITY_PARTNERSHIP: Value        = Value("LimitedLiabilityPartnership")
  val LIMITED_PARTNERSHIP: Value                  = Value("LimitedPartnership")
  val SCOTTISH_PARTNERSHIP: Value                 = Value("ScottishPartnership")
  val SCOTTISH_LIMITED_PARTNERSHIP: Value         = Value("ScottishLimitedPartnership")
  val CHARITABLE_INCORPORATED_ORGANISATION: Value = Value("CIO")
  val OVERSEAS_COMPANY_UK_BRANCH: Value           = Value("OverseasCompanyUkBranch")
  val OVERSEAS_COMPANY_NO_UK_BRANCH: Value        = Value("OverseasCompanyNoUKBranch")

  implicit val format: Format[PartnerTypeEnum] =
    Format(Reads.enumNameReads(PartnerTypeEnum), Writes.enumNameWrites)

  val partnerTypesWhichRepresentPartnerships = Seq(GENERAL_PARTNERSHIP,
                                                   LIMITED_PARTNERSHIP,
                                                   LIMITED_LIABILITY_PARTNERSHIP,
                                                   SCOTTISH_PARTNERSHIP,
                                                   SCOTTISH_LIMITED_PARTNERSHIP
  )

  val partnerTypesWhichMightContainIncorporationDetails =
    Seq(UK_COMPANY, OVERSEAS_COMPANY_UK_BRANCH, OVERSEAS_COMPANY_NO_UK_BRANCH)

}

case class Partner(
  id: String = UUID.randomUUID().toString,
  partnerType: Option[PartnerTypeEnum],
  soleTraderDetails: Option[SoleTraderIncorporationDetails] = None,
  incorporationDetails: Option[IncorporationDetails] = None,
  partnerPartnershipDetails: Option[PartnerPartnershipDetails] = None,
  contactDetails: Option[PartnerContactDetails] = None,
  regWithoutIDFlag: Option[Boolean] = None
) {

  lazy val customerIdentification1: String =
    extractData(soleTraderDetails => Some(soleTraderDetails.ninoOrTrn),
                partnershipDetails => partnershipDetails.partnershipBusinessDetails.map(_.sautr),
                incorpDetails => Some(incorpDetails.companyNumber)
    ).getOrElse(throw new IllegalStateException("First customer identifier is absent"))

  lazy val customerIdentification2: Option[String] =
    extractData(soleTraderDetails => soleTraderDetails.sautr,
                partnershipDetails =>
                  partnershipDetails.partnershipBusinessDetails.map(_.postcode.postcode),
                incorpDetails => incorpDetails.ctutr
    )

  lazy val name: String = extractData(
    soleTraderDetails => Some(s"${soleTraderDetails.firstName} ${soleTraderDetails.lastName}"),
    partnershipDetails => partnershipDetails.name,
    incorpDetails => Some(incorpDetails.companyName)
  ).getOrElse(throw new IllegalStateException("Partner name is absent"))

  private def extractData(
    soleTraderExtractor: SoleTraderIncorporationDetails => Option[String],
    partnershipExtractor: PartnerPartnershipDetails => Option[String],
    incorpExtractor: IncorporationDetails => Option[String]
  ): Option[String] =
    partnerType match {
      case Some(SOLE_TRADER) =>
        soleTraderExtractor(
          this.soleTraderDetails.getOrElse(
            throw new IllegalStateException("Sole trader details absent")
          )
        )
      case Some(LIMITED_LIABILITY_PARTNERSHIP) | Some(SCOTTISH_LIMITED_PARTNERSHIP) | Some(
            SCOTTISH_PARTNERSHIP
          ) =>
        partnershipExtractor(
          this.partnerPartnershipDetails.getOrElse(
            throw new IllegalStateException("Partnership details absent")
          )
        )
      case Some(_) =>
        incorpExtractor(
          incorporationDetails.getOrElse(
            throw new IllegalStateException("Incorporation details absent")
          )
        )
      case None => throw new IllegalStateException("Partner type absent")
    }

}

object Partner {
  implicit val format: OFormat[Partner] = Json.format[Partner]
}
