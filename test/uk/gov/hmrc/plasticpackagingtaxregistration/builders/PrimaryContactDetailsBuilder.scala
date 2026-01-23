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

package builders

import models.{PPTAddress, PrimaryContactDetails}

trait PrimaryContactDetailsBuilder {
  private type Modifier = PrimaryContactDetails => PrimaryContactDetails

  private val baseModel: PrimaryContactDetails = PrimaryContactDetails()

  def somePrimaryContactDetails(modifiers: Modifier*): PrimaryContactDetails =
    modifiers.foldLeft(baseModel)((acc, next) => next(acc))

  def withEmailAddress(email: String): Modifier = _.copy(email = Some(email))

  def withPhoneNumber(no: String): Modifier = _.copy(phoneNumber = Some(no))

  def withName(name: String): Modifier = _.copy(name = Some(name))

  def withJobTitle(jobTitle: String): Modifier = _.copy(jobTitle = Some(jobTitle))

  def withAddress(address: PPTAddress): Modifier = _.copy(address = Some(address))
}
