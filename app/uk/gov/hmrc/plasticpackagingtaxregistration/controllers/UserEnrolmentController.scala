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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.enrolment.EnrolmentFailedCode.EnrolmentFailedCode
import uk.gov.hmrc.plasticpackagingtaxregistration.connectors.models.enrolment.{
  EnrolmentFailedCode,
  UserEnrolmentFailedResponse,
  UserEnrolmentRequest,
  UserEnrolmentSuccessResponse
}
import uk.gov.hmrc.plasticpackagingtaxregistration.controllers.actions.{
  Authenticator,
  AuthorizedRequest
}
import uk.gov.hmrc.plasticpackagingtaxregistration.controllers.response.JSONResponses
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserEnrolmentController @Inject() (
  authenticator: Authenticator,
  override val controllerComponents: ControllerComponents,
  enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector
)(implicit executionContext: ExecutionContext)
    extends BackendController(controllerComponents) with JSONResponses {

  private val logger = Logger(this.getClass)

  def enrol(): Action[UserEnrolmentRequest] =
    authenticator.authorisedAction(authenticator.parsingJson[UserEnrolmentRequest]) {
      implicit request =>
        val userEnrolmentRequest = request.body
        logPayload("PPT User Enrol request", userEnrolmentRequest)

        def failedResult(code: EnrolmentFailedCode) =
          BadRequest(UserEnrolmentFailedResponse(userEnrolmentRequest.pptReference, code))

        def successResult() =
          Created(UserEnrolmentSuccessResponse(userEnrolmentRequest.pptReference))

        getVerifiersErrors(userEnrolmentRequest) flatMap {
          case None =>
            getGroupsWithEnrolmentErrors(userEnrolmentRequest.pptReference).map {
              case None             => successResult()
              case Some(failedCode) => failedResult(failedCode)
            }
          case Some(failedCode) => Future.successful(failedResult(failedCode))
        }

    }

  private def getVerifiersErrors(
    request: UserEnrolmentRequest
  )(implicit hc: HeaderCarrier): Future[Option[EnrolmentFailedCode]] =
    enrolmentStoreProxyConnector.queryKnownFacts(request).map {
      case Some(facts) if facts.pptEnrolmentReferences.contains(request.pptReference) => None
      case Some(_)                                                                    => Some(EnrolmentFailedCode.VerificationFailed)
      case _                                                                          => Some(EnrolmentFailedCode.VerificationMissing)
    }

  private def getGroupsWithEnrolmentErrors(
    pptReference: String
  )(implicit request: AuthorizedRequest[_]): Future[Option[EnrolmentFailedCode]] =
    enrolmentStoreProxyConnector.queryGroupsWithEnrolment(pptReference).map {
      case Some(groups) =>
        // TODO - check if user group is in the list returned
        Some(EnrolmentFailedCode.GroupEnrolled)
      case _ => None
    }

  private def logPayload[T](prefix: String, payload: T)(implicit wts: Writes[T]): T = {
    logger.debug(s"$prefix payload: ${toJson(payload)}")
    payload
  }

}
