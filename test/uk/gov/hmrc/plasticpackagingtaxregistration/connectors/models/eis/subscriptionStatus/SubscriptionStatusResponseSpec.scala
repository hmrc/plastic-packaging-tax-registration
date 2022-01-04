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

package uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.eis.subscriptionStatus

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SubscriptionStatusResponseSpec extends AnyWordSpec with Matchers {

  "SubscriptionStatusResponse" should {
    "convert from ETMPSubscriptionStatusResponse" when {
      "ETMP status is 'NO_FORM_BUNDLE_FOUND'" in {
        val response = SubscriptionStatusResponse.fromETMPResponse(
          ETMPSubscriptionStatusResponse(Some(ETMPSubscriptionStatus.NO_FORM_BUNDLE_FOUND))
        )
        response.status mustBe SubscriptionStatus.NOT_SUBSCRIBED
      }

      "ETMP status is 'SUCCESSFUL'" in {
        val response = SubscriptionStatusResponse.fromETMPResponse(
          ETMPSubscriptionStatusResponse(Some(ETMPSubscriptionStatus.SUCCESSFUL))
        )
        response.status mustBe SubscriptionStatus.SUBSCRIBED
      }

      "ETMP status is something else" in {
        val response = SubscriptionStatusResponse.fromETMPResponse(
          ETMPSubscriptionStatusResponse(Some(ETMPSubscriptionStatus.DS_OUTCOME_IN_PROGRESS))
        )
        response.status mustBe SubscriptionStatus.UNKNOWN
      }
    }

    "handle PPT reference" when {
      "idValue is provided for idType 'ZPPT'" in {
        val response = SubscriptionStatusResponse.fromETMPResponse(
          ETMPSubscriptionStatusResponse(subscriptionStatus =
                                           Some(ETMPSubscriptionStatus.SUCCESSFUL),
                                         idValue = Some("PPTRef"),
                                         idType = Some("ZPPT")
          )
        )
        response.pptReference mustBe Some("PPTRef")
      }

      "idValue is provided for other idType" in {
        val response = SubscriptionStatusResponse.fromETMPResponse(
          ETMPSubscriptionStatusResponse(subscriptionStatus =
                                           Some(ETMPSubscriptionStatus.SUCCESSFUL),
                                         idValue = Some("PPTRef"),
                                         idType = Some("XYZ")
          )
        )
        response.pptReference mustBe None
      }

      "idValue is not provided" in {
        val response = SubscriptionStatusResponse.fromETMPResponse(
          ETMPSubscriptionStatusResponse(subscriptionStatus =
            Some(ETMPSubscriptionStatus.SUCCESSFUL)
          )
        )
        response.pptReference mustBe None
      }

    }
  }
}
