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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, route, status, _}
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import uk.gov.hmrc.plasticpackagingtaxregistration.base.data.UserEnrolmentData
import uk.gov.hmrc.plasticpackagingtaxregistration.base.unit.ControllerSpec
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.enrolmentstoreproxy.GroupsWithEnrolmentsResponse

import scala.concurrent.Future

class UserEnrolmentControllerSpec extends ControllerSpec with UserEnrolmentData {

  private val unknownPptReference = "XMPPT000000000"

  private val post = FakeRequest("POST", "/enrolment")

  override def beforeEach(): Unit = {
    super.beforeEach()

    when(mockEnrolmentStoreProxyConnector.queryKnownFacts(any())(any())).thenReturn(
      Future.successful(Some(queryKnownFactsResponse(knownPptReference)))
    )

    when(mockEnrolmentStoreProxyConnector.queryGroupsWithEnrolment(any())(any())).thenReturn(
      Future.successful(None)
    )
  }

  "User Enrolment Controller" should {

    "return 201 (Create)" when {
      "enrolment is successful" in {
        withAuthorizedUser()

        val userEnrolment = Json.obj("pptReference" -> knownPptReference,
                                     "registrationDate" -> "2021-10-09",
                                     "postcode"         -> "AB1 2CD"
        )

        val result: Future[Result] =
          route(app, post.withJsonBody(toJson(userEnrolment))).get

        status(result) must be(CREATED)
        contentAsJson(result) mustBe Json.obj("pptReference" -> knownPptReference)
      }
    }

    "return 400" when {
      "verification fails" in {
        withAuthorizedUser()
        val userEnrolment = Json.obj("pptReference" -> unknownPptReference,
                                     "registrationDate" -> "2021-10-09",
                                     "postcode"         -> "AB1 2CD"
        )

        val result: Future[Result] =
          route(app, post.withJsonBody(toJson(userEnrolment))).get

        status(result) must be(BAD_REQUEST)
        contentAsJson(result) mustBe Json.obj("pptReference" -> unknownPptReference,
                                              "failureCode"  -> "VerificationFailed"
        )
      }

      "no known facts returned" in {
        withAuthorizedUser()

        when(mockEnrolmentStoreProxyConnector.queryKnownFacts(any())(any())).thenReturn(
          Future.successful(None)
        )

        val userEnrolment = Json.obj("pptReference" -> unknownPptReference,
                                     "registrationDate" -> "2021-10-09",
                                     "postcode"         -> "AB1 2CD"
        )

        val result: Future[Result] =
          route(app, post.withJsonBody(toJson(userEnrolment))).get

        status(result) must be(BAD_REQUEST)
        contentAsJson(result) mustBe Json.obj("pptReference" -> unknownPptReference,
                                              "failureCode"  -> "VerificationMissing"
        )
      }

      "groups exist with enrolment" in {
        withAuthorizedUser()

        when(mockEnrolmentStoreProxyConnector.queryGroupsWithEnrolment(any())(any())).thenReturn(
          Future.successful(Some(GroupsWithEnrolmentsResponse(Some(Seq("some-group-id")), None)))
        )

        val userEnrolment = Json.obj("pptReference" -> knownPptReference,
                                     "registrationDate" -> "2021-10-09",
                                     "postcode"         -> "AB1 2CD"
        )

        val result: Future[Result] =
          route(app, post.withJsonBody(toJson(userEnrolment))).get

        status(result) must be(BAD_REQUEST)
        contentAsJson(result) mustBe Json.obj("pptReference" -> knownPptReference,
                                              "failureCode"  -> "GroupEnrolled"
        )
      }

      "invalid json" in {
        withAuthorizedUser()
        val userEnrolment = Json.obj("theDate" -> "2021-10-09", "thePostcode" -> "AB1 2CD")

        val result: Future[Result] =
          route(app, post.withJsonBody(toJson(userEnrolment))).get

        status(result) must be(BAD_REQUEST)
        contentAsJson(result) mustBe Json.obj("statusCode" -> 400, "message" -> "Bad Request")
      }
    }

    "return 401" when {
      "unauthorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val userEnrolment = Json.obj("pptReference" -> knownPptReference,
                                     "registrationDate" -> "2021-10-09",
                                     "postcode"         -> "AB1 2CD"
        )

        val result: Future[Result] =
          route(app, post.withJsonBody(toJson(userEnrolment))).get

        status(result) must be(UNAUTHORIZED)
      }
    }
  }
}
