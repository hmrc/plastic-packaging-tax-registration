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

package models

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpec

class DeregistrationReasonSpec extends AnyWordSpec {

  "DeregistrationReason" should {
    "return" when {
      "Ceased Trading" in {
        DeregistrationReason.apply("Ceased Trading") mustBe DeregistrationReason.CeasedTrading
      }
      "Registered Incorrectly" in {
        DeregistrationReason.apply(
          "Registered Incorrectly"
        ) mustBe DeregistrationReason.RegisteredIncorrectly
      }
      "Taken into Group Registration" in {
        DeregistrationReason.apply(
          "Taken into Group Registration"
        ) mustBe DeregistrationReason.WantToRegisterAsGroup
      }
      "Below De-minimus" in {
        DeregistrationReason.apply(
          "Below De-minimus"
        ) mustBe DeregistrationReason.NotMetAndDoNotExpectToMeetThreshold
      }
    }

    "throw IllegalStateExceptions" when {
      "key is absent" when {
        "accessing deregistration reason" in {
          intercept[IllegalStateException] {
            DeregistrationReason.apply("XXX")
          }
        }
      }
    }
  }
}
