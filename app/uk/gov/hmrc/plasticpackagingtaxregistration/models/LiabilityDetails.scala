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

import java.time.LocalDate
import play.api.libs.json.{Json, OFormat}

case class LiabilityWeight(totalKg: Option[Long])

object LiabilityWeight {
  implicit val liabilityWeightFormat = Json.format[LiabilityWeight]
}

case class LiabilityExpectedWeight(
  expectToExceedThresholdWeight: Option[Boolean],
  totalKg: Option[Long]
)

object LiabilityExpectedWeight {
  implicit val liabilityWeightExpectedFormat = Json.format[LiabilityExpectedWeight]
}

case class Date(date: LocalDate)

object Date {
  implicit val format: OFormat[Date] = Json.format[Date]
}

case class taxStartDate(day: Option[Int], month: Option[Int], year: Option[Int]) {

  val pretty: String =
    LocalDate.of(year.getOrElse(0), month.getOrElse(0), day.getOrElse(0)).toString

}

object taxStartDate {
  implicit val dateFormat = Json.format[taxStartDate]

  def apply(date: LocalDate): taxStartDate =
    taxStartDate(Some(date.getDayOfMonth), Some(date.getMonthValue), Some(date.getYear))

}

case class LiabilityDetails(
  expectToExceedThresholdWeight: Option[Boolean] = None,
  dateRealisedExpectedToExceedThresholdWeight: Option[Date] = None,
  exceededThresholdWeight: Option[Boolean] = None,
  dateExceededThresholdWeight: Option[Date] = None,
  expectedWeightNext12m: Option[LiabilityWeight] = None,
  // Derived fields - not directly input by user
  startDate: Option[taxStartDate] = None,
  isLiable: Option[Boolean] = None
) {

  def liabilityWeight: Long =
    expectedWeightNext12m.flatMap(x => x.totalKg) match {
      case Some(value) => value
      case None        => throw new IllegalStateException("Missing expectedWeightNext12m.totalKg field")
    }

}

object LiabilityDetails {
  implicit val format: OFormat[LiabilityDetails] = Json.format[LiabilityDetails]
}
