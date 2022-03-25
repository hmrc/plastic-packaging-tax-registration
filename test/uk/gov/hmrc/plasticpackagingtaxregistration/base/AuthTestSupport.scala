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

package uk.gov.hmrc.plasticpackagingtaxregistration.base

import org.mockito.{ArgumentMatcher, ArgumentMatchers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.scalatestplus.mockito.MockitoSugar
import play.api.Logger
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{internalId, _}
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name, Retrieval, _}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.KeyValue
import uk.gov.hmrc.plasticpackagingtaxregistration.models.SignedInUser
import uk.gov.hmrc.plasticpackagingtaxregistration.services.nrs.NonRepudiationService.NonRepudiationIdentityRetrievals

import scala.concurrent.{ExecutionContext, Future}

trait AuthTestSupport extends MockitoSugar {

  lazy val mockAuthConnector: AuthConnector = mock[AuthConnector]
  lazy val mockLogger: Logger               = mock[Logger]

  val userInternalId           = "Int-ba17b467-90f3-42b6-9570-73be7b78eb2b"
  val userGroupIdentifier      = "testGroupId-419b91bc-8f97-4b5e-85ef-d58d4cfd4bb8"
  val userCredentialsId        = "12342370495723"
  val userEnrolledPptReference = "XMPPT00000000001"

  def withAuthorizedUser(
    user: SignedInUser = newUser(),
    userGroup: Option[String] = Some(userGroupIdentifier),
    userCredentials: Option[Credentials] = Some(
      Credentials(userCredentialsId, "GovernmentGateway")
    ),
    userPptReference: Option[String] = None
  ): Unit = {

    def enrolmentWithDelegatedAuth(pptReference: String) =
      Enrolment(KeyValue.pptServiceName).withIdentifier(KeyValue.etmpPptReferenceKey,
                                                        pptReference
      ).withDelegatedAuthRule("ppt-auth")

    def pptEnrollmentMatcherFor(pptReference: String): ArgumentMatcher[Predicate] = {
      (p: Predicate) =>
        p == enrolmentWithDelegatedAuth(pptReference) && user.enrolments.getEnrolment(
          KeyValue.pptServiceName
        ).isDefined
    }

    val expectedAuthPredicateMatcher = userPptReference.map { pptReference =>
      ArgumentMatchers.argThat(pptEnrollmentMatcherFor(pptReference))
    }.getOrElse {
      ArgumentMatchers.eq(EmptyPredicate)
    }

    println(expectedAuthPredicateMatcher)

    when(
      mockAuthConnector.authorise(
        expectedAuthPredicateMatcher,
        ArgumentMatchers.eq(internalId and credentials and groupIdentifier)
      )(any(), any())
    )
      .thenReturn(Future.successful(new ~(new ~(user.internalId, userCredentials), userGroup)))
  }

  def withUnauthorizedUser(error: Throwable): Unit =
    when(mockAuthConnector.authorise(any(), any())(any(), any())).thenReturn(Future.failed(error))

  def newUser(enrolments: Option[Enrolments] = None): SignedInUser =
    SignedInUser(Credentials("123123123", "Plastic Limited"),
                 Name(Some("Aldo"), Some("Rain")),
                 Some("amina@hmrc.co.uk"),
                 "123",
                 Some("Int-ba17b467-90f3-42b6-9570-73be7b78eb2b"),
                 Some(AffinityGroup.Organisation),
                 enrolments.getOrElse(Enrolments(Set()))
    )

  def newEnrolledUser(): SignedInUser = {
    def pptEnrolment(pptEnrolmentId: String) =
      newEnrolments(
        newEnrolment(KeyValue.pptServiceName, KeyValue.etmpPptReferenceKey, pptEnrolmentId)
      )

    val user = newUser(enrolments = Some(pptEnrolment(userEnrolledPptReference)))
    println("!!!!!!!!E " + user)
    user
  }

  def newEnrolments(enrolment: Enrolment*): Enrolments =
    Enrolments(enrolment.toSet)

  def newEnrolment(key: String, identifierName: String, identifierValue: String): Enrolment =
    Enrolment(key).withIdentifier(identifierName, identifierValue)

  def mockAuthorization(
    nrsIdentityRetrievals: Retrieval[NonRepudiationIdentityRetrievals],
    authRetrievalsResponse: NonRepudiationIdentityRetrievals
  )(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): OngoingStubbing[Future[NonRepudiationIdentityRetrievals]] =
    when(
      mockAuthConnector.authorise(ArgumentMatchers.eq(EmptyPredicate),
                                  ArgumentMatchers.eq(nrsIdentityRetrievals)
      )(ArgumentMatchers.eq(hc), ArgumentMatchers.eq(ec))
    ).thenReturn(Future.successful(authRetrievalsResponse))

}
