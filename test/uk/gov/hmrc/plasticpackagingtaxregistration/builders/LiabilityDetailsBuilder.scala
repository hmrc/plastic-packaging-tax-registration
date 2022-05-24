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

package uk.gov.hmrc.plasticpackagingtaxregistration.builders

import java.time.LocalDate

import uk.gov.hmrc.plasticpackagingtaxregistration.models.{LiabilityDetails, LiabilityWeight, OldDate}

trait LiabilityDetailsBuilder {
  private type Modifier = LiabilityDetails => LiabilityDetails

  private val baseModel: LiabilityDetails = LiabilityDetails()

  def aLiability(modifiers: Modifier*): LiabilityDetails = modifiers.foldLeft(baseModel)((acc, next) => next(acc))

  def  withStartDate(startDate: LocalDate): Modifier = _.copy(startDate = Some(OldDate(startDate)))

  def withExpectedWeightNext12m(weightInKg: Long):Modifier = _.copy(expectedWeightNext12m = Some(LiabilityWeight(Some(weightInKg))))
}
