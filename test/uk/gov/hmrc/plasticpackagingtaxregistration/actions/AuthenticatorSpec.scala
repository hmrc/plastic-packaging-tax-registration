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

package uk.gov.hmrc.plasticpackagingtaxregistration.actions

import org.scalatest.EitherValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{INTERNAL_SERVER_ERROR, UNAUTHORIZED}
import play.api.test.Helpers.await
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import uk.gov.hmrc.auth.core.InsufficientConfidenceLevel
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtaxregistration.base.AuthTestSupport
import uk.gov.hmrc.plasticpackagingtaxregistration.controllers.actions.Authenticator
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.ExecutionContext

class AuthenticatorSpec
    extends AnyWordSpec with Matchers with MockitoSugar with AuthTestSupport
    with DefaultAwaitTimeout with EitherValues {

  private val mcc           = stubMessagesControllerComponents()
  private val hc            = HeaderCarrier()
  private val request       = FakeRequest()
  private val authenticator = new Authenticator(mockAuthConnector, mcc)(ExecutionContext.global)

  "Authenticator" should {
    "return 401 unauthorised " when {
      "if user is not authorised" in {
        withUnauthorizedUser(InsufficientConfidenceLevel("User not authorised"))

        val result = await(authenticator.authorisedWithInternalId(hc, request))

        result.left.value.statusCode mustBe UNAUTHORIZED
      }
    }

    "return 500 " when {
      "when returning the InternalId results in an exception" in {
        withUnauthorizedUser(new Exception("Something went wrong"))

        val result = await(authenticator.authorisedWithInternalId(hc, request))

        result.left.value.statusCode mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return 200" when {
      "internalId is available" in {
        withAuthorizedUser(newUser())

        val result = await(authenticator.authorisedWithInternalId(hc, request))

        result.value.registrationId mustBe "Int-ba17b467-90f3-42b6-9570-73be7b78eb2b"
      }
    }
  }

}
