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

package controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, route, status, _}
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import uk.gov.hmrc.http.UpstreamErrorResponse
import base.data.UserEnrolmentData
import base.unit.ControllerSpec
import connectors.TaxEnrolmentsConnector.{AssignEnrolmentToGroupError, AssignEnrolmentToUserError}
import models.enrolment.EnrolmentFailedCode._
import models.enrolmentstoreproxy.GroupsWithEnrolmentsResponse

import scala.concurrent.Future

class UserEnrolmentControllerSpec extends ControllerSpec with UserEnrolmentData {

  private val post = FakeRequest("POST", "/enrolment")

  override def beforeEach(): Unit = {
    super.beforeEach()

    when(mockEnrolmentStoreProxyConnector.queryGroupsWithEnrolment(any())(using any())).thenReturn(
      Future.successful(None)
    )

    when(mockTaxEnrolmentsConnector.assignEnrolmentToGroup(any(), any(), any())(using any())).thenReturn(
      Future.successful(())
    )
  }

  private def groupsWithEnrolmentResponse(groupId: String) =
    GroupsWithEnrolmentsResponse(principalGroupIds = Some(Seq(groupId)), delegatedGroupIds = None)

  "User Enrolment Controller" should {

    "return 201 (Create)" when {
      "no group has enrolment" in {
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

      "group has enrolment and user is in same group" in {
        withAuthorizedUser()

        when(mockEnrolmentStoreProxyConnector.queryGroupsWithEnrolment(any())(using any())).thenReturn(
          Future.successful(Some(groupsWithEnrolmentResponse(userGroupIdentifier)))
        )

        when(mockTaxEnrolmentsConnector.assignEnrolmentToUser(any(), any())(using any())).thenReturn(
          Future.successful(())
        )

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

      "groups exist with enrolment and user not in group" in {
        withAuthorizedUser()

        when(mockEnrolmentStoreProxyConnector.queryGroupsWithEnrolment(any())(using any())).thenReturn(
          Future.successful(Some(groupsWithEnrolmentResponse("some-group-id")))
        )

        val userEnrolment = Json.obj("pptReference" -> knownPptReference,
                                     "registrationDate" -> "2021-10-09",
                                     "postcode"         -> "AB1 2CD"
        )

        val result: Future[Result] =
          route(app, post.withJsonBody(toJson(userEnrolment))).get

        status(result) must be(BAD_REQUEST)
        contentAsJson(result) mustBe Json.obj("pptReference" -> knownPptReference,
                                              "failureCode"  -> GroupEnrolled
        )
      }

      "assign enrolment to group fails" in {
        withAuthorizedUser()

        when(
          mockTaxEnrolmentsConnector.assignEnrolmentToGroup(any(), any(), any())(using any())
        ).thenReturn(Future.failed(UpstreamErrorResponse(AssignEnrolmentToGroupError, 404)))

        val userEnrolment = Json.obj("pptReference" -> knownPptReference,
                                     "registrationDate" -> "2021-10-09",
                                     "postcode"         -> "AB1 2CD"
        )

        val result: Future[Result] =
          route(app, post.withJsonBody(toJson(userEnrolment))).get

        status(result) must be(BAD_REQUEST)
        contentAsJson(result) mustBe Json.obj("pptReference" -> knownPptReference,
                                              "failureCode"  -> GroupEnrolmentFailed
        )
      }

      "assign enrolment to user fails" in {
        withAuthorizedUser()

        when(mockEnrolmentStoreProxyConnector.queryGroupsWithEnrolment(any())(using any())).thenReturn(
          Future.successful(Some(groupsWithEnrolmentResponse(userGroupIdentifier)))
        )

        when(mockTaxEnrolmentsConnector.assignEnrolmentToUser(any(), any())(using any())).thenReturn(
          Future.failed(UpstreamErrorResponse(AssignEnrolmentToUserError, 404))
        )

        val userEnrolment = Json.obj("pptReference" -> knownPptReference,
                                     "registrationDate" -> "2021-10-09",
                                     "postcode"         -> "AB1 2CD"
        )

        val result: Future[Result] =
          route(app, post.withJsonBody(toJson(userEnrolment))).get

        status(result) must be(BAD_REQUEST)
        contentAsJson(result) mustBe Json.obj("pptReference" -> knownPptReference,
                                              "failureCode"  -> UserEnrolmentFailed
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
