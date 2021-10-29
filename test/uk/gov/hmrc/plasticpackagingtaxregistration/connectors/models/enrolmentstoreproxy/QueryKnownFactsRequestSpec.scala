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

package uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.enrolmentstoreproxy

import java.time.LocalDate

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.plasticpackagingtaxregistration.base.data.UserEnrolmentData
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.KeyValue

class QueryKnownFactsRequestSpec extends AnyWordSpec with Matchers with UserEnrolmentData {

  "QueryKnownFactsRequest" should {

    "build successfully" when {

      "created from UserEnrolmentRequest" in {

        val request = QueryKnownFactsRequest.apply(
          userEnrolmentRequest.copy(registrationDate = LocalDate.parse("2021-10-31"),
                                    postcode = Some("ZZ12YY")
          )
        )

        request must be(
          QueryKnownFactsRequest(
            service = "HMRC-PPT-ORG",
            knownFacts = Seq(KeyValue("PPTRegistrationDate", "20211031"), // Note date format
                             KeyValue("BusinessPostCode", "ZZ12YY")
            )
          )
        )

      }
    }
  }

}
