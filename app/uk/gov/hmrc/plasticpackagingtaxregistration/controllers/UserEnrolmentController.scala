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

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json.toJson
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.TaxEnrolmentsConnector.{
  AssignEnrolmentToGroupError,
  AssignEnrolmentToUserError
}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.enrolment.EnrolmentFailedCode.EnrolmentFailedCode
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.enrolment.{
  EnrolmentFailedCode,
  UserEnrolmentFailedResponse,
  UserEnrolmentRequest,
  UserEnrolmentSuccessResponse
}
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.{
  EnrolmentStoreProxyConnector,
  TaxEnrolmentsConnector
}
import uk.gov.hmrc.plasticpackagingtaxregistration.controllers.actions.{
  Authenticator,
  AuthorizedRequest
}
import uk.gov.hmrc.plasticpackagingtaxregistration.controllers.response.JSONResponses
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class UserEnrolmentController @Inject() (
  authenticator: Authenticator,
  override val controllerComponents: ControllerComponents,
  enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector,
  taxEnrolmentsConnector: TaxEnrolmentsConnector
)(implicit executionContext: ExecutionContext)
    extends BackendController(controllerComponents) with JSONResponses {

  private val logger = Logger(this.getClass)

  def enrol(): Action[UserEnrolmentRequest] =
    authenticator.authorisedAction(authenticator.parsingJson[UserEnrolmentRequest]) {
      implicit request =>
        val userEnrolmentRequest = request.body
        logPayload("PPT User Enrol request", userEnrolmentRequest)

        def failedResult(code: EnrolmentFailedCode) = {
          logger.warn(s"Enrol failed [$code]")
          BadRequest(UserEnrolmentFailedResponse(userEnrolmentRequest.pptReference, code))
        }

        def successResult() =
          Created(UserEnrolmentSuccessResponse(userEnrolmentRequest.pptReference))

        findEnrolment(userEnrolmentRequest).flatMap {
          case Success(_) =>
            getGroupsWithEnrolment(userEnrolmentRequest.pptReference).flatMap { groupIds =>
              assignEnrolment(userEnrolmentRequest, groupIds).map {
                case Success(_)                   => successResult()
                case Failure(e: EnrolmentFailure) => failedResult(e.failureCode)
                case Failure(_)                   => failedResult(EnrolmentFailedCode.Failed)
              }
            }
          case Failure(e: EnrolmentFailure) => Future.successful(failedResult(e.failureCode))
          case Failure(_)                   => Future.successful(failedResult(EnrolmentFailedCode.Failed))
        }
    }

  case class EnrolmentFailure(failureCode: EnrolmentFailedCode) extends RuntimeException

  private def findEnrolment(
    request: UserEnrolmentRequest
  )(implicit hc: HeaderCarrier): Future[Try[Unit]] =
    enrolmentStoreProxyConnector.queryKnownFacts(request).map {
      case Some(facts) if facts.pptEnrolmentReferences.contains(request.pptReference) =>
        Success(Unit)
      case Some(_) => Failure(EnrolmentFailure(EnrolmentFailedCode.VerificationFailed))
      case _       => Failure(EnrolmentFailure(EnrolmentFailedCode.VerificationMissing))
    }

  private def getGroupsWithEnrolment(
    pptReference: String
  )(implicit request: AuthorizedRequest[_]): Future[Seq[String]] =
    enrolmentStoreProxyConnector.queryGroupsWithEnrolment(pptReference).map {
      case Some(groupsResponse) =>
        groupsResponse.principalGroupIds match {
          case Some(groupIds) => groupIds
          case None           => Seq()
        }
      case None => Seq()
    }

  private def assignEnrolment(
    userEnrolmentRequest: UserEnrolmentRequest,
    groupsWithEnrolment: Seq[String]
  )(implicit request: AuthorizedRequest[_]): Future[Try[Unit]] = {
    val result: Future[Try[Unit]] =
      if (groupsWithEnrolment.isEmpty)
        taxEnrolmentsConnector.assignEnrolmentToGroup(request.userId,
                                                      request.groupId,
                                                      userEnrolmentRequest
        ).map(_ => Success(Unit))
      else if (groupsWithEnrolment.contains(request.groupId))
        taxEnrolmentsConnector.assignEnrolmentToUser(request.userId,
                                                     userEnrolmentRequest.pptReference
        ).map(_ => Success(Unit))
      else
        Future.successful(Failure(EnrolmentFailure(EnrolmentFailedCode.GroupEnrolled)))

    result.recover {
      case UpstreamErrorResponse(AssignEnrolmentToUserError, _, _, _) =>
        Failure(EnrolmentFailure(EnrolmentFailedCode.UserEnrolmentFailed))
      case UpstreamErrorResponse(AssignEnrolmentToGroupError, _, _, _) =>
        Failure(EnrolmentFailure(EnrolmentFailedCode.GroupEnrolmentFailed))
      case e => Failure(EnrolmentFailure(EnrolmentFailedCode.Failed))
    }
  }

  private def logPayload[T](prefix: String, payload: T)(implicit wts: Writes[T]): T = {
    logger.debug(s"$prefix payload: ${toJson(payload)}")
    payload
  }

}
