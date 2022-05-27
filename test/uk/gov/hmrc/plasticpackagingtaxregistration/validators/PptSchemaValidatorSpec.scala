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

package uk.gov.hmrc.plasticpackagingtaxregistration.validators

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.plasticpackagingtaxregistration.base.data.SubscriptionTestData

class PptSchemaValidatorSpec extends AnyWordSpec with Matchers with SubscriptionTestData {

  "validate" when {
    "given a valid create" should {
      "return success" in {
        PptSchemaValidator.subscriptionValidator.validate(
          ukLimitedCompanySubscription
        ).isSuccess mustBe true
      }

    }

    "given an invalid create" should {
      "return failure" in {
        PptSchemaValidator.subscriptionValidator.validate(
          ukLimitedCompanySubscriptionInvalid
        ).isSuccess mustBe false
      }

    }

  }

}