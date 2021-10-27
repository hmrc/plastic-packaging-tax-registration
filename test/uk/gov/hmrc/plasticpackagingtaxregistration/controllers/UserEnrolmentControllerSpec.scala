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

package uk.gov.hmrc.plasticpackagingtaxregistration.controllers

import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, route, status, _}
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import uk.gov.hmrc.plasticpackagingtaxregistration.base.unit.ControllerSpec
import uk.gov.hmrc.plasticpackagingtaxregistration.builders.{
  RegistrationBuilder,
  RegistrationRequestBuilder
}

import scala.concurrent.Future

class UserEnrolmentControllerSpec
    extends ControllerSpec with RegistrationBuilder with RegistrationRequestBuilder {

  val validPptReference   = "XMPPT000123456"
  val invalidPptReference = "XMPPT000000000"

  def post(pptRef: String = validPptReference) = FakeRequest("POST", s"/enrolment/$pptRef")

  "User Enrolment Controller" should {

    "return 200" when {
      "enrolment is successful" in {
        withAuthorizedUser()
        val userEnrolment = Json.obj("registrationDate" -> "2021-10-09", "postcode" -> "AB1 2CD")

        val result: Future[Result] =
          route(app, post().withJsonBody(toJson(userEnrolment))).get

        status(result) must be(OK)
        contentAsJson(result) mustBe Json.obj("pptReference" -> validPptReference)
      }
    }

    "return 400" when {
      "enrolment fails" in {
        withAuthorizedUser()
        val userEnrolment = Json.obj("registrationDate" -> "2021-10-09", "postcode" -> "AB1 2CD")

        val result: Future[Result] =
          route(app, post(invalidPptReference).withJsonBody(toJson(userEnrolment))).get

        status(result) must be(BAD_REQUEST)
        contentAsJson(result) mustBe Json.obj("pptReference" -> invalidPptReference,
                                              "failureCode"  -> "Failed"
        )
      }

      "invalid json" in {
        withAuthorizedUser()
        val userEnrolment = Json.obj("theDate" -> "2021-10-09", "thePostcode" -> "AB1 2CD")

        val result: Future[Result] =
          route(app, post().withJsonBody(toJson(userEnrolment))).get

        status(result) must be(BAD_REQUEST)
        contentAsJson(result) mustBe Json.obj("statusCode" -> 400, "message" -> "Bad Request")
      }
    }

    "return 401" when {
      "unauthorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val userEnrolment = Json.obj("registrationDate" -> "2021-10-09", "postcode" -> "AB1 2CD")

        val result: Future[Result] =
          route(app, post().withJsonBody(toJson(userEnrolment))).get

        status(result) must be(UNAUTHORIZED)
      }
    }
  }
}
