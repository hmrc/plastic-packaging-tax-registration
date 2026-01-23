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

package actions

import org.mockito.Mockito.reset
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{INTERNAL_SERVER_ERROR, UNAUTHORIZED}
import play.api.test.Helpers.await
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import uk.gov.hmrc.auth.core.InsufficientConfidenceLevel
import uk.gov.hmrc.http.HeaderCarrier
import base.AuthTestSupport
import controllers.actions.Authenticator
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.ExecutionContext

class AuthenticatorSpec
    extends AnyWordSpec with Matchers with MockitoSugar with AuthTestSupport with BeforeAndAfterEach
    with DefaultAwaitTimeout with EitherValues {

  private val mcc           = stubMessagesControllerComponents()
  private val hc            = HeaderCarrier()
  private val request       = FakeRequest()
  private val authenticator = new Authenticator(mockAuthConnector, mcc)(ExecutionContext.global)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthConnector)
  }

  "Authenticator" should {
    "return 401 unauthorised " when {
      "if user is not authorised" in {
        withUnauthorizedUser(InsufficientConfidenceLevel("User not authorised"))

        val result = await(authenticator.authorisedWithInternalIdAndGroupIdentifier()(hc, request))

        result.left.value.statusCode mustBe UNAUTHORIZED
      }
    }

    "return 500 " when {
      "when returning the InternalId results in an exception" in {
        withUnauthorizedUser(new Exception("Something went wrong"))

        val result = await(authenticator.authorisedWithInternalIdAndGroupIdentifier()(hc, request))

        result.left.value.statusCode mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return 401 unauthorised " when {
      "user group not available" in {
        withAuthorizedUser(newUser(), userGroup = None)

        val result = await(authenticator.authorisedWithInternalIdAndGroupIdentifier()(hc, request))

        result.left.value.statusCode mustBe UNAUTHORIZED
      }
      "user credentials not available" in {
        withAuthorizedUser(newUser(), userCredentials = None)

        val result = await(authenticator.authorisedWithInternalIdAndGroupIdentifier()(hc, request))

        result.left.value.statusCode mustBe UNAUTHORIZED
      }
    }

    "return 200" when {
      "internalId, credentials and group identifier is available" in {
        withAuthorizedUser(newUser())

        val result = await(authenticator.authorisedWithInternalIdAndGroupIdentifier()(hc, request))

        result.value.registrationId mustBe userInternalId
        result.value.userId mustBe userCredentialsId
        result.value.groupId mustBe userGroupIdentifier
      }

      "has requested ppt enrolment, internalId, credentials and group identifier is available" in {
        withAuthorizedUser(newEnrolledUser(), userPptReference = Some(userEnrolledPptReference))

        val result = await(
          authenticator.authorisedWithInternalIdAndGroupIdentifier(pptReference =
            Some(userEnrolledPptReference)
          )(hc, request)
        )

        result.value.registrationId mustBe userInternalId
        result.value.userId mustBe userCredentialsId
        result.value.groupId mustBe userGroupIdentifier
      }

    }

  }

}
