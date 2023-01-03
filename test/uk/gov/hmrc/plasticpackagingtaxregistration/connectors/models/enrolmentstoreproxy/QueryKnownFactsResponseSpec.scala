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

package models.enrolmentstoreproxy

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import base.data.UserEnrolmentData

class QueryKnownFactsResponseSpec extends AnyWordSpec with Matchers with UserEnrolmentData {

  "QueryKnownFactsResponse" should {

    "return PPTReferences" when {

      "there is an enrolment with the correct identifier" in {

        val response = QueryKnownFactsResponse("SERVICE", Seq(pptEnrolment("PPT-1")))
        response.pptEnrolmentReferences mustBe Seq("PPT-1")
      }

      "there are multiple PPT enrolments" in {

        val response =
          QueryKnownFactsResponse("SERVICE",
                                  Seq(pptEnrolment("PPT-1"), irsaEnrolment, pptEnrolment("PPT-2"))
          )
        response.pptEnrolmentReferences mustBe Seq("PPT-1", "PPT-2")
      }

      "there are no PPT enrolments" in {
        val response =
          QueryKnownFactsResponse("SERVICE", Seq(irsaEnrolment))
        response.pptEnrolmentReferences mustBe Seq.empty
      }

      "there are no enrolments" in {
        val response =
          QueryKnownFactsResponse("SERVICE", Seq.empty)
        response.pptEnrolmentReferences mustBe Seq.empty
      }

    }
  }

}
