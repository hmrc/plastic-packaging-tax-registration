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

import com.codahale.metrics.SharedMetricRegistries
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.BDDMockito.`given`
import org.mockito.Mockito.{reset, verify, verifyNoInteractions}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{CREATED, UNAUTHORIZED, contentAsJson, route, status, _}
import uk.gov.hmrc.auth.core.{AuthConnector, InsufficientEnrolments}
import uk.gov.hmrc.plasticpackagingtaxregistration.base.AuthTestSupport
import uk.gov.hmrc.plasticpackagingtaxregistration.builders.{
  RegistrationBuilder,
  RegistrationRequestBuilder
}
import uk.gov.hmrc.plasticpackagingtaxregistration.models._
import uk.gov.hmrc.plasticpackagingtaxregistration.repositories.RegistrationRepository

import scala.concurrent.Future

class RegistrationControllerSpec
    extends AnyWordSpec with GuiceOneAppPerSuite with AuthTestSupport with BeforeAndAfterEach
    with ScalaFutures with Matchers with RegistrationBuilder with RegistrationRequestBuilder {

  SharedMetricRegistries.clear()

  override lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[AuthConnector].to(mockAuthConnector),
               bind[RegistrationRepository].to(registrationRepository)
    )
    .build()

  private val registrationRepository: RegistrationRepository = mock[RegistrationRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthConnector)
    reset(registrationRepository)
  }

  "POST /" should {
    val post = FakeRequest("POST", "/registrations")

    "return 201" when {
      "request is valid" in {
        withAuthorizedUser()
        val request      = aRegistrationRequest()
        val registration = aRegistration()
        given(registrationRepository.create(any[Registration])).willReturn(
          Future.successful(registration)
        )

        val result: Future[Result] = route(app, post.withJsonBody(toJson(request))).get

        status(result) must be(CREATED)
        contentAsJson(result) mustBe toJson(registration)
        theCreatedRegistration.id mustBe userInternalId
      }
    }

    "return 400" when {
      "invalid json" in {
        withAuthorizedUser()
        val payload                = Json.toJson(Map("incorpJourneyId" -> false)).as[JsObject]
        val result: Future[Result] = route(app, post.withJsonBody(payload)).get

        status(result) must be(BAD_REQUEST)
        contentAsJson(result) mustBe Json.obj("statusCode" -> 400, "message" -> "Bad Request")
        verifyNoInteractions(registrationRepository)
      }
    }

    "return 401" when {
      "unauthorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result: Future[Result] =
          route(app, post.withJsonBody(toJson(aRegistrationRequest()))).get

        status(result) must be(UNAUTHORIZED)
        verifyNoInteractions(registrationRepository)
      }
    }
  }

  "GET /:id" should {
    val get = FakeRequest("GET", "/registrations/" + userInternalId)

    "return 200" when {
      "request is valid" in {
        withAuthorizedUser()
        val registration = aRegistration(withId(userInternalId))
        given(registrationRepository.findByRegistrationId(userInternalId)).willReturn(
          Future.successful(Some(registration))
        )

        val result: Future[Result] = route(app, get).get

        status(result) must be(OK)
        contentAsJson(result) mustBe toJson(registration)
        verify(registrationRepository).findByRegistrationId(userInternalId)
      }
    }

    "return 404" when {
      "id is not found" in {
        withAuthorizedUser()
        given(registrationRepository.findByRegistrationId(userInternalId)).willReturn(
          Future.successful(None)
        )

        val result: Future[Result] = route(app, get).get

        status(result) must be(NOT_FOUND)
        contentAsString(result) mustBe empty
        verify(registrationRepository).findByRegistrationId(userInternalId)
      }
    }

    "return 401" when {
      "unauthorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result: Future[Result] = route(app, get).get

        status(result) must be(UNAUTHORIZED)
        verifyNoInteractions(registrationRepository)
      }
    }
  }

  "PUT /:id" should {
    val put = FakeRequest("PUT", "/registrations/id")

    "return 200" when {
      "request is valid" in {
        withAuthorizedUser()
        val primaryContactDetailsRequest = withPrimaryContactDetailsRequest(
          PrimaryContactDetails(name = Some("FirstName LastName"),
                                jobTitle = Some("CEO"),
                                email = Some("test@test.com"),
                                phoneNumber = Some("1234567890"),
                                address = Some(
                                  Address(addressLine1 = "addressLine1",
                                          townOrCity = "Town",
                                          postCode = "PostCode"
                                  )
                                )
          )
        )
        val request = aRegistrationRequest(primaryContactDetailsRequest,
                                           withOrganisationDetailsRequest(
                                             OrganisationDetails(organisationType =
                                                                   Some(OrgType.UK_COMPANY),
                                                                 businessRegisteredAddress =
                                                                   Some(
                                                                     Address(addressLine1 =
                                                                               "addressLine1",
                                                                             townOrCity = "Town",
                                                                             postCode = "PostCode"
                                                                     )
                                                                   )
                                             )
                                           ),
                                           withUserHeaders(
                                             Map("testHeaderKey" -> "testHeaderValue")
                                           )
        )

        val primaryContactDetails = withPrimaryContactDetails(
          PrimaryContactDetails(name = Some("FirstName LastName"),
                                jobTitle = Some("CEO"),
                                email = Some("test@test.com"),
                                phoneNumber = Some("1234567890"),
                                address = Some(
                                  Address(addressLine1 = "addressLine1",
                                          townOrCity = "Town",
                                          postCode = "PostCode"
                                  )
                                )
          )
        )
        val registration =
          aRegistration(withIncorpJourneyId("f368e653-790a-4a95-af62-4132f0ffd433"),
                        primaryContactDetails
          )
        given(registrationRepository.findByRegistrationId(anyString())).willReturn(
          Future.successful(Some(registration))
        )
        given(registrationRepository.update(any[Registration])).willReturn(
          Future.successful(Some(registration))
        )

        val result: Future[Result] = route(app, put.withJsonBody(toJson(request))).get

        status(result) must be(OK)
        contentAsJson(result) mustBe toJson(registration)
        val updatedRegistration = theUpdatedRegistration
        updatedRegistration.id mustBe userInternalId
        updatedRegistration.incorpJourneyId mustBe Some("f368e653-790a-4a95-af62-4132f0ffd433")
        updatedRegistration.primaryContactDetails mustBe request.primaryContactDetails
        updatedRegistration.organisationDetails mustBe request.organisationDetails
      }
    }

    "return 404" when {
      "registration is not found - on find" in {
        withAuthorizedUser()
        val request = aRegistrationRequest()
        given(registrationRepository.findByRegistrationId(anyString())).willReturn(
          Future.successful(None)
        )
        given(registrationRepository.update(any[Registration])).willReturn(Future.successful(None))

        val result: Future[Result] = route(app, put.withJsonBody(toJson(request))).get

        status(result) must be(NOT_FOUND)
        contentAsString(result) mustBe empty
      }

      "registration is not found - on update" in {
        withAuthorizedUser()
        val request      = aRegistrationRequest()
        val registration = aRegistration(withId("id"))
        given(registrationRepository.findByRegistrationId(anyString())).willReturn(
          Future.successful(Some(registration))
        )
        given(registrationRepository.update(any[Registration])).willReturn(Future.successful(None))

        val result: Future[Result] = route(app, put.withJsonBody(toJson(request))).get

        status(result) must be(NOT_FOUND)
        contentAsString(result) mustBe empty
      }
    }

    "return 401" when {
      "unauthorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result: Future[Result] =
          route(app, put.withJsonBody(toJson(aRegistrationRequest()))).get

        status(result) must be(UNAUTHORIZED)
        verifyNoInteractions(registrationRepository)
      }
    }
  }

  def theCreatedRegistration: Registration = {
    val captor: ArgumentCaptor[Registration] = ArgumentCaptor.forClass(classOf[Registration])
    verify(registrationRepository).create(captor.capture())
    captor.getValue
  }

  def theUpdatedRegistration: Registration = {
    val captor: ArgumentCaptor[Registration] = ArgumentCaptor.forClass(classOf[Registration])
    verify(registrationRepository).update(captor.capture())
    captor.getValue
  }

}
